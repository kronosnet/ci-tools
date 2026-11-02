package org.kronosnet.ci

class Dlm extends Project {
    Dlm(steps, info, node) { super(steps, info, node) }

    @Override
    protected void sharedSetupPost() {
        // dlm requires PKG_CONFIG_PATH exported before build
        steps.sh "export PKG_CONFIG_PATH=${generalInfo['EXTERNAL_CONFIG_PATH']}"
    }
}
