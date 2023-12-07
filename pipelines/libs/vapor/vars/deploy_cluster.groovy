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
	    customrepoopts=""
	    if [ -n "${info['customrepo']}" ]; then
		customrepoopts="-m ${info['customrepo']}"
	    fi
	    brewbuildopts=""
	    if [ -n "${info['brewbuild']}" ]; then
		brewbuildopts="-x ${info['brewbuild']}"
	    fi
	    echo "Deploy test cluster"
	    $HOME/ci-tools/ci-wrap fn-testing/validate-cloud -c deploy -d -p ${info['provider']} -P ${info['projectid']} -b ${BUILD_NUMBER} -r ${info['rhelver']} -z ${info['zstream']} -u ${info['upstream']} \$customrepoopts \$iscsiopts \$brewbuildopts
	"""
    }
}
