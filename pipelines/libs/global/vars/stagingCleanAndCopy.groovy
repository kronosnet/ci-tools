import java.io.File;

// Get a list of all jobs active in jenkins for this project
// Anything not in this list but still in staging can be removed
@NonCPS
def get_all_jobs_for_project(String project)
{
    def jobdirs = []
    Jenkins.instance.getAllItems(Job).each {
	jobBuilds = it.getBuilds().each {
	    def paths = it.getUrl().split('/')
	    if (paths[1] == project) {
		jobdirs += "/${it.getUrl()}" // Add slash so it looks like a dirname
	    }
	}
    }
    return jobdirs
}


// Remove empty files and directories
def clean_dirs(String project, String staging_dir)
{
    sh("""
       cd ${staging_dir}/job
       find . -type f -size 0 -print -delete
       find . -type d -empty -print -delete
    """)
}

// For multi-job pipelines we need to recurse down the branches
def recurse_branches(String staging_dir, String job_staging_dir, ArrayList jenkins_jobs)
{
    def staging_paths = new File(job_staging_dir).list()

    // First remove missing branches
    remove_old_job_logs(staging_dir, job_staging_dir, jenkins_jobs)

    // Loop through the job branches, any jobs not
    // in the Jenkins dir gets removed
    for (String spath:staging_paths) {
	remove_old_job_logs(staging_dir, "${job_staging_dir}/${spath}",
			    jenkins_jobs)
    }
}

// Removes old jobs from the staging area
// when all the jobs from a branch have been deleted
// then clean_dirs() above will remove the empty directory
def remove_old_job_logs(String staging_dir, String job_staging_dir, ArrayList jenkins_jobs)
{
    def staging_paths = new File(job_staging_dir).list()

    // Loop through the staging paths, anything not
    // in the Jenkins url list gets removed
    for (String spath:staging_paths) {
	def short_path = job_staging_dir + '/' + spath - staging_dir

	def found = 0

	// If the build doesn't exist in Jenkins then delete it from staging
	jenkins_jobs.each() {
	    def spl = short_path.length()
	    if (it.length() > spl && it.substring(0, spl) == short_path)
		found = 1
	}

	// Sanity check before we do rm -rf
	if (! found && job_staging_dir.length() > 12) {
	    sh("rm -rfv ${job_staging_dir}/${spath}")
	}
    }
}

// Rsync everything (like - *everything*!) to the external logs node
def rsync_to_external(String staging_dir)
{
    sh("""
        rsync -av --delete-after ${staging_dir}/job/ jweb:${staging_dir}/job/.
       """)
}

// Copy the logs the staging area in a hierarchy suitable for web use
def copy_our_logs(Map info, String staging_dir)
{
    def job_bits = env.JOB_NAME.split('/')

    // for github/lab multijobs - the split is
    //  [0] project name
    //  [1] job name
    //  [2] branch name
    //
    // for non-multijobs it's just
    //  [0] project name
    //  [1] job name

    def logsdir = ''
    def target_logsdir = ''
    def jobsdir = ''
    def target_jobsdir = ''
    def recurse = 0

    if (job_bits.size() == 2) { // non-multijob
	logsdir = "${env.JENKINS_HOME}/jobs/${job_bits[0]}/jobs/${job_bits[1]}/builds/${env.BUILD_NUMBER}/"
	target_logsdir="${staging_dir}/job/${job_bits[0]}/job/${job_bits[1]}/${env.BUILD_NUMBER}/"
	jobsdir = "${env.JENKINS_HOME}/jobs/${job_bits[0]}/jobs/${job_bits[1]}/builds"
	target_jobsdir="${staging_dir}/job/${job_bits[0]}/job/${job_bits[1]}"
    } else { // multijob
	logsdir = "${env.JENKINS_HOME}/jobs/${job_bits[0]}/jobs/${job_bits[1]}/branches/${job_bits[2]}/builds/${env.BUILD_NUMBER}/"
	target_logsdir="${staging_dir}/job/${job_bits[0]}/job/${job_bits[1]}/job/${job_bits[2]}/${env.BUILD_NUMBER}/"
	jobsdir = "${env.JENKINS_HOME}/jobs/${job_bits[0]}/jobs/${job_bits[1]}/branches"
	target_jobsdir="${staging_dir}/job/${job_bits[0]}/job/${job_bits[1]}/job"
	recurse = 1
    }

    // Copy logs to the staging area
    println("Copying logs to ${target_logsdir}")
    sh("mkdir -p ${target_logsdir}")

    // Use curl to get the logs from jenkins so the internal URIs in them are
    // readable
    def LOG_URL = env.BUILD_URL.replaceAll('https://haci.fast.eng.rdu2.dc.redhat.com', 'http://localhost:8080')
    sh("curl ${LOG_URL}/consoleText > ${target_logsdir}/consoleText")

    // Get the (potentially mangled) root of the artifact log files
    def f = new File(currentBuild.rawBuild.getRootDir(), "archive")
    if (f != null && f.exists()) {
	sh("rsync -atr ${f}/* ${target_logsdir}/artifact/")
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

	// 'project' needs to be the actual top-level project name, not what's in info['project']
	def project = env.JOB_NAME.split('/')[0]
	RWLock(info, "log_archive", "WRITE", "projectFinishUp",
	       {
		// Copy the jobs from this job
		def (job_staging_dir, job_jenkins_dir, recurse) = copy_our_logs(info, staging_dir)

		// Remove empty dirs
		clean_dirs(project, staging_dir)

		// Remove job & build logs that have been removed from jenkins
		def jenkins_jobs = get_all_jobs_for_project(project)
		if (recurse) {
		    recurse_branches(staging_dir, job_staging_dir, jenkins_jobs)
		} else
		    remove_old_job_logs(staging_dir, job_staging_dir, jenkins_jobs)

		// Copy to ci.kronosnet.org
		rsync_to_external(staging_dir)
	    })
    }
}
