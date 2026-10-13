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
        if (generalInfo['install'] == 1) phase('Install') { install() }
    }

    // wraps every phase with timeout + stage (preserves runStage behaviour)
    protected void phase(String name, Closure body) {
        steps.timeout(time: steps.getBuildTimeout()) { steps.stage(name) { body() } }
    }

    // ---- shared helpers ----
    protected String make()         { generalInfo['MAKE'] ?: 'make' }
    protected String parallelMake() { generalInfo['PARALLELMAKE'] ?: '' }
    protected String pkgConfig()    { generalInfo['EXTERNAL_CONFIG_PATH'] ?: '' }

    // ---- phases (defaults = the generic `*)` case) ----
    void setupRpm() { }   // TODO: ciRpmCommon().installrpmdeps when build=='rpm' && RPMDEPS

    void setupSrc() {
        sharedSetupPre()
        configure()        // the seam projects override
        sharedSetupPost()
    }

    void buildSrc() { steps.sh "${make()} ${parallelMake()}" }
    void testSrc()  { steps.sh "${make()} ${parallelMake()} check" }
    void install()  { steps.sh "${make()} install" }

    // ---- setup seams (default; subclasses override) ----
    protected void sharedSetupPre() {
        if (steps.fileExists('autogen.sh')) steps.sh './autogen.sh'
    }
    protected void configure() {
        if (steps.fileExists('configure'))
            steps.sh "./configure ${generalInfo['DISTROCONFOPTS'] ?: ''} PKG_CONFIG_PATH=${pkgConfig()}"
    }
    protected void sharedSetupPost() { }
}
