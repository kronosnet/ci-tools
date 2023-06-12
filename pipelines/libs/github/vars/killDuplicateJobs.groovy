// Find any duplicate jobs for this PR (and only for PRs)
// and kill them. There's no point in running old jobs and
// the most likely candidate for this would be an old
// external PR still waiting for admin approval.
//
// Based on https://www.shellhacks.com/jenkins-list-all-jobs-groovy-script/
//
def call(Map info)
{
    // Only do this for PR runs. We need to keep all merge builds etc
    if (env.JOB_NAME.contains("PR-")) {

	// IndexOf() or parseInt() can throw exceptions, if that happens
	// we just ignore this bit and carry on without doing anything
	try {
	    Jenkins.instance.getAllItems(Job).each{
		def jobBuilds=it.getBuilds()
		for (i = 0; i< jobBuilds.size(); i++) {
		    jobBuilds[i].each { build ->
			if (build.isBuilding()) {
			    def String name = build // convert it to an actual string
			    def this_jobname = name.substring(0, name.indexOf(' '))
			    def this_jobnum = name.substring(name.indexOf('#')+1)

			    // Kill only older jobs from our PR.
			    // The chances of newer ones showing up here is small but !=0
			    if (this_jobname == env.JOB_NAME &&
				this_jobnum != env.BUILD_ID &&
				Integer.parseInt(this_jobnum) < Integer.parseInt(env.BUILD_ID)) {

				// Document it
				println("*** Duplicate job killed: ${this_jobname} (${this_jobnum})")
				info['email_extra_text'] += """Duplicate job killed: ${this_jobname} (${this_jobnum})
"""
				// Do it
				build.doKill()
			    }
			}
		    }
		}
	    }
	} catch (err) {
	    println("Error caught in killDuplicateJobs() "+ err);
	}
    }
}
