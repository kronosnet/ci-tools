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
    props['MAKEINSTALLOPTS'] = ''
    props['EXTRACHECKS'] = ''
    props['extraver'] = "kronosnet-${localinfo['kronosnetver']}"
    props['RPMDEPS'] = 'libnozzle1-devel libknet1-devel libqb-devel'
    props['DISTROCONFOPTS'] = '--enable-snmp --enable-dbus --enable-systemd --enable-nozzle'

    if (!localinfo.containsKey('compiler') || localinfo['compiler'] == 'gcc') {
	props['DISTROCONFOPTS'] += ' --enable-fatal-warnings'
    }

    if (agentName.startsWith('freebsd')) {
	props['DISTROCONFOPTS'] += ' --disable-systemd'
    }

    return props
}
