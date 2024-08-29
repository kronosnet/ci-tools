def run_cleanup(Map info)
{
    println('Attempt to run cleanup')
    def vapor_args = ['command': 'test',
		      'provider': info['provider'],
		      'project': info['projectid'],
		      'buildnum': env.BUILD_NUMBER,
		      'rhelver': info['rhelver'],
		      'nodes': info['tonodes'],
		      'debug': env.vapordebug,
		      'tests': 'cleanup',
		      'post': '']
    vapor_wrapper(vapor_args)
}

def run_reboot(Map info)
{
    println('Attempt to reboot test env')
    def vapor_args = ['command': 'reboot',
		      'provider': info['provider'],
		      'project': info['projectid'],
		      'buildnum': env.BUILD_NUMBER,
		      'rhelver': info['rhelver'],
		      'nodes': info['tonodes'],
		      'debug': env.vapordebug]
    vapor_wrapper(vapor_args)
}

def hard_recover(Map info, Map runstate)
{
    if (runstate['RET'] == 'ABORT') {
	currentBuild.result = 'ABORTED'
	throw (runstate['EXP'])
    }
    timeout(time: 20, unit: 'MINUTES') {
	run_reboot(info)
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
