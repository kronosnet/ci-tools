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
        phase('Install')   { install() }
    }

    // wraps every phase with timeout + logging + stats (preserves runStage behavior)
    protected void phase(String name, Closure body) {
        steps.timeout(time: steps.getBuildTimeout()) { steps.stage(name) { body() } }
    }

    void setupRpm() {} //Shared step, have to convert ci-rpm-common, installrpmdeps if $build == "rpm" and -n $RPMDEPS
    void setupSrc() {
        sharedSetupPre()
        function_to_override_by_the_project()
        sharedSetupPost()
        } //
    void buildSrc() { }
    void testSrc()  {  }
    void install() {}

    protected void sharedSetupPre() {

    }
    protected void configure() {
    }
    protected void sharedSetupPost() { }

}
