// Send completion emails
def call(Map info)
{
    def nonvoting_fail = 0
    def voting_fail = 0
    def stages_fail = 0
    def nonvoting_run = 0
    def voting_run = 0
    def stages_run = 0
    def state = "script error"
    def email_addrs = ''
    def project = env.BUILD_TAG
    def branch = env.BRANCH_NAME

    if (info.containsKey('voting_fail')) {
	voting_fail = info['voting_fail']
    }
    if (info.containsKey('nonvoting_fail')) {
	nonvoting_fail = info['nonvoting_fail']
    }
    if (info.containsKey('stages_fail')) {
	stages_fail = info['stages_fail']
    }
    if (info.containsKey('voting_run')) {
	voting_run = info['voting_run']
    }
    if (info.containsKey('nonvoting_run')) {
	nonvoting_run = info['nonvoting_run']
    }
    if (info.containsKey('stages_run')) {
	stages_run = info['stages_run']
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
    if (!info.containsKey('email_extra_text')) {
	info['email_extra_text'] = ''
    }

    // Thi should go back in postFunctions, we'll do that
    // when everything is upgraded to full pipelines
    sh "rm /var/www/ci.kronosnet.org/buildsources/${info['tarfile']}"
    
    // Get the per-project email option ('all', 'none', 'only-failures')
    def email_opts = getEmailOptions()
    println("Project email_opts: ${email_opts}")

    // Get the per-project email addresses (if wanted), then add the 'always' one.
    // This looks like it could be 'simplified' into one 'if', but I argue it's clearer
    // this way, as we deal in positive conditions only
    if (state == 'success' && email_opts == 'only-failures') {
	println('email_option is "only-failures" and pipeline has succeeded, only default email sent')
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
    def email_replyto = getEmailReplyTo()
    if (email_replyto == '') {
	email_replyto = 'devel@lists.kronosnet.org' // default
    }
    println("reply-to: ${email_replyto}")

    // Remove "and counting" from the end of the duration string
    def duration = currentBuild.durationString
    def jobDuration = duration.substring(0, duration.length() - 13)

    // Build email strings that apply to all statuses
    def email_title = "[jenkins] ${info['project']} ${branch} (build ${env.BUILD_ID})"
    def email_trailer = """
total runtime: ${jobDuration}
${info['email_extra_text']}
Split logs: ${env.BUILD_URL}artifact/
Full log:   ${env.BUILD_URL}consoleText/
"""

    // Make it look nice
    def voting_colon = ''
    if (voting_fail > 0) {
	voting_colon = ':'
    }
    def nonvoting_colon = ''
    if (nonvoting_fail > 0) {
	nonvoting_colon = ':'
    }
    def voting_s = 's'
    if (voting_fail == 1) {
	voting_s = ''
    }
    def nonvoting_s = 's'
    if (nonvoting_fail == 1) {
	nonvoting_s = ''
    }
    def stage_s = 's'
    if (stages_fail == 1) {
	stage_s = ''
    }

    // Now actually send the email
    if (state == "success") {
	if (nonvoting_fail > 0) {
	    mail to: email_addrs,
		replyTo: "${email_replyto}",
		subject: "${email_title} succeeded but with ${nonvoting_fail}/${nonvoting_run} non-voting fail${nonvoting_s}",
		body: """
failed job${nonvoting_s}: ${info['nonvoting_fail_nodes']}
${email_trailer}
"""
	} else {
	    mail to: email_addrs,
		replyTo: "${email_replyto}",
		subject: "${email_title} succeeded",
		body: "${email_trailer}"
	}
    } else {
	// If this pipeline has no voting/non-voting options, then show as 'stages' failed
	if (stages_fail > 0) {

	    mail to: email_addrs,
		replyTo: "${email_replyto}",
		subject: "${email_title} completed with state: ${state}",
		body: """
${stages_fail}/${stages_run} Stage${stage_s} failed
${email_trailer}
"""
	} else {
	    mail to: email_addrs,
		replyTo: "${email_replyto}",
		subject: "${email_title} completed with state: ${state}",
		body: """
${nonvoting_fail}/${nonvoting_run} Non-voting fail${nonvoting_s}${nonvoting_colon} ${info['nonvoting_fail_nodes']}
${voting_fail}/${voting_run} Voting fail${voting_s}${voting_colon} ${info['voting_fail_nodes']}
${email_trailer}
"""
	}
    }
}
