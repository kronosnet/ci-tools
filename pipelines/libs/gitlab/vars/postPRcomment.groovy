// Post a comment to a GitLab merge request
// Must be run within withCredentials block
def call(String msg) {
    // Get MR IID from environment - only available for MR builds
    def mrIid = env.CHANGE_ID

    if (!mrIid) {
        println("No MR IID found - not posting comment (this is not an MR build)")
        return
    }

    // Get MR URL and extract project path
    // env.CHANGE_URL looks like: https://gitlab.com/fabbione/knet-ci-test/-/merge_requests/1
    def mrUrl = env.CHANGE_URL
    if (!mrUrl) {
        println("No CHANGE_URL found - cannot determine project path")
        return
    }

    // Parse project path from URL
    def urlParts = mrUrl.split('/')
    def owner = urlParts[3]
    def repo = urlParts[4]
    def projectPath = "${owner}/${repo}".replaceAll('/', '%2F')

    // GitLab API endpoint for posting MR notes (comments)
    def apiUrl = "https://gitlab.com/api/v4/projects/${projectPath}/merge_requests/${mrIid}/notes"

    // Escape message for JSON - replace quotes and newlines
    def escapedMsg = msg.replaceAll('"', '\\\\"').replaceAll('\n', '\\\\n')

    println("Posting comment to MR !${mrIid} in ${owner}/${repo}")

    sh """
        curl -s --request POST \
             --header \"PRIVATE-TOKEN: \$GIT_PASSWORD\" \
             --header 'Content-Type: application/json' \
             --data '{"body": "${escapedMsg}"}' \
             ${apiUrl}
    """
}
