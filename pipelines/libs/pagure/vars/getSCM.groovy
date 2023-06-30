// perform pagure git checkout
def call(Map params)
{
    project = params['project']
    upstream_repo = params['upstream_repo']
    checkout = params['checkout']
    isPullRequest = params['isPullRequest']

    // when Jenkins handles the checkout of Jenkins files and source code
    // it is all done during Declerative checkout stage and Jenkins takes
    // care to place all the code in different paths.
    // with this pipeline simple mode, the checkout only has info about ci-tools
    // and the our workspace collides.
    // https://itnext.io/jenkins-tutorial-part-10-work-with-git-in-pipeline-b5e42f6d124b
    //
    // we are inside ci-tools checkout here, dive in another directory
    // to avoid collitions

    dir(project) {
	checkout([$class: 'GitSCM',
		  branches: [[name: checkout]],
		  doGenerateSubmoduleConfigurations: false,
		  extensions: [[$class: 'CleanCheckout']],
		  submoduleCfg: [],
		  userRemoteConfigs: [[url: upstream_repo]]])

	if (isPullRequest) {
	    sh """
		if [ -n "$BRANCH_TO" ] && [ "$BRANCH_TO" != "None" ]; then
		    if [ -n "$REPO" -a -n "$BRANCH" ]; then
			git config user.email "jenkins@kronosnet.org"
			git config user.name "Sir Jenkins"
			git remote rm proposed 2>/dev/null || true
			git remote add proposed "$REPO"
			git fetch proposed
			git checkout "origin/$BRANCH_TO"
			git checkout -b "PR$cause"
			git merge --log=999 --no-ff -m "Merge PR$cause" "proposed/$BRANCH"
			git show --no-patch
		    fi
		fi
	    """
	}
    }
}
