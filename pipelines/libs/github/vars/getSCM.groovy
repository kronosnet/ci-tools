// Checkout github
// This is mainly to put everything in the project directory, for pagure compatibility
def call(Map params)
{
    dir (params['project']) {
	checkout scm
    }
}
