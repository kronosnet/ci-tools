// Update BaseOS on one node
//
// This needs to be in the global lib so it can be run from
// a parallel Map
// realNode might not match agentName if the node is updated using ansible
//    on built-in, but we still need to know it
def update_node(String agentName, Map info, String realNode)
{
    println("Running updateOSNode on ${realNode}")

    node("${agentName}") {
	cleanWs(disableDeferredWipeout: true, deleteDirs: true)
	mark_node_offline(realNode)

	try {
	    runWithArtifacts(info, "update_${agentName}.log", {
		info['stages_run']++;

		def localinfo = getNodeProperties(agentName)
		def exports = getShellVariables(localinfo)

		// special case freebsd devel that needs ansible from built-in node
		if (agentName == 'built-in' && info['packager'] == 'freebsd') {
		    sh """
		     cd $HOME/ci-tools/bsd-update
		     ${exports} ./run-update -d
		    """
		} else if (agentName == 'built-in' && info['packager'] == 'apk') {
		    sh """
		     cd ${env.WORKSPACE}/../update-all-apk/ansible
		     ${exports} ansible-playbook update.yml -i testing --limit ${realNode}
		    """
		} else {
		    sh """
		     ${exports} $HOME/ci-tools/ci-wrap ci-update-${info['packager']}
		    """
		}

		// special case anvil bm nodes and save a whole pipeline to be plugged around
		if (agentName.startsWith('anvil-ci-bm-phy')) {
		    sh """
		     ${exports} $HOME/ci-tools/ci-wrap ci-destroy-anvil-bm-vm || true
		     ${exports} $HOME/ci-tools/ci-wrap ci-setup-anvil-bm
		    """
		}
	    })
	}
	// Catch any exceptions and record them
	catch (e) {
	    info['stages_fail'] += 1
	    info['stages_fail_nodes'] += "${agentName} "
	    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
		shNoTrace("exit 1", "Marking this stage as a failure")
	    }
	}
    }

    reconnect_node(realNode)
}

// Polymorph it until everything is on ansible
def call(String agentName, Map info)
{
    update_node(agentName, info, agentName)
}

def call(String agentName, Map info, String realNode)
{
    update_node(agentName, info, realNode)
}

def mark_node_offline(String nodeName)
{
    for (aSlave in hudson.model.Hudson.instance.slaves) {
	def computer = aSlave.getComputer();
	if (computer.name == nodeName) {
	    println("Mark node ${nodeName} offline")
	    def offline_cause = new hudson.slaves.OfflineCause.UserCause(User.current(), "offline to update the node")
	    computer.setTemporaryOfflineCause(offline_cause)
	    println("Waiting for node ${nodeName} to be offline")
	    computer.waitUntilOffline()
	    println("${nodeName} is now offline")
	    // special case freebsd devel
	    def workers = 1
	    if (nodeName in ['freebsd-devel-x86-64', 'tb-alpine-x86-64']) {
		workers = 0
	    }
	    while (computer.countBusy() != workers) {
		println("Waiting 300 seconds for node ${nodeName} to be idle")
		sleep(300)
	    }
	    println("${nodeName} is idle")
	}
    }
}

def reconnect_node(String nodeName)
{
    for (aSlave in hudson.model.Hudson.instance.slaves) {
	def computer = aSlave.getComputer();
	if (computer.name == nodeName) {
	    println("Checking if node ${nodeName} is idle")
	    while (!computer.isIdle()) {
		println("Waiting 60 seconds for node ${nodeName} to be idle")
		sleep(60)
	    }
	    println("Node ${nodeName} is idle")
	    println("Disconnecting ${nodeName}")
	    def offline_cause = new hudson.slaves.OfflineCause.UserCause(User.current(), "Disconnecting node")
	    computer.disconnect(offline_cause)
	    while (computer.isConnected()) {
		println("Waiting 1 second for node ${nodeName} to be disconnected")
		sleep(1)
	    }
	    println("Connecting ${nodeName}")
	    computer.connect(true)
	    while (!computer.isConnected()) {
		println("Waiting 10 seconds for node ${nodeName} to be connected")
		sleep(10)
	    }
	    println("Mark node ${nodeName} online")
	    computer.setTemporaryOfflineCause(null)
	    println("Waiting for node ${nodeName} to be online")
	    computer.waitUntilOnline()
	    println("${nodeName} is now online")
	}
    }
}
