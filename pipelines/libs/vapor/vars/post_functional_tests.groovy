def call(Map info)
{
    delete_cluster(info)
    cleanWs(disableDeferredWipeout: true, deleteDirs: true)
    node('built-in') {
	script {
	    // Show 'pipeline console' rather than full text
	    info['emailOptions'] = ['showConsole']
	    projectFinishUp(info)
	}
    }
}
