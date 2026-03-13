import java.io.File;

// Remove empty files and directories
def clean_dirs(String project, String staging_dir)
{
    sh("""
       cd ${staging_dir}/job
       find . -type f -size 0 -print -delete
       find . -type d -empty -print -delete
    """)
}


def recurse_branches(String staging_dir, String jenkins_dir)
{
    def staging_paths = new File(staging_dir).list()
    def jenkins_paths = new File(jenkins_dir).list()

    // First remove missing branches
    remove_old_job_logs(staging_dir, jenkins_dir)

    // Loop through the job branches, anything jobs
    // in the Jenkins dir gets removed
    for (String spath:staging_paths) {
	remove_old_job_logs("${staging_dir}/${spath}/builds",
			    "${jenkins_dir}/${spath}")
    }
}


// Removes old jobs from the staging area
// when all the jobs from a branch have been deleted
// then clean_dirs() above will remove the empty directory
def remove_old_job_logs(String staging_dir, String jenkins_dir)
{
    def staging_paths = new File(staging_dir).list()
    def jenkins_paths = new File(jenkins_dir).list()

    // Loop through the staging paths, anything not
    // in the Jenkins dir gets removed
    for (String spath:staging_paths) {
	def found = 0
	for (String jpath:jenkins_paths) {
	    if (jpath == spath) {
		found = 1
	    }
	}
	// Make sure %{staging_dir} has something in it before
	// we do rm -rf
	if (found == 0 && staging_dir.length() > 12) {
	    sh("echo rm -rfv ${staging_dir}/${spath}")
	}
    }
}

// Rsync everything to the external logs node
def rsync_to_external(String staging_dir)
{
    sh("""
        rsync -av --delete-after ${staging_dir}/job/ jweb:${staging_dir}/job/.
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
    def jobsdir = ''
    def target_jobsdir = ''
    def recurse = 0
    if (job_bits.size() == 2) {
	logsdir = "${env.JENKINS_HOME}/jobs/${job_bits[0]}/jobs/${job_bits[1]}/builds/${env.BUILD_NUMBER}/"

	target_logsdir="${staging_dir}/job/${job_bits[0]}/job/${job_bits[1]}/${env.BUILD_NUMBER}/"
	jobsdir = "${env.JENKINS_HOME}/jobs/${job_bits[0]}/jobs/${job_bits[1]}/builds"
	target_jobsdir="${staging_dir}/job/${job_bits[0]}/job/${job_bits[1]}"
    } else {
	logsdir = "${env.JENKINS_HOME}/jobs/${job_bits[0]}/jobs/${job_bits[1]}/branches/${job_bits[2]}/builds/${env.BUILD_NUMBER}/"

	target_logsdir="${staging_dir}/job/${job_bits[0]}/job/${job_bits[1]}/job/${job_bits[2]}/${env.BUILD_NUMBER}/"
	jobsdir = "${env.JENKINS_HOME}/jobs/${job_bits[0]}/jobs/${job_bits[1]}/branches"

	target_jobsdir="${staging_dir}/job/${job_bits[0]}/job/${job_bits[1]}/job"
	recurse = 1
    }

    // Copy logs to the staging area
    println("Copying logs to ${target_logsdir}")
    sh("mkdir -p ${target_logsdir}")

    // Use curl to get the logs from jenkins so the internal URIs are
    // tidied up.
    def LOG_URL = env.BUILD_URL.replaceAll('https://haci.fast.eng.rdu2.dc.redhat.com', 'http://localhost:8080')
    sh("curl ${LOG_URL}/consoleText > ${target_logsdir}/consoleText")
    def f = new File("${logsdir}/archive")
    if (f.exists()) {
	sh("rsync -atr ${logsdir}/archive/* ${target_logsdir}/artifact/")
    }
    return new Tuple(target_jobsdir, jobsdir, recurse)
}

// Called from projectFinishUp() (or other) to clean up
//   old logs and copy them to the external access system.
// We do the remote rsync for ALL jobs, so that if one fails
//   (eg network issues) the next job will copy those logs too.
def call(Map info)
{
    node('built-in') {
	def staging_dir = '/var/www/ci.kronosnet.org'
	RWLock(info, "log_archive", "WRITE", "projectFinishUp",
	       {
		def (job_staging_dir, job_jenkins_dir, recurse) = copy_our_logs(info, staging_dir)
		if (recurse) {
		    recurse_branches(job_staging_dir, job_jenkins_dir)
		} else
		    remove_old_job_logs(job_staging_dir, job_jenkins_dir)
		clean_dirs(info['project'], staging_dir)
		rsync_to_external(staging_dir)
	    })
    }
}
