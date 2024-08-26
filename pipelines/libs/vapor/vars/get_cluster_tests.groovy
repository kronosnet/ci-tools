def call(Map info)
{
    println(info)

    // Some common test groups we use
    pcs_basic_tests = [
	'pcs,cli,Auth',
	'pcs,cli,ClusterCibConcurrentDiff',
	'pcs,cli,ClusterCibPush',  // cib-push command, crucial for system role
	'pcs,cli,ClusterSetupParametrized',
	'pcs,cli,ClusterStartStop',
	'pcs,cli,CVE-2018-1079',
	'pcs,cli,CVE-2018-1086',
	'pcs,cli,DaemonSanity',  // critical HTTP headers
	'pcs,cli,NodeAddRemove',
	'pcs,cli,NodeMaintenance',
	'pcs,cli,NodeStandby',
	'pcs,cli,OperationDefaults',  // resource create with default operations
	'pcs,cli,Properties',
	'pcs,cli,ResourceCreate',
	'pcs,cli,ResourceManageUnmanageMonitor',
	'pcs,cli,ResourceSafeDisable',  // critical feature
	'pcs,cli,rhbz1380372',  // cluster stop when one node fails
	'pcs,cli,rhbz1382004',  // resource create produces invalid cib
	'pcs,cli,rhbz1387106',  // handle utf8 chars in output of subprocesses
	'pcs,cli,rhbz1390609',  // deleting of pacemaker remote causes fencing
	'pcs,cli,rhbz1574898',  // pcs resource debug-* commands
	'pcs,cli,Stonith',
	'pcs,cli,StonithLevel',
	'pcs,cli,Tags'
    ]

    pcs_advanced_tests = [
	'pcs,cli,BackupRestore', // cluster config backup + restore
	'pcs,cli,ClusterAuthkey',  // change corosync authkey
	'pcs,cli,ClusterConfigUpdate', // update corosync.conf
	'pcs,cli,DefaultsSet',  // rsc and op defaults
	'pcs,cli,QuorumDevice',
	'pcs,cli,RemoteGuestNodes',
	'pcs,cli,rhbz1328870',  // pcs command fails right after pcsd starts
	'pcs,cli,rhbz1362493',  // location constraint with rsc-pattern
	'pcs,cli,rhbz1419661',  // systemd services with @ and : in name
	'pcs,cli,rhbz1443418',  // allow to set ids of resource operations
	'pcs,cli,rhbz1502715',  // pcs resource update of a guest-node resource
	'pcs,cli,rhbz1568353',  // don't create empty elemets in cib
	'pcs,cli,SetupRandomPorts',
	'pcs,cli,Timeouts',  // pcs to pcs connection timeout
    ]

    advanced_tests = [
	'pacemaker,recovery,RecoverySwitchFailure',
	'pacemaker,recovery,RecoveryNodeNetworkFailure',
	'pacemaker,recovery,RecoveryRandomMultiNode',
	'pacemaker,recovery,RecoveryRandomMultiNodeNQ',
	'pacemaker,recovery,RecoveryNodeNetworkFailure,sbd-only,network_disruptor',
	'pacemaker,recovery,RecoveryNodeNetworkFailure,sbd-with-one-device,network_disruptor',
    ]

    sanity_tests = [
	'cleanup',
	'setup'
    ]

    smoke_tests = [
	'pacemaker,resource,IPv4,recovery',
	'pacemaker,recovery,RecoveryActiveNode',
	'setup,setup_sbd,sbd-only'
    ]

    basic_tests_rhel8 = [
	'lvm,lvm_config,cluster-lvmlockd'
    ]
    basic_tests_rhel9 = [
	'lvm,lvm_config_no-vdo,cluster-lvmlockd'
    ]
    basic_tests = [
	'pacemaker,recovery,RecoveryRandomNode',
	'pacemaker,recovery,RecoveryRestartPacemaker-AllNodes',
	'pacemaker,recovery,RecoveryResourceFailure',
	'pacemaker,recovery,RecoveryActiveNode,sbd-only,kill_reboot',
	'pacemaker,resource,Apache,recovery',
	'pacemaker,resource,Apache,variant:latency,latency:300ms',
	'pacemaker,resource,Clone',
	'pacemaker,resource,ConstraintsTest',
	'pacemaker,resource,Group',
	'pacemaker,resource,Filesystem',
	'pacemaker,resource,FilesystemGroup',
	'pacemaker,usecase,ha-apache',
	'pacemaker,usecase,NFS_Server-AP,lvmlockd,exclusive'
    ]

    // Define the actual tests to run
    def tests = [:]

    // kernel tests are always run on 3 nodes by design
    tests['kernel,smoke,tests,generic'] = sanity_tests + 'skeet'
    tests['kernel,all,tests,generic'] = tests['kernel,smoke,tests,generic'] + 'lvm,lvm_config_no-vdo,cluster-lvmlockd'
    tests['kernel,all,tags,generic'] = ['brawl_quick']

    tests['pcs,smoke,tests,generic'] = sanity_tests + 'pcs,cli,Setup'

    tests['pcs,all,tests,generic'] = tests['pcs,smoke,tests,generic'] + pcs_basic_tests
    // we don´t test pcs on one node, but the list is consistent and will be used for 2 nodes as well
    tests['pcs,all,tests,1'] = tests['pcs,all,tests,generic'] + pcs_advanced_tests
    tests['pcs,all,tests,3'] = tests['pcs,all,tests,1'] + 'pcs,cli,ResourceMove'
    tests['pcs,all,tests,4'] = tests['pcs,all,tests,1']

    tests['default,smoke,tests,generic'] = tests['pcs,smoke,tests,generic'] + smoke_tests
    tests['default,smoke,tests,1'] = ['lvm,snapper,display_snap,nostack,singlenode',
				      'lvm,raid_sanity,display_raid,raid4,singlenode']

    tests['default,all,tests,generic'] = tests['default,smoke,tests,generic'] + pcs_advanced_tests + advanced_tests
    tests['default,all,tests,1'] = tests['default,smoke,tests,1']
    tests['default,all,tests,2,rhel8'] = tests['pcs,smoke,tests,generic'] + smoke_tests + pcs_basic_tests + basic_tests + basic_tests_rhel8
    tests['default,all,tests,2,rhel9'] = tests['pcs,smoke,tests,generic'] + smoke_tests + pcs_basic_tests + basic_tests + basic_tests_rhel9
    tests['default,all,tests,2'] = tests['pcs,smoke,tests,generic'] + smoke_tests + pcs_basic_tests + basic_tests + basic_tests_rhel9
    tests['default,all,tests,3'] = tests['default,all,tests,2'] + pcs_advanced_tests + advanced_tests + 'pcs,cli,ResourceMove'
    tests['default,all,tests,3,rhel8'] = tests['default,all,tests,2,rhel8'] + pcs_advanced_tests + advanced_tests + 'pcs,cli,ResourceMove'
    tests['default,all,tests,3,rhel9'] = tests['default,all,tests,2,rhel9'] + pcs_advanced_tests + advanced_tests + 'pcs,cli,ResourceMove'
    tests['default,all,tests,4'] = tests['default,all,tests,2'] + pcs_advanced_tests + advanced_tests + 'pacemaker,usecase,apachewebfarm,haproxy'
    tests['default,all,tests,4,rhel8'] = tests['default,all,tests,2,rhel8'] + pcs_advanced_tests + advanced_tests + 'pacemaker,usecase,apachewebfarm,haproxy'
    tests['default,all,tests,4,rhel9'] = tests['default,all,tests,2,rhel9'] + pcs_advanced_tests + advanced_tests + 'pacemaker,usecase,apachewebfarm,haproxy'

    // Get the right tests set
    def ret = []

    // These need to be 'proper' Java strings, not groovy Gstring things
    def String array_ptr0 = "${info['testvariant']},${info['tests']},${info['testtype']},${info['nodes']},rhel${info['rhelver']}"
    def String array_ptr1 = "${info['testvariant']},${info['tests']},${info['testtype']},${info['nodes']}"
    def String array_ptr2 = "${info['testvariant']},${info['tests']},${info['testtype']},generic"

    // If there's an exact match then use it. Otherwise use try generic versions
    if (tests.containsKey(array_ptr0)) {
	ret = tests[array_ptr0]
    } else if (tests.containsKey(array_ptr1)) {
	ret = tests[array_ptr1]
    } else if (tests.containsKey(array_ptr2)) {
	ret = tests[array_ptr2]
    }

    // Check for cloud provider-specific limitations
    def providers = getProviderProperties()
    def provider = providers[info['provider']]

    if (provider != null) {
	def deletelist = []

	// Remove watchdog tests for those providers that don't support it
	if (provider['has_watchdog'] == false) {
	    for (t in ret) {
		if (t.contains('sbd')) {
		    deletelist += t
		}
	    }
	}

	// Remove lvm tests for those providers that don't have enough storage
	if (provider['has_storage'] == false) {
	    for (t in ret) {
		if (t.contains('lvm')) {
		    deletelist += t
		}
	    }
	}

	ret = ret.minus(deletelist)
    }

    return ret
}
