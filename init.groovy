import jenkins.model.*
import hudson.model.*

def jobName = "global/update-all-ci-tools/main"
// Get the multi-branch pipeline job
def multibranchJob = Jenkins.instance.getItemByFullName(jobName)

if (multibranchJob) {
  multibranchJob.scheduleBuild2(0)
}
