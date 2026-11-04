package org.kronosnet.ci

class Pacemaker extends Project {
    Pacemaker(steps, info, node) { super(steps, info, node) }

    @Override
    protected void configure(){
        steps.echo("./configure ${generalInfo['DISTROCONFOPTS']} PKG_CONFIG_PATH=${generalInfo['EXTERNAL_CONFIG_PATH']}")
        steps.sh "./configure ${generalInfo['DISTROCONFOPTS']} PKG_CONFIG_PATH=${generalInfo['EXTERNAL_CONFIG_PATH']} ${extraOptsConfigure()}"
    }

    @Override
    protected void buildRpm(){
        this.generalInfo['SPECVERSION'] = steps.env.BUILD_NUMBER
    }

    // pacemaker rebuilds with install-specific prefix before `make install`
    @Override
    protected void additionalInstall(Map generalInfo) {
        String installPath = installPath()
        steps.echo("Additional cleaning pacemaker")

        steps.echo("${make()} clean")
        steps.sh "${make()} clean"

        String configCmd = "./configure --prefix=${installPath} --exec-prefix=${installPath} " +
                          "--sysconfdir=${installPath}/etc --with-initdir=${installPath}/etc/init.d " +
                          "--with-ocfdir=${installPath}/lib/ocf " +
                          "--with-systemdsystemunitdir=${installPath}/usr/lib/systemd/system " +
                          "${generalInfo['DISTROCONFOPTS'] ?: ''} PKG_CONFIG_PATH=${pkgConfig()}"
        steps.echo(configCmd)
        steps.sh configCmd

        String makeCmd = "${make()} ${parallelMake()} ${generalInfo['MAKEOPTS'] ?: ''}"
        steps.echo(makeCmd)
        steps.sh makeCmd
    }

    // cross-project symlink hacks for pcs testing
    @Override
    protected void createSymlinks(Map generalInfo) {
        String installPath = installPath()
        steps.echo("Create symlinks pacemaker, ${installPath}")
        String boothSite = (generalInfo['target'] == 'main') ?
            '/srv/booth/origin/main-pacemaker-main/lib/ocf/resource.d/pacemaker/booth-site' :
            "/srv/booth/origin/main-pacemaker-${generalInfo['PACEMAKER_RELEASE']}/lib/ocf/resource.d/pacemaker/booth-site"
        steps.sh """
            cd ${installPath}/lib/ocf
            rm -f lib
            ln -sf /srv/resource-agents/origin/main/lib/ocf/lib
            cd resource.d
            rm -f heartbeat
            ln -sf /srv/resource-agents/origin/main/lib/ocf/resource.d/heartbeat
            cd pacemaker
            rm -f booth-site
            ln -sf ${boothSite}
        """
    }
}