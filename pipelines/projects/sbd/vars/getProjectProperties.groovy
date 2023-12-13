// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// localinfo contains all of the build information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]

    props['TESTUSELDPATH'] = 'yes'
    props['BOOTH_RUNTESTS_ROOT_USER'] = '1'

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

    return props
}
