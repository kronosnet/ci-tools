package org.kronosnet.ci

class Kronosnet extends Project {
    Kronosnet(steps, info, node) { super(steps, info, node) }

    @Override
    protected String extraOptsConfigure() {
        def opts = super.extraOptsConfigure()

        // Add Rust bindings if enabled
        if (generalInfo['RUSTBINDINGS'] == 'yes') {
            opts += ' --enable-rust-bindings'
        }

        return opts.trim()
    }

    @Override
    protected void test(Map generalInfo) {
        // Set extraopts for distcheck if rust bindings enabled
        if (generalInfo['RUSTBINDINGS'] == 'yes') {
            generalInfo['extraopts'] = '--enable-rust-bindings'
        }
    }
}
