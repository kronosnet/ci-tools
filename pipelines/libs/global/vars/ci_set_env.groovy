def call(Map info, Map extras, String stageName)
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

    def path = sh(script: "echo \$PATH", returnStdout: true).trim()
    def home = sh(script: "echo \$HOME", returnStdout: true).trim()
    cienv['PATH'] = "/opt/coverity/bin:${path}:${home}/ci-tools"

    // Global things
    cienv['PIPELINE_VER'] = '1'

    return cienv
}
