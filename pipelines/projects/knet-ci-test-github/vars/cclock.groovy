// Just to see if this works
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

class jnaflock {
    interface CLibrary extends Library {
	CLibrary INSTANCE = (CLibrary)	Native.load("c", CLibrary.class);
	
	void printf(String format, Object... args);
	int open(String file, int mode);
	int creat(String file, int mode);
	int flock(int fd, int mode);
	int close(int fd);
    }
}


def call(String lockname, String mode, Closure thingtorun)
{
    // This MUST run on the Jenkins host
    node('built-in') {
	def lockmode = 0;
	if (mode == 'READ') {
	    lockmode = 1; // LOCK_SH
	}
	if (mode == 'WRITE') {
	    lockmode = 2; // LOCK_EX
	}
	if (lockmode == 0) {
	    println("jnaflock: Unknown lock mode ${mode}")
	    return -1
	}
	
	def fd = jnaflock.CLibrary.INSTANCE.creat("/tmp/${lockname}", 0666)
	println("FD for test.lock is " + fd)
	jnaflock.CLibrary.INSTANCE.flock(fd, lockmode)
	thingtorun()
	jnaflock.CLibrary.INSTANCE.close(fd)
	println("Closed")
    }
}
