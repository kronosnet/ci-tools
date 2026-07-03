// Returns 1 if this branch and coverity scan results need to be published, 0 if not
// Relies on project-specific function getPublishBranches()
def call(String branch)
{
    def publish_branches = getPublishBranches()
    if (publish_branches.contains(branch)) {
	println("RPMs and Coverity scan results will be published for the branch ${branch}")
	return 1
    }
    else {
	println("RPMs and Coverity scan results will not be published for the branch ${branch}")
	return 0
    }

}
