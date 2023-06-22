def call(Integer maxnodes)
{
    println("Creating test cluster")
    if ("${dryrun}" == '1') {
	return
    }
    timeout(time: 60, unit: 'MINUTES') {
	sh """
	    echo "Creating test cluster"
	    cd $HOME/ci-tools/fn-testing
	    ./validate-cloud -c create -d -p ${provider} -b ${BUILD_NUMBER} -r ${rhelver} -n ${maxnodes}
	"""
    }
}
