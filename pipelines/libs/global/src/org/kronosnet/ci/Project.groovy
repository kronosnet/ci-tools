package org.kronosnet.ci

class Project implements Serializable {

    // ============= Constants =============
    protected static final String BUILD_RPM = 'rpm'
    protected static final String BUILD_COVERITY = 'coverity'
    protected static final String BUILD_CROSSCOMPILE = 'crosscompile'
    protected static final String BUILD_STANDARD = ''

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
            steps.echo("Will start installing ")
            phase('Install')   { install() }
        }
    }

    // ============= Phase wrapper =============
    protected void phase(String name, Closure body) {
        steps.timeout(time: steps.getBuildTimeout()) {
            steps.stage(name) { body() } }
    }

    // ============= Build Type Helpers =============
    protected String buildType() {
        return generalInfo['build'] ?: BUILD_STANDARD
    }

    protected boolean isRpmBuild()        { buildType() == BUILD_RPM }
    protected boolean isCoverityBuild()   { buildType() == BUILD_COVERITY }
    protected boolean isCrossBuild()      { buildType() == BUILD_CROSSCOMPILE }
    protected boolean isStandardBuild()   { buildType() == BUILD_STANDARD }

    protected boolean shouldInstall() {
        return generalInfo["install"]?.toString() == '1'
    }

    protected boolean hasRpmDeps() {
        return generalInfo['RPMDEPS']
    }

    protected boolean shouldSkipTests() {
        return generalInfo['CHECKS'] == 'nochecks'
    }


        // ---- phases (defaults = the generic `*)` case) ----
    void setupRpm() {
        if (!isRpmBuild()) {return}

        if (hasRpmDeps()) {
            rpmHelper.installRpmDeps(generalInfo)
        } else {
            steps.echo("No rpm deps requested")
        }
    }

    void setupSrc() {
        if (steps.fileExists('autogen.sh')) {
            steps.echo('./autogen.sh')
            steps.sh './autogen.sh'
        }

        if (steps.fileExists('configure')) {
            configure()
        }

        sharedSetupPost()
    }

    void buildSrc() {
        if (isRpmBuild()){
            buildRpm()
            rpmHelper.generalRpmBuild(generalInfo['MAKE'], generalInfo['MAKERPMOPTS'], generalInfo['RPMDEPS'])
        }else if (isCoverityBuild()){
            buildCoverity()
            coverityHelper.generalCoverity(generalInfo["covopts"])
        } else {
            build_general()
        }
    }

    void testSrc()  {
        if (isRpmBuild()) {return}
        else if (isCoverityBuild()){
            coverityHelper.testCoverity(generalInfo)
            return
        }else if (isCrossBuild()){
            int rc = steps.sh(returnStatus: true,
                script: "[ \"\$(find . -print -type f -exec file {} \\; | grep ELF | grep ${generalInfo['ARCH']} | wc -l)\" != 0 ]")
            if (rc != 0) {
                steps.error("Crosscompilation test failed. Unable to find any ${generalInfo['ARCH']} binaries")
            }
            return
        }
        if(shouldSkipTests()) {return}

        if (generalInfo['PARALLELTEST'] == 'no') {
            steps.echo("${generalInfo['project']} does not support parallel test. disabling PARALLELMAKE")
            generalInfo['PARALLELMAKE'] = ""
        }

        generalInfo['extraopts'] = ''

        test(generalInfo)

        post_test(generalInfo['extraopts'])
    }

    void install()  {
        if (generalInfo['build'] != ''){
            steps.echo("${generalInfo['build']} detected, nothing to do here")
            return
        }

        String installPath = installPath()

        steps.echo("rm -rf ${installPath}")
        steps.dir(installPath) { steps.deleteDir() }

        additionalInstall(generalInfo)

        String installCmd = "${make()} ${generalInfo['MAKEINSTALLOPTS'] ?: ''} install"
        steps.echo(installCmd)
        steps.sh installCmd
        steps.sh "echo '${steps.env.JOB_URL}/${steps.env.BUILD_NUMBER}/' > ${installPath}/.build-info"

        createSymlinks(generalInfo)
    }

    // ============= Common Helpers =============

    protected String make()         { return generalInfo['MAKE'] ?: 'make' }
    protected String parallelMake() { return generalInfo['PARALLELMAKE'] ?: '' }
    protected String pkgConfig()    { return generalInfo['EXTERNAL_CONFIG_PATH'] ?: '' }

     /**
     * Override in subclasses to customize install path.
     * Default: /srv/<project>/origin/<target>/
     */
    protected String installPath() {
        return "/srv/${generalInfo['project']}/origin/${generalInfo['target']}/"
    }

    /**
     * Extra ./configure options (debug build adds DEBUGOPTS).
     */
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


    /**
     * Override to customize ./configure command.
     * Default: generic configure with prefix, distro opts, pkg-config
     */
    protected void configure() {
        String cmd = "./configure --prefix=${installPath()} --exec-prefix=${installPath()} " +
                     "${extraOptsConfigure()} ${generalInfo['DISTROCONFOPTS'] ?: ''} PKG_CONFIG_PATH=${pkgConfig()}"
        steps.echo(cmd)
        steps.sh cmd
    }

    protected void post_test(String extraopts = '') {
        String checks = generalInfo['CHECKS'] ?: "check distcheck ${generalInfo['EXTRACHECKS'] ?: ''}".trim()

        if (generalInfo['TESTUSELDPATH'] == 'yes') {
            steps.echo "export LD_LIBRARY_PATH=${generalInfo['EXTERNAL_LD_LIBRARY_PATH']}"
            generalInfo['LD_LIBRARY_PATH']=generalInfo['EXTERNAL_LD_LIBRARY_PATH'] ?: ''
        }

        for (String i in checks.split()) {
            cleanlibqb()
            String testCmd = "${make()} ${parallelMake()} ${generalInfo['MAKETESTOPTS'] ?: ''} ${i} " +
                            "DISTCHECK_CONFIGURE_FLAGS=\"${extraopts} ${generalInfo['DISTROCONFOPTS'] ?: ''}\" " +
                            "PKG_CONFIG_PATH=${pkgConfig()} " +
                            "|| (find . -name '*test*suite.log' -exec cat {} \\; ; false)"
            steps.echo(testCmd)
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

     // ============= Override Hooks (subclasses customize these) =============

    /**
     * Override to add project-specific symlinks after install.
     * Default: only logs message for pcs testing
     */
    protected void createSymlinks(Map generalInfo){
        steps.echo('Creating cross projects symlinks hacks for pcs testing')
        return
    }

     /**
     * Override for standard build customization.
     * Default: make with parallel and custom opts
     */
    protected void build_general(){
        String cmd = "${make()} ${parallelMake()} ${generalInfo['MAKEOPTS'] ?: ''}"
        steps.echo(cmd)
        steps.sh cmd
        steps.echo("_____________Finishing build____________")
    }

    /**
     * Override for additional install steps (e.g., pacemaker clean/reconfigure).
     */
    protected void additionalInstall(Map generalInfo){
        return
    }

    /**
     * Override for project-specific test setup (e.g., kronosnet/corosync rust bindings).
     */
    protected void test(Map generalInfo) {return}

    /**
     * Override for RPM-specific build steps (e.g., pacemaker SPECVERSION).
     */
    protected void buildRpm() {return}

    /**
     * Override for Coverity-specific build steps (e.g., fence-agents/pcs multi-dir).
     */
    protected void buildCoverity () {
        String shell = "cov-build --dir=./cov ${make()} ${parallelMake()} ${generalInfo['MAKECOVOPTS'] ?: ''}"
        steps.echo(shell)
        steps.sh shell
    }

    protected void sharedSetupPost() { return }
}
