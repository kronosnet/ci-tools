
def call(Map info, int permits, String lockname, String mode, String stagename, Closure thingtorun) {
    def acquired = false

    while (!acquired) {
        for (int slot = 0; slot < permits; slot++) {

            String slotLock = "${lockname}_${slot}"

            println("Trying ${slotLock}")

            //Polymorph for trying to lock the specific slotLock 
            // one time and return the result to act upon it
            def busy = RWLock(info, slotLock, mode, stagename)

            if (!busy) {
                acquired = true
                def e = null
                try {
                    thingtorun()
                } catch (exp) {
                    e = exp
                } finally {
                    //Polymorph for unlocking one lock
                    RWLock(info, slotLock, stagename)
                }
                if (e != null) {
                    throw(e)
                }
                break
            }
            println("${slotLock} is busy, trying next one")
        }
        if (!acquired) {
            println("all locks are busy, sleeping ")
            sleep(60)
        }
    }
    return 0
}
