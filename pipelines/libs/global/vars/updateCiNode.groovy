// Update ci-tools on one node
//
// This needs to be in the global lib so it can be run from
// a parallel Map
def call(String agentName, Map info)
{
    println("Running updateCiNode on ${agentName}")

    node("${agentName}") {

	try {
	    info['stages_run']++;
	    // Some things don't need to run on the 'built-in' Jenkins node
	    if (agentName != 'built-in') {
		// Update Jenkins node labels from getNodes()
		// - this preserves 'down'
		updateLabels(info)
		if (params.reinstall == '1') {
		    sh '''
                     rm -f /bin/citbash
                   '''
		}
	    }

	    // Clear out ci-tools
	    if (params.reinstall == '1') {
		sh '''
                 rm -rf $HOME/ci-tools
               '''
	    }

	    // Update them
	    sh '''
             if [ -d $HOME/ci-tools ]; then
               cd $HOME/ci-tools
               git pull
             else
                cd $HOME
                git clone https://github.com/kronosnet/ci-tools.git
             fi
           '''

	    // Jenkins init script needs to live in Jenkins $HOME
	    if (agentName == 'built-in') {
		sh '''
                 cp $HOME/ci-tools/init.groovy $HOME
               '''
	    } else {
		// built-in runs as Jenkins user so can't write to /bin (see also above)
		sh '''
                 if [ ! -f /bin/citbash ]; then
                   ln -sf `which bash` /bin/citbash
                 fi
               '''
	    }
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
}
