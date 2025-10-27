// Sorts the weekly testing report by provider so
// the email is more easily human readable

@NonCPS
def call(String results)
{
    // Gather results into a string per test job
    def splitup=[:]
    def last=""
    for (def i in results.split('\n')) {
	if (i.startsWith('-')) {
	    last = i
	    splitup[last] = ''
	} else {
	    if (i.length() > 0) {
		splitup[last] += i + '\n'
	    }
	}
    }

    // Sort it (This is the bit that requires nonCPS)
    def sorted = splitup.sort()
    def sresult = ''
    for (def i in sorted) {
	sresult += i.key + '\n'
	sresult += i.value + '\n'
    }

    return sresult
}
