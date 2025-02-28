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

// Abstracted out so we can use this for a 'normal' unlock and the 'emergency' one in post{always{}}
def do_unlock(Map info)
{
    node('built-in') {
	if (info.containsKey('lockfd') && info['lockfd'] >= 0) {
	    if (jnaflock.CLibrary.INSTANCE.flock(info['lockfd'], 8) == -1) { // 8 = LOCK_UNLOCK
		println("RWLock: unlock failed on fd ${info['lockfd']}")
		// Don't return here, still try to close the file
	    }
	    if (jnaflock.CLibrary.INSTANCE.close(info['lockfd']) == 0) {
		println("RWLock on ${info['lockname']} fd ${info['lockfd']} released")
	    } else {
		println("RWLock: close failed on fd ${info['lockfd']}")
		return -1
	    }
	}
	info['lockfd'] = -1
    }
    return 0
}

def call(Map info, String lockname, String mode, Closure thingtorun)
{
    def lockdir = "${JENKINS_HOME}/locks"
    def lockmode = 0

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
	return do_unlock(info)
    }
    if (lockmode == 0) {
	throw(new Exception("RWLock: Unknown lock mode ${mode}"))
	return -1
    }

    // Of course, this needs to be after the UNLOCK check
    if (info.containsKey('lockfd') && info['lockfd'] >= 0) {
	throw(new Exception("RWLock: Request for lock ${lockname}, while lock on fd ${info['lockfd']} already held (only 1 lock allowed at a time)"))
	return -1
    }

    // This MUST run on the Jenkins host, that's where the flocks are
    node('built-in') {
	sh "mkdir -p ${lockdir}"

	def lockfd = info['lockfd'] = jnaflock.CLibrary.INSTANCE.creat("${lockdir}/${lockname}.lock", 0666)
	if (lockfd == -1) {
	    throw(new Exception("RWLock: Failed to 'creat' file for lock ${lockdir}/${lockname}"))
	    return -1
	}
	println("RWLock: FD for lock ${lockname} is ${lockfd}")
	if (jnaflock.CLibrary.INSTANCE.flock(lockfd, lockmode) == -1) {
	    throw(new Exception("RWLock: Failed to 'flock' file for lock ${lockdir}/${lockname} at ${lockmode}"))
	    return -1
	}
	info['lockfd'] = lockfd
	info['lockname'] = lockname // Save it in case of errors later
	println("RWLock: ${lockname} locked for ${mode}")
    }

    // Run a thing inside the lock
    thingtorun()

    // Tidy up
    do_unlock(info)

    return 0
}
