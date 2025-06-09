def call(Map info)
{
    println("Deleting test cluster")
    if (info['dryrun'] == '1') {
	return
    }
    def vapor_args = ['command': 'delete',
		      'provider': info['provider'],
		      'project': info['projectid'],
		      'buildnum': env.BUILD_NUMBER,
		      'osver': info['osver'],
		      'debug': env.vapordebug]
    vapor_wrapper(vapor_args, info, 60)
}
