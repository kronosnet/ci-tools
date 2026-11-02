package org.kronosnet.ci

class Sbd extends Project {
    Sbd(steps, info, node) { super(steps, info, node) }

    @Override
    protected String installPath() {
        // sbd install path includes pacemaker version
        return "/srv/${generalInfo['project']}/origin/${generalInfo['target']}-pacemaker-${generalInfo['pacemakerver']}/"
    }
}
