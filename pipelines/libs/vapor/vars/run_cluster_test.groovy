def run_test(Map info)
{
    sh "rm -f vapor.log"
    tee ("vapor.log") {
	if ("${dryrun}" == '0') {
	    sh """
		echo "Running test"
		cd $HOME/ci-tools/fn-testing
		./validate-cloud -c test -d -p ${provider} -b ${BUILD_NUMBER} -j "jenkins:${BUILD_URL}" -r ${rhelver} -n ${info['nodes']} -e ${info['testopt']} ${info['testtag']} -a "${WORKSPACE}/${info['logsrc']}"
	    """
	} else {
	    // keep this for debugging
	    sh """
		echo "FAKE RUNNING TEST"
		cd $HOME/ci-tools/fn-testing
		echo ./validate-cloud -c test -d -p ${provider} -b ${BUILD_NUMBER} -j "jenkins:${BUILD_URL}" -r ${rhelver} -n ${info['nodes']} -e ${info['testopt']} ${info['testtag']} -a "${WORKSPACE}/${info['logsrc']}"
		echo "SLEEP 10"
		sleep 10
		if [ "${info['testtag']}" = "fake_failure" ]; then
		    exit 1
		fi
		if [ "${info['testtag']}" = "fake_timeout" ]; then
		    sleep 600
		fi
	    """
	}
    }
}

def post_run_test(Map info, Map locals)
{
    // don´t collect anything on user abort or attempt to recover
    // simply return the error and be done.
    if (locals['RET'] == 'ABORT') {
	currentBuild.result = 'ABORTED'
	throw (locals['EXP'])
    }

    def logdst = ""

    // always collect logs from a test run
    sh """
	mkdir -p ${info['logsrc']}
	mv vapor.log ${info['logsrc']}/
	tar Jcvpf ${info['logsrc']}.xz ${info['logsrc']}/
	rm -rf ${info['logsrc']}
    """
    if (locals['RET'] == 'OK') {
	logdst = "SUCCESS_${info['logsrc']}.xz"
    } else {
	logdst = "FAILED_${info['logsrc']}.xz"
    }
    sh "mv ${info['logsrc']}.xz ${logdst}"
    archiveArtifacts artifacts: "${logdst}", fingerprint: false
}

def call(String testtags, String testtag, Integer nodes, Integer timeout)
{
    // bypass sandbox check
    def testtimeout = timeout

    def testopt = '-t'
    if (testtags == 'tag') {
	testopt = '-T'
    }

    // keep this for debugging
    if ("${dryrun}" == '1') {
	testtimeout = 2
    }

    def logsrc = "rhel${rhelver}-zstream_${zstream}-upstream_${upstream}-nodes_${nodes}-${testtag}"
    // sanitize path by removing , and _ from testtag
    logsrc = logsrc.replace(',','_').replace(':','_')

    def locals = [:]

    def info = [:]
    info['logsrc'] = "${logsrc}"
    info['testtag'] = "${testtag}"
    info['nodes'] = nodes
    info['testopt'] = "${testopt}"

    runWithTimeout(testtimeout, { run_test(info) }, info, locals, { post_run_test(info, locals) }, { post_run_test(info, locals) })

    // handle errors et all
    if (locals['RET'] != 'OK') {
	recover_cluster(info['nodes'])
	catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
	    sh 'exit 1'
	}
    }
}
