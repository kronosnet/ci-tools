
import jenkins.model.*


// Given a full list of nodes, remove all of those NOT in the list for this project
def nodeRestrictions(ArrayList nodeList, String project)
{
    // List of nodes that have project restrictions
    // If a node is in this list then it should only be used for
    // the listed projects
    def restrict = [:]
    restrict['openindiana-x86-64'] = ['libqb', 'corosync', 'kronosnet', 'ci-test']

    def removelist = []
    for (def n in nodeList) {
	if (restrict.containsKey(n)) {
	    if (!restrict[n].contains(project)) {
		removelist += n
	    }
	}
    }
    println("nodeRestrictions: removelist=" + removelist)
    return removelist
}



// If bootstrap == 1 then there are things we don't need to do
def optimiseOut(Map info, Map extras, String stageName)
{
    def optimise_out = true
    def publishrpm = info['publishrpm']
    def covinstall = info['covinstall']
    def install = info['install']

    // extras[:] can override these
    if (extras.containsKey('publishrpm')) {
	publishrpm = extras['publishrpm']
    }
    if (extras.containsKey('install')) {
	install = extras['install']
    }
    if (extras.containsKey('covinstall')) {
	covinstall = extras['covinstall']
    }
    println("stage: ${stageName}, extras: "+extras)

    // This view inside Fabio's twisted mind, is an optimisation
    // so we don't run stuff that doesn't need to be run when bootstrapping
    if (info['bootstrap'] == 1) {
	if ((publishrpm == 1) && stageName.endsWith('buildrpms')) {
	    optimise_out = false
	}
	if ((install == 1) && stageName.endsWith('voting')) { // catches voting AND non-voting
	    optimise_out = false
	}
	if ((covinstall == 1) && stageName.endsWith('covscan')) {
	    optimise_out = false
	}
	if (optimise_out) {
	    println("Stage ${stageName} skipped. bootstrap==1 && install==${install}, covinstall==${covinstall}, publishrpm==${publishrpm}")
	}
    } else {
	// Just get on with it
	optimise_out = false
    }
    return optimise_out
}

// This is the bit that does most of the work
def buildTheRunMap(List nodeList, String label, Map info, Boolean voting, Map extravars, String exclude_regexp) {
    def collectBuildEnv = [:]

    def project_opts = getProjectProperties([:], 'builddefs')
    def enable_debug = false
    if (project_opts.containsKey('DEBUGJOBS') &&
	project_opts['DEBUGJOBS'].contains(label) &&
	info['bootstrap'] != 1) {
	enable_debug = true
    }

    // These are nodes that are deliberately switched off and should be ignored
    def downnodes = getLabelledNodes('down')
    println('"Down" nodes: '+downnodes)

    for (def i=0; i<nodeList.size(); i++) {
        def agentName = nodeList[i]

	def restrictions = nodeRestrictions(nodeList, info['project'])

        // Skip any null entries and exclusions
        if (agentName != null && !agentName.matches(exclude_regexp) &&
	    !downnodes.contains(agentName) && !restrictions.contains(agentName)) {
	    collectBuildEnv[label + '_' + agentName] = {
		// This works because runStage is also in the global library
                runStage(info, agentName, label, voting, extravars)
            }
	    // Do we also want to run a debug build for this target?
	    if (enable_debug) {
		def debug_extras = extravars + ['debug': project_opts['DEBUGOPTS']]
		debug_extras['CHECKS'] = 'check'
		debug_extras['install'] = '0'
		collectBuildEnv[label + '_debug_' + agentName] = {
		    runStage(info, agentName, "${label}_debug", voting, debug_extras)
		}
            }
	}
    }
    printf("run map for ${label}: "+collectBuildEnv)
    return collectBuildEnv
}

// A 'basic' version
def call(String label, Map info, Boolean voting) {
    def nodeList = getNodes(label)
    return buildTheRunMap(nodeList, label, info, voting, '')
}

// A full-fat API that allows all the combinations
// and does the bootstrap optimisation
def call(String joblabel, Map info, Map options)
{
    // This needs to be first so we have 'extravars'
    def extravars = [:]
    if (options.containsKey('extravars')) {
	extravars = options['extravars']
    }

    // Do we need to do this at all?
    if (optimiseOut(info, extravars, joblabel)) {
	return [:]
    }

    // Explicitly specified nodes
    def nodeList = []
    if (options.containsKey('nodelist')) {
	nodeList = options['nodelist']
    }
    // Nodes with this label
    if (options.containsKey('nodelabel')) {
	nodeList += getNodes(options['nodelabel'])
    }
    // but exclude this regexp
    def excludes = ''
    if (options.containsKey('excludes')) {
	excludes = options['excludes']
    }
    // Voting or not
    def voting = false
    if (options.containsKey('voting')) {
	voting = options['voting']
    }
    // If 'bootstrap' is set (full rebuild) then all
    // jobs should be 'voting' so we can spot failures.
    if (info['bootstrap'] == 1) {
	voting = true
    }

    return buildTheRunMap(nodeList, joblabel, info, voting, extravars, excludes)
}
