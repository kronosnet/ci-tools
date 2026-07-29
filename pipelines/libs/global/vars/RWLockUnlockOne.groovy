import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Platform

class RWLockUnlockOne_jnaflock {
    interface CLibrary extends com.sun.jna.Library {
        CLibrary INSTANCE = (CLibrary)Native.load('c', CLibrary.class)
        int creat(String file, int mode);
        int flock(int fd, int mode);
        int close(int fd);
    }
}

def call(Map info, String lockname_info) {
    if (RWLockUnlockOne_jnaflock.CLibrary.INSTANCE.flock(info[lockname_info]['fd'], 8) == -1) { // 8 = LOCK_UNLOCK
        println("RWLock: unlock failed for ${info[lockname_info]['name']} in stage ${info[lockname_info]['stage']} on fd ${info[lockname_info]['fd']}")
    }
    if (RWLockUnlockOne_jnaflock.CLibrary.INSTANCE.close(info[lockname_info]['fd']) == -1) {
        println("RWLock: close failed for ${info[lockname_info]['name']} in stage ${info[lockname_info]['stage']} on fd ${info[lockname_info]['fd']}")
        return -1
    }
    println("RWLock: on ${info[lockname_info]['name']} in stage ${info[lockname_info]['stage']}, fd ${info[lockname_info]['fd']} released")
    RWLockLogLock('UNLOCKED', info[lockname_info]['mode'], info[lockname_info]['name'], info[lockname_info]['stage'])
}
