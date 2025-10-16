
// Return a list of possible jobs
def buildJobList(String job_type)
{
    // OS/upstream versions as pairs
    def versions = [['rhel8', 'none'], ['rhel8', 'stable'],
		    ['rhel9', 'none'], ['rhel9', 'stable'], ['rhel9', 'next-stable'], ['rhel9', 'main'],
		    ['rhel10', 'none'], ['rhel10', 'next-stable'], ['rhel10', 'main'],
		    ['centos10', 'none'], ['centos10', 'next-stable'], ['centos10', 'main']]
    def zstream = ['no','yes']

    def joblist = []
    for (def v in versions) {
	// Only RHEL has Zstreams
	if (v[0].substring(0, 4) == 'rhel') {
	    for (def b in zstream) {
		joblist += ['osver': v[0], 'zstream': b, 'upstream': v[1], 'testlevel': job_type]
	    }
	} else {
	    joblist += ['osver': v[0], 'zstream': 'no', 'upstream': v[1], 'testlevel': job_type]
	}
    }
    return joblist
}

// sort needs to be NonCPS
@NonCPS
def sort_jobs(ArrayList alljobs)
{
    return alljobs.sort{ it.eligible_providers }
}

// Build a string containing the name of the current pipeline stage
def mkstagename(Map job)
{
    return "${job['osver']} zstream:${job['zstream']} upstream:${job['upstream']}"
}

// MAIN entry point for this call.
// I've set it up like this really just to keep all the code in one file
// as it's all related, and uses a few common dependancies
def call(String jobtype, String provider, String dryrun, Map info, Map provider_failflags)
{
    if (jobtype == 'smoke') {
	return genSmokeJobs(dryrun, provider, info)
    } else {
	return genAllJobs(dryrun, provider, info, provider_failflags)
    }
}


// Edit the providers list according to the 'provlist' param
def limitProviders(String provlist, Map providers)
{
    if (provlist == 'all') {
	return providers
    }

    def new_providers = [:]
    for (def i in providers) {
	if (provlist.contains(i.key)) {
	    new_providers[i.key] = i.value
	}
    }
    return new_providers
}

def genSmokeJobs(String dryrun, String provider_param, Map info)
{
    // Cloud providers and their limits
    def providers = limitProviders(provider_param, getProviderProperties())

    // List of jobs
    def smokejobs = buildJobList('smoke')

    // Build the jobs list for each provider
    def smoke_matrix = [:]
    def prov_failflags = [:]
    for (def p in providers) {
	def provider_jobs = [:]
	def prov = p.key
	def pinfo = p.value

	// We don't run weekly jobs for all providers
	if (pinfo['weekly']) {
	    provider_jobs['provider'] = prov
	    provider_jobs['pinfo'] = pinfo
	    provider_jobs['smokejobs'] = smokejobs

	    // Make a copy of smokejobs per provider as the 'scheduler' removes items
	    // from this list as they get run. At this stage we also remove
	    // jobs for unsupported RHEL versions
	    def provider_smokejobs = []
	    for (def sj in smokejobs) {
		if (pinfo['vers'].contains(sj['osver'])) {
		    provider_smokejobs += sj
		}
	    }

	    // If 'maxjobs' is set to 0 then that means we can run as many instances
	    // as we like, so schedule as many jobs as there are 'smoke' tests
	    def maxjobs = pinfo['maxjobs']
	    if (maxjobs == 0) {
		maxjobs = provider_smokejobs.size()
	    }

	    // Clear out the 'FAIL' status for all smoke jobs
	    def failflags = [:]
	    for (def s in provider_smokejobs) {
		failflags[mkstagename(s)] = false
	    }

	    provider_jobs['failflags'] = failflags
	    prov_failflags[prov] = failflags
	    // Set up the runners. create 'maxjobs' runners per provder
	    for (def i = 1; i <= maxjobs; i++) {
		smoke_matrix["${prov} ${i}"] = { runTestList(provider_jobs, info, provider_smokejobs, prov_failflags, dryrun) }
	    }
	}
    }

    // Set up info[:] for 'stages' runs
    info['state'] = 'completed'
    info['stages_run'] = 0
    info['stages_fail'] = 0
    info['stages_fail_nodes'] = ''

    // Feed the first of these into parallel, and the second into genAllJobs once they have all run
    return new Tuple2(smoke_matrix, prov_failflags)
}

