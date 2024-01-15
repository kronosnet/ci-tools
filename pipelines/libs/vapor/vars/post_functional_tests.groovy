def call(Map info)
{
    delete_cluster(info)
    cleanWs(disableDeferredWipeout: true, deleteDirs: true)
    node('built-in') {
	script {
	    // Show main build URL rather than full text
	    info['emailOptions'] = ['showTop']
	    projectFinishUp(info)
	}
    }
}
