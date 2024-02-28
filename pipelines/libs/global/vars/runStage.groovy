// Run a generic CI stage with the right params and record success/failure
// This is here most of the main gubbins happens
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
    def collect_timeout = 30

    // This map is used for communicating state around the exception handlers
    // and runWithTimeout
    def stagestate = [:]

    // The title can be overridden
    def stageTitle = localinfo['stageName']
    if (localinfo.containsKey('title')) {
	stageTitle = localinfo['title']
    }

    // Things to run - in order
    // groovy uses a LinkedHashMap so they retain insertion order
    def stages=[:]
    stages['Get OS Info'] = 'ci_os_info'

    // Old shell based (will disappear)
    def shell_stages = [:]
    shell_stages['Setup RPMs'] = 'ci-setup-rpm'
    shell_stages['Setup source'] = 'ci-setup-src'
    shell_stages['Build source'] = 'ci-build-src'
    shell_stages['Run tests'] = 'ci-tests-src'
    shell_stages['Install'] = 'ci-install-src'

    // Run stuff! On this node!
    node("${agentName}") {
	echo "Building ${stageTitle} for ${localinfo['project']} on ${agentName} (${localinfo['stageType']})"

	cleanWs(disableDeferredWipeout: true, deleteDirs: true)

	// Save the local workspace directory for later
	def workspace = env.WORKSPACE + '/' + localinfo['project']

	// Update run stats
	info["${localinfo['stageType']}_run"]++

	// This is the log file we will add to the artifacts
	stagestate['logfile'] = "${localinfo['stageName']}-${agentName}.log"

	// Keep the logs in separate files per node/function so they are easy to find
	stage("Build for ${stageTitle} on ${agentName}") {
	    tee (stagestate['logfile']) {
		stagestate['runstage'] = 'checkout'
		def rc = runWithTimeout(collect_timeout, { getSCM(info) }, stagestate,
					{ processRunSuccess(info, localinfo, stagestate) },
					{ processRunException(info, localinfo, stagestate) })
		if (rc != 'OK') {
		    println("RC runWithTimeout returned "+rc)
		    // Big stick here, we can't continue
		    shNoTrace("exit 1", "Marking this stage as a failure")
		}

		// Add node-specific properties
		localinfo += getNodeProperties(agentName)

		// Get any job-specific configuration variables
		localinfo += getProjectProperties(localinfo, agentName)

		def build_timeout = getBuildTimeout()
		def running = true

		// Set the build environment
		stagestate['runstage'] = 'ci_set_env'
		rc = runWithTimeout(collect_timeout,
				    { localinfo += ci_set_env(localinfo, agentName) }, stagestate,
				    { processRunSuccess(info, localinfo, stagestate) },
				    { processRunException(info, localinfo, stagestate) })
		if (rc != 'OK') {
		    println("RC runWithTimeout (get_ci_info) returned "+rc)

		    // Stop here
		    running = false
		}

		// Run all converted groovy stages first
		for (stageinfo in stages) {
		    if (running) { // break does weird shit
			stagestate['runstage'] = stageinfo.key

			// Run everything in the checked-out directory
			dir (localinfo['project']) {
			    runWithTimeout(build_timeout,
					   { "${stageinfo.value}"(localinfo) },
					   stagestate,
					   { processRunSuccess(info, localinfo, stagestate) },
					   { processRunException(info, localinfo, stagestate) })
			}

			// This marks it red in the graph view if it failed
			if (stagestate['failed']) {
			    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
				shNoTrace("exit 1", "Marking this stage as a failure")
			    }
			    // Don't run any more stages if one fails
			    running = false
			}
		    }
		}

		// Convert localinfo map into shell variables
		def exports = getShellVariables(localinfo)

		// Run all the shell stages (will disappear)
		for (stageinfo in shell_stages) {
		    if (running) { // break does weird shit
			stagestate['runstage'] = stageinfo.key

			// Run everything in the checked-out directory
			dir (localinfo['project']) {
			    cmdWithTimeout(build_timeout,
					   "${exports} $HOME/ci-tools/ci-wrap ${stageinfo.value}",
					   stagestate,
					   { processRunSuccess(info, localinfo, stagestate) },
					   { processRunException(info, localinfo, stagestate) })
			}

			// This marks it red in the graph view if it failed
			if (stagestate['failed']) {
			    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
				shNoTrace("exit 1", "Marking this stage as a failure")
			    }
			    // Don't run any more stages if one fails
			    running = false
			}
		    }
		}
	    }
	}

	// Save the log (if it exists)
	if (stagestate.containsKey('logfile')) {
	    info['have_split_logs'] = true
	    dir (env.WORKSPACE) {
		// This sed removes the 'bold' links which look a bit like an exposed encrypted thing (but aren't)
		sh """#!/bin/citbash -e
                   sed \$'s/\\033\\\\[8m.*\\033\\\\[0m//' <${stagestate['logfile']} >TMP_${stagestate['logfile']}"""

		if (stagestate['failed']) {
		    // Rename the log so we know it all went badly
		    sh "mv TMP_${stagestate['logfile']} FAILED_${stagestate['logfile']}"
		    archiveArtifacts artifacts: "FAILED_${stagestate['logfile']}", fingerprint: false
		} else {
		    // Rename the log so we know it all went fine
		    sh "mv TMP_${stagestate['logfile']} SUCCESS_${stagestate['logfile']}"
		    archiveArtifacts artifacts: "SUCCESS_${stagestate['logfile']}", fingerprint: false
		}
	    }
	}

	println('STAGESTATE: '+stagestate)

	// Keep a list of EXTRAVERs, it's more efficient (and less racy)
	// to de-duplicate these at the end, in postStage()
	info['EXTRAVER_LIST'] += localinfo['extraver']

	// Gather any covscan results - if there are any.
	// Covscan can fail and we still get here, so we can publish the report.
	// 'fullrebuild' is param set by a parent job to prevent uploads from weekly jobs
	//    and will normally be 0
	if (localinfo['stageName'].endsWith('covscan') && fileExists("${info['project']}/cov.json") &&
	    localinfo['fullrebuild'] == 0) {
	    stage("Get covscan artifacts for ${stageTitle} on ${agentName}") {
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

	// Don't run RPM build job if the build failed
	if (stagestate['failed']) {
	    println('this job failed, collection not happening')
	    return false
	}


	// Gather any RPM builds
	if (localinfo['stageName'].endsWith('buildrpms') && localinfo['publishrpm'] == 1 && localinfo['fullrebuild'] == 0) {
	    stage("Get RPM artifacts for ${stageTitle} on ${agentName}") {
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

	// Tidy the workspace (remember we are still on the build node here)
	stage("Cleanup for ${stageTitle} on ${agentName}") {
	    cleanWs(disableDeferredWipeout: true, deleteDirs: true)
	}
    }
    return true
}

// It went well
def processRunSuccess(Map info, Map localinfo, Map stagestate)
{
    stagestate['failed'] = false
}


// Called if the stage called from runStage() fails for any reason
def processRunException(Map info, Map localinfo, Map stagestate)
{
    println("processRunException "+stagestate)

    // Tell runStage
    stagestate['failed'] = true

    // We can't have hyphens as keys in info[:] as they get exported
    // as environment variable names.
    def stage = localinfo['stageName'].replace('-','_')

    def runtype = stagestate['runstage']

    // Stats, and save the job name for the email
    info["${localinfo['stageType']}_fail"]++
    info["${localinfo['stageType']}_fail_nodes"] += env.NODE_NAME + "(${localinfo['stageName']}: ${runtype})" + ' '
    info["${stage}_failed"] = 1 // One of these failed. that's all we need to know

    // If the jobs was aborted, then GO AWAY!
    if (stagestate['RET'] == 'ABORT') {
	currentBuild.result = 'ABORTED'
    }

    // Are there any new covscan errors?
    if (localinfo['stageName'].endsWith('covscan') && fileExists('cov.html/new/index.html')) {
	stagestate['new_cov_errors'] = true
    } else {
	stagestate['new_cov_errors'] = false
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
