def call(Map info)
{
    stage('Prep build env') {
	cleanWs(disableDeferredWipeout: true, deleteDirs: true)
	info['branch'] = "${info['provider']} ${info['osver']} zstream: ${info['zstream']} upstream: ${info['upstream']} tests: ${info['tests']}"
	delete_cluster(info)
    }
    stage("Create ${info['provider']} ${info['osver']} test cluster") {
	create_cluster(info)
    }
    stage("Deploy ${info['provider']} ${info['osver']} zstream: ${info['zstream']} upstream: ${info['upstream']} test cluster") {
	deploy_cluster(info)
    }
}
