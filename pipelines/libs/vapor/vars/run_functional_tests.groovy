def call(Map info)
{
    for (info['nodes'] = info['fromnodes']; info['nodes'] <= info['tonodes']; info['nodes']++) {

	// tests
	info['testtype'] = 'tests'
	info['runtesttimeout'] = info['testtimeout']
	def finaltestlist = [ ]
	if ("${info['testlist']}" == 'auto') {
	    finaltestlist = get_cluster_tests(info)
	} else if ("${info['testlist']}" != '') {
	    finaltestlist = "${info['testlist']}".split(';')
	}
	println(finaltestlist)
	for (test in finaltestlist) {
	    info['runtest'] = test
	    stage("Run test '${info['runtest']}' on ${info['nodes']} nodes") {
		run_cluster_test(info)
	    }
	}

	// tags
	info['testtype'] = 'tags'
	info['runtesttimeout'] = info['tagtimeout']
	def finaltaglist = [ ]
	if ("${info['taglist']}" == 'auto') {
	    finaltaglist = get_cluster_tests(info)
	} else if ("${info['taglist']}" != '') {
	    finaltaglist = "${info['taglist']}".split(';')
	}
	println(finaltaglist)
	for (test in finaltaglist) {
	    info['runtest'] = test
	    stage("Run tag '${info['runtest']}' on ${info['nodes']} nodes") {
		run_cluster_test(info)
	    }
	}
    }
    // This tells parent jobs what happened - as a string because it's in env
    env.STAGES_FAIL = "${info['stages_fail']}"
    // This is the detail
    env.FAIL_INFO = "${info['stages_fail_nodes']}"
}
