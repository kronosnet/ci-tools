
// Run a groovy Closure and store the results in an artifact
// Rethrows any exceptions
def call (Map info, String logfile, Closure cmd)
{
    def failed = 0
    def new_logfile = ''
    def caught_exception = null

    info['have_split_logs'] = true

    try {
	tee (logfile) {
	    cmd()
	}
    }
    catch (e) {
	failed = 1
	caught_exception = e
    }

    // This sed removes the 'bold' links which look a bit like an exposed encrypted thing (but aren't)
    def sanitised_logfile = "TMP_${logfile}"
    sh """#!/bin/citbash -e
                   sed \$'s/\\033\\\\[8m.*\\033\\\\[0m//' <${logfile} >${sanitised_logfile}
       """
    if (failed == 1) {
	new_logfile = "FAILED_${logfile}"
	// Save it for the email
	if (!info.containsKey('failedlogs')) {
	    info['failedlogs'] = []
	}
	info['failedlogs'] += "${new_logfile}"
    } else {
	new_logfile = "SUCCESS_${logfile}"
    }
    sh "mv ${sanitised_logfile} ${new_logfile}"
    archiveArtifacts artifacts: "${new_logfile}", fingerprint: false

    if (failed && !info.containsKey('runWithArtifactsDontRethrow')) {
	throw (caught_exception)
    }
}
