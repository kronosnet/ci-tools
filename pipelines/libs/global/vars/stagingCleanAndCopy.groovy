import java.io.File;

// Remove empty files and directories
def clean_dirs(String project, String staging_dir)
{
    sh("""
       cd ${staging_dir}
       find . -type f -size 0 -print -delete
       find . -type d -empty -print -delete
    """)
}

def process_log_dirs(String staging_dir)
{
    // Look for each build branch
    sh ("""
        cd ${staging_dir}
        for i in \$(find . -name branches -type d); do
          oldpwd=\$(pwd)
          cd \$i
          builds="\$(ls -1 | sort -n)"
          numbuilds="\$(echo "\$builds" | wc -l)"
          if [ "\$numbuilds" -gt 200 ]; then
                purgenum=\$((numbuilds - 200))
                candidates="\$(echo "\$builds" | head -n \$purgenum)"
                for candidate in \$candidates; do
                        echo "Removing old build: \$candidate"
                        echo rm -rf \$candidate
                done
          fi
          cd \$oldpwd
        done
    """)
}

// Rsync everything to the external logs node
def rsync_to_external(String staging_dir)
{
    sh("""
        rsync -av --delete-after ${staging_dir}/ jweb:${staging_dir}/.
       """)
}



def copy_our_logs(Map info, String staging_dir)
{
    // Copy the logs the staging area in a hierarchy suitable for web use

    // for github/lab multijobs - the split is
    // [0] project name
    // [1] job name
    // [2] branch name
    //
    // for non-multijobs it's just
    // [0] project name
    // [1] job name

    def job_bits = env.JOB_NAME.split("/")
    def logsdir = ''
    def target_logsdir = ''
    if (job_bits.size() == 2) {
	logsdir = "${env.JENKINS_HOME}/jobs/${job_bits[0]}/jobs/${job_bits[1]}/builds/${env.BUILD_NUMBER}/"

	target_logsdir="${staging_dir}/job/${job_bits[0]}/job/${job_bits[1]}/${env.BUILD_NUMBER}/"
    } else {
	logsdir = "${env.JENKINS_HOME}/jobs/${job_bits[0]}/jobs/${job_bits[1]}/branches/${job_bits[2]}/builds/${env.BUILD_NUMBER}/"

	target_logsdir="${staging_dir}/job/${job_bits[0]}/job/${job_bits[1]}/${job_bits[2]}/${env.BUILD_NUMBER}/"
	}

    // Copy logs to the staging area
    println("Copying logs to ${target_logsdir}")
    sh("mkdir -p ${target_logsdir}")
    sh("cp ${logsdir}/log ${target_logsdir}/consoleText")
    def f = new File("${logsdir}/archive")
    if (f.exists()) {
	sh("rsync -atr ${logsdir}/archive/* ${target_logsdir}/artifact/")
    }
}

// Called from projectFinishUp() (or other) to clean up
//   old logs and copy them to the external access system.
// We do this for ALL jobs, so that if one rsync fails
//   (eg network issues) the next job will copy those logs too
def call(Map info)
{
    def staging_dir = "/var/www/ci.kronosnet.org"
    RWLock(info, "log_archive", "WRITE", "projectFinishUp",
       {

	    copy_our_logs(info, staging_dir)
	    clean_dirs(info['project'], staging_dir)
	    process_log_dirs(staging_dir)

	    rsync_to_external(staging_dir)
	})
}
