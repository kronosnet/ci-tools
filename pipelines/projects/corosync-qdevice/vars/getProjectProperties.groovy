// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// local info contains all of the build information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]

    props['MAKEOPTS'] = ''
    props['MAKEINSTALLOPTS'] = ''
    props['CHECKS'] = ''
    props['EXTRACHECKS'] = ''
    props['SPECVERSION'] = env.BUILD_NUMBER
    props['RPMDEPS'] = 'corosynclib-devel'
    props['DISTROCONFOPTS'] = '--enable-systemd'

    if (agentName.startsWith('freebsd')) {
	props['DISTROCONFOPTS'] += ' --disable-systemd'
    }

    return props
}
