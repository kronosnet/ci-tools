// Create var=value pairs for the shell from everything
// in 'info', plus some other things the running scripts need

def mapToShellVars(Map info)
{
    def exports=''

    info.each { exports += "${it.key}='${it.value}' " }
    return exports
}


def call(Map info, Map extras)
{
    def exports = mapToShellVars(info)
    exports += mapToShellVars(extras)

    return exports
}
