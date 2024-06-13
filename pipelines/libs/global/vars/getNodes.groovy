
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
    labels['centos-9-power9-ppc64le'] = ['centos-9','stable','power9-ppc64le','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['centos-9-s390x'] = ['centos-9','stable','s390x','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['centos-9-x86-64'] = ['centos-9','stable','x86-64','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['debian-11-x86-64'] = ['debian-11','x86-64','voting','apt','stable','nonvoting-clang']
    labels['debian-12-ci-test-x86-64'] = ['debian-12','x86-64','apt','stable','ci-test','test-nonvoting']
    labels['debian-12-x86-64'] = ['debian-12','x86-64','voting','apt','stable','nonvoting-clang']
    labels['debian-experimental-x86-64'] = ['debian','experimental','x86-64','nonvoting','apt','nonvoting-clang']
    labels['debian-testing-x86-64'] = ['debian','testing','x86-64','voting','apt','nonvoting-clang']
    labels['debian-unstable-cross-x86-64'] = ['debian','unstable','cross','x86-64','apt']
    labels['debian-unstable-x86-64'] = ['debian','unstable','x86-64','nonvoting','apt','nonvoting-clang']
    labels['fedora-39-x86-64'] = ['fedora-39','stable','x86-64','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora-40-power9-ppc64le'] = ['fedora-40','stable','power9-ppc64le','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora-40-s390x'] = ['fedora-40','stable','x390x','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora-40-x86-64'] = ['fedora-40','stable','x86-64','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora-rawhide-power9-ppc64le'] = ['fedora-rawhide','unstable','power9-ppc64le','nonvoting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora-rawhide-s390x'] = ['fedora-rawhide','unstable','s390x','nonvoting','rpm','yum','nonvoting-clang','buildrpms']
    labels['fedora-rawhide-x86-64'] = ['fedora-rawhide','unstable','x86-64','nonvoting','rpm','yum','nonvoting-clang','buildrpms']
    labels['freebsd-13-x86-64'] = ['freebsd-13','stable','x86-64','voting','freebsd','nonvoting-clang']
    labels['freebsd-14-x86-64'] = ['freebsd-14','stable','x86-64','voting','freebsd','nonvoting-clang']
    labels['freebsd-devel-x86-64'] = ['freebsd-devel','unstable','x86-64','nonvoting','freebsd','nonvoting-clang']
    labels['opensuse-15-x86-64'] = ['opensuse-15','stable','x86-64','voting','rpm','zypper','nonvoting-clang','buildrpms', 'pcs-010']
    labels['opensuse-tumbleweed-x86-64'] = ['opensuse-tumbleweed','unstable','x86-64','nonvoting','rpm','zypper','nonvoting-clang','buildrpms']
    labels['rhel810z-power9-ppc64le'] = ['rhel810z','stable','power9','ppc64le','voting','rpm','yum','nonvoting-clang','buildrpms', 'pcs-010']
    labels['rhel810z-s390x'] = ['rhel810z','stable','s390x','voting','rpm','yum','nonvoting-clang','buildrpms', 'pcs-010']
    labels['rhel810z-x86-64'] = ['rhel810z','stable','x86-64','voting','rpm','yum','nonvoting-clang','buildrpms', 'pcs-010']
    labels['rhel8-ci-test-x86-64'] = ['rhel810z','stable','x86-64','rpm','yum','ci-test','test-voting','test-buildrpms']
    labels['rhel8-coverity-x86-64'] = ['rhel810z','stable','x86-64','voting','rhel8-coverity','yum','covscan']
    labels['rhel94z-power9-ppc64le'] = ['rhel94z','stable','power9-ppc64le','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['rhel94z-s390x'] = ['rhel94z','stable','s390x','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['rhel94z-x86-64'] = ['rhel94z','stable','x86-64','voting','rpm','yum','nonvoting-clang','buildrpms']
    labels['rhel9-ci-test-x86-64'] = ['rhel94z','stable','x86-64','rpm','yum','ci-test','test-voting']
    labels['rhel9-coverity-x86-64'] = ['rhel94z','stable','x86-64','voting','rhel9-coverity','yum','covscan','test-covscan']
    labels['rhel9-vapor-rdu-1-x86-64'] = ['rhel9','stable','x86-64','yum','osp','az','gcp','ibmvpc']
    labels['rhel9-vapor-rdu-2-x86-64'] = ['rhel9','stable','x86-64','yum','kbuild']
    labels['rhel9-vapor-rdu-3-x86-64'] = ['rhel9','stable','x86-64','yum','libvirtd']
    labels['rhel9-vapor-rdu-4-x86-64'] = ['rhel9','stable','x86-64','yum','libvirtd']
    labels['rhel9-vapor-rdu-5-x86-64'] = ['rhel9','stable','x86-64','yum','libvirtd']
    labels['ubuntu-20-04-lts-x86-64'] = ['ubuntu20.04','stable','x86-64','voting','apt','nonvoting-clang']
    labels['ubuntu-22-04-lts-x86-64'] = ['ubuntu22.04','stable','x86-64','voting','apt','nonvoting-clang']
    labels['ubuntu-23-10-x86-64'] = ['ubuntu23.10','stable','x86-64','voting','apt','nonvoting-clang']
    labels['ubuntu-24-04-lts-x86-64'] = ['ubuntu24.04','stable','x86-64','voting','apt','nonvoting-clang']
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
