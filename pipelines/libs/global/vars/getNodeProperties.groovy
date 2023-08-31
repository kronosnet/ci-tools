// List of properties for each node

// NOTE:
// properties that are <project>DISTROCONFOPTS are here as reminders *only*
// and really belong in the getProjectProperties file
def call(String node)
{
    def node_env=[:]
    def props = [:]
    node_env['anvil-ci-bm-phy01'] = []
    node_env['anvil-ci-bm-phy02'] = []
    node_env['anvil-ci-bm-phy03'] = []
    node_env['anvil-ci-bm-phy04'] = []
    node_env['anvil-ci-centos-8-stream'] = []
    node_env['anvil-ci-centos-9-stream'] = []
    node_env['anvil-ci-rhel-8'] = []
    node_env['anvil-ci-rhel-9'] = []
    node_env['centos-8-x86-64'] = []
    node_env['centos-9-power9-ppc64le'] = []
    node_env['centos-9-s390x'] = []
    node_env['centos-9-x86-64'] = []
    node_env['debian10-x86-64'] = []
    node_env['debian11-x86-64'] = []
    node_env['debian12-ci-test-x86-64'] = ['CCTEST=justtesting']
    node_env['debian12-x86-64'] = []
    node_env['debian-experimental-x86-64'] = []
    node_env['debian-testing-x86-64'] = []
    node_env['debian-unstable-cross-x86-64'] = ['EXTRA_ARCH=armhf']
    node_env['debian-unstable-x86-64'] = []
    node_env['fedora37-x86-64'] = ['RUSTBINDINGS=yes']
    node_env['fedora38-power9-ppc64le'] = ['RUSTBINDINGS=yes']
    node_env['fedora38-s390x'] = ['RUSTBINDINGS=yes']
    node_env['fedora38-x86-64'] = ['RUSTBINDINGS=yes']
    node_env['fedora-rawhide-power9-ppc64le'] = ['RUSTBINDINGS=yes']
    node_env['fedora-rawhide-s390x'] = ['RUSTBINDINGS=yes']
    node_env['fedora-rawhide-x86-64'] = ['RUSTBINDINGS=yes']
    node_env['freebsd-12-x86-64'] = ['CORODISTROCONFOPTS=--disable-systemd', 'python=python3.9', 'QDEVICEDISTROCONFOPTS=--disable-systemd', 'USEGMAKE=1']
    node_env['freebsd-13-x86-64'] = ['CORODISTROCONFOPTS=--disable-systemd', 'python=python3.9', 'QDEVICEDISTROCONFOPTS=--disable-systemd', 'RUSTBINDINGS=yes', 'USEGMAKE=1']
    node_env['freebsd-devel-x86-64'] = ['CORODISTROCONFOPTS=--disable-systemd', 'python=python3.9', 'QDEVICEDISTROCONFOPTS=--disable-systemd', 'RUSTBINDINGS=yes', 'USEGMAKE=1']
    node_env['jenkins-jumphost'] = []
    node_env['opensuse-15-x86-64'] = []
    node_env['opensuse-tumbleweed-x86-64'] = []
    node_env['rhel88z-power9-ppc64le'] = ['PCMKDISTROCONFOPTS=--with-cibsecrets=yes --with-concurrent-fencing-default=true --enable-legacy-links=yes']
    node_env['rhel88z-s390x'] = ['PCMKDISTROCONFOPTS=--with-cibsecrets=yes --with-concurrent-fencing-default=true --enable-legacy-links=yes']
    node_env['rhel88z-x86-64'] = ['PCMKDISTROCONFOPTS=--with-cibsecrets=yes --with-concurrent-fencing-default=true --enable-legacy-links=yes']
    node_env['rhel8-ci-test-x86-64'] = []
    node_env['rhel8-coverity-x86-64'] = []
    node_env['rhel92z-power9-ppc64le'] = []
    node_env['rhel92z-s390x'] = ['PCMKDISTROCONFOPTS=--with-cibsecrets=yes --with-concurrent-fencing-default=true --enable-legacy-links=yes']
    node_env['rhel92z-x86-64'] = []
    node_env['rhel9-ci-test-x86-64'] = []
    node_env['rhel9-coverity-x86-64'] = []
    node_env['rhel9-kbuild-x86-64'] = []
    node_env['rhel9-vapor-x86-64'] = []
    node_env['ubuntu-20-04-lts-x86-64'] = []
    node_env['ubuntu-22-04-lts-x86-64'] = []
    node_env['ubuntu-22-10-x86-64'] = []
    node_env['ubuntu-23-04-x86-64'] = []
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
