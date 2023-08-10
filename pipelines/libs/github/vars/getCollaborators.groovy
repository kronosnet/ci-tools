// needs to be run under withCredentials
def call(String url)
{
    println("getCollab: url="+url)
    // Turn the URL into project/repo
    s = url.split("/")
    owner = s[3]
    repo = s[4]
    url = "https://api.github.com/repos/${owner}/${repo}/collaborators"

    collabs = sh (
	script: 'curl -s -u $GIT_USERNAME:$GIT_PASSWORD ' + "${url}",
	returnStdout: true)

    if (collabs == null) {
	println("Failed to get collaborators from Github")
	return []
    }

    parsed = readJSON text: collabs
    cnum = parsed.login.size()

    collabs = [];
    for (i=0; i<cnum; i++ ) {
        if ((parsed.role_name[i] == "admin") || (parsed.role_name[i] == "write")) {
             collabs.add(parsed.login[i])
        }
    }
    return collabs
}
