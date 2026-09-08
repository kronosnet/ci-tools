// Configure the "skip build" strategies on the multibranch project that owns
// the CURRENT build. Safe to call on every run: it only saves when something
// actually changes, and it only ever touches this build's own multibranch job.
//
// Usage from a project pipeline (e.g. from getBuildInfo):
//   setMultibranchSkipStrategy(getProjectSkipRegex(), getProjectSkipFiles(), false)
//
//   regex  - commit-message regex that suppresses a build (matched with find());
//            null leaves the commit-message strategy untouched
//   files  - newline-separated String or List of file globs; a change touching
//            only these paths is skipped. null leaves it untouched, '' clears it.
//   dryrun - true reports what would change without saving

import jenkins.branch.BranchSource
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import com.igalg.jenkins.plugins.multibranch.buildstrategy.ExcludeMessageBranchBuildStrategy
import com.igalg.jenkins.plugins.multibranch.buildstrategy.ExcludeRegionByFieldBranchBuildStrategy

@NonCPS
boolean reconcileStrategy(BranchSource bs, Class type, def desired, Closure sameValue) {
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
boolean applyStrategy(WorkflowMultiBranchProject mbp, String regex, String regions, boolean dryrun) {
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

    if (changed && !dryrun) {
        mbp.save()
    }
    return changed
}

@NonCPS
String normalizeRegions(def regions) {
    if (regions == null) {
        return null
    }
    if (regions instanceof List) {
        return regions.join('\n')
    }
    return regions.toString()
}

def call(String regex = '\\[(ci skip|skip ci)\\]', String files = null, boolean dryrun = false) {
    def owner = currentBuild.rawBuild.getParent().getParent()
    if (!(owner instanceof WorkflowMultiBranchProject)) {
        echo "setMultibranchSkipStrategy: this build is not part of a multibranch project - nothing to do"
        return
    }

    String regions = normalizeRegions(files)
    boolean changed = applyStrategy((WorkflowMultiBranchProject) owner, regex, regions, dryrun)

    echo "setMultibranchSkipStrategy for ${owner.fullName}:"
    echo "  commit-skip regex:    ${regex == null ? '(unchanged)' : regex}"
    echo "  changed-file regions: ${regions == null ? '(unchanged)' : (regions.trim() ?: '(cleared)')}"
    echo "  result:               ${changed ? (dryrun ? 'would change (dry run)' : 'saved') : 'already up to date'}"
}
