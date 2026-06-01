def call(Map info)
{
    // Optimized default timeout for most tests
    def defaultTimeout = 70

    // Exception timeout for LVM lvmlockd tests
    def lvmTimeout = 120

    // Get the configured timeout from parameters
    def configuredTimeout = (info['testtype'] == 'tags') ? info['tagtimeout'] : info['testtimeout']

    // For tags, always use the configured timeout (default: 600 minutes)
    if (info['testtype'] == 'tags') {
        return configuredTimeout
    }

    // For LVM lvmlockd tests, use the HIGHER of configured or LVM-specific timeout
    // This ensures we never lower a timeout, respecting pipelines with higher defaults
    // Test names use commas: lvm,lvm_config_no-vdo,cluster-lvmlockd
    if (info['runtest'].contains('cluster-lvmlockd')) {
        return Math.max(configuredTimeout, lvmTimeout)
    }

    // If configured timeout differs from new default, use the configured value
    // This respects both explicit pipeline defaults and user overrides
    if (configuredTimeout != defaultTimeout) {
        return configuredTimeout
    }

    // Use the optimized default
    return defaultTimeout
}
