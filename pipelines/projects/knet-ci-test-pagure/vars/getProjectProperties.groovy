// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// localinfo contains all of the build information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]

    if (agentName.startsWith('debian')) {
	props['DISTROCONFOPTS'] = '--with-debug'
    }

    if (agentName.startsWith('rhel') ||
	agentName.startsWith('fedora') ||
	agentName.startsWith('opensuse') ||
	agentName.startsWith('centos')) {
	//props['RPMDEPS'] = 'libknet1-devel'
    }

    return props
}
