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

	// Save the local workspace directory for later
	def workspace = env.WORKSPACE + '/' + info['project']

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

	// Add node-specific properties
	extras += getNodeProperties(agentName)

	// Get any job-specific configuration variables
	extras += getProjectProperties(info, extras, agentName, info['target_branch'])

	// EXTRAVERS is needed for building the repos in postStage, and is overridden
	// (where needed) in getProjectProperties()
	if (extras.containsKey('EXTRAVER')) {
	    info['EXTRAVER'] = extras['EXTRAVER']
	}

	def build_timeout = getBuildTimeout()

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
	// 'fullrebuild' is param set by a parent job to prevent uploads from weekly jobs
	//    and will normally be the String '0'
	if (stageName.endsWith('covscan') && info['fullrebuild'] == '0') {
	    stage("${stageName} on ${agentName} - get covscan artifacts") {
		// Yes - you can nest 'node's
		node('built-in') {
		    if (info['isPullRequest']) {
			info['covtgtdir'] = "pr${info['pull_id']}"
		    } else {
			info['covtgtdir'] = "${info['actual_commit']}"
		    }
		    def covdir = "coverity/${info['project']}/${agentName}/${info['covtgtdir']}/${env.BUILD_NUMBER}/"
		    cmdWithTimeout(collect_timeout,
				   "~/ci-tools/ci-get-artifacts ${agentName} ${workspace} ${covdir} cov",
				   info, locals, {}, { postFnError(info, locals) })
		    info['cov_results_urls'] += covdir
		}
	    }
	}

	// Allow overrides for stages
	def publishrpm = info['publishrpm']
	if (extras.containsKey('publishrpm')) {
	    publishrpm = extras['publishrpm']
	}

	// RPM builds
	if (stageName.endsWith('buildrpms') && publishrpm == 1 && info['fullrebuild'] == '0') {
	    stage("${stageName} on ${agentName} - get RPM artifacts") {
		node('built-in') {
		    if (info['isPullRequest']) {
			cmdWithTimeout(collect_timeout,
				       "~/ci-tools/ci-get-artifacts ${agentName} ${workspace} builds/${info['project']}/pr/${info['pull_id']}/${agentName} rpm",
				       info, locals, {}, { postFnError(info, locals) })
		    } else {
			cmdWithTimeout(collect_timeout,
				       "~/ci-tools/ci-get-artifacts ${agentName} ${workspace} builds/${info['project']}/${agentName}/${info['actual_commit']}/${env.BUILD_NUMBER}/ rpm",
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
    if (locals['stageName'].endsWith('covscan') && fileExists('cov.json')) {
	locals['failed'] = false // Coverity failed but we want the results
    } else {
	locals['failed'] = true
    }
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
