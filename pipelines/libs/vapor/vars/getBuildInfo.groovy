def call(String project, String projectid, Map info)
{
    sh 'env|sort'

    // Fill in the common parts
    getBuildInfoCommon(info)

    // config from job parameter
    info['project'] = project
    info['projectid'] = projectid // short for ha-functional-testing

    info['provider'] = env.provider
    if (env.fromnodes) {
	info['fromnodes'] = "${env.fromnodes}" as int
    } else {
	info['fromnodes'] = 1
    }
    if (env.tonodes) {
	info['tonodes'] = "${env.tonodes}" as int
    } else {
	info['tonodes'] = 4
    }
    info['rhelver'] = env.rhelver
    info['zstream'] = env.zstream
    if (env.brewbuild) {
	info['brewbuild'] = env.brewbuild
    } else {
	info['brewbuild'] = ''
    }
    info['upstream'] = env.upstream
    info['iscsi'] = ''
    info['block'] = ''
    info['customrepo'] = ''
    info['tests'] = env.tests
    info['testvariant'] = env.testvariant
    info['testlist'] = env.testlist
    info['taglist'] = env.taglist
    if (env.testtimeout) {
	info['testtimeout'] = "${env.testtimeout}" as int
    } else {
	info['testtimeout'] = 180
    }
    if (env.tagtimeout) {
	info['tagtimeout'] = "${env.tagtimeout}" as int
    } else {
	info['tagtimeout'] = 600
    }
    info['dryrun'] = env.dryrun
    // dynamic config
    info['nodes'] = 0
    info['runtest'] = '' // test name to run
    info['runtesttimeout'] = 0 // set below based on testtype and override for dryrun in run_cluster_test
    info['testtype'] = '' // tests or tags
    info['testopt'] = '' // set by run_cluster_test
    info['logsrc'] = '' // set by run_cluster_test
    info['logdst'] = '' // set by run_cluster_test for final artifact archiving
    // state
    info['stages_fail_nodes'] = ''
    info['stages_fail'] = 0
    info['stages_run'] = 0
    // logging
    info['vapordebug'] = ''
    if (env.vapordebug == 'yes') {
	info['vapordebug'] = '-d'
    }

    info['state'] = 'completed'
    println("info map: ${info}")

    return info
}
