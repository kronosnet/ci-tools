package org.kronosnet.ci

class Coverity implements Serializable {
    private final def steps
    Coverity(steps) { this.steps = steps }

    protected void general_coverity(String covopts) {
        def ws = steps.env.WORKSPACE
        steps.sh """
            cov-analyze --dir=./cov --wait-for-license ${covopts}
            cov-format-errors --dir=./cov --json-output-v7 cov.json
            cov-format-errors --dir=./cov --emacs-style > cov.txt
            cov-format-errors --dir=./cov --html-output cov.html
            for i in cov.json cov.txt; do
                sed -i -e "s#${ws}##g" \$i
            done
            find cov.html -type f -exec sed -i -e "s#${ws}##g" {} \\;
        """
    }

}
