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

    // Global things
    cienv['PIPELINE_VER'] = '1'

    return cienv
}
