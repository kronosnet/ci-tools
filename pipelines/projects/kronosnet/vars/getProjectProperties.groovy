// Return options for this build.
// Return a map of key=value pairs that will
// be passed into the environment on the building node.
//
// localinfo contains all of the global information
// agentName is the node we are building on,
def call(Map localinfo, String agentName)
{
    def props = [:]

    props['PARALLELTEST'] = 'no'
    props['RPMDEPS'] = 'libqb-devel doxygen2man'

    if (agentName.startsWith("debian-unstable-cross")) {
	if (localinfo['ARCH'] == 'ARM') {
	    props['compiler'] = 'arm-linux-gnueabihf-gcc'
	    props['DISTROCONFOPTS'] = ' --host=arm-linux-gnueabihf --target=arm-linux-gnueabihf --disable-wireshark-dissector'
	}
    }
    if (agentName.startsWith("openindiana")) {
	props['DISTROCONFOPTS'] = ' --disable-crypto-nss --disable-wireshark-dissector'
	props['DISTCHECK_CONFIGURE_FLAGS'] = ' --disable-crypto-nss --disable-wireshark-dissector'
    }

    // Which job types to run debug options in
    props['DEBUGJOBS'] = ['voting', 'nonvoting', 'nonvoting-clang']
    // ./configure option for debug
    props['DEBUGOPTS'] = '--enable-debug'

    return props
}
