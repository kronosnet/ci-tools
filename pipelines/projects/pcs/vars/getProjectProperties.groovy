// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// localinfo contains all of the build information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]

    props['MAKEOPTS'] = ''
    props['PARALLELTEST'] = 'no'
    props['MAKEINSTALLOPTS'] = ''
    props['TOPTS'] = ''
    props['EXTRACHECKS'] = ''
    props['EXTERNAL_LD_LIBRARY_PATH'] = ''
    props['DISTROCONFOPTS'] = ''
    props['extraver'] = "pacemaker-${localinfo['pacemakerver']}"

    if (localinfo['target'] == 'main') {
	if (agentName.startsWith('rhel') ||
	    agentName.startsWith('fedora') ||
	    agentName.startsWith('centos')) {
	    props['RPMDEPS'] = 'corosynclib-devel pacemaker-libs-devel'
        }

	if (agentName.startsWith('opensuse-tumbleweed')) {
	    props['RPMDEPS'] = 'corosynclib-devel libpacemaker3-devel'
        }

	if (agentName.startsWith('opensuse-15')) {
	    props['RPMDEPS'] = 'corosynclib-devel libpacemaker-devel'
	}

	props['RPMDEPS'] += ' booth corosync-qdevice-devel corosynclib-devel fence-agents-common resource-agents sbd'
    }

    return props
}
