// Do the non-specific parts of getBuildInfo
// This is called at the end of a provider-specific getBuildInfo.groovy
def call(Map info)
{
    // Clear things out ready for adding to
    info['nonvoting_fail'] = 0
    info['nonvoting_fail_nodes'] = ''
    info['voting_fail'] = 0
    info['voting_fail_nodes'] = ''
    info['nonvoting_run'] = 0
    info['voting_run'] = 0
    info['exception_text'] = ''
    info['email_extra_text'] = ''
    info['state'] = 'script error'
    info['covtgtdir'] = ''
    info['cov_results_urls'] = []
    info['new_cov_results_urls'] = []
    info['EXTRAVER_LIST'] = []

    // Make sure the params are in here so they get propogated to the scripts
    // Also convert them to ints so that info is consistent
    info['bootstrap'] = params.bootstrap as int
    info['fullrebuild'] = params.fullrebuild as int

    // fullrebuild overrides some things
    if (info['fullrebuild'] == 1) {
	info['install'] = 0
	info['covinstall'] = 0
	info['maininstall'] = 0
	info['stableinstall'] = 0
	info['publish_rpm'] = 0 // TODO Remove once all in new pipelines
	info['publishrpm'] = 0
    }
}
