// Checkout github
// This is mainly to put everything in the project directory, for pagure compatibility
def call(Map info)
{
    def tarfile = "sources-${env.BUILD_TAG}.tar.gz"
    println("tarfile = ${tarfile}, node=${env.NODE_NAME}")

    if (env.NODE_NAME == 'built-in') {
	sh "tar --exclude=${tarfile} -czf /var/www/ci.kronosnet.org/buildsources/${tarfile} ."
    } else {
	dir (info['project']) {
	    // Random delay to stop hitting the server too hard
	    sleep(new Random().nextInt(12))
	    sh "wget https://ci.kronosnet.org/buildsources/${tarfile}"
	    sh "tar --no-same-owner -xzf ${tarfile}"
	}
    }
}
