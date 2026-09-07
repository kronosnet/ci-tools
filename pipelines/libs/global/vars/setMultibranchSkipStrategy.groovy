// Usage from a pipeline:
//   setMultibranchSkipStrategy(regex: '\\[(ci skip|skip ci)\\]',
//                              dryrun: true,
//                              jobs: ['kronosnet', 'corosync'])   // [] = all

import jenkins.model.Jenkins
import jenkins.branch.BranchSource
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import com.igalg.jenkins.plugins.multibranch.buildstrategy.ExcludeMessageBranchBuildStrategy

@NonCPS
private boolean reconcileStrategy(BranchSource bs, Class type, def desired, Closure sameValue) {
    def strategies = new ArrayList(bs.buildStrategies)
    def existing = strategies.findAll { type.isInstance(it) }

    if (desired == null) {
        if (existing.isEmpty()) {
            return false
        }
        strategies.removeAll(existing)
        bs.setBuildStrategies(strategies)
        return true
    }

    if (existing.size() == 1 && sameValue(existing[0])) {
        return false
    }

    strategies.removeAll(existing)
    strategies.add(desired)
    bs.setBuildStrategies(strategies)
    return true
}

@NonCPS
def applyStrategy(String regex, String regions, boolean dryrun, String project) {
    def updated = []
    def skipped = []

    for (mbp in Jenkins.instance.getAllItems(WorkflowMultiBranchProject)) {
        if (project != mbp.fullName) {
            echo "MBP name is : ${mbp.fullName}"
            echo "Looking for : ${project}"
            continue
        }

        boolean changed = false
        for (BranchSource bs in mbp.sources) {

            if (regex != null) {
                changed |= reconcileStrategy(bs,
                    ExcludeMessageBranchBuildStrategy,
                    new ExcludeMessageBranchBuildStrategy(regex),
                    { it.excludedMessages == regex })
            }

            if (regions != null) {
                def desired = regions.trim() ? new ExcludeRegionByFieldBranchBuildStrategy(regions) : null
                changed |= reconcileStrategy(bs,
                    ExcludeRegionByFieldBranchBuildStrategy,
                    desired,
                    { it.excludedRegions == regions })
            }
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

@NonCPS
private String normalizeRegions(def regions) {
    if (regions == null) {
        return null
    }
    if (regions instanceof List) {
        return regions.join('\n')
    }
    return regions.toString()
}

def call(String project, String regex = '\\[(ci skip|skip ci)\\]', String repo_files, boolean dryrun) {

    def res = applyStrategy(project, regex, normalizeRegions(repo_files), dryrun)

    echo "Commit-skip regex: ${regex}"
    echo "Changed-file skip regions: ${regions == null ? '(unchanged)' : (regions.trim() ?: '(cleared)')}"
    echo(dryrun ? "DRY RUN - no jobs were saved" : "Changes saved")
    echo "Updated (${res['updated'].size()}): ${res['updated'].join(', ')}"
    echo "Already set / skipped (${res['skipped'].size()}): ${res['skipped'].join(', ')}"

    return res['updated']
}
