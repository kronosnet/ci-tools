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
    props['SPECVERSION'] = env.BUILD_NUMBER

    // Give the tests time to run, even if the CI is busy
    props['CK_TIMEOUT_MULTIPLIER'] = 10

    props['DISTROCONFOPTS'] = "--with-socket-dir=/tmp/libqb-${localinfo['pull_id']}-${env.BUILD_NUMBER}"

    if (agentName.startsWith("debian-experimental")) {
	props['DISTROCONFOPTS'] += ' --enable-debug'
    }

    return props
}
