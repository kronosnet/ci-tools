// Do the non-specific parts of getBuildInfo
// This is called at the end of a provider-specific getBuildInfo.groovy
def call(Map info)
{
    // If a global rebuild is in progress, then wait until it has finished.
    // Unless (of course) were are part of that global rebuild, or manually started
    def runreason = ''
    if (currentBuild.getBuildCauses().shortDescription.size() > 0) {
	runreason = currentBuild.getBuildCauses().shortDescription[0]
    }
    if (!runreason.startsWith('Started by upstream project "global/reinstall-all-manual') &&
	!runreason.startsWith('Started by user')) {
	lock('MAINTENANCE LOCKOUT') {
	    // We don't need to do anything here. just know that we're not locked out
	}
    }

    // Clear things out ready for adding to by our groovy scripts
    info['nonvoting_fail'] = 0
    info['nonvoting_fail_nodes'] = ''
    info['voting_fail'] = 0
    info['voting_fail_nodes'] = ''
    info['nonvoting_run'] = 0
    info['voting_run'] = 0
    info['exception_text'] = ''
    info['email_extra_text'] = ''
    info['covtgtdir'] = ''
    info['cov_results_urls'] = []
    info['repo_urls'] = []
    info['new_cov_results_urls'] = []
    info['EXTRAVER_LIST'] = []
    info['subjobs'] = []

    // A helpful default in case things go bad
    info['state'] = 'script error'

    // env variables for the build scripts to use.
    // These are often overridden in getProjectProperties
    info['CHECKS'] = ''
    info['DISTROCONFOPTS'] = ''
    info['EXTRACHECKS'] = ''
    info['MAKEINSTALLOPTS'] = ''
    info['MAKEOPTS'] = ''
    info['RPMDEPS'] = ''

    // Make sure the job params are in here so they get propogated to the scripts.
    // Also convert them to ints so that info is consistent
    info['bootstrap'] = params.bootstrap as int
    info['fullrebuild'] = params.fullrebuild as int

    // Put the library branch name into the environment
    // so that shell scripts can get it
    env.CITBRANCH = env.'library.GlobalLib.version'

    // fullrebuild overrides some things
    if (info['fullrebuild'] == 1) {
	info['install'] = 0
	info['covinstall'] = 0
	info['maininstall'] = 0
	info['stableinstall'] = 0
	info['publishrpm'] = 0
    }
}
