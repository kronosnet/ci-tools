import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Platform

class RWLockTryAcquire_jnaflock {
    interface CLibrary extends com.sun.jna.Library {
        CLibrary INSTANCE = (CLibrary)Native.load('c', CLibrary.class)
        int creat(String file, int mode);
        int flock(int fd, int mode);
        int close(int fd);
    }
}

def call(Map info, Map lockstate, String lockname, String mode, String stagename) {
    node('built-in') {
        sh "mkdir -p ${lockstate.lockdir}"
        def lockmode = lockstate.lockmode | 4 // LOCK_NB

        def lockfd = RWLockTryAcquire_jnaflock.CLibrary.INSTANCE.creat("${lockstate.lockdir}/${lockname}.lock", 0666)
        if (lockfd == -1) {
            return [status: 'error', reason: "Failed to creat ${lockstate.lockdir}/${lockname}"]
        }
        println("RWLock: FD for lock ${lockname} in stage ${stagename} is ${lockfd}")

        if (RWLockTryAcquire_jnaflock.CLibrary.INSTANCE.flock(lockfd, lockmode) == -1) {
            def e = Native.getLastError()
            RWLockTryAcquire_jnaflock.CLibrary.INSTANCE.close(lockfd)
            if (e == 11) { return [status: 'busy'] }
            return [status: 'error', reason: "flock failed for ${lockname}, errno ${e}"]
        }

        def lockname_info = lockstate.lockname_info
        info[lockname_info] = [:]
        info[lockname_info]['fd'] = lockfd
        info[lockname_info]['name'] = lockname
        info[lockname_info]['stage'] = stagename
        info[lockname_info]['mode'] = mode
        println("RWLock: ${lockname} in stage ${stagename} locked for ${mode}")
        RWLockLogLock('LOCKED', mode, lockname, stagename)
        return [status: 'ok', fd: lockfd]
    }
}
