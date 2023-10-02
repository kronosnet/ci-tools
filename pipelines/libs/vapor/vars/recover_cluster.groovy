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

def hard_recover(Map info, Map runstate)
{
    if (runstate['RET'] == 'ABORT') {
	currentBuild.result = 'ABORTED'
	throw (runstate['EXP'])
    }
    timeout(time: 20, unit: 'MINUTES') {
	run_reboot(info)
	access_cluster(info)
	run_cleanup(info)
	runstate['RET'] = 'OK'
    }
}

def call(Map info)
{
    println("Recovering test cluster")
    if (info['dryrun'] == '1') {
	return
    }

    def runstate = [:]

    runWithTimeout(30, { run_cleanup(info) }, runstate, { }, { hard_recover(info, runstate) })

    if (runstate['RET'] != 'OK') {
	throw (runstate['EXP'])
    }
}
