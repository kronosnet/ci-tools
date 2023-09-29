import Jenkins.model.*

// Functions to return nodes as known to Jenkins.
// not to be confused with the nodes in getNodes() which are our
// own labels.

// Returns all nodes that have the Jenkins (real) label.
// labelled NonCPS so it runs (effectively) atomically
// without saving state.
@NonCPS
def doGetLabelledNodes(String label) {
    def nodelist = []
    for (thisAgent in jenkins.model.Jenkins.instance.nodes) {
	labelarray = thisAgent.labelString.split(' ')
        if (label == '' || labelarray.contains(label)) {
            nodelist += thisAgent.name
        }
    }
    return nodelist
}

// Get ALL nodes
def call()
{
    return doGetLabelledNodes('')
}

// Get nodes with a specific label
def call(String label)
{
    return doGetLabelledNodes(label)
}
