def call(Map info)
{
    delete_cluster(info)
    cleanWs(disableDeferredWipeout: true, deleteDirs: true)
    node('built-in') {
	script {
	    projectFinishUp(info)
	}
    }
}
