// doBuildJob.groovy

// Runs an external job, but keeps a note of it in info[:]
// so it can be cleaned up if this job gets aborted.
def call(String jobname, ArrayList params, Map info)
{
    def a = build job: jobname,
		  parameters: params,
		  propagate: false,
		  waitForStart: true,
		  wait: false

    // Save it in case we get aborted before it all completes
    String jobId = "${a.getFullProjectName()} #${a.getId()}"
    info['subjobs'] += jobId

    // If we are already aborted then just exit here and let
    // the post routine tidy up.
    // TBH I'm not sure this can happen, but if it can it
    // will speed things up :)
    if (currentBuild.result == 'ABORTED') {
	println("Not waiting for ${jobId} as we are aborting")
	return a
    }

    waitForBuild a.externalizableId

    // If it finished then we can remove it from the list
    info['subjobs'] -= jobId

    // Callers often want this
    return a
}
