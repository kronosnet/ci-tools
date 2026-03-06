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
    # 5 = project name
    # 7 = job name
    # 9 = branch name
    # 11 = build number
    # 12 = 'log'(file) or 'archive' (dir)
    # 13 = artifact name
    target_dir=${TARGET}/${BITS[5]}/job/${BITS[7]}/job/${BITS[9]}/${BITS[11]}
    if [ "${BITS[12]}" = "log" ]
    then
	file="console"
	do_copy=1
    fi
    if [ "${BITS[12]:0:7}" = "archive" -a -f "$1" ]
    then
	file="artifact/${BITS[13]}"
	mkdir -p ${target_dir}/artifact
	do_copy=1
    fi
    if [ "$do_copy" = "1" ]
    then
	if [ ! -f "${target_dir}/${file}" ]
	then
	    echo cp $1 $target_dir/$file
	fi
    fi
    
}

# Run it!
find  $SOURCE | { while read line; do copy_if_needed $line ; done; }
