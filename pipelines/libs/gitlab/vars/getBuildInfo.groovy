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
def call(String project, Map info)
{
    getBuildInfoCommon(info)
    sh "env|sort"

    // GitLab specific
    def isPullRequest = env.CHANGE_ID ? true : false
    def is_draft = false

    // Create the main dictionary
    info['isPullRequest'] = isPullRequest
    info['project'] = project

    // Validate the user. This should Abort if disallowed.
    // def cred_uuid = getCredUUID()
    // withCredentials([string(credentialsId: cred_uuid, variable: 'GIT_PASSWORD')]) {
    //     info['authcheck'] = getAuthCheck(['isPullRequest': isPullRequest])
    // }

	info['authcheck'] = true

    // Display/kill any old duplicates of this job that are running
    killDuplicateJobs(info)

    // Set parameters for the sub-jobs.
    if (isPullRequest) {
	info['target'] = env.CHANGE_TARGET
	info['pull_id'] = env.CHANGE_ID
	info['source'] = env.CHANGE_BRANCH
	info['install'] = 0
	info['maininstall'] = 0
	info['stableinstall'] = 0
	info['publishrpm'] = buildPRRPMs(['isPullRequest': isPullRequest, 'branch': info['target']])

	// Draft MRs can be forced to run with a GitLab comment
	def runreason = ''
	if (currentBuild.getBuildCauses().shortDescription.size() > 0) {
	    runreason = currentBuild.getBuildCauses().shortDescription[0]
	}

	// Check for MRs marked 'draft' or 'WIP' (Work In Progress)
	// GitLab sets GITLAB_OA_WIP to 'true' or 'false'
	is_draft = (env.GITLAB_OA_WIP == 'true')
	if ((is_draft) && (env.ISDRAFTOVERRIDE == null) &&
	    !runreason.contains("retest this please")) {
	    // Default for most HA jobs - abort
	    currentBuild.result = 'ABORTED'
	    error('MR is marked as draft/WIP - pipeline will not run')
	} else {
	    is_draft = false
	}
    } else {
	info['target'] = env.BRANCH_NAME
	info['source'] = env.BRANCH_NAME
	info['pull_id'] = 1
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
    }
    info['is_draft'] = is_draft

    // Copy the SCM into artifacts so that other nodes can use them.
    // catchError makes sure that info[:] is returned even if it fails,
    // so that sendEmails knows what to do
    catchError {
	getSCM(info)
    }
    println("info map: ${info}")
    return info
}
