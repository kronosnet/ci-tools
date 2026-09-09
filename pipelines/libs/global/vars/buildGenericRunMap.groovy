// Split out params and run the job
def runParameterisedJob(String job, Map info)
{
    // Extract the params
    def params = []
    def jobsplit = job.split(';') // semi-colon separates job from its params
    def jobname = jobsplit[0]
    for (def i=1; i<jobsplit.size(); i++) {
	def parambits = jobsplit[i].split('=')
	params += string(name:parambits[0], value:parambits[1])
    }

    def thisJob = doBuildJob(jobname, params, info)
    info['stages_run'] += 1
    if (thisJob.result != 'SUCCESS') {
	info['stages_fail'] += 1
	info['stages_fail_nodes'] += "${jobname} "
	if (info['email_extra_text'] == '') {
	    info['email_extra_text'] = "Failed jobs:"
	}
	def LOG_URL = thisJob.absoluteUrl.replaceAll('haci.fast.eng.rdu2.dc.redhat.com', 'ci.kronosnet.org')
	info['email_extra_text'] += "\n${LOG_URL}"
    }
}

// Generate a run-list
def call(Map info, ArrayList jobs)
{
    def collectBuildEnv = [:]

    for (def j in jobs) {
	def job = j // needs to be local to the loop
	collectBuildEnv[job] = {
	    runParameterisedJob(job, info)
	}
    }

    return collectBuildEnv
}
