// Unified GitLab status update function
// Calls both updateGitlabCommitStatus (plugin) and posts to MR ref (our fix)
// Handles credentials internally
def call(Map info, Map params) {
    def state = params['state']  // pending, running, success, failed, canceled
    def description = params.get('description', '')
    def name = params.get('name', 'jenkins-ci')

    // Always call the plugin's updateGitlabCommitStatus for branch ref
    updateGitlabCommitStatus name: name, state: state

    //For MR builds, also post to the correct MR ref to fix visibility issue
    if (info['isPullRequest']) {
        def cred_uuid = getCredUUID()
        withCredentials([string(credentialsId: cred_uuid, variable: 'GIT_PASSWORD')]) {
            postMRRef(info, state, description, name)
        }
    }
}

// Internal function to post status to MR ref
// Must be called within withCredentials block
private def postMRRef(Map info, String state, String description, String name) {
    def mrIid = info['pull_id']
    def sha = env.GIT_COMMIT
    def mrUrl = env.CHANGE_URL

    if (!mrUrl) {
        println("updateGitlabStatus: No CHANGE_URL found - skipping MR ref status post")
        return
    }

    // Parse project path from URL
    def urlParts = mrUrl.split('/')
    def owner = urlParts[3]
    def repo = urlParts[4]
    def projectPath = "${owner}/${repo}".replaceAll('/', '%2F')

    // Use MR ref instead of branch ref - this fixes the visibility issue
    def ref = "refs/merge-requests/${mrIid}/head"

    def apiUrl = "https://gitlab.com/api/v4/projects/${projectPath}/statuses/${sha}"
    def escapedDesc = description.replaceAll('"', '\\\\"').replaceAll('\n', '\\\\n')
    def targetUrl = env.BUILD_URL

    println("updateGitlabStatus: Posting ${state} to MR !${mrIid} with ref=${ref}")

    sh """
        curl -s --request POST \
             --header "PRIVATE-TOKEN: \$GIT_PASSWORD" \
             --header 'Content-Type: application/json' \
             --data '{"state": "${state}", "ref": "${ref}", "name": "${name}", "target_url": "${targetUrl}", "description": "${escapedDesc}"}' \
             ${apiUrl}
    """
}
