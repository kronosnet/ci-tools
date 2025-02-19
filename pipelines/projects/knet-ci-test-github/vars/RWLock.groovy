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
	    throw(new Exception("jnaflock: Unknown lock mode ${mode}"))
	    return -1
	}

	def fd = jnaflock.CLibrary.INSTANCE.creat("/var/lib/jenkins/${lockname}.lock", 0666)
	if (fd == -1) {
	    throw(new Exception("Failed to 'creat' file for lock ${lockname}"))
	    return -1
	}
	println("FD for lock ${lockname} is " + fd)
	jnaflock.CLibrary.INSTANCE.flock(fd, lockmode)
	thingtorun()
	jnaflock.CLibrary.INSTANCE.close(fd)
	println("Lock ${lockname} released")
    }
}
