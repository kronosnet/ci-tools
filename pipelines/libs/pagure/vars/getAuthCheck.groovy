// Check if a PR has been submitted by an authorized user
def call(Map params) {
    // requirered param
    // upstream_repo is needed to determine the PR user
    // and for postPRcomment to generate the URL
    // to post comments
    def upstream_repo = params['upstream_repo']
    def isPullRequest = params['isPullRequest']

    sh 'env|sort'

    println("upstream_repo: ${upstream_repo}")
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

    // Triggered by admin comment in the PR is not supported on pagure.
    // pagure offers a comment interface, but it is translated internally
    // to a job notification to Jenkins with the usual variables from the
    // pipeline. Even an admin asking to "Rerun CI" from the web UI
    // will need to OK/ABORT a build of a PR done by an external contributor.

    def prrepo = env.REPO
    println("PR repo: ${prrepo}")

    // if the PR is done from a user with read/write access to the repo
    // it should be safe enough to run.
    if (env.REPO == upstream_repo) {
	println("Build Triggered by PR from within upstream repo")
	return true
    }

    // user that has issued a PR from an external fork
    def s = prrepo.split("/")
    pruser = s[4]

    println("PR user: ${pruser}")

    // Caused by a PR. Check it's from a valid user
    // Get pagure collaborators list
    def valid_admins = getCollaborators(upstream_repo)
    // Any extras defined for the project
    valid_admins += getValidPRUsers()
    // Global admins
    valid_admins += getGlobalAdminUsers()
    println("valid admins: ${valid_admins}")

    if (valid_admins.contains(pruser)) {
	println("Build Triggered by PR authorized user")
	return true
    }

    //
    // PR from an unknown user - get approval to run this
    // If this is aborted by admin, then it can be re-run
    // using jenkins comments as before.
    //

    // Put a message in github/pagure that links to this job run
    postPRcomment(upstream_repo, "Can one of the admins check and authorise this run please: ${env.BUILD_URL}input")

    // Ask for approval
    echo "Approval needed from Jenkins administrator"
    def result = input(message: "Please verify this is safe to run", ok: "OK",
		   submitterParameter: 'submitter')
    println(result)

    println("Build Triggered by PR manual approval")

    // If we get this far then it's been approved. Abort is the other option!
    return true
}
