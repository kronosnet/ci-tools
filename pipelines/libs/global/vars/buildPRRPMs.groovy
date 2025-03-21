// Return true if we need to build PR RPMs for this job
// mandatory params:
// isPullRequest: (true/false)
// branch: target branch we are building for
//
// Calls into getPRBranchRPM() in the project lib
//
def call(Map params)
{
    def isPullRequest = params["isPullRequest"]
    def branch = params["branch"]

    // Calls into the project-specific library
    def branchnames = getPRBranchRPM()
    println("Publish PR RPMs Branchnames: ${branchnames}")

    if (isPullRequest == true) {
	if (branchnames.contains(branch)) {
	    return 1
	}
    }
    return 0
}
