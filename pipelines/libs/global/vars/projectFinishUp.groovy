// Called at the end of a pipeline run (post/always)
// to tidy up and send emails
def call(Map info)
{
    // Clean up the tarball containing the sources
    if (info['tarfile'] != null) {
	// If this fails, tough.
	try {
	    sh("rm /var/tmp/jenkins-sources/${info['tarfile']}")
	} catch (err) {}
    }

    // Unlock any flocks
    RWLock(info, 'UNLOCK')

    // Clean up any straggling sub-jobs
    if (info['subjobs'].size() > 0) {
	Jenkins.instance.getAllItems(Job).each {
	    for (b in it.getBuilds()) {
		if (b.isInProgress()) {
		    def String name = b
		    if (info['subjobs'].contains(name)) {
			println("Stopping job: "+b)
			b.doStop()
		    }
		}
	    }
	}
    }

    // Do ... err ... well you get the idea
    sendEmails(info)
}
