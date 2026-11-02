package org.kronosnet.ci

class ProjectFactory implements Serializable {

    // Map of project names to their classes
    private static final Map<String, Class<? extends Project>> PROJECTS = [
        'pacemaker': Pacemaker,
        'pcs': Pcs,
        'kronosnet': Kronosnet,
        'corosync': Corosync,
        'booth': Booth,
        'sbd': Sbd,
        'dlm': Dlm,
        'fence-agents': FenceAgents,
        'libqb': Project,
        'resource-agents': Project,
        'gfs2-utils': Project,
        'corosync-qdevice': Project
    ]

    /**
     * Create a project instance for the given project name.
     * Falls back to base Project class if no specific implementation exists.
     */
    static Project create(String projectName, steps, Map info, String node) {
        Class<? extends Project> projectClass = PROJECTS.get(projectName, Project)
        return projectClass.newInstance(steps, info, node)
    }
}
