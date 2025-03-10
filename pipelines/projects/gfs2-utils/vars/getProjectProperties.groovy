// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// localinfo contains all of the build information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]

    props['DISTROCONFOPTS'] = "--with-testvol=/dev/shm/testvol-${localinfo['pull_id']}-${env.BUILD_NUMBER}"
    props['TOPTS'] = '--always-clean-testvol'

    return props
}
