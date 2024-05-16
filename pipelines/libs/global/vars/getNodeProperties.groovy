// List of properties for each node
//
// Each entry in this map is a node, and that
// map entry contains a String array of envronment variables
//
// When called for a node, a new map (props) is created that contains
// key=value pairs which are added to localinfo[:] in the caller,
// and then subsequently passed down to the node's shell environment
// when the job is run.

def call(String node)
{
    def node_env=[:]
    def props = [:]
    node_env['anvil-ci-almalinux-9'] = []
    node_env['anvil-ci-bm-phy01'] = []
    node_env['anvil-ci-bm-phy02'] = []
    node_env['anvil-ci-bm-phy03'] = []
    node_env['anvil-ci-bm-phy04'] = []
    node_env['anvil-ci-bm-phy05'] = []
    node_env['anvil-ci-bm-phy06'] = []
    node_env['anvil-ci-bm-phy07'] = []
    node_env['anvil-ci-bm-phy99'] = []
    node_env['anvil-ci-centos-8-stream'] = []
    node_env['anvil-ci-rhel-8'] = []
    node_env['anvil-ci-rhel-9'] = []
    node_env['centos-9-power9-ppc64le'] = []
    node_env['centos-9-s390x'] = []
    node_env['centos-9-x86-64'] = []
    node_env['debian11-x86-64'] = []
    node_env['debian12-ci-test-x86-64'] = ['CCTEST=justtesting']
    node_env['debian12-x86-64'] = []
    node_env['debian-experimental-x86-64'] = []
    node_env['debian-testing-x86-64'] = []
    node_env['debian-unstable-cross-x86-64'] = ['EXTRA_ARCH=armhf']
    node_env['debian-unstable-x86-64'] = []
    node_env['fedora39-x86-64'] = ['RUSTBINDINGS=yes']
    node_env['fedora40-power9-ppc64le'] = ['RUSTBINDINGS=yes']
    node_env['fedora40-s390x'] = ['RUSTBINDINGS=yes']
    node_env['fedora40-x86-64'] = ['RUSTBINDINGS=yes']
    node_env['fedora-rawhide-power9-ppc64le'] = ['RUSTBINDINGS=yes']
    node_env['fedora-rawhide-s390x'] = ['RUSTBINDINGS=yes']
    node_env['fedora-rawhide-x86-64'] = ['RUSTBINDINGS=yes']
    node_env['freebsd-13-x86-64'] = ['RUSTBINDINGS=yes', 'MAKE=gmake']
    node_env['freebsd-14-x86-64'] = ['RUSTBINDINGS=yes', 'MAKE=gmake']
    node_env['freebsd-devel-x86-64'] = ['RUSTBINDINGS=yes', 'MAKE=gmake']
    node_env['opensuse-15-x86-64'] = []
    node_env['opensuse-tumbleweed-x86-64'] = []
    node_env['rhel89z-power9-ppc64le'] = []
    node_env['rhel89z-s390x'] = []
    node_env['rhel89z-x86-64'] = []
    node_env['rhel8-ci-test-x86-64'] = []
    node_env['rhel8-coverity-x86-64'] = []
    node_env['rhel94z-power9-ppc64le'] = []
    node_env['rhel94z-s390x'] = []
    node_env['rhel94z-x86-64'] = []
    node_env['rhel9-ci-test-x86-64'] = []
    node_env['rhel9-coverity-x86-64'] = []
    node_env['rhel9-vapor-rdu-1-x86-64'] = []
    node_env['rhel9-vapor-rdu-2-x86-64'] = []
    node_env['rhel9-vapor-rdu-3-x86-64'] = []
    node_env['rhel9-vapor-rdu-4-x86-64'] = []
    node_env['rhel9-vapor-rdu-5-x86-64'] = []
    node_env['ubuntu-20-04-lts-x86-64'] = []
    node_env['ubuntu-22-04-lts-x86-64'] = []
    node_env['ubuntu-23-10-x86-64'] = []
    node_env['ubuntu-24-04-lts-x86-64'] = []
    node_env['ubuntu-devel-x86-64'] = []

    if (node_env.containsKey(node)) {
	// Add it to props[]
	node_env[node].each {
	    def keyval = it.split('=', 2)
	    if (keyval.size() == 2) {
		props[keyval[0]] = keyval[1]
	    }
	}
    }
    println("getNodeProperties: node = ${node} - env='${node_env[node]}' - props:"+props)
    return props
}
