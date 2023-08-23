def call(Map info)
{
    println("Creating test cluster")
    timeout(time: 60, unit: 'MINUTES') {
	sh """
	    echo "Creating test cluster"
	    if [ "${info['dryrun']}" = "1" ]; then
		if [ "${info['rhelver']}" = "666" ]; then
		    exit 1
		fi
		exit 0
	    fi
	    iscsiopts=""
	    if [ -n "${info['iscsi']}" ]; then
		iscsiopts="-i ${info['iscsi']}"
	    fi
	    blockopts=""
	    if [ -n "${info['block']}" ]; then
		blockopts="-s ${info['block']}"
	    fi
	    cd $HOME/ci-tools/fn-testing
	    ./validate-cloud -c create -d -p ${info['provider']} -P ${info['projectid']} -b ${BUILD_NUMBER} -r ${info['rhelver']} -n ${info['tonodes']} \$iscsiopts \$blockopts
	"""
    }
}
