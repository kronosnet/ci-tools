def call(Map info) {
    node('built-in') {
        def to_remove = []
        for (def i in info) {
            if (i.key.startsWith('lockfd_')) {
                println("RWLock: unlock_all Unlocking ${i.key}")
                RWLockUnlockOne(info, i.key)
                to_remove += i.key
            }
        }
        for (def k in to_remove) {
            info.remove(k)
        }
    }

    return 0
}
