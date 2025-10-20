def call(Map info)
{
    stage('Prep build env') {
	cleanWs(disableDeferredWipeout: true, deleteDirs: true)
	info['branch'] = "${info['provider']} ${info['osver']} zstream: ${info['zstream']} upstream: ${info['upstream']} tests: ${info['tests']}"
	info['runtest'] = 'Initial delete_cluster'
	delete_cluster(info)
    }
    stage("Create ${info['provider']} ${info['osver']} test cluster") {
	info['runtest'] = 'create_cluster'
	create_cluster(info)
    }
    stage("Deploy ${info['provider']} ${info['osver']} zstream: ${info['zstream']} upstream: ${info['upstream']} test cluster") {
	info['runtest'] = 'deploy_cluster'
	deploy_cluster(info)
    }
}
