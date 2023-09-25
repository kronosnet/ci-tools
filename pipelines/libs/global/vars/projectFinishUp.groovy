// Called at the end of a pipeline run (post/always)
// to tidy up and send emails
def call(Map info)
{
    // Clean up the tarball containing the sources
    if (info['tarfile'] != null) {
	// If this fails, tough.
	try {
	    shNoTrace("rm /var/www/ci.kronosnet.org/buildsources/${info['tarfile']}",
		      "rm <redacted-web-dir>/${info['tarfile']}")
	} catch (err) {}
    }

    // Do ... err ... well you get the idea
    sendEmails(info)
}
