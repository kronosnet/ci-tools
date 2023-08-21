def call(Map info)
{
    println("Deleting test cluster")
    if ("${dryrun}" == '1') {
	return
    }
    timeout(time: 60, unit: 'MINUTES') {
	sh """
	    echo "Deleting test cluster"
	    cd $HOME/ci-tools/fn-testing
	    ./validate-cloud -c delete -d -p ${provider} -b ${BUILD_NUMBER} -r ${rhelver}
	"""
    }
}
