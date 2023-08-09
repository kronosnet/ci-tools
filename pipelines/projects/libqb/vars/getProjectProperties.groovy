// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// info contains all of the global build information
//      (do NOT change anything in here)
// agentName is the node we are building on,
// branch is the git branch we are building (for)
def call(Map info, String agentName, String branch)
{
    def props = [:]

//    props['DEST'] = 'qb'
    props['MAKEOPTS'] = ''
    props['PARALLELMAKE'] = ''
    props['MAKEINSTALLOPTS'] = ''
    props['TOPTS'] = ''
    props['CHECKS'] = ''
    props['EXTRACHECKS'] = ''
    props['EXTRAVER'] = ''
    props['EXTERNAL_LD_LIBRARY_PATH'] = ''
    props['SPECVERSION'] = env.BUILD_NUMBER

    // Give the tests time to run, even if the CI is busy
    props['CK_TIMEOUT_MULTIPLIER'] = 10

    props['DISTROCONFOPTS'] = "--with-socket-dir=/tmp/libqb-${info['pull_id']}-${env.BUILD_NUMBER}"

    if (agentName.startsWith("debian-experimental")) {
	props['DISTROCONFOPTS'] += ' --enable-debug'
    }

    if (agentName.startsWith("freebsd")) {
	props['DISTROCONFOPTS'] += ' MAKE=gmake'
    }

    return props
}
