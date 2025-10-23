// Check if a PR has been submitted by an authorized user
def call(Map params) {
    // requirered param
    def isPullRequest = params['isPullRequest']

    sh 'env|sort'

    println("isPullRequest: ${isPullRequest}")

    def bc = currentBuild.getBuildCauses()
    println("Build Cause: ${bc}")

    // Triggered by admin on the website
    def userEvent = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')
    if (!userEvent.size().equals(0)) {
	println("Build Triggered from Jenkins web UI")
	return true
    }

    // Triggered by Jenkins reindex event
    userEvent = currentBuild.getBuildCauses('jenkins.branch.BranchIndexingCause')
    if (!userEvent.size().equals(0)) {
	println("Build Triggered from re-index event")
	return true
    }

    if (isPullRequest == false) {
	println("Build Triggered by merge into branch")
	// This is a merge into main, so allowed.
	// Note that this still works if the user's branch is called "main" because
	// Jenkins creates its own branch named after the PR
	return true
    }

    // Triggered by admin comment in the PR
    userEvent = currentBuild.getBuildCauses('com.adobe.jenkins.github_pr_comment_build.GitHubPullRequestCommentCause')
    if (!userEvent.size().equals(0)) {
	println("Build Triggered from PR comment")
	return true
    }

    // Caused by a PR. Check it's from a valid user
    // Get github collaborators list
    def valid_admins = getCollaborators(env.CHANGE_URL)
    // Any extras defined for the project
    valid_admins += getValidPRUsers()
    // Global admins
    valid_admins += getGlobalAdminUsers()
    println("valid admins: ${valid_admins.unique()}")

    if (valid_admins.contains(env.CHANGE_AUTHOR)) {
	println("Build Triggered by PR authorized user")
	return true
    }

    //
    // PR from an unknown user - get approval to run this
    // If this is aborted by admin, then it can be re-run
    // using jenkins comments as before.
    // Timeouts are day-of-week specific, weekend jobs will get 72 hrs,
    // weekdays just 24hours, otherwise the queue gets too big.
    //
    // Groovy days start on Sunday (1) and end on Saturday(7)
    //
    def day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    def long timeoutInMinutes = 1440 // 24 hours
    if (day in [1,6,7]) { // Friday is honorarily the 'weekend'
	timeoutInMinutes = 4320 // 72 hours
    }

    // Put a message in github/pagure that links to this job run
    postPRcomment("Can one of the project admins check and authorise this run please: ${env.BUILD_URL}input")

    // Ask for approval
    echo "Approval needed from project administrator, waiting for ${timeoutInMinutes / 60} hours before aborting"

    def long startTime = System.currentTimeMillis()
    try {
	timeout(time: timeoutInMinutes, unit: 'MINUTES') {
	    result = input(message: "Please verify this is safe to run", ok: "OK",
			   submitterParameter: 'submitter')
	}
    } catch (err) {
	def long timePassed = System.currentTimeMillis() - startTime
	if (timePassed >= timeoutInMinutes * 60000) {
            echo 'Wait for admin response timed out'

	    email_addrs = getEmails()
	    if (email_addrs != '') {
		email_addrs += ','
	    }
	    email_addrs += 'commits@lists.kronosnet.org'
	    mail to: email_addrs,
		subject: "${env.BUILD_TAG} from user ${env.CHANGE_AUTHOR} - timeout-out waiting for admin response"
	    body: "see ${env.BUILD_URL}"
	}
        throw err
    }
    println(result)

    println("Build Triggered by PR manual approval")

    // If we get this far then it's been approved. Abort is the other option!
    return true
}
