// Relies on project-specific function getTrackingBranches()
def call(String branch)
{
    def tracking_branches = getTrackingBranches()
    if (tracking_branches.contains(branch)) {
	println("Tracking branch ${branch}")
	return true
    }
    else {
	println("Not tracking branch ${branch}")
	return false
    }
}
