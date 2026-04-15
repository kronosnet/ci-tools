
// Get all nodes that match a specific internal 'label'
// but remove all 'down' nodes
def call(String label)
{
    // Use the polymorph, less list building
    def nodeList = []
    if (label == '') {
	nodeList = getNodes()
    } else {
	nodeList = getNodes(label)
    }

    // Remove the really labelled 'down' nodes
    def downNodes = getLabelledNodes('down')
    def newNodeList = []
    for (def i in nodeList) {
	if (!downNodes.contains(i)) {
	    newNodeList += i
	}
    }
    return newNodeList
}
