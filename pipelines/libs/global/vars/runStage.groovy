// Run a generic CI stage with the right params and record success/failure

def call(Map info, String agentName, String stageName, Boolean voting, Map extravars)
{
    println('doRunStage info='+info+', extravars='+extravars)

    // Put everything in 'localinfo' for consistency & clarity.
    // This also makes sure that anything in extravars[:] overrides
    // its equivalent in info[:] .
    // 'info' itself still needs to be written to for stats though, so should
    // only appear on the LHS of expressions in here.
    def localinfo = info + extravars

    // Are we voting or non-voting?
    // - used to record stats in info[]
    if (voting) {
	localinfo['stageType'] = 'voting'
    } else {
	localinfo['stageType'] = 'nonvoting'
    }
    localinfo['stageName'] = stageName
    localinfo['voting'] = voting

    try {
	doRunStage(agentName, info, localinfo)
    } catch (err) {
	// Catch BAD things (not script failures, but stuff like nodes failing or source download died)
	// these are logged into info[:] in the exception handlers
	println("runStage() caught "+err)
	info['exception_text'] += "Exception caught on ${agentName} during ${stageName}: ${err}\n"
    }
}


def doRunStage(String agentName, Map info, Map localinfo)
{
    // Timeout (minutes) for the collection stages
    def collect_timeout = 10

    // This map is used for communicating state around the exception handlers
    // and runWithTimeout
    def stagestate = [:]

    // The title can be overridden
    def stageTitle = localinfo['stageName']
    if (localinfo.containsKey('title')) {
	stageTitle = localinfo['title']
    }

    // Run stuff!
    node("${agentName}") {
	echo "Building ${stageTitle} for ${localinfo['project']} on ${agentName} (${localinfo['stageType']})"
	cleanWs(disableDeferredWipeout: true, deleteDirs: true)

	// Save the local workspace directory for later
	def workspace = env.WORKSPACE + '/' + localinfo['project']

	info["${localinfo['stageType']}_run"]++
	stage("${stageTitle} on ${agentName} - checkout") {
	    stagestate['runstage'] = 'checkout'
	    def rc = runWithTimeout(collect_timeout, { getSCM(info) }, stagestate,
				    { processRunSuccess(info, localinfo, stagestate) },
				    { processRunException(info, localinfo, stagestate) })
	    if (rc != 'OK') {
		println("RC runWithTimeout returned "+rc)
		// Big stick here, we can't continue
		sh "exit 1"
	    }
	}

	// Add node-specific properties
	localinfo += getNodeProperties(agentName)

	// Get any job-specific configuration variables
	localinfo += getProjectProperties(localinfo, agentName)

	// Converting ci-tools/ci-set-env to groovy maps
	localinfo += ci_set_env(localinfo, localinfo['stageName'], agentName)

	def build_timeout = getBuildTimeout()

	stage("${stageTitle} on ${agentName} - build") {
	    stagestate['logfile'] = "${localinfo['stageName']}-${agentName}.log"
	    stagestate['runstage'] = 'run'

	    // Keep the log in a separate file so they are easy to find
	    tee (stagestate['logfile']) {
		// Run everything in the checked-out directory
		dir (localinfo['project']) {
		    def exports = getShellVariables(localinfo)
		    cmdWithTimeout(build_timeout,
				   "${exports} $HOME/ci-tools/ci-wrap ci-build",
				   stagestate,
				   { processRunSuccess(info, localinfo, stagestate) },
				   { processRunException(info, localinfo, stagestate) })
		}
	    }
	    // This marks it red in the graph view
	    if (stagestate['failed']) {
		catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
		    shNoTrace("exit 1", "Marking this stage as a failure")
		}
	    }
	}

	println('STAGESTATE: '+stagestate)
	// Run dependant global scripts for some jobs ... if we suceeded
	if (stagestate['failed']) {
	    println('this job failed, collection not happening')
	    return false
	}

	// Keep a list of EXTRAVERs, it's more efficient (and less racy)
	// to de-duplicate these at the end, in postStage()
	info['EXTRAVER_LIST'] += localinfo['extraver']

	// Gather covscan results
	// 'fullrebuild' is param set by a parent job to prevent uploads from weekly jobs
	//    and will normally be 0
	if (localinfo['stageName'].endsWith('covscan') && localinfo['fullrebuild'] == 0) {
	    stage("${localinfo['stageName']} on ${agentName} - get covscan artifacts") {
		// Yes - you can nest 'node's
		node('built-in') {
		    if (localinfo['isPullRequest']) {
			info['covtgtdir'] = "pr${info['pull_id']}"
		    } else {
			info['covtgtdir'] = "origin/${info['target']}"
		    }
		    def covdir = "coverity/${info['project']}/${agentName}/${info['covtgtdir']}/${localinfo['extraver']}/${env.BUILD_NUMBER}"
		    cmdWithTimeout(collect_timeout,
				   "$HOME/ci-tools/ci-wrap ci-get-artifacts ${agentName} ${workspace} ${covdir} cov",
				   stagestate, {}, { postFnError(stagestate) })
		    info['cov_results_urls'] += covdir

		    // Make a note of any new errors
		    if (stagestate['new_cov_errors']) {
			info['new_cov_results_urls'] += covdir + '/new'
		    }
		}
	    }
	}

	// RPM builds
	if (localinfo['stageName'].endsWith('buildrpms') && localinfo['publishrpm'] == 1 && localinfo['fullrebuild'] == 0) {
	    stage("${localinfo['stageName']} on ${agentName} - get RPM artifacts") {
		node('built-in') {
		    if (localinfo['isPullRequest']) {
			// TODO: fix pr path and adjust for 'extraver'
			cmdWithTimeout(collect_timeout,
				       "$HOME/ci-tools/ci-wrap ci-get-artifacts ${agentName} ${workspace} builds/${info['project']}/pr/${info['pull_id']}/${agentName} rpm",
				       stagestate, {}, { postFnError(stagestate) })
			info['repo_urls'] += "https://ci.kronosnet.org/" + "builds/${info['project']}/pr/${info['pull_id']}/${agentName}"
		    } else {
			cmdWithTimeout(collect_timeout,
				       "$HOME/ci-tools/ci-wrap ci-get-artifacts ${agentName} ${workspace} builds/${info['project']}/${agentName}/origin/${info['target']}/${localinfo['extraver']}/${env.BUILD_NUMBER}/ rpm",
				       stagestate, {}, { postFnError(stagestate) })
			info['repo_urls'] += "https://ci.kronosnet.org/" + "builds/${info['project']}/${agentName}/origin/${info['target']}/${localinfo['extraver']}/${env.BUILD_NUMBER}/"

		    }
		}
	    }
	}

	cleanWs(disableDeferredWipeout: true, deleteDirs: true)
    }
    return true
}

