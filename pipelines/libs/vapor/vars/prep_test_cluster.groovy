def call(Map info)
{
    stage('Prep build env') {
	cleanWs(disableDeferredWipeout: true, deleteDirs: true)
	delete_cluster(info)
	info['branch'] = "${info['provider']} ${info['osver']} zstream: ${info['zstream']} upstream: ${info['upstream']} tests: ${info['tests']}"
    }
    stage("Create ${info['provider']} ${info['osver']} test cluster") {
	create_cluster(info)
    }
    stage("Deploy ${info['provider']} ${info['osver']} zstream: ${info['zstream']} upstream: ${info['upstream']} test cluster") {
	deploy_cluster(info)
    }
}
