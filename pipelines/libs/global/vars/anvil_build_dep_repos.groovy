def call(String agentName, Map info)
{
    def stagestate = [:]
    def collect_timeout = 30

    node("${agentName}") {
	cleanWs(disableDeferredWipeout: true, deleteDirs: true)

	stage('checkout') {
	    stagestate['runstage'] = 'checkout'
	    def rc = runWithTimeout(collect_timeout, { getSCM(info) }, stagestate,
				    { processRunSuccess(info, stagestate) },
				    { processRunException(info, stagestate) })
	    if (rc != 'OK') {
		println("RC runWithTimeout returned "+rc)
		shNoTrace("exit 1", "Marking this stage as a failure")
		return false
	    }
	}

	def buildtarget = "${agentName}".replaceAll('anvil-ci-', '')
	stage("Building ${buildtarget} repo on node ${agentName}") {
	    try {
		dir (info['project']) {
		runWithArtifacts(info, "build_deps_repo_${buildtarget}.log", {
		    info['stages_run']++;
		    def localinfo = getNodeProperties(agentName)
		    def exports = getShellVariables(localinfo)

		    sh """
		     echo ./build-anvil-ext-repo -d ${buildtarget} -p /var/www/html
		    """
		    sh """
		     cd /var/www/html/
		     rsync -av --progress *.repo anvil-ci-proxy:/var/www/html/.
		     rsync -av --progress --delete-after ${buildtarget} anvil-ci-proxy:/var/www/html/
		     rsync -av --progress --delete-after ${buildtarget}-test-update anvil-ci-proxy:/var/www/html/
		    """
		})
		}
	    }
	    catch (e) {
		info['stages_fail'] += 1
		info['stages_fail_nodes'] += "${agentName}"
		catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
		    shNoTrace("exit 1", "Marking this stage as a failure")
		}
	    }
	}
    }
}

def processRunSuccess(Map info, Map stagestate)
{
    stagestate['failed'] = false
}

def processRunException(Map info, Map stagestate)
{
    stagestate['failed'] = true
    if (stagestate['RET'] == 'ABORT') {
	currentBuild.result = 'ABORTED'
    }
}
