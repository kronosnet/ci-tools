def validate(Map p)
{
    if (!p.containsKey('command')) {
	println('vapor_wrapper called with no command')
	println(p)
	return false
    }
    if (!p.containsKey('provider')) {
	println('vapor_wrapper called with no provider')
	println(p)
	return false
    }
    if (!p.containsKey('rhelver')) {
	println('vapor_wrapper called with no rhelver')
	println(p)
	return false
    }
    return true
}


// Set one default
def set_default(Map i, String key, the_default)
{
    if (!i.containsKey(key)) {
	i[key] = the_default
    }
}

// Set all the defaults
def set_defaults(Map p)
{
    set_default(p, 'nodes', '4')
    set_default(p, 'zstream', 'no')
    set_default(p, 'upstream', 'none')
    set_default(p, 'tests', 'setup')
    set_default(p, 'testtype', 'tests')
    set_default(p, 'iscsisize', '0')
    set_default(p, 'blocksize', '0')
    set_default(p, 'brewbuild', '')
    set_default(p, 'teststags', '')
    set_default(p, 'debug', '')
    set_default(p, 'post', '')
    set_default(p, 'jobid', '')
    set_default(p, 'buildnum', '')
    set_default(p, 'customrepopath', '')
    set_default(p, 'testlogdir', '')
    set_default(p, 'project', '')
    set_default(p, 'echorun', '')
    p['setup_fn'] = {}

    // hardcoded sizing for now
    set_default(p, 'links', '1')
}

def cloud_delete(Map p)
{
    return sh(returnStatus: true,
       script:
       """
         ${p['echorun']} vapor ${p['vaporopts']} delete ${p['clusteropts']} ${p['provideropts']}
       """
    )
}
def cloud_create(Map p)
{
    // Run provider-specific setup
    p['setup_fn']()

    return sh(returnStatus: true,
       script:
       """
         ${p['echorun']} vapor ${p['vaporopts']} create ${p['clusteropts']} ${p['createbaseopts']} ${p['createopts']} ${p['provideropts']}
       """
    )
}
def cloud_deploy(Map p)
{
    return sh(returnStatus: true,
       script:
       """
         ${p['echorun']} vapor ${p['vaporopts']} deploy ${p['clusteropts']} ${p['deploybaseopts']} ${p['deployopts']} ${p['provideropts']}
       """
    )
}
def cloud_test(Map p)
{
    return sh(returnStatus: true,
       script:
       """
         ${p['echorun']} vapor ${p['vaporopts']} test ${p['clusteropts']} ${p['testbaseopts']} --nodes ${p['nodes']} ${p['testopts']} --${p['testtype']} \"${p['tests']}\" ${p['provideropts']}
       """
    )
}
def cloud_reboot(Map p)
{
    return sh(returnStatus: true,
       script:
       """
         ${p['echorun']} vapor ${p['vaporopts']} reboot ${p['clusteropts']} ${p['provideropts']}
       """
    )
}

def call(Map p)
{
    if (!validate(p)) {
	return 1
    }
    set_defaults(p)

    def providers = getProviderProperties()
    p += providers[p['provider']]

    if (!p.containsKey('rhelver')) {
	println("ERROR: ${p['rhelver']} not supported by ${p['provider']}")
	return 1
    }
    p += p["rhel${p['rhelver']}"]

    if (params.containsKey('echorun') &&
	params['echorun'] == 'yes') {
	p['echorun'] = 'echo'
    }

    p['createbaseopts'] = "--nodes ${p['nodes']}"
    p['deploybaseopts'] = ''
    if (p['iscsisize'] == '0' && p['defaultiscsi'] != '') {
	p['iscsisize'] = p['defaultiscsi']
    }
    if (p['iscsisize'] != '0') {
	p['createbaseopts'] = "${p['createbaseopts']} --iscsi ${p['iscsisize']}"
	p['deploybaseopts'] = "${p['deploybaseopts']} --iscsi ${p['iscsisize']}"
    }
    if (p['defaultuseiscsi'] == 'yes') {
	p['deploybaseopts'] = "${p['deploybaseopts']} --use-iscsi"
    }

    if (p['blocksize'] == '0' && p['defaultblocksize'] != '') {
	p['blocksize'] = p['defaultblocksize']
    }
    if (p['blocksize'] != '0') {
	p['createbaseopts'] = "${p['createbaseopts']} --block ${p['blocksize']}"
    }
    if (p['upstream'] != 'none') {
	p['deploybaseopts'] = "${p['deploybaseopts']}  --use-upstream-repo ${p['upstream']}"
    }
    if (p['zstream'] == 'yes') {
	p['deploybaseopts'] = "${p['deploybaseopts']}  --use-zstream"
    }
    if (p['customrepopath'] != '') {
	p['deploybaseopts'] = "${p['deploybaseopts']}  --use-custom-repo ${p['customrepopath']}"
    }
    if (p['brewbuild'] != '') {
	p['deploybaseopts'] = "${p['deploybaseopts']}  --package ${p['brewbuild']}"
    }

    p['testbaseopts'] = ''
    if (p['jobid'] != '') {
	p['testbaseopts'] = "${p['testbaseopts']} -j${p['jobid']}"
    }
    if (p['post'] != '') {
	p['testbaseopts'] = "${p['testbaseopts']} --post-results"
    }
    if (p['testlogdir'] != '') {
	p['testbaseopts'] = "${p['testbaseopts']} --collect-debug-dir=${p['testlogdir']}"
    }

    p['vaporopts'] = '--nocolorlog --log-level '
    if (p['debug'] == 'yes') {
	p['vaporopts'] = "${p['vaporopts']} debug"
    } else {
	p['vaporopts'] = "${p['vaporopts']} info"
    }

    // Generate cluster name
    if (p['buildnum'] != '') {
	p['buildnum'] = "j${p['buildnum']}"
    }
    p['clusteropts'] = "--cluster ${p['project']}${p['provider']}c${p['rhelver']}${p['buildnum']}"

    // Put all options together
    p['provideropts'] = "${p['provider']} ${p['authopts']}"

    def ret = 1
    switch (p['command']) {
	case 'del':
	case 'delete':
	    ret = cloud_delete(p)
	    break
	case 'create':
	    ret = cloud_create(p)
	    break
	case 'deploy':
	    ret = cloud_deploy(p)
	    break
	case 'test':
	    ret = cloud_test(p)
	    break
	case 'reboot':
	    ret = cloud_reboot(p)
	    break
	default:
	    println("Unknown command ${p['command']}")
	    ret = 1
    }
    if (ret != 0) {
	catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
	    shNoTrace("exit 1", "Marking this stage as a failure")
	}
	// The outer layers expect an exception
	throw (new Exception("Error returned from ${p['command']}: ${ret}"))
    }
    return ret
}
