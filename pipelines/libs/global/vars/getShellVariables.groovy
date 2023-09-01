// Create var=value pairs for the shell from everything
// in 'info', plus some other things the running scripts need


def mapToShellVars(Map info)
{
    def exports=''

    info.each { exports += "${it.key}='${it.value}' " }
    return exports
}


def call(Map info, Map extras, String job_type)
{
    // Normally this is 'project', but some jobs have different names
    // eg pacemaker -> pcmk
    if (!info.containsKey('DEST')) {
	info['DEST'] = info['project']
    }
    def exports = mapToShellVars(info)
    exports += mapToShellVars(extras)

    // Global things
    exports += "PIPELINE_VER=1 JOB_BASE_NAME=${info['project']}-${job_type}"
    return exports
}
