
// Run a groovy Closure and store the results in an artifact
// Rethrows any exceptions
def call (Map info, String logfile, Closure cmd)
{
    def failed = 0
    def new_logfile = ''
    def caught_exception = null

    info['have_split_logs'] = true

    try {
	echo "DEBUG runWithArtifacts: Starting tee block for ${logfile}"
	tee (logfile) {
	    cmd()
	}
	echo "DEBUG runWithArtifacts: Tee block completed for ${logfile}"
    }
    catch (e) {
	failed = 1
	caught_exception = e
	echo "DEBUG runWithArtifacts: Caught exception in tee block: ${e}"
    }

    // This sed removes the 'bold' links which look a bit like an exposed encrypted thing (but aren't)
    def sanitised_logfile = "TMP_${logfile}"
    echo "DEBUG runWithArtifacts: About to run sed sanitization on ${logfile}"
    sh """#!/bin/citbash -e
                   sed \$'s/\\033\\\\[8m.*\\033\\\\[0m//' <${logfile} >${sanitised_logfile}
       """
    echo "DEBUG runWithArtifacts: Sed sanitization completed"
    if (failed == 1) {
	new_logfile = "FAILED_${logfile}"
	echo "DEBUG runWithArtifacts: Build failed, using logfile ${new_logfile}"
	// Save it for the email
	if (!info.containsKey('failedlogs')) {
	    info['failedlogs'] = []
	}
	info['failedlogs'] += "${new_logfile}"
    } else {
	new_logfile = "SUCCESS_${logfile}"
	echo "DEBUG runWithArtifacts: Build succeeded, using logfile ${new_logfile}"
    }
    echo "DEBUG runWithArtifacts: Moving ${sanitised_logfile} to ${new_logfile}"
    sh "mv ${sanitised_logfile} ${new_logfile}"
    echo "DEBUG runWithArtifacts: Archiving ${new_logfile}"
    archiveArtifacts artifacts: "${new_logfile}", fingerprint: false

    echo "DEBUG runWithArtifacts: Completed, failed=${failed}, runWithArtifactsDontRethrow=${info.containsKey('runWithArtifactsDontRethrow')}"
    if (failed && !info.containsKey('runWithArtifactsDontRethrow')) {
	throw (caught_exception)
    }
}
