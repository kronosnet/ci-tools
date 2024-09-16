def run_test(Map info)
{
    // set testopt for vapor/vedder call
    info['testopt'] = 'tests'
    if (info['testtype'] == 'tags') {
	info['testopt'] = 'teststags'
    }

    sh "rm -f vapor.log"
    tee ("vapor.log") {
	if (info['dryrun'] == '0') {
	    echo "Running test"
	    def vapor_args = ['command': 'test',
			      'provider': info['provider'],
			      'project': info['projectid'],
			      'buildnum': env.BUILD_NUMBER,
			      'rhelver': info['rhelver'],
//			      'jobid': "jenkins:${BUILD_URL}", // Not used right now. needs revisiting
			      'nodes': info['nodes'],
			      'testlogdir': "${WORKSPACE}/${info['logsrc']}",
			      "${info['testopt']}": info['runtest'],
			      'debug': env.vapordebug]
	    vapor_wrapper(vapor_args)
	} else {
	    echo "SLEEP 5"
	    sleep(5)
	    if (info['runtest'] == "fake_failure") {
		catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
		    shNoTrace("exit 1", "Marking this stage as a failure")
		}
	    }
	    if (info['runtest'] == "fake_timeout") {
		sleep(600)
	    }
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

    // define log source
    def logsrc = "rhel${info['rhelver']}-zstream_${info['zstream']}-upstream_${info['upstream']}-nodes_${info['nodes']}-${info['runtest']}"
    // sanitize path by removing , and _ from info['runtest']
    logsrc = logsrc.replace(',','_').replace(':','_')
    info['logsrc'] = "${logsrc}"

    // define runstate for return status/errors
    def runstate = [:]

    runWithTimeout(info['runtesttimeout'], { run_test(info) }, runstate, { post_run_test(info, runstate) }, { post_run_test(info, runstate) })

    // handle errors et al
    if (runstate['RET'] != 'OK') {
	recover_cluster(info)
	catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
	    shNoTrace("exit 1", "Marking this stage as a failure")
	}
    }
}
