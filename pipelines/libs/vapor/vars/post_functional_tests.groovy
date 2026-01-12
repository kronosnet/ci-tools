def call(Map info)
{
    // Don't let failure of this prevent emails from being sent
    try {
	delete_cluster(info)
    } catch (err) {
	println("delete_cluster in post failed, you might want to check the workspace")
	info['email_extra_text'] = 'delete_cluster() in post function failed, you might want to check the workspace'
	info['runtest'] = 'Final delete_cluster'
    }

    cleanWs(disableDeferredWipeout: true, deleteDirs: true)
    node('built-in') {
	script {

	    // Show main build URL rather than full text
	    info['emailOptions'] = ['showTop']

	    // If we were started by the weekly job then emails are not needed
	    def runreason = ''
	    if (currentBuild.getBuildCauses().shortDescription.size() > 0) {
		runreason = "Run reason: ${currentBuild.getBuildCauses().shortDescription[0]}"
	    }

	    if (runreason.contains('ha-functional-testing-weekly')) {
		info['emailOptions'] = ['nosend']
		println('job was called from ha-functional-testing-weekly, not sending email')
	    }

	    projectFinishUp(info)
	}
    }
}
