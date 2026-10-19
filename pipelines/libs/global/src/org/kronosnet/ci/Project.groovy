package org.kronosnet.ci

class Project implements Serializable {

    protected final def steps
    protected final Map generalInfo
    protected final String agentName
    protected final RpmCommon rpmHelper
    protected final Coverity coverityHelper


    Project(steps, info, node) {
        this.steps = steps
        this.generalInfo = info
        this.agentName = node
        this.rpmHelper = new RpmCommon(this.steps)
        this.coverityHelper = new Coverity(this.steps)
    }

    void performSteps() {
        phase('Setup rpm') { setupRpm() }
        phase('Setup src') { setupSrc() }
        phase('Build src') { buildSrc() }
        phase('Test src')  { testSrc() }

        if (generalInfo["install"] == 1) {
            phase('Install')   { install() }
        }
    }

    // wraps every phase with timeout + stage (preserves runStage behaviour)
    protected void phase(String name, Closure body) {
        steps.timeout(time: steps.getBuildTimeout()) { steps.stage(name) { body() } }
    }

    // ---- phases (defaults = the generic `*)` case) ----
    void setupRpm() {
        if (this.generalInfo['build'] == "rpm" && generalInfo['RPMDEPS']){
            this.rpmHelper.installRpmDeps(generalInfo)
        }
    }

    void setupSrc() {
        if (steps.fileExists('autogen.sh')) {
            steps.sh './autogen.sh'
        }

        if (steps.fileExists('configure')) {
            configure()
        }

        sharedSetupPost()
    }

    void buildSrc() {
        if (generalInfo["build"] == "rpm"){
            build_rpm()
            rpmHelper.general_rpm_remove(generalInfo['MAKE'], generalInfo['MAKERPMOPTS'], generalInfo['RPMDEPS'])
        }else if (generalInfo["build"] == "coverity"){
            build_coverity()
            coverityHelper.general_coverity(generalInfo["covopts"])
        } else {
            steps.sh "${make()} ${parallelMake()} ${generalInfo['MAKEOPTS'] ?: ''}"
        }
    }

    void testSrc()  {
        if (generalInfo["build"] == "rpm"){
            return
        }else if (generalInfo["build"] == "coverity"){
            coverityHelper.test_coverity(generalInfo)
            return
        }else if (generalInfo["build"] == "crosscompile"){
            int rc = steps.sh(returnStatus: true,
                script: "[ \"\$(find . -print -type f -exec file {} \\; | grep ELF | grep ${generalInfo['ARCH']} | wc -l)\" != 0 ]")
            if (rc != 0) {
                steps.error("Crosscompilation test failed. Unable to find any ${generalInfo['ARCH']} binaries")
            }
            return
        }

        if (generalInfo['CHECKS'] == 'nochecks') { return }
        if (generalInfo['PARALLELTEST'] == 'no') {
            println("${generalInfo['project']} does not support parallel test. disabling PARALLELMAKE")
            generalInfo['PARALLELMAKE'] = ""
         }

        generalInfo['extraopts'] = ''

        test(generalInfo)

        post_test(generalInfo['extraopts'])
    }

    void install()  { steps.sh "${make()} install" }

    protected String make()         { return generalInfo['MAKE'] ?: 'make' }
    protected String parallelMake() { return generalInfo['PARALLELMAKE'] ?: '' }
    protected String pkgConfig()    { return generalInfo['EXTERNAL_CONFIG_PATH'] ?: '' }

    // default install prefix = the shell `*)` case: /srv/<project>/origin/<target>/
    protected String installPath() {
        return "/srv/${generalInfo['project']}/origin/${generalInfo['target']}/"
    }

    // extra ./configure opts (debug build adds DEBUGOPTS)
    protected String extraOptsConfigure() {
        def o = ''
        if (generalInfo['debug'] == 'yes') {
            o += " ${generalInfo['DEBUGOPTS'] ?: ''}"
        }
        return o.trim()
    }

    // ---- setup seams (default; subclasses override) ----
    protected void sharedSetupPre() {
        if (steps.fileExists('autogen.sh')) {
            steps.sh './autogen.sh'
        }
    }

    // default ./configure (generic `*)` branch of ci-setup-src) //overwrite
    protected void configure() {
        steps.sh "./configure --prefix=${installPath()} --exec-prefix=${installPath()} " +
                 "${extraOptsConfigure()} ${generalInfo['DISTROCONFOPTS'] ?: ''} PKG_CONFIG_PATH=${pkgConfig()}"
    }

    protected void post_test(String extraopts = '') {
        String checks = generalInfo['CHECKS'] ?: "check distcheck ${generalInfo['EXTRACHECKS'] ?: ''}".trim()

        List env = []
        if (generalInfo['TESTUSELDPATH'] == 'yes') {
            steps.echo "export LD_LIBRARY_PATH=${generalInfo['EXTERNAL_LD_LIBRARY_PATH']}"
            env << "LD_LIBRARY_PATH=${generalInfo['EXTERNAL_LD_LIBRARY_PATH'] ?: ''}"
        }

        steps.withEnv(env) {
            for (String i in checks.split()) {
                cleanlibqb()
                steps.sh "${make()} ${parallelMake()} ${generalInfo['MAKETESTOPTS'] ?: ''} ${i} " +
                         "DISTCHECK_CONFIGURE_FLAGS=\"${extraopts} ${generalInfo['DISTROCONFOPTS'] ?: ''}\" " +
                         "PKG_CONFIG_PATH=${pkgConfig()} " +
                         "|| (find . -name '*test*suite.log' -exec cat {} \\; ; false)"
            }
        }

        cleanlibqb()
        pacemakerextratests()
    }

    protected void cleanlibqb() {
        if (generalInfo['project'] == 'libqb') {
            steps.sh '''
                if [ -d /dev/shm ]; then
                    find /dev/shm -name 'qb-*-test*' | xargs rm -rf
                fi
                find /tmp -type d -name 'libqb*' | xargs rm -rf
            '''
        }
    }

    protected void pacemakerextratests() {
        if (generalInfo['project'] != 'pacemaker') {
            return
        }
        steps.sh './cts/cts-regression -V'
    }

    protected void test(Map generalInfo) {return}

    //overwrite
    protected void build_rpm() {return}
    //overwrite
    protected void build_coverity () {
        String shell = "cov-build --dir=./cov ${make()} ${parallelMake()} ${generalInfo['MAKECOVOPTS'] ?: ''}"
        println(shell)
        steps.sh shell
    }

    protected void sharedSetupPost() { return }
}
