// retryFails.groovy
//
// Repeat an operation a set number of times or until it succeeds.
// If it fails every time then (optionally) email someone.
//
// Returns OK if everything went well. See runWithTimeout.groovy for other errors
//
// NB: 'title' should ideally closely match the closure text as we can't print the closure in the email
//
def call(Integer timeout, Integer max_retries, Closure cmd, String title, String fail_email)
{
    def state = [:]
    def r = 'Unknown'

    for (i=0; i<max_retries; i++) {
	r = runWithTimeout(timeout, cmd, state, {}, {})
	if (r == 'OK') {
	    break
	}
    }

    if ((r != 'OK') && (fail_email != null)) {
	println("Sending failure email to ${fail_email}")
	mail to: fail_email,
	    subject: "[jenkins] [cidev] ${title} failed after ${max_retries} retries",
	    body: """${title} on ${env.JOB_NAME} failed
state map: ${state}
see log ${env.BUILD_URL}consoleText
"""
    }
    return r
}