def processRunSuccess(Map info, Map localinfo, Map stagestate)
{
    // Rename the log so we know it all went fine
    // The log file might not be there, if getSCM failed.
    if (stagestate.containsKey('logfile')) {
	dir('..') {
	    sh "mv ${stagestate['logfile']} SUCCESS_${stagestate['logfile']}"
	    archiveArtifacts artifacts: "SUCCESS_${stagestate['logfile']}", fingerprint: false
	    info['have_split_logs'] = true
	}
    }

    stagestate['failed'] = false
}


// Called if the stage called from runStage() fails for any reason
def processRunException(Map info, Map localinfo, Map stagestate)
{
    println("processRunException "+stagestate)

    // We can't have hyphens as keys in info[:] as they get exported
    // as environment variable names.
    def stage = localinfo['stageName'].replace('-','_')

    def runtype = ''
    if (stagestate['runstage'] == 'checkout') {
	runtype = ' source download'
    }
    // Stats, and save the job name for the email
    info["${localinfo['stageType']}_fail"]++
    info["${localinfo['stageType']}_fail_nodes"] += env.NODE_NAME + "(${localinfo['stageName']}${runtype})" + ' '
    info["${stage}_failed"] = 1 // One of these failed. that's all we need to know

    // The log file might not be there, if getSCM failed.
    if (stagestate.containsKey('logfile')) {
	dir ('..') {
	    sh "mv ${stagestate['logfile']} FAILED_${stagestate['logfile']}"
	    archiveArtifacts artifacts: "FAILED_${stagestate['logfile']}", fingerprint: false
	    info['have_split_logs'] = true
	}
    }

    // If the jobs was aborted, then GO AWAY!
    if (stagestate['RET'] == 'ABORT') {
	currentBuild.result = 'ABORTED'
    }

    // Tell runStage()
    stagestate['new_cov_errors'] = false
    if (localinfo['stageName'].endsWith('covscan') && fileExists('cov.json')) {
	stagestate['failed'] = false // Coverity failed but we want the results

	if (fileExists('cov.html/new/index.html')) { // New errors
	    stagestate['new_cov_errors'] = true
	}
    } else {
	stagestate['failed'] = true
    }
}

// Called if the collection stage failed for any reason
def postFnError(Map stagestate)
{
    if (stagestate['RET'] == 'ABORT') {
	currentBuild.result = 'ABORTED'
    } else {
	throw (stagestate['EXP'])
    }
}
