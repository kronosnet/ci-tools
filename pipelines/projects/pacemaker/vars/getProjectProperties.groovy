// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// localinfo contains all of the build information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]

    props['SPECVERSION'] = env.BUILD_NUMBER
    props['RPMDEPS'] = 'corosynclib-devel'
    props['MAKERPMOPTS'] = ''
    props['DISTROCONFOPTS'] = ''

    // Skip one distro just to ensure compiling works both ways
    if (!agentName.startsWith('debian')) {
	props['DISTROCONFOPTS'] += '--with-cibsecrets=yes --enable-nls'
    }

    if (agentName.startsWith('rhel') && localinfo['target'] == '2.1') {
	props['DISTROCONFOPTS'] += ' --with-concurrent-fencing-default=true'
    }

    if (agentName.startsWith('rhel-8') && localinfo['target'] == '2.1') {
	props['DISTROCONFOPTS'] += ' --enable-compat-2.0 --enable-legacy-links=yes'
    }

    if (agentName.startsWith('opensuse') && localinfo['target'] in ['main', '3.0']) {
	props['WITH'] = '--with linuxha --without doc'
    }

    return props
}
