#!/bin/sh
# Copies all [project] log files from the jenkins area
# to the staging area

TARGET=/var/www/ci.kronosnet.org/job

# Optional project parameter
if [ -n "$1" ]
then
    SOURCE=~jenkins/jobs/$1/
else
    SOURCE=~jenkins/jobs
fi

copy_if_needed()
{
    do_copy=0
    IFS='/' read -ra BITS <<< "$1"
    # For github/gitlab jobs
    #  5 = project name
    #  7 = job name
    #  9 = branch name
    #  11 = build number
    #  12 = 'log'(file) or 'archive' (dir)
    #  13 = artifact name
    #
    # for other jobs
    #  5 = project name
    #  7 = job name
    #  9 = build number
    #  10 = 'log'(file) or 'archive' (dir)
    #  11 = artifact name
    if [ "${BITS[8]}" = "branches" ]
    then # github/gitlab
        project=${BITS[5]}
        job=${BITS[7]}
        branch=${BITS[9]}
        build=${BITS[11]}
        log_or_archive=${BITS[12]}
        artifact=${BITS[13]}
        target_dir=${TARGET}/${project}/job/${job}/job/${branch}/${build}
    else # non-multijob jobs
        project=${BITS[5]}
        job=${BITS[7]}
        branch=""
        build=${BITS[9]}
        log_or_archive=${BITS[10]}
        artifact=${BITS[11]}
        target_dir=${TARGET}/${project}/job/${job}/${build}
    fi

    # Is is the main log or an artifact directory?
    if [ "${log_or_archive}" = "log" ]
    then
        file="console"
        do_copy=1
    fi
    if [ "${log_or_archive:0:7}" = "archive" -a -f "$1" ]
    then
	file="artifact/${BITS[13]}"
        mkdir -p ${target_dir}/artifact
        do_copy=1
    fi
    if	[ "$do_copy" = "1" ]
    then
        if [ ! -f "${target_dir}/${file}" ]
        then
            cp $1 ${target_dir}/${file}
        fi
    fi
}

# Run it!
find  $SOURCE | { while read line; do copy_if_needed $line ; done; }
