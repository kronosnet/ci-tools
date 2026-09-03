// setMultibranchSkipStrategy - add a commit-message "CI skip" build strategy
// to existing multibranch pipeline jobs, in place, without touching any other
// configuration.
//
// Lives in GlobalLib because it mutates live Jenkins model objects
// (WorkflowMultiBranchProject / BranchSource) with full permissions - this is
// NOT allowed in a sandboxed pipeline or Job DSL script.
//
// Requires the "Pipeline: Multibranch build strategy extension" plugin
// (multibranch-build-strategy-extension).
//
// Usage from a pipeline:
//   setMultibranchSkipStrategy(regex: '\\[(ci skip|skip ci)\\]',
//                              dryrun: true,
//                              jobs: ['kronosnet', 'corosync'])   // [] = all

import jenkins.model.Jenkins
import jenkins.branch.BranchSource
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import com.igalg.jenkins.plugins.multibranch.buildstrategy.ExcludeMessageBranchBuildStrategy

// The heavy lifting runs outside CPS: it iterates non-serializable Jenkins
// model objects and uses closures, which the CPS transform can't handle.
@NonCPS
def applyStrategy(String regex, boolean dryrun, List only) {
    def updated = []
    def skipped = []

    for (mbp in Jenkins.instance.getAllItems(WorkflowMultiBranchProject)) {
        if (only && !only.contains(mbp.fullName)) {
            continue
        }

        boolean changed = false
        for (BranchSource bs in mbp.sources) {
            def strategies = new ArrayList(bs.buildStrategies)
            strategies.add(new ExcludeMessageBranchBuildStrategy(regex))
            bs.setBuildStrategies(strategies)
            dchanged = true
        }

        if (changed) {
            if (!dryrun) {
                mbp.save()
            }
            updated << mbp.fullName
        } else {
            skipped << mbp.fullName
        }
    }

    return [updated: updated, skipped: skipped]
}

def call(Map args = [:]) {
    def regex  = args.get('regex', '\\[(ci skip|skip ci)\\]')
    def dryrun = args.get('dryrun', false)
    def only   = args.get('jobs', [])

    def res = applyStrategy(regex, dryrun, only)

    echo "Commit-skip regex: ${regex}"
    echo(dryrun ? "DRY RUN - no jobs were saved" : "Changes saved")
    echo "Updated (${res['updated'].size()}): ${res['updated'].join(', ')}"
    echo "Already set / skipped (${res['skipped'].size()}): ${res['skipped'].join(', ')}"

    return res['updated']
}
