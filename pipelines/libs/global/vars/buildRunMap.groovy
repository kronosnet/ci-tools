
import jenkins.model.*

// Returns all nodes that have the label.
// labelled NonCPS so it runs (effectively) atomically
// without saving state.
@NonCPS
def getLabelledNodes(String label) {
    def nodelist = []
    for (thisAgent in jenkins.model.Jenkins.instance.nodes) {
	labelarray = thisAgent.labelString.split(' ')
        if (labelarray.contains(label)) {
            nodelist += thisAgent.name
        }
    }
    return nodelist
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

    // These are nodes that are deliberately switched off and should be ignored
    def downnodes = getLabelledNodes('down')
    println('"Down" nodes: '+downnodes)

    for (i=0; i<nodeList.size(); i++) {
        def agentName = nodeList[i]

        // Skip any null entries and exclusions
        if (agentName != null && !agentName.matches(exclude_regexp) &&
	    !downnodes.contains(agentName)) {
            collectBuildEnv[label + '_' + agentName] = {
		// This works because runStage is also in the global library
                runStage(info, agentName, label, voting, extravars)
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

    return buildTheRunMap(nodeList, joblabel, info, voting, extravars, excludes)
}
