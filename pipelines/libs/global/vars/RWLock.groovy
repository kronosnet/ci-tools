// Provide READ/WRITE locking in Jenkins, using flock over jna
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

class jnaflock {
    // com.sun.jna.Library need to be named explicity for some reason.
    interface CLibrary extends com.sun.jna.Library {
	CLibrary INSTANCE = (CLibrary)Native.load("c", CLibrary.class);

	int creat(String file, int mode);
	int flock(int fd, int mode);
	int close(int fd);
    }
}

// Normal single unlock
def do_unlock_one(Map info, String lockname_info)
{
    if (jnaflock.CLibrary.INSTANCE.flock(info[lockname_info]['fd'], 8) == -1) { // 8 = LOCK_UNLOCK
	println("RWLock: unlock failed for ${info[lockname_info]['name']} in stage ${info[lockname_info]['stage']} on fd ${info[lockname_info]['fd']}")
	// Don't return yet, still try to close the file
    }
    if (jnaflock.CLibrary.INSTANCE.close(info[lockname_info]['fd']) == -1) {
	println("RWLock: close failed for ${info[lockname_info]['name']} in stage ${info[lockname_info]['stage']} on fd ${info[lockname_info]['fd']}")
	return -1
    }
    println("RWLock: on ${info[lockname_info]['name']} in stage ${info[lockname_info]['stage']}, fd ${info[lockname_info]['fd']} released")
    info.remove(lockname_info)
}

// Unlock all held locks
def do_unlock_all(Map info)
{
    node('built-in') {
	for (def i in info) {
	    if (i.key.startsWith('lockfd_')) {
		println("RWLock: unlock_all Unlocking ${i.key}")
		do_unlock_one(info, i.key)
	    }
	}
    }
    return 0
}


// Global unlock polymorph. mode MUST be 'UNLOCK'
def call(Map info, String mode)
{
    if (mode != 'UNLOCK') {
	println('RWLock: unlock polymorph called without UNLOCK')
    } else {
	do_unlock_all(info)
    }
}

// info       - the global info[:] array of the job - we store the lock FDs in here
// lockname   - name of the lock to get (a file in $JENKINS_HOME/locks/)
// mode       - READ or WRITE
// stagename  - a string that must be unique in this job. it is used to identify lock FDs in info[:]
// thingtorun - a closure to run with the lock held
def call(Map info, String lockname, String mode, String stagename, Closure thingtorun)
{
    def lockdir = "${JENKINS_HOME}/locks"
    def lockmode = 0
    def lockname_info = "lockfd_${stagename}_${lockname}".replace('-','_')

    if (mode == 'READ') {
	lockmode = 1 // LOCK_SH
    }
    if (mode == 'WRITE') {
	lockmode = 2 // LOCK_EX
    }
    // Normally this doesn't need to be called as the closure in 'thingtorun' is
    // run then the lock is cleared. This is for post{always{}} to tidy up
    // in case of 'accidents'
    if (mode == 'UNLOCK') {
	return do_unlock_all(info)
    }
    if (lockmode == 0) {
	throw(new Exception("RWLock: Unknown lock mode ${mode}"))
	return -1
    }

    // Of course, this needs to be after the UNLOCK check
    if (info.containsKey(lockname_info) && info[lockname_info]['fd'] >= 0) {
	throw(new Exception("RWLock: Request in stage ${stagename} for lock ${lockname}, while lock on fd ${info[lockname_info]['fd']} already held (only 1 lock allowed at a time)"))
	return -1
    }

    // This MUST run on the Jenkins host, that's where the flocks are
    node('built-in') {
	sh "mkdir -p ${lockdir}"
	lockmode |= 4 // LOCK_NB (no blocking - so the job doesn't seem to "die" according to jenkins

	def lockfd = jnaflock.CLibrary.INSTANCE.creat("${lockdir}/${lockname}.lock", 0666)
	if (lockfd == -1) {
	    throw(new Exception("RWLock: Failed to 'creat' file for lock ${lockdir}/${lockname}"))
	    return -1
	}
	println("RWLock: FD for lock ${lockname} in stage ${stagename} is ${lockfd}")

	def wait_time = 0
	def waiting = true
	while (waiting) {
	    sleep(wait_time)
	    if (jnaflock.CLibrary.INSTANCE.flock(lockfd, lockmode) == -1) {
		def e = Native.getLastError()
		if (e == 11) { // 11 = EAGAIN
		    wait_time = 60
		    println("Waiting for lock ${lockname}")
		} else {
		    throw(new Exception("RWLock: Failed to 'flock' file for lock ${lockdir}/${lockname} at ${lockmode}, ${e}"))
		    return -1
		}
	    } else {
		waiting = false // We have the lock
	    }
	}
	info[lockname_info] = [:]
	info[lockname_info]['fd'] = lockfd
	info[lockname_info]['name'] = lockname
	info[lockname_info]['stage'] = stagename
	println("RWLock: ${lockname} in stage ${stagename} locked for ${mode}")
    }

    // Run a thing inside the lock
    thingtorun()

    // Tidy up
    do_unlock_one(info, lockname_info)

    return 0
}
