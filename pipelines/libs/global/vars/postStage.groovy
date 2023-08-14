// Called after the main stages to build the RPM and covscan repos for this build
def call(Map info)
{
    def publish_timeout = 15 // Minutes

    // Archive the accumulated covscan results
    if (info['fullrebuild'] != '1') { // Covers the case where it might be null too
	stage("Publish Coverity results") {
	    node('built-in') {
		lock('ci-cov-repos') { // This script needs to be serialised
		    timeout (time: publish_timeout, unit: 'MINUTES') {
			sh "~/ci-tools/ci-cov-repos ${info['project']}"
		    }
		}
	    }
	}
    }

    // Archive the accumulated RPMs (IFF all rpm builds suceeded)
    if ((info['buildrpms_failed'] != 1) &&
	(info['publishrpm'] == 1) &&
	(info['fullrebuild'] != '1')) { // Covers the case where it might be null too
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
	currentBuild.result = 'FAILED'
	info['state'] = 'failure'
	error('Failed voting stage(s) caused build to abort')
    }
}
