// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// localinfo contains all of the build information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]

    props['DISTROCONFOPTS'] = ''

    if (agentName.contains('x86-64')) {
	props['DISTROCONFOPTS'] += "--with-testvol=/dev/shm/testvol-${localinfo['pull_id']}-${env.BUILD_NUMBER}"
    }

    props['MAKEOPTS'] = ''
    props['MAKEINSTALLOPTS'] = ''
    props['TOPTS'] = '--always-clean-testvol'
    props['EXTRACHECKS'] = ''
    props['EXTERNAL_LD_LIBRARY_PATH'] = ''

    return props
}
