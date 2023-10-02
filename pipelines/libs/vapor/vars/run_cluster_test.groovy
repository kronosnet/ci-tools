def run_test(Map info)
{
    sh "rm -f vapor.log"
    tee ("vapor.log") {
	if (info['dryrun'] == '0') {
	    sh """
		echo "Running test"
		cd $HOME/ci-tools/fn-testing
		./validate-cloud -c test -d -p ${info['provider']} -P ${info['projectid']} -b ${BUILD_NUMBER} -j "jenkins:${BUILD_URL}" -r ${info['rhelver']} -n ${info['nodes']} -e ${info['testopt']} ${info['runtest']} -a "${WORKSPACE}/${info['logsrc']}"
	    """
	} else {
	    // keep this for debugging
	    sh """
		echo "FAKE RUNNING TEST"
		cd $HOME/ci-tools/fn-testing
		echo ./validate-cloud -c test -d -p ${info['provider']} -P ${info['projectid']} -b ${BUILD_NUMBER} -j "jenkins:${BUILD_URL}" -r ${info['rhelver']} -n ${info['nodes']} -e ${info['testopt']} ${info['runtest']} -a "${WORKSPACE}/${info['logsrc']}"
		echo "SLEEP 5"
		sleep 5
		if [ "${info['runtest']}" = "fake_failure" ]; then
		    exit 1
		fi
		if [ "${info['runtest']}" = "fake_timeout" ]; then
		    sleep 600
		fi
	    """
	}
    }
}

def post_run_test(Map info, Map runstate)
{
    // don´t collect anything on user abort or attempt to recover
    // simply return the error and be done.
    if (runstate['RET'] == 'ABORT') {
	currentBuild.result = 'ABORTED'
	throw (runstate['EXP'])
    }

    // always collect logs from a test run
    sh """
	mkdir -p ${info['logsrc']}
	mv vapor.log ${info['logsrc']}/
	tar Jcvpf ${info['logsrc']}.tar.xz ${info['logsrc']}/
	rm -rf ${info['logsrc']}
    """
    if (runstate['RET'] == 'OK') {
	info['logdst'] = "SUCCESS_${info['logsrc']}.tar.xz"
    } else {
	info['logdst'] = "FAILED_${info['logsrc']}.tar.xz"
	info['stages_fail_nodes'] += "\n- ${info['runtest']} (nodes: ${info['nodes']})"
	info['stages_fail']++
    }
    info['stages_run']++
    sh "mv ${info['logsrc']}.tar.xz ${info['logdst']}"
    archiveArtifacts artifacts: "${info['logdst']}", fingerprint: false
}

def call(Map info)
{
    // keep this for debugging
    if (info['dryrun'] == '1') {
	info['runtesttimeout'] = 2
    }

    // set testopt for vapor/vedder call
    info['testopt'] = '-t'
    if (info['testtype'] == 'tags') {
	info['testopt'] = '-T'
    }

    // define log source
    def logsrc = "rhel${info['rhelver']}-zstream_${info['zstream']}-upstream_${info['upstream']}-nodes_${info['nodes']}-${info['runtest']}"
    // sanitize path by removing , and _ from info['runtest']
    logsrc = logsrc.replace(',','_').replace(':','_')
    info['logsrc'] = "${logsrc}"

    // define runstate for return status/errors
    def runstate = [:]

    runWithTimeout(info['runtesttimeout'], { run_test(info) }, runstate, { post_run_test(info, runstate) }, { post_run_test(info, runstate) })

    // handle errors et all
    if (runstate['RET'] != 'OK') {
	recover_cluster(info)
	catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
	    sh 'exit 1'
	}
    }
}
