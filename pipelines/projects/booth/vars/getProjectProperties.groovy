// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// localinfo contains all of the build information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]

    props['DISTROCONFOPTS'] = '--without-glue'
    props['TESTUSELDPATH'] = 'yes'
    props['BOOTH_RUNTESTS_ROOT_USER'] = '1'
    props['RPMDEPS'] = ''

    if (agentName.startsWith('rhel') ||
	agentName.startsWith('fedora') ||
	agentName.startsWith('centos')) {
	props['RPMDEPS'] = 'corosynclib-devel pacemaker-libs-devel'
    }

    props['RPMDEPS'] += ' libqb-devel'

    return props
}
