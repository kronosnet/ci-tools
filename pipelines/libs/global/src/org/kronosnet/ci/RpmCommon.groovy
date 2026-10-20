package org.kronosnet.ci

class RpmCommon implements Serializable {
    private final def steps
    RpmCommon(steps) { this.steps = steps }

    static final String REMOVERPMS =
        'libnozzle1 libknet1 libqb* corosynclib libcorosync* resource-agents ' +
        'libpacemaker-devel pacemaker-libs-devel pacemaker-schemas fence-agents ' +
        'fence-agents-common corosync-qdevice-devel dlm*'

    private static final List<String> REPO_KEYS = [
        'KRONOSNET', 'LIBQB', 'COROSYNC', 'PACEMAKER', 'COROSYNC_QDEVICE', 'SBD',
        'FENCE_AGENTS', 'RESOURCE_AGENTS', 'BOOTH', 'DLM', 'GFS2UTILS'
    ]

    void cleanbr() {
    steps.sh '''
        if [ -n "$(which yum 2>/dev/null)" ]; then
          yum remove -y libnozzle1 libknet1 libqb* corosynclib libcorosync* resource-agents libpacemaker-devel pacemaker-libs-devel pacemaker-schemas fence-agents fence-agents-common corosync-qdevice-devel dlm* || true
          for r in kronosnet libqb corosync pacemaker corosync-qdevice sbd fence-agents resource-agents booth dlm gfs2utils; do
            rm -rf /etc/yum.repos.d/$r-build.repo || true
          done
          yum clean all
        fi
    '''
}

void removeRpmDeps() {
    cleanbr()
    if (!hasYum()) { return }
    def osdeps = 'libqb-devel'
    if (steps.env.NODE_NAME?.contains('ci-test')) { osdeps += ' libknet1-devel' }
    steps.sh "yum install -y ${osdeps}"
}

void installRpmDeps(Map info) {
    cleanbr()
    if (!hasYum()) {return}
    boolean bootstrap = info['bootstrap']?.toString() == '1'
    REPO_KEYS.each { k ->
        def url = info["${k}_REPO"]
        if (url) {
            def f = "/etc/yum.repos.d/${k.toLowerCase().replace('_','-')}-build.repo"
            int rc = steps.sh(returnStatus: true, script: "wget -4 -O ${f} '${url}'")
            if (rc != 0 && !bootstrap) { removeRpmDeps(); steps.error("repo fetch failed: ${k}") }
        }
    }
    int rc = steps.sh(returnStatus: true, script: "yum install -y ${info['RPMDEPS'] ?: ''}")
    if (rc != 0) { removeRpmDeps(); steps.error("yum install failed") }
}

void generalRpmBuild(String make, String makerpmopts, String rpmdeps){
    println("${make} ${makerpmopts} rpm")
    if (rpmdeps?.trim()) {
        int rc = steps.sh (returnStatus: true, script: "${make} ${makerpmopts} rpm")
        removeRpmDeps()
        if (rc != 0 ) {steps.error('make rpm failed')}
    } else {
        steps.sh "${make} ${makerpmopts} rpm"
    }

}

private boolean hasYum() { return steps.sh(returnStatus: true, script: 'which yum >/dev/null 2>&1') == 0 }

}
