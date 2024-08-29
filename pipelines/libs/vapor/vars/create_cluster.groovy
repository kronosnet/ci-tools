def call(Map info)
{
    println("Creating test cluster")
    timeout(time: 60, unit: 'MINUTES') {
	println("Creating test cluster")
	if (info['dryrun'] == '1') {
	    if (info['rhelver'] == '666') {
		return 1
	    } else {
		return 0
	    }
	}

	def vapor_args = ['command': 'create',
			  'provider': info['provider'],
			  'project': info['projectid'],
			  'buildnum': env.BUILD_NUMBER,
			  'rhelver': info['rhelver'],
			  'nodes': info['tonodes'],
			  'debug': env.vapordebug]
	if ("${info['iscsi']}" != '') {
	    vapor_args += ['iscsisize': info['iscsi']]
	}
	if ("${info['block']}" != '') {
	    vapor_args += ['blocksize': info['block']]
	}
	vapor_wrapper(vapor_args)
    }
}
