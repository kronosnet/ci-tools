// Set up the build information for a pipeline
// All the non-pipeline-specific things are set
// up here in a dictionary called 'info'.
//
// ALL methods in this function must be available on
// all projects unless we can check if a given method exists
// https://gerg.dev/2020/11/jenkins-check-whether-a-dsl-method-exists/ ?
//
// We also call get[Github]AuthCheck() to verify that the
// user is allowed to run the pipeline at all
//
// This var is Github-specific in a few ways. Whether we
// add conditionals for Pagure or make them a different
// file depends on how different it is!
def call(String project, String draft_override)
{
    sh 'env|sort'

    // Github specific
    isPullRequest = env.CHANGE_ID ? true : false
    is_draft = false

    // Create the main dictionary
    def info = ['isPullRequest': isPullRequest]

    // Validate the user. This should Abort if disallowed.
    cred_uuid = getCredUUID()
    withCredentials([gitUsernamePassword(credentialsId: cred_uuid, gitToolName: 'Default')]) {
	info['authcheck'] = getGithubAuthCheck(['isPullRequest': isPullRequest])
    }

    // Set parameters for the sub-jobs.
    if (isPullRequest) {
	info['target_branch'] = env.CHANGE_TARGET
	info['target'] = env.CHANGE_TARGET
	info['pull_id'] = env.CHANGE_ID
	info['actual_commit'] = env.GIT_COMMIT
	info['install'] = 0
	info['maininstall'] = 0
	info['stableinstall'] = 0
	info['publish_pr_rpm'] = buildPRRPMs(['isPullRequest': isPullRequest, 'branch': info['target']])

	// Check for PRs marked 'draft' - this is Github-specific
	is_draft = pullRequest.isDraft()
	if ((is_draft) && (draft_override == '0')) {
	    // Default for most HA jobs - abort
	    currentBuild.result = 'ABORTED'
	    error('PR is marked as draft - pipeline will not run')
	}
        // For special jobs (manual override)
	// if (info['is_draft']) {
	//     set_some_variables_for_build_or_something
	//     is_draft = false
	// }
    } else {
	info['actual_commit'] = "origin/${env.BRANCH_NAME}"
	info['target'] = env.BRANCH_NAME
	info['install'] = isThisAnInstallBranch(info['target'])
	if ("${info['install']}" == '1') {
	    if ("${info['target']}" == 'main') {
		info['maininstall'] = 1
		info['stableinstall'] = 0
	    } else {
		info['maininstall'] = 0
		info['stableinstall'] = 1
	    }
	}
    }
    info['is_draft'] = is_draft
    info['covopts'] = getCovOpts(info['target'])

    println("info map: ${info}")
    return info
}
