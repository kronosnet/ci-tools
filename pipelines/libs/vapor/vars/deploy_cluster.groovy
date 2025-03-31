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
			  'osver': info['osver'],
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

	// Allow us to run sts from RHEL10 on rhel9 for pcmk3.x and pcs
	if (info['osver'] == 'rhel9' &&
	    info['projectid'] != 'kft' &&
	    (info['upstream'] == 'main' ||
	     info['upstream'] == 'next-stable')) {
	    vapor_args['extraopts'] = '--sts-version 10'
	}
	RWLock(info, 'ci-rpm-repos', 'READ', {
	    vapor_wrapper(vapor_args)
	})
    }
}
