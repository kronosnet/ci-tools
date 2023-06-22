def call()
{
    println("Deploy test cluster")
    if ("${dryrun}" == '1') {
	return
    }
    timeout(time: 120, unit: 'MINUTES') {
	sh """
	    echo "Deploy test cluster"
	    cd $HOME/ci-tools/fn-testing
	    ./validate-cloud -c deploy -d -p ${provider} -b ${BUILD_NUMBER} -r ${rhelver} -z ${zstream} -u ${upstream}
	"""
    }
}
