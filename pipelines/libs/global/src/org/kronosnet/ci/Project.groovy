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

        boolean shouldInstall = generalInfo["install"]?.toString() == '1'
        if (shouldInstall) {
            println("Will start installing ")
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
        } else if (this.generalInfo['build'] == "rpm") {
            println("No rpm deps requested")
        }
    }

    void setupSrc() {
        if (steps.fileExists('autogen.sh')) {
            println('./autogen.sh')
            steps.sh './autogen.sh'
        }

        if (steps.fileExists('configure')) {
            configure()
        }

        sharedSetupPost()
    }

    void buildSrc() {
        if (generalInfo["build"] == "rpm"){
            buildRpm()
            rpmHelper.generalRpmBuild(generalInfo['MAKE'], generalInfo['MAKERPMOPTS'], generalInfo['RPMDEPS'])
        }else if (generalInfo["build"] == "coverity"){
            buildCoverity()
            coverityHelper.generalCoverity(generalInfo["covopts"])
        } else {
            build_general()
        }
    }

    void testSrc()  {
        if (generalInfo["build"] == "rpm"){
            return
        }else if (generalInfo["build"] == "coverity"){
            coverityHelper.testCoverity(generalInfo)
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

    void install()  {
        if (generalInfo['build'] != ''){
            println("${generalInfo['build']} detected, nothing to do here")
            return
        }

        String installPath = installPath()

        println("rm -rf ${installPath}")
        steps.dir(installPath) { steps.deleteDir() }

        additionalInstall(generalInfo)

        String installCmd = "${make()} ${generalInfo['MAKEINSTALLOPTS'] ?: ''} install"
        println(installCmd)
        steps.sh installCmd
        steps.sh "echo '${steps.env.JOB_URL}/${steps.env.BUILD_NUMBER}/' > ${installPath}/.build-info"

        createSymlinks(generalInfo)
    }

    // HELPERS and function to overwrite

    protected String make()         { return generalInfo['MAKE'] ?: 'make' }
    protected String parallelMake() { return generalInfo['PARALLELMAKE'] ?: '' }
    protected String pkgConfig()    { return generalInfo['EXTERNAL_CONFIG_PATH'] ?: '' }

    // default install prefix = the shell `*)` case: /srv/<project>/origin/<target>/
    //overwrite
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


    //overwrite
    // default ./configure (generic `*)` branch of ci-setup-src) //overwrite
    protected void configure() {
        String cmd = "./configure --prefix=${installPath()} --exec-prefix=${installPath()} " +
                     "${extraOptsConfigure()} ${generalInfo['DISTROCONFOPTS'] ?: ''} PKG_CONFIG_PATH=${pkgConfig()}"
        println(cmd)
        steps.sh cmd
    }

    protected void post_test(String extraopts = '') {
        String checks = generalInfo['CHECKS'] ?: "check distcheck ${generalInfo['EXTRACHECKS'] ?: ''}".trim()

        if (generalInfo['TESTUSELDPATH'] == 'yes') {
            steps.echo "export LD_LIBRARY_PATH=${generalInfo['EXTERNAL_LD_LIBRARY_PATH']}"
            generalInfo['LD_LIBRARY_PATH']=${generalInfo['EXTERNAL_LD_LIBRARY_PATH'] ?: ''}
        }

        for (String i in checks.split()) {
            cleanlibqb()
            String testCmd = "${make()} ${parallelMake()} ${generalInfo['MAKETESTOPTS'] ?: ''} ${i} " +
                            "DISTCHECK_CONFIGURE_FLAGS=\"${extraopts} ${generalInfo['DISTROCONFOPTS'] ?: ''}\" " +
                            "PKG_CONFIG_PATH=${pkgConfig()} " +
                            "|| (find . -name '*test*suite.log' -exec cat {} \\; ; false)"
            println(testCmd)
            steps.sh testCmd
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

    //overwrite
    protected void createSymlinks(Map generalInfo){
        println('Creating cross projects symlinks hacks for pcs testing')
        return
        //
        //     if (generalInfo['project'] == 'pacemaker') {
        //         String boothSite = (generalInfo['target'] == 'main') ?
        //             '/srv/booth/origin/main-pacemaker-main/lib/ocf/resource.d/pacemaker/booth-site' :
        //             "/srv/booth/origin/main-pacemaker-${generalInfo['PACEMAKER_RELEASE']}/lib/ocf/resource.d/pacemaker/booth-site"
        //         steps.sh """
        //             cd ${installPath}/lib/ocf
        //             rm -f lib
        //             ln -sf /srv/resource-agents/origin/main/lib/ocf/lib
        //             cd resource.d
        //             rm -f heartbeat
        //             ln -sf /srv/resource-agents/origin/main/lib/ocf/resource.d/heartbeat
        //             cd pacemaker
        //             rm -f booth-site
        //             ln -sf ${boothSite}
        //         """
        // }
    }

    //overwrite
    protected void build_general(){
        String cmd = "${make()} ${parallelMake()} ${generalInfo['MAKEOPTS'] ?: ''}"
        println(cmd)
        steps.sh cmd
        println("_____________Finishing build____________")
    }

    //oberwrite
    protected void additionalInstall(Map generalInfo){
        return
    }

    //overwrite
    protected void test(Map generalInfo) {return}

    //overwrite
    protected void buildRpm() {return}
    //overwrite
    protected void buildCoverity () {
        String shell = "cov-build --dir=./cov ${make()} ${parallelMake()} ${generalInfo['MAKECOVOPTS'] ?: ''}"
        println(shell)
        steps.sh shell
    }

    protected void sharedSetupPost() { return }
}
