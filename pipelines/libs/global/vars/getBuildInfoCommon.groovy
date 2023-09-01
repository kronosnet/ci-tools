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
    info['EXTRAVER'] = ''
    info['exception_text'] = ''
    info['email_extra_text'] = ''
    info['state'] = 'script error'

    // Make sure the params are in here so they get propogated to the scripts
    info['bootstrap'] = params.bootstrap
    info['fullrebuild'] = params.fullrebuild

    // fullrebuild overrides some things
    if (info['fullrebuild'] == '1') { // params are always strings
	info['install'] = 0
	info['covinstall'] = 0
	info['maininstall'] = 0
	info['stableinstall'] = 0
	info['publish_rpm'] = 0 // TODO Remove once all in new pipelines
	info['publishrpm'] = 0
    }
}
