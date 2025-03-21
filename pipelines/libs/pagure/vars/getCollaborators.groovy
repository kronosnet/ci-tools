// does not requires credentials
def call(String url)
{
    // Turn the URL into project/repo
    def s = url.split("/")
    def repo = s[3].substring(0,s[3].length()-4)

    // https://src.fedoraproject.org/api/0/#projects-tab -> Contributors of a project
    def collabsurl = "https://pagure.io/api/0/${repo}/contributors"

    def collabsjson = sh (
	script: 'curl -s ' + "${collabsurl}",
	returnStdout: true)

    if (collabsjson == null) {
	println("Failed to get collaborators from Pagure")
    }

    def parsed = readJSON text: collabsjson

    collabs = [];

    // https://pagure.io/knet-ci-test/adduser
    //
    // explains different access levels. filter out "ticket" as it has no permission
    // to touch anything other than issues.
    //
    // a collaborator has privileges to access all or a selected set of branches.
    // hence it can be trusted to do a PR without the need to check the target branch.

    // add project users first
    def users_roles = parsed.users.keySet()
    users_roles.eachWithIndex { user_role, idx ->
	if (user_role != "ticket") {
	    users = parsed.users.get(user_role)
	    if (users) {
		users.eachWithIndex { user, i ->
		    collabs.add(user)
		}
	    }
	}
    }

    // process and add project groups
    def groups_roles = parsed.groups.keySet()
    groups_roles.eachWithIndex { group_role, idx ->
	if (group_role != "ticket") {
	    groups = parsed.groups.get(group_role)
	    if (groups) {
		// https://src.fedoraproject.org/api/0/#groups-tab -> Group information
		groups.eachWithIndex { group, i ->
		    groupurl = "https://pagure.io/api/0/group/${group}"
		    groupjson = sh (
			script: 'curl -s ' + "${groupurl}",
			returnStdout: true)

		    if (groupjson == null) {
			println("Failed to get group members from Pagure")
		    }

		    groupparsed = readJSON text: groupjson

		    members_size = groupparsed.members.size()
		    for (i=0; i<members_size; i++) {
			collabs.add(groupparsed.members[i])
		    }
		}
	    }
	}
    }

    return collabs
}
