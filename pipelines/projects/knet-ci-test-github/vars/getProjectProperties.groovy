// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// local info contains all of the build information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]
    props['MAKERPMOPTS'] = ''

    if (agentName.startsWith('debian')) {
	props['DISTROCONFOPTS'] = '--enable-debug'
    }

    if (agentName.startsWith('rhel') ||
	agentName.startsWith('fedora') ||
	agentName.startsWith('centos')) {
	props['RPMDEPS'] = 'libknet1-devel'
    }

    // Which job types to run debug options in
    props['DEBUGJOBS'] = ['voting', 'nonvoting', 'nonvoting-clang']
    // ./configure option for debug
    props['DEBUGOPTS'] = '--enable-debug'

    return props
}
