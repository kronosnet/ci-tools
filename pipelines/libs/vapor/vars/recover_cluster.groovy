def run_cleanup(Map info)
{
    println('Attempt to run cleanup')
    sh """
	cd $HOME/ci-tools/fn-testing
	./validate-cloud -c test -p ${provider} -b ${BUILD_NUMBER} -r ${rhelver} -n ${info['nodes']} -e -t cleanup >/dev/null 2>&1
    """
}

def run_reboot(Map info)
{
    println('Attempt to reboot test env')
    sh """
	cd $HOME/ci-tools/fn-testing
	./validate-cloud -c reboot -p ${provider} -b ${BUILD_NUMBER} -r ${rhelver} -n ${info['nodes']}
    """
}

def hard_recover(Map info, Map locals)
{
    if (locals['RET'] == 'ABORT') {
	currentBuild.result = 'ABORTED'
	throw (locals['EXP'])
    }
    timeout(time: 20, unit: 'MINUTES') {
	run_reboot(info)
	access_cluster()
	run_cleanup(info)
	locals['RET'] = 'OK'
    }
}

def call(Integer nodes)
{
    println("Recovering test cluster")
    if ("${dryrun}" == '1') {
	return
    }

    def locals = [:]
    def info = [:]
    info['nodes'] = nodes

    runWithTimeout(30, { run_cleanup(info) }, info, locals, { }, { hard_recover(info, locals) })

    if (locals['RET'] != 'OK') {
	throw (locals['EXP'])
    }
}
