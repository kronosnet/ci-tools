def call(Map info)
{
    println("Deleting test cluster")
    if (info['dryrun'] == '1') {
	return
    }
    timeout(time: 60, unit: 'MINUTES') {
	sh """
	    echo "Deleting test cluster"
	    $HOME/ci-tools/ci-wrap fn-testing/validate-cloud -c delete -d -p ${info['provider']} -P ${info['projectid']} -b ${BUILD_NUMBER} -r ${info['rhelver']}
	"""
    }
}
