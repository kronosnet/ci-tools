def call(Map info) {
    // Called at the end of the pipeline
    nonvoting_fail = 0
    voting_fail = 0
    stages_fail = 0
    state = "unknown"
    email_addrs = ''
    project = env.BUILD_TAG
    branch = env.BRANCH_NAME

    if (info.containsKey('voting_fail')) {
	voting_fail = info['voting_fail']
    }
    if (info.containsKey('nonvoting_fail')) {
	nonvoting_fail = info['nonvoting_fail']
    }
    if (info.containsKey('stages_fail')) {
	stages_fail = info['stages_fail']
    }
    if (info.containsKey('state')) {
	state = info['state']
    }
    if (info.containsKey('project')) {
	project = info['project']
    }
    if (info.containsKey('branch')) {
	branch = info['branch']
    }

    // Get the per-project email option ('all', 'none', 'only-failures')
    email_opts = getEmailOptions()
    println("Project email_opts: ${email_opts}")

    // Get the per-project email addresses (if wanted), then add the 'always' one.
    // This looks like it could be 'simplified' into one 'if, but I argue it's clearer
    // this way, as we deal in positive conditions only
    if (state == 'success' && email_opts == 'only-failures') {
	println('email_option is "only-failures" and pipeline has succeded, only default email sent')
    } else {
	if ((email_opts == 'all' || email_opts == '') ||
	    (state == 'failure')) {
	    email_addrs = getEmails()
	    if (email_addrs != '') {
		email_addrs += ','
	    }
	}
    }
    email_addrs += 'commits@lists.kronosnet.org'
    println("Sending email to ${email_addrs}")

    // Projects can override the email Reply-To header too
    email_replyto = getEmailReplyTo()
    if (email_replyto == '') {
	email_replyto = 'devel@lists.kronosnet.org' // default
    }
    println("reply-to: ${email_replyto}")

    // Remove "and counting" from the end of the time string
    duration = currentBuild.durationString
    jobDuration = duration.substring(0, duration.length() - 13)

    email_title = "[jenkins] ${info['project']} ${branch} (build ${env.BUILD_ID})"
    email_trailer = """
total runtime = ${jobDuration}
See ${env.BUILD_URL}pipeline-console/
"""

    // Now actually send the email
    if (state == "success") {
	if (nonvoting_fail > 0) {
	    emailext to: email_addrs,
		replyTo: "${email_replyto}",
		subject: "${email_title} succeeded but with ${nonvoting_fail} non-voting fails",
		body: "${email_trailer}"
	} else {
	    mail to: email_addrs,
		replyTo: "${email_replyto}",
		subject: "${email_title} succeeded",
		body: "${email_trailer}"
	}
    } else {
	// If this pipeline has no voting/non-voting options, then show as 'stages' failed
	if (stages_fail > 0) {
	    emailext to: email_addrs,
		replyTo: "${email_replyto}",
		subject: "${email_title} completed with state: ${state}",
		body: """
${stages_fail} Stages failed
${email_trailer}
"""
	} else {
	    emailext to: email_addrs,
		replyTo: "${email_replyto}",
		subject: "${email_title} completed with state: ${state}",
		body: """
${nonvoting_fail} Non-voting fails
${email_trailer}
"""
	}
    }
}
