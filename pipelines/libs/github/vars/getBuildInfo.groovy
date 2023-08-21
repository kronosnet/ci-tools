// Set up the build information for a pipeline
// All the non-pipeline-specific things are set
// up here in a dictionary called 'info'.
//
// ALL methods in this function must be available on
// all projects unless we can check if a given method exists
// https://gerg.dev/2020/11/jenkins-check-whether-a-dsl-method-exists/ ?
//
// We also call getAuthCheck() to verify that the
// user is allowed to run the pipeline at all
def call(String project)
{
    sh 'env|sort'

    // Github specific
    isPullRequest = env.CHANGE_ID ? true : false
    is_draft = false

    // Create the main dictionary
    def info = ['isPullRequest': isPullRequest]
    info['project'] = project
    info['nonvoting_fail'] = 0
    info['nonvoting_fail_nodes'] = ''
    info['voting_fail'] = 0
    info['voting_fail_nodes'] = ''
    info['nonvoting_run'] = 0
    info['voting_run'] = 0
    info['exception_text'] = ''
    info['state'] = 'script error'

    // Validate the user. This should Abort if disallowed.
    cred_uuid = getCredUUID()
    withCredentials([gitUsernamePassword(credentialsId: cred_uuid, gitToolName: 'Default')]) {
	info['authcheck'] = getAuthCheck(['isPullRequest': isPullRequest])
    }

    // Display/kill any old duplicates of this job that are running
    info['email_extra_text'] = ''
    killDuplicateJobs(info)

    // Set parameters for the sub-jobs.
    if (isPullRequest) {
	info['actual_commit'] = env.GIT_COMMIT
	info['target'] = env.CHANGE_TARGET
	info['target_branch'] = env.CHANGE_TARGET
	info['pull_id'] = env.CHANGE_ID
	info['install'] = 0
	info['maininstall'] = 0
	info['stableinstall'] = 0
	info['covinstall'] = 0
	info['publish_rpm'] = 0  // TODO Remove once all in new pipelines
	info['publish_pr_rpm'] = buildPRRPMs(['isPullRequest': isPullRequest, 'branch': info['target']])
	info['publishrpm'] = info['publish_pr_rpm']

	// Check for PRs marked 'draft' - this is Github-specific
	is_draft = pullRequest.isDraft()
	if ((is_draft) && (env.ISDRAFTOVERRIDE == null)) {
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
	info['target_branch'] = ''
	info['pull_id'] = 1
	info['publish_rpm'] = 1 // TODO Remove once all in new pipelines
	info['publishrpm'] = 1
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
	info['covinstall'] = 1
    }
    info['is_draft'] = is_draft
    info['covopts'] = getCovOpts(info['target'])

    // Make sure the params are in here so they get propogated to the scripts
    info['bootstrap'] = params.bootstrap
    info['fullrebuild'] = params.fullrebuild

    // fullrebuild overrides some things
    if (info['fullrebuild'] == '1') { // params are always strings
	info['install'] = 0
	info['covinstall'] = 0
	info['maininstall'] = 0
	info['stableinstall'] = 0
	info['publish_rpm'] = 0 // TODO Remove once all in new pipelines
	info['publishrpm'] = 0
    }

    // Copy the SCM into artifacts so that other nodes can use them.
    // catchError makes sure that info[:] is returned even if it fails,
    // so that sendEmails knows what to do
    catchError {
	getSCM(info)
    }
    println("info map: ${info}")
    return info
}
