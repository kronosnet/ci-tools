// Create var=value pairs for the shell from everything
// in 'info'
def mapToShellVars(Map localinfo)
{
    def exports = ''

    localinfo.each { exports += "${it.key}='${it.value}' " }
    return exports
}


def call(Map localinfo)
{
    def exports = mapToShellVars(localinfo)
    return exports
}
