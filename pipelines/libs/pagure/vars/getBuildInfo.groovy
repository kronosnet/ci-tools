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
def call(String project, String upstream_repo)
{
    sh 'env|sort'

    // Jenkins pipeline is configured as "This project is parametrized" and the envvars
    // come from there.
    //
    // pagure specifies 4 envvars to do the whole dance:
    // REPO = source repo of the pull request / build.
    //        this come in the form of either upstream_repo or:
    //        https://pagure.io/forks/$USER/$PROJECT.git
    // BRANCH = for a merge, this points to the branch where the PR is landing
    //          for a PR, this is the branch in the source REPO.
    // BRANCH_TO = for a merge is set to None
    //             for a PR is set to the target branch of the PR in the upstream_repo
    // cause = for a merge is set to the sha1 commit to build (unused at the moment)
    //         for a PR is the PR number (1, 2...)

    // pagure specific
    isPullRequest = env.BRANCH_TO != 'None' ? true : false

    // Create the main dictionary
    def info = ['isPullRequest': isPullRequest]
    info['project'] = project
    info['nonvoting_fail'] = 0
    info['nonvoting_fail_nodes'] = ''
    info['voting_fail'] = 0
    info['voting_fail_nodes'] = ''
    info['nonvoting_run'] = 0
    info['voting_run'] = 0
    info['state'] = 'script error'
    info['email_extra_text'] = ''
    info['exception_text'] = ''

    info['upstream_repo'] = upstream_repo

    // pagure needs credentials only to post comments on PRs.
    // knet-ci-bot user has been created in pagure, and has the API key
    // registered. NOTE API keys expires every 2 years.
    // the current key allows _only_ to post comments on PRs with very strict ACL.
    // the post to comments does NOT require for knet-ci-bot to be part of any groups
    // in any projects.
    cred_uuid = getCredUUID()
    withCredentials([string(credentialsId: cred_uuid, variable: 'paguresecret')]) {
	info['authcheck'] = getAuthCheck(['upstream_repo': upstream_repo, 'isPullRequest': isPullRequest])
    }

    // NOTE: the Github version runs killDuplicate() jobs here
    // but we dont have a Pagure one.

    // Set parameters for the sub-jobs.
    if (isPullRequest) {
	info['actual_commit'] = "origin/${env.BRANCH_TO}"
	info['target_branch'] = env.BRANCH_TO
	info['target'] = env.BRANCH_TO
	info['pull_id'] = env.cause
	info['checkout'] = env.BRANCH_TO
	info['install'] = 0
	info['maininstall'] = 0
	info['stableinstall'] = 0
	info['covinstall'] = 0
	info['publish_rpm'] = 0  // TODO Remove once all in new pipelines
	info['publish_pr_rpm'] = buildPRRPMs(['isPullRequest': isPullRequest, 'branch': info['target']])
	info['publishrpm'] = info['publish_pr_rpm']
	info['jobname'] = "PR-${env.cause}"
	info['branch'] = "PR-${env.cause}"
    } else {
	info['actual_commit'] = "origin/${env.BRANCH}"
	info['target_branch'] = env.BRANCH
	info['target'] = env.BRANCH
	info['pull_id'] = '1'
	if (env.cause == '') {
	    info['checkout'] = env.BRANCH
	} else {
	    info['checkout'] = env.cause
	}
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
	info['publish_rpm'] = 1  // TODO Remove once all in new pipelines
	info['publishrpm'] = 1
	info['jobname'] = "${env.BRANCH} ${env.cause}"
	info['branch'] = "${env.BRANCH}"
    }
    info['covopts'] = getCovOpts(info['target'])
    // because the pipeline has no concept of git checkout, we cannot filter
    // branches to build from Jenkins. This check avoid contributors pushing
    // to upstream_repo branch foo and have "free builds".
    // as for draft, we can only abort the pipeline.
    info['tracking'] = isThisATrackingBranch(info['target'])
    if (info['tracking'] == false) {
	currentBuild.result = 'ABORTED'
	info['state'] = 'build-ignored'
	return info
    }

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
