// Returns "1" if this branch needs to be installed, "0" if not
// Relies on project-specific function getInstallBranches()
def call(String branch)
{
    install_branches = getInstallBranches()
    if (install_branches.contains(branch)) {
	println("installing branch ${branch}")
	return "1"
    }
    else {
	println("Not installing branch ${branch}")
	return "0"
    }

}
