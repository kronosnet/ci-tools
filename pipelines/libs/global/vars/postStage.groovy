// Called after the main stages to build the RPM and covscan repos for this build
def call(Map info)
{
    def publish_timeout = 15 // Minutes

    // Don't do this for the weekly jobs
    if (info['fullrebuild'] != '1') {

	// Archive the accumulated covscan results
	stage("Publish Coverity results") {
	    node('built-in') {
		lock('ci-cov-repos') { // This script needs to be serialised
		    timeout (time: publish_timeout, unit: 'MINUTES') {
			sh "~/ci-tools/ci-cov-repos ${info['project']}"
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
			timeout (time: publish_timeout, unit: 'MINUTES') {
			    sh "~/ci-tools/ci-rpm-repos ${info['project']} ${info['actual_commit']} ${info['EXTRAVER']}"
			}
		    }
		}
	    }
	}
    } else {
	println("fullrebuild set - rpm & covscan repos not updated")
    }

    // Clean up the tarball containing the sources
    if (info['tarfile'] != null) {
	// If this fails, tough.
	try {
	    shNoTrace("rm /var/www/ci.kronosnet.org/buildsources/${info['tarfile']}",
		      "rm <redacted-web-dir>/${info['tarfile']}")
	} catch (err) {}

    }

    // Fail the pipeline if any voting jobs failed
    info['state'] = 'success'
    if (info['voting_fail'] > 0) {
	currentBuild.result = 'FAILURE'
	info['state'] = 'failure'
	error('Failed voting stage(s) caused build to abort')
    }
}
