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
    props['DISTROCONFOPTS'] = ''
    props['DISTCHECK_CONFIGURE_FLAGS'] = ''
    props['MAKERPMOPTS'] = ''

    if (agentName.startsWith("debian-unstable-cross")) {
	if (localinfo['ARCH'] == 'ARM') {
	    props['compiler'] = 'arm-linux-gnueabihf-gcc'
	    props['DISTROCONFOPTS'] += ' --host=arm-linux-gnueabihf --target=arm-linux-gnueabihf --disable-wireshark-dissector'
	}
    }
    if (agentName.startsWith("openindiana")) {
	props['DISTROCONFOPTS'] += ' --disable-crypto-nss --disable-wireshark-dissector'
	props['DISTCHECK_CONFIGURE_FLAGS'] += ' --disable-crypto-nss --disable-wireshark-dissector'
    }

    // Check wireshark version - it needs >= 4.6.0
    def String ws_ver_check = sh(returnStatus: true, script: '''
                          pkg-config --atleast-version 4.6.0 wireshark
                          echo $?
                          ''')
    if (ws_ver_check == '1') {
	props['DISTROCONFOPTS'] += ' --disable-wireshark-dissector'
	props['DISTCHECK_CONFIGURE_FLAGS'] += ' --disable-wireshark-dissector'
	props['MAKERPMOPTS'] += ' --disable-wireshark-dissector'
    }


    // Which job types to run debug options in
    props['DEBUGJOBS'] = ['voting', 'nonvoting', 'nonvoting-clang']
    // ./configure option for debug
    props['DEBUGOPTS'] = '--enable-debug'

    return props
}
