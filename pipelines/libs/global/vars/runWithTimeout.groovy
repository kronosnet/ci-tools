// Run a groovy fn with a timeout and call errors if it times out or aborts
//
// Parameters:
//  time   - timeout in minutes
//  fn     - Closure to call
//  info   - The job-global info map
//  locals - a map with args specific to this call
//  success_callback - a closure called if the run succeeded
//  failure_callback - a closure called if the run failed,
//                     return code in locals['RET']
//                     exception caught in locals['EXP']
//
// locals['CALLRET'] will always contain the return code of the closure (if any)
//
// Callbacks will be called with the result set in locals['RET']
// Valid values are:
//   OK      - it all went well
//   TIMEOUT - the job timed-out
//   ABORT   - the job was aborted by user or another job with doKill()
//   ERROR   - a shell script caused an error
// All other exceptions will be rethrown
//
def call(Integer time, Closure fn, Map info, Map locals,
	 Closure success_callback, Closure error_callback)
{
    println("runWithTimeout "+time);
    def String retval = 'OK'

    try {
	timeout(time: time, unit: 'MINUTES') {
	    locals['CALLRET'] = fn()
	}
    } catch (hudson.AbortException ae) {
	locals['EXP'] = ae
	println("runWithTimeout caught AbortException "+ae)
	// This is actually an abort
	// https://gist.github.com/stephansnyt/3ad161eaa6185849872c3c9fce43ca81?permalink_comment_id=2198976
	if (ae.getMessage().contains('script returned exit code 143')) {
	    println('runWithTimeout: "code 143" abort')
	    retval = 'ABORT'
	} else {
	    // This just means the script return != 0
	    println('runWithTimeout: script exit non-zero')
	    retval = 'ERROR'
	}
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException err) {
	locals['EXP'] = err
	println("runWithTimeout caught FlowInterrupted exception "+err)
	// If no 'cause' is given then we don't know what happened
	// so just rethrow it.
	if (err.getCauses() == 0) {
	    throw (err)
	}
	def String cause = err.getCauses()[0]
	if (cause.startsWith('org.jenkinsci.plugins.workflow.steps.TimeoutStepExecution$ExceededTimeout')) {
	    println('runWithTimeout: Timeout exceeded')
	    retval = 'TIMEOUT'
	}
	else if (cause.startsWith('jenkins.model.CauseOfInterruption$UserInterruption')) {
	    println('runWithTimeout: user abort')
	    retval = 'ABORT'
	} else {
	    // Not for us - rethrow it
	    throw(err)
	}
    }

    // Value has to be in the Map so it's mutated after the 'fn' closure
    locals['RET'] = retval
    if (retval == 'OK') {
	success_callback()
    } else {
	error_callback()
    }
    return retval
}
