// Return a map of cloud providers and their possibilities/limitaions
def call()
{
    // Cloud providers and their limits
    def providers = [:]

    providers['libvirtd'] = ['maxjobs': 4, 'testlevel': 'all', 'rhelvers': ['8', '9'], 'has_watchdog': true, 'has_storage': true, 'weekly': true]
    providers['osp'] = ['maxjobs': 4, 'testlevel': 'all', 'rhelvers': ['8', '9'], 'has_watchdog': true, 'has_storage': true, 'weekly': true]
    //providers['ocpv'] = ['maxjobs': 3, 'testlevel': 'all', 'rhelvers': ['8', '9'], 'has_watchdog': true, 'has_storage': false, 'weekly': true]
    //providers['ibmvpc'] = ['maxjobs': 0, 'testlevel': 'all', 'rhelvers': ['8','9'], 'has_watchdog': false]
    //providers['aws'] = ['maxjobs': 1, 'testlevel': 'smoke', 'rhelvers': ['8', '9'], 'has_watchdog': true]

    return providers
}
