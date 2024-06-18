// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// localinfo contains all of the build information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]

    props['PARALLELTEST'] = 'no'
    props['RPMDEPS'] = ''

    // pcs-0.10 branch does not Requires any ha upstream packages
    // pcs-0.11 and main are currently aligned
    if ((localinfo['target'] == 'main') ||
	(localinfo['target'] == 'pcs-0.11')) {
	if (agentName.startsWith('rhel') ||
	    agentName.startsWith('fedora') ||
	    agentName.startsWith('centos')) {
	    props['RPMDEPS'] = 'corosynclib-devel pacemaker-libs-devel'
        }

	props['RPMDEPS'] += ' booth corosync-qdevice-devel corosynclib-devel fence-agents-common resource-agents sbd'
    }

    return props
}
