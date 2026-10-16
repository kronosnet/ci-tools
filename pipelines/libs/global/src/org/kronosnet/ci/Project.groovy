package org.kronosnet.ci

class Project implements Serializable {

    protected final def steps
    protected final Map generalInfo
    protected final String agentName
    protected final RpmCommon rpmHelper

    Project(steps, info, node) {
        this.steps = steps
        this.generalInfo = info
        this.agentName = node
        this.rpmHelper = new RpmCommon(this.steps)
    }

    void performSteps() {
        phase('Setup rpm') { setupRpm() }
        phase('Setup src') { setupSrc() }
        phase('Build src') { buildSrc() }
        phase('Test src')  { testSrc() }
        phase('Install')   { install() }
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
            new Coverity(steps).general_coverity(generalInfo["covopts"])
        } else {
            steps.sh "${make()} ${parallelMake()} ${generalInfo['MAKEOPTS'] ?: ''}"
        }
    }
    void testSrc()  { steps.sh "${make()} ${parallelMake()} check" }
    void install()  { steps.sh "${make()} install" }

    protected String make()         { return generalInfo['MAKE'] ?: 'make' }
    protected String parallelMake() { return generalInfo['PARALLELMAKE'] ?: '' }
    protected String pkgConfig()    { return generalInfo['EXTERNAL_CONFIG_PATH'] ?: '' }

    // default install prefix = the shell `*)` case: /srv/<project>/origin/<target>/
    protected String installPath() {
        return "/srv/${generalInfo['project']}/origin/${generalInfo['target']}/"
    }

    // extra ./configure opts (debug build adds DEBUGOPTS)
    protected String extraOpts() {
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

    // default ./configure (generic `*)` branch of ci-setup-src)
    protected void configure() {
        steps.sh "./configure --prefix=${installPath()} --exec-prefix=${installPath()} " +
                 "${extraOpts()} ${generalInfo['DISTROCONFOPTS'] ?: ''} PKG_CONFIG_PATH=${pkgConfig()}"
    }

    protected void build_rpm() {return}
    protected void build_coverity () {
        String shell = "cov-build --dir=./cov ${make()} ${parallelMake()} ${generalInfo['MAKECOVOPTS'] ?: ''}"
        println(shell)
        steps.sh shell
    }

    protected void sharedSetupPost() { return }
}
