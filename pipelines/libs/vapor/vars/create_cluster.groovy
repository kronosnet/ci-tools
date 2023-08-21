def call(Map info, Integer maxnodes)
{
    println("Creating test cluster")
    timeout(time: 60, unit: 'MINUTES') {
	sh """
	    echo "Creating test cluster"
	    if [ "${dryrun}" = "1" ]; then
		if [ "${rhelver}" = "666" ]; then
		    exit 1
		fi
		exit 0
	    fi
	    cd $HOME/ci-tools/fn-testing
	    ./validate-cloud -c create -d -p ${provider} -b ${BUILD_NUMBER} -r ${rhelver} -n ${maxnodes}
	"""
    }
}
