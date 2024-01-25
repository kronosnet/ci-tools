def call(Map localinfo)
{
    // var expansion here REQUIRES bash
    def os_info = sh(script: '''#!/bin/citbash -e
	echo ===== PACKAGE LIST =====
	# BSD
	if which pkg > /dev/null 2>&1; then
	    pkg info
	fi
	# DEB
	if which dpkg > /dev/null 2>&1; then
	    dpkg -l
	fi
	# rpm
	if which rpm > /dev/null 2>&1; then
	    rpm -qa | sort -u
	fi
	echo ===== HOST INFO =====
	echo "uname -a"
	uname -a
    ''', returnStdout: true, label: 'Collect node os info')

    println(os_info)
}