// Now we know what failed in the 'smoke' tests, schedule
// the 'all' tests as efficiently as possible
def genAllJobs(String dryrun, String provider_param, Map info, Map prov_failflags)
{
    def all_matrix = [:]

    // Work out how many providers each job can run on.
    // schedule them (1...<n>) on to the least busy suitable provider
    def alljobs = buildJobList('all')
    def providers = limitProviders(provider_param, getProviderProperties())

    for (def job in alljobs) {
	job['eligible_providers'] = 0
	for (def p in providers) {
	    if (p.value['weekly'] && p.value['testlevel'] == 'all') {
		def provider = p.key
		def failflags = prov_failflags[provider]
		def stagename = mkstagename(job)
		if (failflags[stagename] == false) {
		    job['eligible_providers'] += 1
		}
	    }
	}
    }

    // Clear out/initialise some things
    for (def p in providers) {
	p.value['numjobs'] = 0
	p.value['alljobs'] = []
    }

    // Sort by eligible_providers.
    // This means that all the jobs that can only run on 1 provider
    // get allocated first.
    def sorted_jobs = sort_jobs(alljobs)
    for (def job in sorted_jobs) {
	def stagename = mkstagename(job)
	def least_busy = null

	// Find the least busy provider that can run this job
	for (def p in providers) {
	    def provider = p.key
	    def pinfo = p.value
	    if (pinfo['weekly'] == true &&
		pinfo['testlevel'] == 'all' &&
		prov_failflags[provider][stagename] == false &&
		pinfo['vers'].contains(job['osver'])) {

		if (least_busy == null ||
		    pinfo['numjobs'] < providers[least_busy]['numjobs']) {
		    least_busy = p.key
		    println("${provider} eligible for job ${stagename}")
		}
	    }
	}
	// Give this job to the least busy, eligible, provider
	if (least_busy != null) {
	    providers[least_busy]['alljobs'] += job
	    providers[least_busy]['numjobs'] += 1
	}
    }

    // Set up run map for the job runners
    for (def p in providers) {
	if (p.value['alljobs'].size() > 0) {
	    def provider_jobs = [:]
	    def prov = p.key
	    def pinfo = p.value

	    provider_jobs['provider'] = prov
	    provider_jobs['pinfo'] = pinfo
	    provider_jobs['alljobs'] = pinfo['alljobs']

	    // Don't start more runners than we really need (or are allowed)
	    def maxjobs = Math.min(pinfo['maxjobs'], pinfo['numjobs'])
	    if (maxjobs == 0) {
		maxjobs = pinfo['alljobs'].size()
	    }
	    for (def i = 1; i <= maxjobs; i++) {
		all_matrix["${prov} ${i}"] = { runTestList(provider_jobs, info, pinfo['alljobs'], [:], dryrun) }
	    }
	}
    }
    // Return a tuple, for consistency with 'smoke'
    return new Tuple2(all_matrix, [:])
}

// Called from parallel to run a list of tests on one node
// Both 'smoke' and 'all' call here
def runTestList(Map provider_jobs, Map info, ArrayList joblist, Map failflags, String dryrun)
{
    def provider = provider_jobs['provider']
    def pinfo = provider_jobs['pinfo']
    def smokejobs = provider_jobs['jobs']
    def found = true
    def runningjob = [:]
    def stagename = ''

    // Dummy stage to get the working names to make sense
    stage("${provider} runner") {
	println("Starting runner thread on ${provider} - joblist: ${joblist}")
    }

    // Jenkins groovy doesn't like do {} while loops
    while (found) {
	found = false

	// Lock this while we find a valid job
	lock("${info['project']} ${provider} runner") {

	    // Iterator makes it easy to delete items
	    // It's also safe because we have the lock
	    def Iterator itr = joblist.iterator();
	    while (found == false && itr.hasNext()) {
		// Find the next job that we can run on this provider
		def s = itr.next()

		runningjob = s
		itr.remove()
		found = true
	    }
	}

	// Run it
	if (found) {
	    stagename = mkstagename(runningjob)
	    info['stages_run']++
	    stage("${provider} ${stagename} ${runningjob['testlevel']}") {

		def (state, url, detail) = run_job(provider, runningjob, dryrun, info)
		if (state != 'SUCCESS') {
		    info['stages_fail']++
		    info['stages_fail_nodes'] += "\n- ${provider} ${stagename} tests:${runningjob['testlevel']}: ${url}"

		    // Format the failure detail
		    if (detail != null && detail.length() > 0) {
			for (d in detail.split('\n')) {
			    if (d.length() > 0) {
				info['stages_fail_nodes'] += "\n  ${d}"
			    }
			}
		    }
		    info['stages_fail_nodes'] += '\n'

		    // Mark stage as failed in Jenkins
		    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
			shNoTrace("exit 1", "Marking this stage as a failure")
		    }

		    // If a smoke test failed, tell future runners on this provider not to run 'all'
		    if (runningjob['testlevel'] == 'smoke') {
			failflags[provider][stagename] = true
		    }
		}
	    }
	}
    }
}

// Does what it says on the tin
def run_job(String provider, Map job, String dryrun, Map info)
{
    // Allow us to fake failures for testing
    def fail_rate = params.failure_rate.toInteger()
    def testlist = 'auto'
    if (fail_rate > 0 && job['testlevel'] == 'smoke') {
	if (new Random().nextInt(100) < fail_rate) {
	    testlist = 'fake_failure'
	    println("Faking failure for ${provider} ${job}")
	}
    }

    // Run it.
    def status = 'FAIL'
    def joburl = 'Aborted'
    def detail = ''
    try {
	def name = mkstagename(job)
	echo "Running job for ${name} on ${provider}, testlist = ${testlist}"

	def thisjob = doBuildJob('global/ha-functional-testing',
				 [[$class: 'LabelParameterValue', name: 'provider', label: provider],
				  string(name: 'dryrun', value : "${dryrun}"),
				  string(name: 'osver', value: "${job['osver']}"), // NOTE change of job params
				  string(name: 'zstream', value: "${job['zstream']}"),
				  string(name: 'upstream', value: "${job['upstream']}"),
				  string(name: 'testlist', value: "${testlist}"),
				  string(name: 'tonodes', value: "2"),
				  string(name: 'tests', value: "${job['testlevel']}")],
				 info)

	// These jobs always post SUCCESS (unless things went BADLY wrong),
	// so we need to look into exported variable STAGES_FAIL to see what really happened
	if (thisjob.result == 'SUCCESS' &&
	    thisjob.buildVariables != null &&
	    thisjob.buildVariables['STAGES_FAIL'] == '0') {
	    status = 'SUCCESS'
	}
	detail = thisjob.buildVariables['FAIL_INFO']
	joburl = thisjob.absoluteUrl
    } catch (err) {
	println("Caught sub-job failure ${err} in ${job['osver']} ${job['zstream']} ${job['upstream']}")
    }
    return new Tuple(status, joburl, detail)
}
