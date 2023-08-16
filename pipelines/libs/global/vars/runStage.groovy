// Run a generic CI stage with the right params and record success/failure

def call(Map info, String agentName, String stageName, Boolean voting, Map extravars)
{
    // Are we voting or non-voting?
    // used to record stats in info[]
    def stageType = ''
    if (voting) {
	stageType = 'voting'
    } else {
	stageType = 'nonvoting'
    }

    try {
	doRunStage(info, agentName, stageName, voting, stageType, extravars)
    } catch (err) {
	// Catch BAD things (not script failures, but stuff like nodes failing or source download died)
	// these are logged into info[:] in the exception handlers
	println("runStage() caught "+err)
	info['exception_text'] += "Exception caught on ${agentName} during ${stageName}: ${err}\n"
    }
}


def doRunStage(Map info, String agentName, String stageName, Boolean voting, String stageType, Map extravars)
{
    // Timeout (minutes) for the collection stages
    def collect_timeout = 10

    // We need these later
    println("extravars "+extravars)
    def extras = extravars
    def locals = ['voting': voting,
		  'stageName': stageName,
		  'stageType': stageType]

    def stageTitle = ''
    if (extravars.containsKey('title')) {
	stageTitle = extravars['title']
    } else {
	stageTitle = stageName
    }

    // Run stuff!
    node("${agentName}") {
	echo "Building ${stageTitle} for ${info['project']} on ${agentName} (${stageType})"
	cleanWs(disableDeferredWipeout: true, deleteDirs: true)

	info["${stageType}_run"]++
	stage("${stageTitle} on ${agentName} - checkout") {
	    locals['runstage'] = 'checkout'
	    def rc = runWithTimeout(collect_timeout, { getSCM(info) }, info, locals,
				    { processRunSuccess(info, locals) },
				    { processRunException(info, locals) })
	    if (rc != 'OK') {
		println("RC runWithTimeout returned "+rc)
		// Big stick here, we can't continue
		sh "exit 1"
	    }
	}

	// Get any job-specific configuration variables
	extras += getProjectProperties(info, extras, agentName, info['target_branch'])
	def build_timeout = getBuildTimeout()

	// Save the local workspace directory etc for postStage()
	info['workspace'] = env.WORKSPACE + '/' + info['project']
	info['EXTRAVER'] = extras['EXTRAVER']

	stage("${stageTitle} on ${agentName} - build") {
	    locals['logfile'] = "${stageName}-${agentName}.log"
	    locals['runstage'] = 'run'

	    // Keep the log in a separate file so they are easy to find
	    tee (locals['logfile']) {
		// Run everything in the checked-out directory
		dir (info['project']) {
		    def exports = getShellVariables(info, extras, stageName)
		    cmdWithTimeout(build_timeout,
				   "${exports} ~/ci-tools/ci-build",
				   info, locals,
				   { processRunSuccess(info, locals) },
				   { processRunException(info, locals) })
		}
	    }
	}

	println('LOCALS: '+locals)
	// Run dependant global scripts for some jobs ... if we suceeded
	if (locals['failed']) {
	    println('this job failed, collection not happening')
	    return false
	}

	// Gather covscan results
	// 'fullrebuild' is set by a parent job to prevent uploads from weekly jobs.
	// Yes - you can nest 'node's
	if (stageName == 'covscan' && info['fullrebuild'] != '1') { // Covers the case where it might be null too
	    stage("${stageName} on ${agentName} - get covscan artifacts") {
		node('built-in') {
		    cmdWithTimeout(collect_timeout,
				   "~/ci-tools/ci-get-artifacts ${agentName} ${info['workspace']} cov/${info['project']}/${agentName}/${env.BUILD_NUMBER}/ cov",
				   info, locals, {}, { postFnError(info, locals) })
		}
	    }
	}

	// RPM builds
	if (stageName == 'buildrpms' && info['publishrpm'] == 1 && info['fullrebuild'] != '1') { // Covers the case where it might be null too
	    stage("${stageName} on ${agentName} - get RPM artifacts") {
		node('built-in') {
		    if (info['isPullRequest']) {
			cmdWithTimeout(collect_timeout,
				       "~/ci-tools/ci-get-artifacts ${agentName} ${info['workspace']} rpmrepos/${info['project']}/pr/${info['pull_id']}/${agentName} rpm",
				       info, locals, {}, { postFnError(info, locals) })
		    } else {
			cmdWithTimeout(collect_timeout,
				       "~/ci-tools/ci-get-artifacts ${agentName} ${info['workspace']} rpmrepos/${info['project']}/${agentName}/${info['actual_commit']}/${env.BUILD_NUMBER}/ rpm",
				       info, locals, {}, { postFnError(info, locals) })
		    }
		}
	    }
	}
	cleanWs(disableDeferredWipeout: true, deleteDirs: true)
    }
    return true
}

def processRunSuccess(Map info, Map locals)
{
    // Rename the log so we know it all went fine
    // The log file might not be there, if getSCM failed.
    if (locals.containsKey('logfile')) {
	dir('..') {
	    sh "mv ${locals['logfile']} SUCCESS_${locals['logfile']}"
	    archiveArtifacts artifacts: "SUCCESS_${locals['logfile']}", fingerprint: false
	}
    }

    locals['failed'] = false
}


// Called if the stage called from runStage() fails for any reason
def processRunException(Map info, Map locals)
{
    println("processRunException "+locals)

    // We can't have hyphens as keys in info[:] as they get exported
    // as environment variable names.
    def stage = locals['stageName'].replace('-','_')

    def runtype = ''
    if (locals['runstage'] == 'checkout') {
	runtype = ' source download'
    }
    // Stats, and save the job name for the email
    info["${locals['stageType']}_fail"]++
    info["${locals['stageType']}_fail_nodes"] += env.NODE_NAME + "(${locals['stageName']}${runtype})" + ' '
    info["${stage}_failed"] = 1 // One of these failed. that's all we need to know

    // The log file might not be there, if getSCM failed.
    if (locals.containsKey('logfile')) {
	dir ('..') {
	    sh "mv ${locals['logfile']} FAILED_${locals['logfile']}"
	    archiveArtifacts artifacts: "FAILED_${locals['logfile']}", fingerprint: false
	}
    }

    // If the jobs was aborted, then GO AWAY!
    if (locals['RET'] == 'ABORT') {
	currentBuild.result = 'ABORTED'
    }

    // Tell runStage()
    locals['failed'] = true
}

// Called if the collection stage failed for any reason
def postFnError(Map info, Map locals)
{
    if (locals['RET'] == 'ABORT') {
	currentBuild.result = 'ABORTED'
    } else {
	throw (locals['EXP'])
    }
}
