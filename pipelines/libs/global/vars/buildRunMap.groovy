
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


// This is the bit that does most of the work
def buildTheRunMap(List nodeList, String label, Map info, Boolean voting, Map extravars, String exclude_regexp) {
    collectBuildEnv = [:]

    // These are nodes that are deliberately switched off and should be ignored
    def downnodes = getLabelledNodes('down')
    println('"Down" nodes: '+downnodes);

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
def call(String joblabel, Map info, Map options)
{
    def nodeList = []
    // Explicitly specified nodes
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
    // Voting
    def voting = false
    if (options.containsKey('voting')) {
	voting = options['voting']
    }

    def extravars = [:]
    if (options.containsKey('extravars')) {
	extravars = options['extravars']
    }

    return buildTheRunMap(nodeList, joblabel, info, voting, extravars, excludes)
}
