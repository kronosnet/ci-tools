
// Return the whole node array
def call()
{
    def labels = [:]
    labels['anvil-ci-almalinux-9'] = ['almalinux-9','stable','x86-64','rpm','yum','anvil', 'anvilvoting', 'anvilbuildrpms']
    labels['anvil-ci-bm-phy01'] = ['almalinux-9','stable','x86-64','rpm','yum']
    labels['anvil-ci-bm-phy02'] = ['almalinux-9','stable','x86-64','rpm','yum','anvil-bm']
    labels['anvil-ci-bm-phy03'] = ['almalinux-9','stable','x86-64','rpm','yum','anvil-bm']
    labels['anvil-ci-bm-phy04'] = ['almalinux-9','stable','x86-64','rpm','yum','anvil-bm']
    labels['anvil-ci-bm-phy05'] = ['almalinux-9','stable','x86-64','rpm','yum','anvil-bm']
    labels['anvil-ci-bm-phy06'] = ['almalinux-9','stable','x86-64','rpm','yum','anvil-bm']
    labels['anvil-ci-bm-phy07'] = ['almalinux-9','stable','x86-64','rpm','yum','anvil-bm']
    labels['anvil-ci-bm-phy99'] = ['almalinux-9','stable','x86-64','rpm','yum']
    labels['anvil-ci-centos-8-stream'] = ['centos-8','stable','x86-64','rpm','yum','anvil', 'anvilvoting', 'anvilbuildrpms']
    labels['anvil-ci-rhel-8'] = ['rhel-8','stable','x86-64','rpm','yum','anvil', 'anvilvoting', 'anvilbuildrpms']
    labels['anvil-ci-rhel-9'] = ['rhel-9','stable','x86-64','rpm','yum','anvil', 'anvilvoting', 'anvilbuildrpms']
    labels['centos-8-x86-64'] = ['centos-8','stable','x86-64','voting','rpm','yum','nonvoting-clang','buildrpms', 'pcs-010']
    labels['centos-9-power9-ppc64le'] = ['centos-9','unstable','power9-ppc64le','nonvoting','rpm','yum','nonvoting-clang','buildrpms']
    labels['centos-9-s390x'] = ['centos-9','unstable','s390x','nonvoting','rpm','yum','nonvoting-clang','buildrpms']
    labels['centos-9-x86-64'] = ['centos-9','unstable','x86-64','nonvoting','rpm','yum','nonvoting-clang','buildrpms']
    labels['debian10-x86-64'] = ['debian10','x86-64','voting','apt','stable','nonvoting-clang']
    labels['debian11-x86-64'] = ['debian11','x86-64','voting','apt','stable','nonvoting-clang']
    labels['debian12-ci-test-x86-64'] = ['debian12','x86-64','apt','stable','ci-test','test-nonvoting']
    labels['debian12-x86-64'] = ['debian12','x86-64','voting','apt','stable','nonvoting-clang']
    labels['debian-experimental-x86-64'] = ['debian','experimental','x86-64','nonvoting','apt','nonvoting-clang']
    labels['debian-testing-x86-64'] = ['debian','testing','x86-64','voting','apt','nonvoting-clang']
    labels['debian-unstable-cross-x86-64'] = ['debian','unstable','cross','x86-64','apt']
    labels['debian-unstable-x86-64'] = ['debian','unstable','x86-64','nonvoting','apt','nonvoting-clang']
    labels['fedora38-x86-64'] = ['fedora38','stable','x86-64','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora39-power9-ppc64le'] = ['fedora39','stable','power9-ppc64le','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora39-s390x'] = ['fedora39','stable','x390x','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora39-x86-64'] = ['fedora39','stable','x86-64','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora-rawhide-power9-ppc64le'] = ['fedora-rawhide','unstable','power9-ppc64le','nonvoting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora-rawhide-s390x'] = ['fedora-rawhide','unstable','s390x','nonvoting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora-rawhide-x86-64'] = ['fedora-rawhide','unstable','x86-64','nonvoting','rpm','yum','nonvoting-clang','buildrpms']
    labels['freebsd-13-x86-64'] = ['freebsd-13','stable','x86-64','voting','freebsd','nonvoting-clang']
    labels['freebsd-14-x86-64'] = ['freebsd-14','stable','x86-64','voting','freebsd','nonvoting-clang']
    labels['freebsd-devel-x86-64'] = ['freebsd-devel','unstable','x86-64','nonvoting','freebsd','nonvoting-clang']
    labels['opensuse-15-x86-64'] = ['opensuse-15','stable','x86-64','voting','rpm','zypper','nonvoting-clang','buildrpms', 'pcs-010']
    labels['opensuse-tumbleweed-x86-64'] = ['opensuse-tumbleweed','unstable','x86-64','nonvoting','rpm','zypper','nonvoting-clang','buildrpms']
    labels['rhel89z-power9-ppc64le'] = ['rhel89z','stable','power9','ppc64le','voting','rpm','yum','nonvoting-clang','buildrpms', 'pcs-010']
    labels['rhel89z-s390x'] = ['rhel89z','stable','s390x','voting','rpm','yum','nonvoting-clang','buildrpms', 'pcs-010']
    labels['rhel89z-x86-64'] = ['rhel89z','stable','x86-64','voting','rpm','yum','nonvoting-clang','buildrpms', 'pcs-010']
    labels['rhel8-ci-test-x86-64'] = ['rhel89z','stable','x86-64','rpm','yum','ci-test','test-voting','test-buildrpms']
    labels['rhel8-coverity-x86-64'] = ['rhel89z','stable','x86-64','voting','rhel8-coverity','yum','covscan']
    labels['rhel93z-power9-ppc64le'] = ['rhel93z','stable','power9-ppc64le','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['rhel93z-s390x'] = ['rhel93z','stable','s390x','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['rhel93z-x86-64'] = ['rhel93z','stable','x86-64','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['rhel9-ci-test-x86-64'] = ['rhel93z','stable','x86-64','rpm','yum','ci-test','test-voting']
    labels['rhel9-coverity-x86-64'] = ['rhel93z','stable','x86-64','voting','rhel9-coverity','yum','covscan','test-covscan']
    labels['rhel9-kbuild-x86-64'] = ['rhel9','stable','x86-64','yum','libvirtd']
    labels['rhel9-vapor-x86-64'] = ['rhel9','stable','x86-64','yum','osp','az','gcp','ocpv','ibmvpc']
    labels['ubuntu-20-04-lts-x86-64'] = ['ubuntu20.04','stable','x86-64','voting','apt','nonvoting-clang']
    labels['ubuntu-22-04-lts-x86-64'] = ['ubuntu22.04','stable','x86-64','voting','apt','nonvoting-clang']
    labels['ubuntu-23-10-x86-64'] = ['ubuntu23.10','stable','x86-64','voting','apt','nonvoting-clang']
    labels['ubuntu-devel-x86-64'] = ['ubuntu-devel','unstable','x86-64','nonvoting','apt','nonvoting-clang']

    return labels
}

// Return the nodes that have a specific label
def call(String target)
{
    def labels = call()

    def nodelist = []
    for (i in labels) {
	if (labels[i.key].contains(target)) {
	    nodelist += i.key
	}
    }
    return nodelist
}
