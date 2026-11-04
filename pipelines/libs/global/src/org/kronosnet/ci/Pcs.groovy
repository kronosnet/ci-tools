package org.kronosnet.ci

class Pcs extends Project {
    Pcs(steps, info, node) { super(steps, info, node) }

    @Override
    protected void configure() {
        // Random sleep to avoid parallel pcs builds hitting resource limits
        int sleeptimer = (new Random().nextInt(240))
        steps.echo("Random sleep: ${sleeptimer}")
        steps.sleep(time: sleeptimer, unit: 'SECONDS')

        String pcspython = 'python3'
        String pcspipinstall = 'no'
        String nodeName = steps.env.NODE_NAME

        // Determine python version based on node and target
        if (nodeName.startsWith('rhel-9')) {
            if (generalInfo['target'] != 'pcs-0.11') {
                pcspython = 'python3.12'
                pcspipinstall = 'yes'
            }
        }

        // Create Python virtual environment
        steps.sh "rm -rf .venv"
        steps.sh "${pcspython} -m venv --system-site-packages .venv"
        steps.sh ". .venv/bin/activate && export PATH=\"\$(pwd)/.venv\"/bin/:\$PATH"

        // Determine localbuild options based on node
        String localbuild = ''
        if (nodeName.startsWith('debian') || nodeName.startsWith('ubuntu')) {
            localbuild = ''
        } else if (nodeName.startsWith('rhel-8')) {
            steps.sh 'python3 -m pip install --upgrade setuptools'
            steps.sh 'python3 -m pip install concurrencytest'
            localbuild = '--enable-local-build --enable-ci-rpm-workarounds'
        } else if (nodeName.startsWith('rhel-9')) {
            localbuild = '--enable-local-build'
            if (pcspipinstall == 'yes') {
                steps.sh "python3 -m pip install 'flit-core >= 3.11' pyparsing setuptools-scm six"
                localbuild = "${localbuild} --with-python-version=3.12 --enable-ci-rpm-workarounds"
            }
        } else {
            localbuild = '--enable-local-build'
        }

        steps.echo("./configure --enable-destructive-tests --enable-parallel-tests ${localbuild} ${generalInfo['DISTROCONFOPTS']} PKG_CONFIG_PATH=${pkgConfig()}")
        steps.sh "./configure --enable-destructive-tests --enable-parallel-tests ${localbuild} ${generalInfo['DISTROCONFOPTS'] ?: ''} PKG_CONFIG_PATH=${pkgConfig()}"
    }

    @Override
    protected void buildCoverity() {
        // pcs requires separate coverity capture for pcs and pcs_test
        for (String dir : ['pcs', 'pcs_test']) {
            steps.echo("cd ${dir} && coverity capture --dir ../cov && cd ..")
            steps.sh """
                cd ${dir}
                coverity capture --dir ../cov
                cd ..
            """
        }
    }

    @Override
    protected void buildRpm() {
        // Activate venv for RHEL 9 non-0.11 builds
        String nodeName = steps.env.NODE_NAME
        if (nodeName.startsWith('rhel-9') && generalInfo['target'] != 'pcs-0.11') {
            if (steps.fileExists('.venv/bin/activate')) {
                steps.sh ". .venv/bin/activate && export PATH=\"\$(pwd)/.venv\"/bin/:\$PATH"
            }
        }
    }

    @Override
    protected void build_general() {
        // Activate venv if exists
        if (steps.fileExists('.venv/bin/activate')) {
            steps.sh ". .venv/bin/activate && export PATH=\"\$(pwd)/.venv\"/bin/:\$PATH"
        }

        String cmd = "${make()} ${parallelMake()} ${generalInfo['MAKEOPTS'] ?: ''}"
        steps.echo(cmd)
        steps.sh cmd
    }

    @Override
    protected void test(Map generalInfo) {
        // Determine localbuild options based on node
        String nodeName = steps.env.NODE_NAME
        String localbuild = ''
        if (nodeName.startsWith('debian') || nodeName.startsWith('ubuntu')) {
            localbuild = ''
        } else if (nodeName.startsWith('rhel-8')) {
            localbuild = '--enable-local-build --enable-ci-rpm-workarounds'
        } else {
            localbuild = '--enable-local-build'
        }

        // Activate venv if exists
        if (steps.fileExists('.venv/bin/activate')) {
            steps.sh ". .venv/bin/activate && export PATH=\"\$(pwd)/.venv\"/bin/:\$PATH"
        }

        // Run check and distcheck
        steps.sh "${make()} ${parallelMake()} check"
        steps.sh "${make()} ${parallelMake()} distcheck " +
                 "DISTCHECK_CONFIGURE_FLAGS=\"--enable-parallel-tests --enable-destructive-tests ${localbuild} " +
                 "${generalInfo['DISTROCONFOPTS'] ?: ''} PKG_CONFIG_PATH=${generalInfo['EXTERNAL_CONFIG_PATH']}\""
    }
}