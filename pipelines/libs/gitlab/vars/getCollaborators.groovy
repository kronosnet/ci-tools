// needs to be run under withCredentials
def call(String url)
{
    println("getCollab: url="+url)

    // Parse GitLab URL: https://gitlab.com/owner/repo/-/merge_requests/1
    // or https://gitlab.com/owner/repo
    def s = url.split("/")
    def owner = s[3]
    def repo = s[4]

    // URL-encode the project path for GitLab API
    def projectPath = "${owner}/${repo}".replaceAll('/', '%2F')

    // GitLab API endpoint for project members
    def apiUrl = "https://gitlab.com/api/v4/projects/${projectPath}/members/all"

    def collabs = sh (
	script: "curl -s --header \"PRIVATE-TOKEN: \$GIT_PASSWORD\" ${apiUrl}",
	returnStdout: true)

    if (collabs == null || collabs.trim().isEmpty()) {
	println("Failed to get members from GitLab - empty response")
	return []
    }

    def parsed = readJSON text: collabs
    def members = []

    // GitLab access levels: 30=Developer, 40=Maintainer, 50=Owner
    // We allow Developer and above to submit MRs
    for (def i = 0; i < parsed.size(); i++) {
        def member = parsed[i]
        if (member != null && member.access_level != null && member.access_level >= 30) {
            members.add(member.username)
        }
    }

    println("GitLab project members with access level >= 30: ${members}")
    return members
}
