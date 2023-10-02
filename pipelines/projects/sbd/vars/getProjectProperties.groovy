// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// info contains all of the global build information
//      (do NOT change anything in here)
// agentName is the node we are building on,
// branch is the git branch we are building (for)
def call(Map info, Map extras, String agentName, String branch)
{
    def props = [:]

    props['MAKEOPTS'] = ''
    props['MAKEINSTALLOPTS'] = ''
    props['TOPTS'] = ''
    props['EXTRACHECKS'] = ''
    props['EXTERNAL_LD_LIBRARY_PATH'] = ''
    props['DISTROCONFOPTS'] = ''
    props['TESTUSELDPATH'] = 'yes'
    props['BOOTH_RUNTESTS_ROOT_USER'] = '1'
    props['extraver'] = "pacemaker-${extras['pacemakerver']}"

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
