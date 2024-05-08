def call(String node_regex, String cmd, String label)
{
    for (n in jenkins.model.Jenkins.instance.nodes) {
	if (n.name.matches(node_regex)) {
	    // Labels as a list
	    def node_handle = Jenkins.instance.getNode(n.name)
	    def labelarray = node_handle.getLabelString().split()

	    if (cmd == 'add') {
		if (!labelarray.contains(label)) { // Don't duplicate it
		    labelarray += label
		}
	    }
	    if (cmd == 'rm') {
		if (labelarray.contains(label)) {
		    labelarray -= label
		}
	    }
	    // Make it back into a string and put it onto the node
	    def labels_str = ''
	    for (i in labelarray) {
		labels_str += " ${i}"
	    }
	    println("Node ${n.name}, labels = ${labels_str}")
	    node_handle.setLabelString(labels_str)
	}
    }
}
