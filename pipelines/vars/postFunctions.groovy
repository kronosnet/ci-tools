def call(Map info) {
    // Called at the end of the pipeline
    nonvoting_fail = 0
    voting_fail = 0
    state = "unknown"

    if (info.containsKey('voting_fail')) {
	voting_fail = info['voting_fail']
    }
	if (info.containsKey('nonvoting_fail')) {
	nonvoting_fail = info['nonvoting_fail']
    }

    if (info.containsKey("state")) {
	state = info["state"]
    }

    // getEmails() is allowed to retrun ''
    email_addrs = getEmails()
    if (email_addrs != '') {
	email_addrs += ','
    }
    if (state == "success" && nonvoting_fail > 0) {
	email_addrs += 'commits@lists.kronosnet.org'
	mail to: email_addrs,
	    subject: "${env.BUILD_TAG} succeeded but with non-voting fails",
	    body: "Non-voting fails: ${nonvoting_fail}\nsee ${env.BUILD_URL}pipeline-console/"
    } else {
	email_addrs += 'commits@lists.kronosnet.org'
	mail to: email_addrs,
	    subject: "${env.BUILD_TAG} completed with state: ${state}",
	    body: "see ${env.BUILD_URL}pipeline-console/"
    }
}
