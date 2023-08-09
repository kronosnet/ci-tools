// Run a generic CI stage with the right params and record success/failure
def call(Map info, String agentName, String stageName, Boolean voting, Map extravars)
{
    println("runStage")

    // Timeout (minutes) for the collection stages
    def collect_timeout = 10

    // Are we voting or non-voting?
    // used to record stats in info[]
    def stageType = ''
    if (voting) {
	stageType = 'voting'
    } else {
	stageType = 'nonvoting'
    }

    // We need these later
    println("extravars "+extravars)
    def extras = extravars
    def locals = ['voting': voting,
		  'stageName': stageName,
		  'stageType': stageType]

    // Run stuff!
    node("${agentName}") {
	echo "Building ${stageName} for ${info['project']} on ${agentName} (${stageType})"

	info["${stageType}_run"]++
	stage("${stageName} on ${agentName} - checkout") {
	    getSCM(info)
	}

	// Get any job-specific configuration variables
	extras += getProjectProperties(info, agentName, info['target_branch'])
	def build_timeout = getBuildTimeout()

	// Save the local workspace directory etc for postStage()
	info['workspace'] = env.WORKSPACE + '/' + info['project']
	info['EXTRAVER'] = extras['EXTRAVER']

	stage("${stageName} on ${agentName} - build") {

	    // Run everything in the checked-out directory
	    dir (info['project']) {
		def exports = getShellVariables(info, extras, stageName)
		def res = ''

		// Keep the log in a separate file so they are easy to find
		tee ("${stageName}-${agentName}.log") {
		    res = cmdWithTimeout(build_timeout,
					 "${exports} ~/ci-tools/ci-build",
					 info, locals,
					 { processRunSuccess(info, locals) },
					 { processRunException(info, locals) })
		}
	    }
	}
    }

    println('LOCALS: '+locals)
    // Run dependant global scripts for some jobs ... if we suceeded
    if (locals['failed']) {
	println('this job failed, collection not happening')
	return
    }

    // Gather covscan results
    // 'fullrebuild' is set by a parent job to prevent uploads from weekly jobs
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
}

def processRunSuccess(Map info, Map locals)
{
    // Rename the log so we know it all went fine
    def log_name = "${locals['stageName']}-${env.NODE_NAME}.log"
    sh "mv ${log_name} SUCCESS_${log_name}"
    archiveArtifacts artifacts: "SUCCESS_${log_name}", fingerprint: false

    locals['failed'] = false
}


// Called if the stage called from runStage() fails for any reason
def processRunException(Map info, Map locals)
{
    println("processRunException "+locals)

    // We can't have hyphens as keys in info[:] as they get exported
    // as environment variable names.
    def stage = locals['stageName'].replace('-','_')

    // Stats, and save the job name for the email
    info["${locals['stageType']}_fail"]++
    info["${locals['stageType']}_fail_nodes"] += env.NODE_NAME + "(${locals['stageName']})" + ' '
    info["${stage}_failed"] = 1 // One of these failed. that's all we need to know

    // This happens after the above in case the log doesn't exist
    // and causes an exception
    def log_name = "${locals['stageName']}-${env.NODE_NAME}.log"
    sh "mv ${log_name} FAILED_${log_name}"
    archiveArtifacts artifacts: "FAILED_${log_name}", fingerprint: false

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
