// Called after the main stages to build the RPM and covscan repos for this build
def call(Map info)
{
    // This is useful, but it also makes sure that postStage appears properly in the logs
    // if it has nothing to do. (Jenkins hates quiet scripts)
    println('postStage info='+info)

    def publish_timeout = 15 // Minutes

    // Don't do this for the weekly jobs
    if (info['fullrebuild'] == 0) {
	if (info['cov_results_urls'].size() > 0) { // .. and only when there are some covscan results
	    // Archive the accumulated covscan results
	    stage("Publish Coverity results") {
		node('built-in') {
		    lock('ci-cov-repos') { // This script needs to be serialised
			timeout (time: publish_timeout, unit: 'MINUTES') {
			    sh "~/ci-tools/ci-cov-repos ${info['project']} ${info['covtgtdir']}"
			}
		    }
		}
	    }
	}

	// Archive the accumulated RPMs (IFF all rpm builds suceeded)
	if ((info['buildrpms_failed'] != 1) &&
	    (info['publishrpm'] == 1)) {
	    stage("Publish RPMs") {
		node('built-in') {
		    lock('ci-rpm-repos') { // This script needs to be serialised
			def repopath = "origin/${info['target']}"
			if (info['isPullRequest']) {
			    repopath = "pr/${info['pull_id']}"
			}
			for (ver in info['EXTRAVER_LIST'].stream().distinct().collect()) { // Remove duplicates
			    timeout (time: publish_timeout, unit: 'MINUTES') {
				sh "~/ci-tools/ci-rpm-repos ${info['project']} ${repopath} ${ver}"
			    }
			}
		    }
		}
	    }
	}
    } else {
	println("fullrebuild set - rpm & covscan repos not updated")
    }

    // Fail the pipeline if any voting jobs failed
    info['state'] = 'success'
    if (info['voting_fail'] > 0) {
	currentBuild.result = 'FAILURE'
	info['state'] = 'failure'
	error('Failed voting stage(s) caused build to abort')
    }
}
