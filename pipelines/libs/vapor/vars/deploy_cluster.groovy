def call(Map info)
{
    println("Deploy test cluster")
    if (info['dryrun'] == '1') {
	return
    }
    timeout(time: 120, unit: 'MINUTES') {
	echo "Deploy test cluster"
	def vapor_args = ['command': 'deploy',
			  'provider': info['provider'],
			  'project': info['projectid'],
			  'buildnum': env.BUILD_NUMBER,
			  'rhelver': info['rhelver'],
			  'zstream': info['zstream'],
			  'upstream': info['upstream'],
			  'debug': env.vapordebug]
	if ("${info['iscsi']}" != '') {
	    vapor_args += ['iscsisize': info['iscsi']]
	}
	if ("${info['customrepo']}" != '') {
	    vapor_args += ['customrepopath': info['customrepo']]
	}
	if ("${info['brewbuild']}" != '') {
	    vapor_args += ['brewbuildopts': info['brewbuild']]
	}

	vapor_wrapper(vapor_args)
    }
}
