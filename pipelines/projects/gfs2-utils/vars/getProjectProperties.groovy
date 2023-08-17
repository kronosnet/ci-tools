// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// info contains all of the global build information
//      (do NOT change anything in here)
// agentName is the node we are building on,
// branch is the git branch we are building (for)
def call(Map info, Map extras, String agentName, String branch)
{
    def props = [:]

    props['DEST'] = info['project']
    props['EXTRAVER'] = ''
    if (agentName.contains('x86-64')) {
	props['DISTROCONFOPTS'] = "--with-testvol=/dev/shm/testvol-${info['pull_id']}-${env.BUILD_NUMBER}"
    }

    props['MAKEOPTS'] = ''
    props['PARALLELMAKE'] = ''
    props['MAKEINSTALLOPTS'] = ''
    props['TOPTS'] = '--always-clean-testvol'
    props['CHECKS'] = ''
    props['EXTRACHECKS'] = ''
    props['EXTERNAL_LD_LIBRARY_PATH'] = ''

    return props
}
