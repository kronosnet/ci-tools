// perform pagure git checkout
// when Jenkins handles the checkout of Jenkins files and source code
// it is all done during Declerative checkout stage and Jenkins takes
// care to place all the code in different paths.
// with this pipeline simple mode, the checkout only has info about ci-tools
// and the our workspace collides.
// https://itnext.io/jenkins-tutorial-part-10-work-with-git-in-pipeline-b5e42f6d124b
//
// we are inside ci-tools checkout here, dive in another directory
// to avoid collitions

def call(Map info)
{
    def tarfile = "sources-${env.BUILD_TAG}.tar.gz"
    println("tarfile = ${tarfile}, node=${env.NODE_NAME}")

    if (env.NODE_NAME == 'built-in') {
	dir (info['project']) {
	    checkout([$class: 'GitSCM',
		      branches: [[name: "${info['checkout']}"]],
		      doGenerateSubmoduleConfigurations: false,
		      extensions: [[$class: 'CleanCheckout']],
		      submoduleCfg: [],
		      userRemoteConfigs: [[url: "${info['upstream_repo']}"]]])

	    if (info['isPullRequest']) {
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
	    sh('mkdir -p /var/tmp/jenkins-sources')
	    sh("tar --exclude=${tarfile} -czf /var/tmp/jenkins-sources/${tarfile} .")
	    info['tarfile'] = tarfile
	}
    } else {
	dir (info['project']) {
	    // This is needed to create the directory before we scp stuff into it
	    shNoTrace('touch .', '')

	    def buildhost = env.NODE_NAME
	    def workspace = env.WORKSPACE

	    // Get the tarball from the Jenkins host
	    // Random delay to stop hitting the server too hard
	    sleep(new Random().nextInt(15))
	    node('built-in') {
		sh("scp /var/tmp/jenkins-sources/${tarfile} ${buildhost}:${workspace}/${info['project']}")
	    }
	    sh "tar --no-same-owner -xzf ${tarfile}"
	    sh "rm ${tarfile}"
	}
    }
}
