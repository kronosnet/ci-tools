// Return a map of cloud providers and their possibilities/limitaions
def call()
{
    // Cloud providers and their limits
    def providers = [:]

    providers['osp'] = ['maxjobs': 3, 'testlevel': 'all', 'rhelvers': ['8', '9'], 'has_watchdog': true]
    //    providers['ocpv'] = ['maxjobs': 3, 'testlevel': 'smoke', 'rhelvers': ['8', '9'], 'has_watchdog': true]
    //    providers['libvirtd'] = ['maxjobs': 3, 'testlevel': 'all', 'rhelvers': ['8', '9'], 'has_watchdog': true]
    //    providers['ibmvpc'] = ['maxjobs': 0, 'testlevel': 'all', 'rhelvers': ['8','9'], 'has_watchdog': false]
    //    providers['aws'] = ['maxjobs': 1, 'testlevel': 'smoke', 'rhelvers': ['8', '9'], 'has_watchdog': true]

    return providers
}
