package org.kronosnet.ci

class Booth extends Project {
    Booth(steps, info, node) { super(steps, info, node) }

    @Override
    protected String installPath() {
        // booth install path includes pacemaker version
        return "/srv/${generalInfo['project']}/origin/${generalInfo['target']}-pacemaker-${generalInfo['pacemakerver']}/"
    }

    @Override
    protected String extraOptsConfigure() {
        def opts = super.extraOptsConfigure()

        // Add ocfdir pointing to install path
        String installPath = installPath()
        opts += " --with-ocfdir=${installPath}/lib/ocf"

        return opts.trim()
    }

    @Override
    protected void configure() {
        // Booth requires bash as CONFIG_SHELL
        steps.sh "export CONFIG_SHELL=\$(which bash)"
        super.configure()
    }

    @Override
    protected void test(Map generalInfo) {
        // Set extraopts for distcheck
        generalInfo['extraopts'] = '--with-ocfdir=\\$\\${prefix}/lib/ocf'

        // Export CONFIG_SHELL for test environment
        steps.sh "export CONFIG_SHELL=\$(which bash)"
    }
}
