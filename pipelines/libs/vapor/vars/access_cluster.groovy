def call()
{
    println("Waiting for cluster to boot")
    if ("${dryrun}" == '1') {
	return
    }
    sh '''
	echo "Waiting for cluster to boot"
	cd $HOME/ci-tools/fn-testing
	wsip=$(./validate-cloud -c ip -p ${provider} -b ${BUILD_NUMBER} -r ${rhelver})
	waittimeout=600

	while ! nc -z $wsip 22 && [ "$waittimeout" -gt "0" ]; do
	    sleep 1
	    waittimeout=$((waittimeout - 1))
	done

	if [ "$waittimeout" = "0" ]; then
	    echo "Cluster failed to boot"
	    exit 1
	fi

	echo "Cluster done booting"
    '''
}
