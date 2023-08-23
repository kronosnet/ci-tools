def run_cleanup(Map info)
{
    println('Attempt to run cleanup')
    sh """
	cd $HOME/ci-tools/fn-testing
	./validate-cloud -c test -p ${info['provider']} -P ${info['projectid']} -b ${BUILD_NUMBER} -r ${info['rhelver']} -n ${info['nodes']} -e -t cleanup >/dev/null 2>&1
    """
}

def run_reboot(Map info)
{
    println('Attempt to reboot test env')
    sh """
	cd $HOME/ci-tools/fn-testing
	./validate-cloud -c reboot -p ${info['provider']} -P ${info['projectid']} -b ${BUILD_NUMBER} -r ${info['rhelver']} -n ${info['nodes']}
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
	access_cluster(info)
	run_cleanup(info)
	locals['RET'] = 'OK'
    }
}

def call(Map info)
{
    println("Recovering test cluster")
    if (info['dryrun'] == '1') {
	return
    }

    def locals = [:]

    runWithTimeout(30, { run_cleanup(info) }, info, locals, { }, { hard_recover(info, locals) })

    if (locals['RET'] != 'OK') {
	throw (locals['EXP'])
    }
}
