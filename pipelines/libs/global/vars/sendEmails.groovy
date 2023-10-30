// Return true if we are running on the 'main' branch for
// all libraries.
def is_main_lib()
{
    def ret = true

    def envAll = getContext( hudson.EnvVars )
    envAll.collect { k, v ->
	if (k.startsWith("library")) {
	    println("LIB: ${k} = ${v}")
	    if (v != "main") {
		ret = false
	    }
	}
    }

    return ret
}

// Returns a value from a map if it exists, else a default.
// 'normal' is untyped so it will take an int or a string
def ifExists(Map i, normal, String key)
{
    if (i.containsKey(key)) {
	return i[key]
    } else {
	return normal
    }
}

// Make a suffix character depending on the value of
// a variable (eg for adding 's' to ones)
def makeSuffix(String suffix, Closure c)
{
    if (c()) {
	return suffix
    } else {
	return ''
    }
}

// Send completion emails
def call(Map info)
{
    def nonvoting_fail = ifExists(info, 0, 'nonvoting_fail')
    def voting_fail = ifExists(info, 0, 'voting_fail')
    def stages_fail = ifExists(info, 0, 'stages_fail')
    def nonvoting_run = ifExists(info, 0, 'nonvoting_run')
    def voting_run = ifExists(info, 0, 'voting_run')
    def stages_run = ifExists(info, 0, 'stages_run')
    def state = ifExists(info, 'script error', 'state')
    def project = ifExists(info, env.BUILD_TAG, 'project')
    def branch = ifExists(info, env.BRANCH_NAME, 'branch')
    def email_addrs = ''

    if (state == 'build-ignored') {
	println('build has been ignored, not sending emails')
	return
    }
    // Did we REALLY (like properly) fail??
    if (currentBuild.result == 'FAILURE') {
	state = 'failure'
    }

    // A script exception was logged
    if (info['exception_text'] != '') {
	info['exception_text'] = "\nPlease report the following errors to your friendly local Jenkins admin (though they have probably already seen them and are already panicking).\n\n" +
	    info['exception_text']
	state ='Jenkins exception'
    }

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
    def email_title = ''
    if (is_main_lib()) {
	email_title = "[jenkins] ${info['project']} ${branch} (build ${env.BUILD_ID})"
    } else {
	// Don't spam everybody with our test results
	email_title = "[jenkins][cidev] ${info['project']} ${branch} (build ${env.BUILD_ID})"
	email_addrs = "fdinitto@redhat.com, ccaulfie@redhat.com"
    }

    // Add links to coverity scans
    if (info['cov_results_urls'].size() > 0) {
	def cov_urls = '\nCoverity results:\n'
	for (u in info['cov_results_urls']) {
	    cov_urls += "http://ci.kronosnet.org/${u}\n"
	}
	// A bit of a code mess but it keeps the emails tidy
	if (info['email_extra_text'] != '') {
	    info['email_extra_text'] += '\n'
	}
	info['email_extra_text'] += cov_urls
    }

    // Not everyone generates split logs */
    def split_logs = ''
    if (info.containsKey('have_split_logs')) {
	split_logs = "\nSplit logs: ${env.BUILD_URL}artifact/"
    }

    // Not every finds the consoleText useful
    def console_log = "${env.BUILD_URL}consoleText"
    if (info['emailOptions'].contains('showConsole')) {
	console_log = "${env.BUILD_URL}pipeline-console"
    }

    // Show why we were initiated
    def runreason = ''
    if (currentBuild.getBuildCauses().shortDescription.size() > 0) {
	runreason = "Run reason: ${currentBuild.getBuildCauses().shortDescription[0]}"
    }

    def email_trailer = """${runreason}
Total runtime: ${jobDuration}
${info['email_extra_text']}${split_logs}
Full log:   ${console_log}
${info['exception_text']}
"""

    // Make it look nice
    def voting_colon = makeSuffix(':', {voting_fail > 0} )
    def nonvoting_colon = makeSuffix(':', {nonvoting_fail > 0} )
    def stages_colon = makeSuffix(':', {stages_fail > 0} )
    def voting_s = makeSuffix('s', {voting_fail != 1} )
    def nonvoting_s = makeSuffix('s', {nonvoting_fail != 1} )
    def stage_s = makeSuffix('s', {stages_fail != 1} )

    // Now build the email bits
    def subject = ''
    def body = ''
    if (state == 'success' || state == 'completed') {
	// If this pipeline has 'stages' rather than voting/non-voting, then show 'stages' failed
	// FN testing jobs 'complete' but can have stage failures.
	if (stages_fail > 0) {
		subject = "${email_title} completed with state: ${state}"
		body = """
${stages_fail}/${stages_run} Stage${stage_s} failed${stages_colon} ${info['stages_fail_nodes']}

${email_trailer}
"""
	} else if (nonvoting_fail > 0) {
	    // Only non-voting fails
	    subject = "${email_title} succeeded but with ${nonvoting_fail}/${nonvoting_run} non-voting fail${nonvoting_s}"
	    body = """
Failed job${nonvoting_s}: ${info['nonvoting_fail_nodes']}

${email_trailer}
"""
	} else {
	    // Just a normal success
	    subject = "${email_title} ${state}"
	    body = email_trailer
	}

	// Failures...
    } else if (voting_fail == 0) {
	// Failed but no voting/nonvoting jobs
	subject = "${email_title} ${state}"
	body = email_trailer
    } else {
	// Normal failure with voting/nonvoting jobs
	subject = "${email_title} completed with state: ${state}"
	body = """
${nonvoting_fail}/${nonvoting_run} Non-voting fail${nonvoting_s}${nonvoting_colon} ${info['nonvoting_fail_nodes']}
${voting_fail}/${voting_run} Voting fail${voting_s}${voting_colon} ${info['voting_fail_nodes']}

${email_trailer}
"""
    }

    // Dump the email text to the log, so it doesn't get lost
    println("""
email contents:

To: ${email_addrs}
ReplyTo: ${email_replyto}
Subject: ${subject}
${body}
""")

    // Actually send it
    mail to: email_addrs,
	replyTo: email_replyto,
	subject: subject,
	body: body
}
