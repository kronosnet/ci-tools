def call(Map info, String lockname, String mode, String stagename) {
    def lockdir = "${JENKINS_HOME}/locks"
    def lockmode = 0
    def lockname_info = "lockfd_${stagename}_${lockname}".replace('-', '_')

    if (mode == 'READ') {
        lockmode = 1 // LOCK_SH
    }
    if (mode == 'WRITE') {
        lockmode = 2 // LOCK_EX
    }
    if (mode == 'UNLOCK') {
        return RWLockUnlockAll(info)
    }
    if (lockmode == 0) {
        return [status: 'error', reason: "Unknown lock mode ${mode}"]
    }

    if (info.containsKey(lockname_info) && info[lockname_info]['fd'] >= 0) {
        return [status: 'error', reason: "Lock ${lockname} already held in stage ${stagename} on fd ${info[lockname_info]['fd']}"]
    }

    return [status: 'ok', lockdir: lockdir, lockmode: lockmode, lockname_info: lockname_info]
}
