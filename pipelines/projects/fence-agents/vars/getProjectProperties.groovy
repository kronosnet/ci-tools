// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// localinfo contains all of the build information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]

    props['RPMDEPS'] = 'corosynclib-devel'

    props['DISTROCONFOPTS'] = '--enable-cpg-plugin'

    return props
}
