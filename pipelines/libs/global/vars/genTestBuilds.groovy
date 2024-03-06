// Generate the jobs
def call(String dryrun, Map info)
{
    // Cloud providers and their limits
    def providers = getProviderProperties()

    // OS/upstream versions as pairs
    def versions = [['8', 'next-stable'], ['9', 'next-stable'], ['9','main']]
    def zstream = ['no','yes']

    // Build a list of possible smoke jobs
    def smokejobs = []
    for (v in versions) {
	for (b in zstream) {
	    smokejobs += ['rhelver': v[0], 'zstream': b, 'upstream': v[1], 'testlevel': 'smoke']
	}
    }

    // All jobs - this one is global to all providers which pick them off to run
    // as available
    def alljobs = []
    for (v in versions) {
	for (b in zstream) {
	    alljobs += ['rhelver': v[0], 'zstream': b, 'upstream': v[1], 'testlevel': 'all']
	}
    }

    // Build the jobs list for each provider
    def all_matrix = [:]
    def smoke_matrix = [:]
    for (p in providers) {
	def provider_jobs = [:]
	def prov = p.key
	def pinfo = p.value

	provider_jobs['provider'] = prov
	provider_jobs['pinfo'] = pinfo
	provider_jobs['smokejobs'] = smokejobs

	// Make a copy of smokejobs per provider as the 'scheduler' removes items
	// from this list as they get run. At this stage we also remove
	// jobs for unsupported RHEL versions
	def provider_smokejobs = []
	for (sj in smokejobs) {
	    if (pinfo['rhelvers'].contains(sj['rhelver'])) {
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
	for (s in provider_smokejobs) {
	    provider_jobs["rhel${s['rhelver']} zstream:${s['zstream']} ${s['upstream']} FAIL"] = false
	}

	// Set up the runners. create 'maxjobs' runners per provder
	for (i = 1; i <= maxjobs; i++) {
	    smoke_matrix["${prov} ${i}"] = { runTestList(provider_jobs, info, 'smoke', provider_smokejobs, dryrun) }
	}
	if (pinfo['testlevel'] == 'all') {
	    for (i = 1; i <= maxjobs; i++) {
		all_matrix["${prov} ${i}"] = { runTestList(provider_jobs, info, 'all', alljobs, dryrun) }
	    }
	}
    }

    // Set up info[:] for 'stages' runs
    info['state'] = 'completed'
    info['stages_run'] = 0
    info['stages_fail'] = 0
    info['stages_fail_nodes'] = ''

    // Feed these into 'parallel', one after the other ...
    return new Tuple2(smoke_matrix, all_matrix)
}

// Called from parallel to run ALL valid tests on one node
def runTestList(Map provider_jobs, Map info, String testtype, ArrayList joblist, String dryrun)
{
    def provider = provider_jobs['provider']
    def pinfo = provider_jobs['pinfo']
    def smokejobs = provider_jobs['jobs']
    def found = true
    def runningjob = [:]
    def stagename = ''

    // Dummy stage to get the working names to make sense
    stage("${provider} runner") {
	println("Starting runner thread on ${provider}")
    }

    // 'smoke' tests only need to lock *this* provider,
    // 'all' tests lock the whole suite
    def lockname = "${info['project']} runner"
    if (testtype == 'smoke') {
	lockname = "${info['project']} ${provider} runner"
    }

    // Jenkins groovy doesn't like do {} while loops
    while (found) {
	found = false

	// Lock this while we find a valid job
	lock(lockname) {
	    for (j=0; j<joblist.size() && found==false; j++) {
		// Find the next job that we can run on this provider
		def s = joblist[j]
		stagename = "rhel${s['rhelver']} zstream:${s['zstream']} ${s['upstream']}"

		// Can we run it?
		if ((provider_jobs["${stagename} FAIL"] == false) && // Smoke ran successfully
		    pinfo['rhelvers'].contains(s['rhelver'])) {      // RHEL version is supported
		    runningjob = s
		    joblist.remove(j)
		    found = true
		}
	    }
	}

	// Run it
	if (found) {
	    info['stages_run']++
	    stage("${provider} ${stagename} ${runningjob['testlevel']}") {

		def state = run_job(provider, runningjob, dryrun)
		if (state != 'SUCCESS') {
		    info['stages_fail']++
		    info['stages_fail_nodes'] += "\n- ${provider} ${stagename} ${runningjob['testlevel']}"

		    // Mark stage as failed in Jenkins
		    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
			shNoTrace("exit 1", "Marking this stage as a failure")
		    }

		    // If a smoke test failed, tell future runners on this provider not to run 'all'
		    if (runningjob['testlevel'] == 'smoke') {
			provider_jobs["${stagename} FAIL"] = true
		    }
		}
	    }
	}
    }
}

// Does what it says on the tin
def run_job(String provider, Map job, String dryrun)
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
    def thisjob = build job: 'global/ha-functional-testing',
	parameters: [[$class: 'LabelParameterValue', name: 'provider', label: provider],
		     string(name: 'dryrun', value : "${dryrun}"),
		     string(name: 'rhelver', value: "${job['rhelver']}"),
		     string(name: 'zstream', value: "${job['zstream']}"),
		     string(name: 'upstream', value: "${job['upstream']}"),
		     string(name: 'testlist', value: "${testlist}"),
		     string(name: 'tests', value: "${job['testlevel']}")]

    // These jobs always post SUCCESS (unless things went BADLY wrong),
    // so we need to look into exported variable STAGES_FAIL to see what really happened
    if (thisjob.result == 'SUCCESS' &&
	thisjob.buildVariables != null &&
	thisjob.buildVariables['STAGES_FAIL'] == '0') {
	return 'SUCCESS'
    } else {
	return 'FAIL'
    }
}
