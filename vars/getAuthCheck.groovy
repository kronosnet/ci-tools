// Check if a PR has been submitted by an authorized user
def call() {

    sh 'env|sort'
    bc = currentBuild.getBuildCauses()
    println("Build Cause: ${bc}")

    // Triggered by admin on the website
    userEvent = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')
    if (!userEvent.size().equals(0)) {
	return true
    }

    // Triggered by admin comment in the PR
    userEvent = currentBuild.getBuildCauses('com.adobe.jenkins.github_pr_comment_build.GitHubPullRequestCommentCause') 
    if (!userEvent.size().equals(0)) {
	return true
    }

    // Commit from a branch
    branchEvent = currentBuild.getBuildCauses('jenkins.branch.BranchEventCause')
    if (!branchEvent.size().equals(0) && env.BRANCH_NAME == "main") {
	// This is a merge into main, so allowed.
	// Note that this still works if the user's branch is called "main" because
	// Jenkins creates its own branch named after the PR
	return true;
    }
    
    // Caused by a PR. Check it's from a valid user
    valid_admins = getValidPRUsers()
    if (valid_admins.contains(env.CHANGE_AUTHOR)) {
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

    // If we get this far then it's been approved. Abort is the other option!
    return true
}
