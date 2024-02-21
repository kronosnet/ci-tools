// Generate the jobs
def call(String dryrun, Map info)
{
    // Cloud providers and their limits
    def providers = [:]
    providers['osp'] = ['maxjobs': 3, 'testlevel': 'all']
    providers['ocpv'] = ['maxjobs': 3, 'testlevel': 'smoke']
//    providers['aws'] = ['maxjobs': 1, 'testlevel': 'smoke']
//    providers['ibmvpc'] = ['maxjobs': 255, 'testlevel': 'all']

    // OS/upstream versions as pairs
    def versions = [['8', 'next-stable'], ['9', 'next-stable'], ['9','main']]
    def zstream = ['no','yes']

    // Build a list of possible jobs
    def jobs = []
    for (v in versions) {
	for (b in zstream) {
	    jobs += ['rhelver': v[0], 'zstream': b, 'upstream': v[1]]
	}
    }

    // Build the jobs, and decide what can be run in parallel and what
    // needs to be serialised
    def runjobs = [:]
    for (p in providers) {
	def prov = p.key
	def pinfo = providers[prov]

	// Work out how many stages we need to run in serial to keep under the
	// provider's instance limit
	def jobs_per_stage = Math.max(Math.round((jobs.size() / pinfo['maxjobs'])), 1)

	// And divide them up...
	def s = 0
	while (s < jobs.size()) {
	    def start_s = s
	    def joblist = []
	    for (i=0; i < jobs_per_stage &&  s < jobs.size(); i++) {
		joblist += jobs[s]
		s += 1
	    }
	    runjobs["${prov} ${start_s+1}-${s}"] = { runTestStages(['provider': prov, 'pinfo': pinfo, 'jobs': joblist,
								    'dryrun': dryrun], info) }
	}
    }

    // Set up info[:] for 'stages' runs
    info['state'] = 'completed'
    info['stages_run'] = 0
    info['stages_fail'] = 0
    info['stages_fail_nodes'] = ''

    // Feed this into 'parallel'
    return runjobs
}

// Called from parallel to run on one node
def runTestStages(Map stageinfo, Map info)
{
    def provider = stageinfo['provider']
    def pinfo = stageinfo['pinfo']
    def run_all = false

    println("runComplexStage: ${stageinfo}")

    for (s in stageinfo['jobs']) {
	stage("${provider} rhel${s['rhelver']} zstream:${s['zstream']} ${s['upstream']} smoke") {
	    info['stages_run']++
	    def thisjob1 = build job: 'global/ha-functional-testing',
		parameters: [[$class: 'LabelParameterValue', name: 'provider', label: provider],
			     string(name: 'dryrun', value : "${stageinfo['dryrun']}"),
			     string(name: 'rhelver', value: "${s['rhelver']}"),
			     string(name: 'zstream', value: "${s['zstream']}"),
			     string(name: 'upstream', value: "${s['upstream']}"),
			     string(name: 'tests', value: 'smoke')]

	    // These jobs always post SUCCESS (unless things went BADLY wrong),
	    // so we need to look into exported variable STAGES_FAIL to see what really happened
	    if (thisjob1.result == 'SUCCESS' &&
		thisjob1.buildVariables != null &&
		thisjob1.buildVariables['STAGES_FAIL'] == '0') {
		run_all = true
	    } else {
		info['stages_fail']++
		info['stages_fail_nodes'] += "\n- rhel${s['rhelver']} ${s['zstream']} ${s['upstream']} smoke"

		// Mark the stage as failed
		catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
		    shNoTrace("exit 1", "Marking this stage as a failure")
		}
	    }
	}

	// If that succeeds, and provider allows 'all', then run the other tests
	if (run_all && pinfo['testlevel'] == 'all') {
	    stage("${provider} rhel${s['rhelver']} zstream:${s['zstream']} ${s['upstream']} all") {
		info['stages_run']++
		def thisjob2 = build job: 'global/ha-functional-testing',
		    parameters: [[$class: 'LabelParameterValue', name: 'provider', label: provider],
				 string(name: 'dryrun', value : "${stageinfo['dryrun']}"),
				 string(name: 'rhelver', value: "${s['rhelver']}"),
				 string(name: 'zstream', value: "${s['zstream']}"),
				 string(name: 'upstream', value: "${s['upstream']}"),
				 string(name: 'tests', value: 'all')]

		// These jobs always post SUCCESS (unless things went BADLY wrong),
		// so we need to look into exported variable STAGES_FAIL to see what really happened
		if (thisjob2.result == 'SUCCESS' &&
		    thisjob2.buildVariables != null &&
		    thisjob2.buildVariables['STAGES_FAIL'] == '0') {
		    println("That went well")
		} else {
		    info['stages_fail']++
		    info['stages_fail_nodes'] += "\n- rhel${s['rhelver']} ${s['zstream']} ${s['upstream']} ${stageinfo['tests']}"

		    // Mark the stage as failed
		    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
			shNoTrace("exit 1", "Marking this stage as a failure")
		    }
		}
	    }
	}
    }
}
