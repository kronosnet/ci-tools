def call(Map localinfo, String stageName, String agentName)
{
    def cienv = [:]

    // Disable 'make check' if we are bootstrapping
    if (localinfo['bootstrap'] == 1) {
	cienv['CHECKS'] = 'nochecks'
    }

    cienv['build'] = ''
    if (stageName.endsWith('covscan')) {
	cienv['build'] = 'coverity'
    }
    if (stageName.endsWith('buildrpms')) {
	cienv['build'] = 'rpm'
    }
    if (stageName.endsWith('crosscompile')) {
	cienv['build'] = 'crosscompile'
    }

    if (!localinfo.containsKey('compiler')) {
	cienv['compiler'] = 'gcc'
	cienv['CC'] = cienv['compiler']
    } else {
	cienv['CC'] = localinfo['compiler']
    }

    if (!localinfo.containsKey('MAKE')) {
	cienv['MAKE'] = 'make'
    } else {
	// this is only necessary to simply paralleloutput check
	cienv['MAKE'] = localinfo['MAKE']
    }

    if (localinfo.containsKey('depbuildname')) {
	cienv["${localinfo['depbuildname']}ver"] = localinfo['depbuildversion']
	cienv['extraver'] = localinfo['depbuildname']+'-'+localinfo['depbuildversion']
    } else {
	cienv['extraver'] = ''
    }

    def path = sh(script: "echo \$PATH", returnStdout: true).trim()
    cienv['PATH'] = "/opt/coverity/bin:${path}"
    // def home = sh(script: "echo \$HOME", returnStdout: true).trim()
    // cienv['PATH'] = "/opt/coverity/bin:${path}:${home}/ci-tools"

    def numcpu = sh(script: "nproc", returnStdout: true).trim()

    cienv['PARALLELMAKE'] = "-j ${numcpu}"

    def paralleloutput = sh(script: """
				    rm -f Makefile.stub
				    echo "all:" > Makefile.stub
				    PARALLELOUTPUT=""
				    if ${cienv['MAKE']} -f Makefile.stub ${cienv['PARALLELMAKE']} -O >/dev/null 2>&1; then
					PARALLELOUTPUT="-O"
				    fi
				    if ${cienv['MAKE']} -f Makefile.stub ${cienv['PARALLELMAKE']} -Orecurse >/dev/null 2>&1; then
					PARALLELOUTPUT="-Orecurse"
				    fi
				    rm -f Makefile.stub
				    echo \$PARALLELOUTPUT
				    """, returnStdout: true).trim()

    cienv['PARALLELMAKE'] = "-j ${numcpu} ${paralleloutput}"

    // pacemaker version handling
    // Latest Pacemaker release branch
    cienv['PACEMAKER_RELEASE'] = '2.1'

    if (!cienv.containsKey('pacemakerver')) {
	if (localinfo['target'] == 'main') {
	    cienv['pacemakerver'] = 'main'
	} else {
	    cienv['pacemakerver'] = cienv['PACEMAKER_RELEASE']
	}
    }

    cienv['EXTERNAL_LD_LIBRARY_PATH'] = ''

    // Global things
    cienv['PIPELINE_VER'] = '1'

    return cienv
}
