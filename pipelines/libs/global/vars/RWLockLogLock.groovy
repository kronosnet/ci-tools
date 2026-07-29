def call(String message, String mode, String lockname, String stagename) {
    def outfile = new FileWriter("${JENKINS_HOME}/logs/locking.log", true)

    def now = new Date()
    def datetext = now.format('YYYY-MM-dd HH:mm')
    def short_job = env.BUILD_URL - env.JENKINS_URL

    outfile.write("${datetext} ${lockname} ${message} for ${mode} ${short_job} Stage ${stagename}\n")

    outfile.flush()
    outfile.close()
}
