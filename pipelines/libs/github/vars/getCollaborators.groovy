// needs to be run under withCredentials
def call(String url)
{
    println("getCollab: url="+url)
    // Turn the URL into project/repo
    def s = url.split("/")
    def owner = s[3]
    def repo = s[4]
    def url = "https://api.github.com/repos/${owner}/${repo}/collaborators"

    def collabs = sh (
	script: 'curl -s -u $GIT_USERNAME:$GIT_PASSWORD ' + "${url}",
	returnStdout: true)

    if (collabs == null) {
	println("Failed to get collaborators from Github")
	return []
    }

    def parsed = readJSON text: collabs
    def cnum = parsed.login.size()

    collabs = []
    for (def i=0; i<cnum; i++ ) {
        if ((parsed.role_name[i] == "admin") || (parsed.role_name[i] == "write")) {
             collabs.add(parsed.login[i])
        }
    }
    return collabs
}
