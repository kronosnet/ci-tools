package org.kronosnet.ci

class Coverity implements Serializable {
    private final def steps
    Coverity(steps) { this.steps = steps }

    protected void generalCoverity(String covopts) {
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

    protected void testCoverity(Map generalInfo) {
        String covroot  = "/srv/covscan/${generalInfo['project']}/origin/${generalInfo['target']}"
        String extraver = generalInfo['extraver'] ?: ''
        String publish  = (generalInfo['install'] == 1 || generalInfo['covinstall'] == 1) ? '1' : '0'
        //TODO refactor, should get return code
        steps.sh """
                echo "===== CURRENT COVERITY SCAN ERRORS ====="
                cat cov.txt
                echo "===== END CURRENT COVERITY SCAN ERRORS ====="

                ret=0
                if [ "${publish}" = 1 ]; then
                    mkdir -p ${covroot}/${extraver}/
                    rm -f ${covroot}/${extraver}/cov*
                    cp cov.json cov.txt ${covroot}/${extraver}/
                else
                    if [ ! -d ${covroot} ]; then
                        echo "WARNING: target branch origin/${generalInfo['target']} no available for compare. Skipping."
                        exit 0
                    fi
                    csdiff -xc --no-color ${covroot}/${extraver}/cov.json cov.json > cov.diff
                    errors="\$(cat cov.diff | grep ^Error: | wc -l)"
                    if [ "\$errors" -ne "0" ]; then
                        echo "===== NEW COVERITY SCAN ERRORS FIXED ====="
                        cat cov.diff
                        echo "===== END NEW COVERITY SCAN ERRORS FIXED ====="
                    fi
                    csdiff -c --no-color ${covroot}/${extraver}/cov.json cov.json > cov.diff
                    errors="\$(cat cov.diff | grep ^Error: | wc -l)"
                    if [ "\$errors" -ne "0" ]; then
                        echo "===== NEW COVERITY SCAN ERRORS DETECTED ====="
                        cat cov.diff
                        echo "===== END NEW COVERITY SCAN ERRORS DETECTED ====="
                        ret=1
                        mkdir cov.html/new
                        cat cov.diff | cshtml - > cov.html/new/index.html
                    fi
                fi
                exit \$ret
            """
            return
    }

}
