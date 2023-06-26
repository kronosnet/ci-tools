def call(String url, String msg) {
    // this is upstream_repo from Jenkins file.
    // we might be able to get it differently?
    s = url.split("/")
    repo = s[3].substring(0,s[3].length()-4)

    // https://src.fedoraproject.org/api/0/#pull_requests-tab -> Comment on a Pull Request
    commenturl = "https://pagure.io/api/0/${repo}/pull-request/${cause}/comment"

    postresponsejson = sh (
	script: 'curl -s -H "Content-type: application/x-www-form-urlencoded" -H "Authorization: token ${paguresecret}" -X POST -d "comment=' + msg + '" ' + "${commenturl}",
	returnStdout: true)

    println(postresponsejson)
}
