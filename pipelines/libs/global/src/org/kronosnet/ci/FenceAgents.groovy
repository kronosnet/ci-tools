package org.kronosnet.ci

class FenceAgents extends Project {
    FenceAgents(steps, info, node) { super(steps, info, node) }

    @Override
    protected void buildCoverity() {
        // First run cov-build on make
        println("cov-build --dir=./cov ${make()} ${generalInfo['MAKECOVOPTS'] ?: ''}")
        steps.sh "cov-build --dir=./cov ${make()} ${generalInfo['MAKECOVOPTS'] ?: ''}"

        // Then capture agents directory
        println("cd agents && coverity capture --dir ../cov && cd ..")
        steps.sh """
            cd agents
            coverity capture --dir ../cov
            cd ..
        """
    }
}
