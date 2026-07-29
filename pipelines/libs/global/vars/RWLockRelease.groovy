def call(Map info, String lockname, String stagename) {
    def lockname_info = "lockfd_${stagename}_${lockname}".replace('-', '_')
    node('built-in') {
        RWLockUnlockOne(info, lockname_info)
        info.remove(lockname_info)
    }
}
