def call(String agentName, Map info)
{
    println("Running update-kcli-cache on ${agentName}")
    node("${agentName}") {
	sh "echo THIS IS SPARTA: ${agentName}"
    }
}
