package org.kronosnet.ci

class Project implements Serializable {

    protected final def steps
    protected final Map generalInfo
    protected final String agentName
    Project(steps, info, node) { this.steps = steps; this.generalInfo = info; this.agentName = node }

    void performSteps() {
        phase('Setup rpm') { setupRpm() }
        phase('Setup src') { setupSrc() }
        phase('Build src') { buildSrc() }
        phase('Test src')  { testSrc() }
        if (generalInfo['install'] == 1) {
            phase('Install') { install() }
        }
    }

    // wraps every phase with timeout + stage (preserves runStage behaviour)
    protected void phase(String name, Closure body) {
        steps.timeout(time: steps.getBuildTimeout()) { steps.stage(name) { body() } }
    }

    // ---- phases (defaults = the generic `*)` case) ----
    void setupRpm() {
        if (this.generalInfo['build'] == "rpm" && generalInfo['RPMDEPS']){
            new RpmCommon(steps).installRpmDeps(generalInfo)
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

    void buildSrc() { steps.sh "${make()} ${parallelMake()}" }
    void testSrc()  { steps.sh "${make()} ${parallelMake()} check" }
    void install()  { steps.sh "${make()} install" }

    // ---- shared helpers ----
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


    protected void sharedSetupPost() { return }
}
