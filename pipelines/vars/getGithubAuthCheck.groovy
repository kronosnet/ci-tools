// Check if a PR has been submitted by an authorized user
def call(Map params) {
    // requirered param
    isPullRequest = params["isPullRequest"]

    sh 'env|sort'

    println("isPullRequest: ${isPullRequest}")

    bc = currentBuild.getBuildCauses()
    println("Build Cause: ${bc}")

    // Triggered by admin on the website
    userEvent = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')
    if (!userEvent.size().equals(0)) {
	println("Build Triggered from Jenkins web UI")
	return true
    }

    if (isPullRequest == false) {
	println("Build Triggered by merge into branch")
	// This is a merge into main, so allowed.
	// Note that this still works if the user's branch is called "main" because
	// Jenkins creates its own branch named after the PR
	return true;
    }

    // Triggered by admin comment in the PR
    userEvent = currentBuild.getBuildCauses('com.adobe.jenkins.github_pr_comment_build.GitHubPullRequestCommentCause')
    if (!userEvent.size().equals(0)) {
	println("Build Triggered from PR comment")
	return true
    }

    // Caused by a PR. Check it's from a valid user
    // Get github collaborators list
    valid_admins = getGithubCollaborators(env.GIT_URL)
    // Any extras defined for the project
    valid_admins += getValidPRUsers()
    // Global admins
    valid_admins += getGlobalAdminUsers()
    println("valid admins: ${valid_admins}")

    if (valid_admins.contains(env.CHANGE_AUTHOR)) {
	println("Build Triggered by PR authorized user")
	return true
    }

    //
    // PR from an unknown user - get approval to run this
    // If this is aborted by admin, then it can be re-run
    // using jenkins comments as before.
    //

    // Put a message in github/pagure that links to this job run
    clusterLibSendReply("github", "Can one of the admins check and authorise this run please: ${env.BUILD_URL}input")

    // Ask for approval
    echo "Approval needed from Jenkins administrator"
    result = input(message: "Please verify this is safe to run", ok: "OK",
		   submitterParameter: 'submitter')
    println(result)

    println("Build Triggered by PR manual approval")

    // If we get this far then it's been approved. Abort is the other option!
    return true
}
