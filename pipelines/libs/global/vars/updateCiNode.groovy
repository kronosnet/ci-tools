// Update ci-tools on one node
//
// This needs to be in the global lib so it can be run from
// a parallel Map
def call(String agentName, Map info)
{
    println("Running updateCiNode on ${agentName}")

    node("${agentName}") {
	// Update Jenkins node labels from getNodes()
	// - this preserves 'down'
	updateLabels(info)

	// Clear it out
	if (params.reinstall == '1') {
	    sh '''
                 rm -f /bin/citbash
                 rm -rf ci-tools
               '''
	}

	// Update it
	sh '''
             if [ -d ci-tools ]; then
               cd ci-tools
               git pull
               cd -
             else
                git clone https://github.com/kronosnet/ci-tools.git
             fi

             if [ ! -f /bin/citbash ]; then
               ln -sf `which bash` /bin/citbash
             fi
           '''
    }
}
