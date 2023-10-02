def call(Map info, Map extras, String stageName, String agentName)
{
    def cienv = [:]

    // Disable 'make check' if we are bootstrapping
    if (info['bootstrap'] == 1) {
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

    if (!extras.containsKey('compiler')) {
	cienv['compiler'] = 'gcc'
	cienv['CC'] = cienv['compiler']
    } else {
	cienv['CC'] = extras['compiler']
    }

    if (!extras.containsKey('MAKE')) {
	cienv['MAKE'] = 'make'
    }

    if (!extras.containsKey('PYTHON')) {
	cienv['PYTHON'] = 'python3'
    }

    def path = sh(script: "echo \$PATH", returnStdout: true).trim()
    def home = sh(script: "echo \$HOME", returnStdout: true).trim()
    cienv['PATH'] = "/opt/coverity/bin:${path}:${home}/ci-tools"

    def numcpu = ''
    if (agentName.startsWith('freebsd-12') ||
        agentName.startsWith('freebsd-13')) {
	numcpu = sh(script: "sysctl -n hw.ncpu", returnStdout: true).trim()
    } else {
	numcpu = sh(script: "nproc", returnStdout: true).trim()
    }

    cienv['PARALLELMAKE'] = "-j ${numcpu}"

    // Global things
    cienv['PIPELINE_VER'] = '1'

    return cienv
}
