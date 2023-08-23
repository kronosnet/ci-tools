def call(Map info)
{
    println("Deploy test cluster")
    if (info['dryrun'] == '1') {
	return
    }
    timeout(time: 120, unit: 'MINUTES') {
	sh """
	    iscsiopts=""
	    if [ -n "${info['iscsi']}" ]; then
		iscsiopts="-i ${info['iscsi']}"
	    fi
	    echo "Deploy test cluster"
	    cd $HOME/ci-tools/fn-testing
	    ./validate-cloud -c deploy -d -p ${info['provider']} -P ${info['projectid']} -b ${BUILD_NUMBER} -r ${info['rhelver']} -z ${info['zstream']} -u ${info['upstream']} \$iscsiopts
	"""
    }
}
