// Run a shell script with a timeout and call errors if it times out or aborts
// args are as runWithTimeout except that it takes a shell command, not a groovy closure
def call(Integer time, String cmd, Map state,
	 Closure success_callback, Closure error_callback)
{
    println("cmdWithTimeout "+cmd)
    runWithTimeout(time,
		   { sh "${cmd}" },
		   state, success_callback, error_callback)
}

