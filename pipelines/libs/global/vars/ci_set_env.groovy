@NonCPS
def parse_ld_add(String ldvars)
{
    def varmap = [:]

    ldvars.eachLine {
	def keyval = it.split('=', 2)
	if ((keyval[0] == 'EXTERNAL_CONFIG_PATH') && (keyval[1] != '')) {
	    varmap['EXTERNAL_CONFIG_PATH'] = "${keyval[1]}"
	}
	if ((keyval[0] == 'EXTERNAL_LD_LIBRARY_PATH') && (keyval[1] != '')) {
	   varmap['EXTERNAL_LD_LIBRARY_PATH'] = "${keyval[1]}"
	}
    }

    return varmap
}

def get_build_info(Map ldmap, Map localinfo)
{
    def exports = getShellVariables(ldmap)
    // var expansion here REQUIRES bash
    def build_info = sh(script: """#!/bin/citbash -e
	${exports}
	""" + '''
	EXTERNAL_CONFIG_PATH=""
	EXTERNAL_LD_LIBRARY_PATH=""
	for project in $PROJECTS; do
	    found=""
	    pkgcfg="${project}_PKGCFG"
	    installpath="${project}_INSTALL_PATH"

	    if [ -d "${!installpath}" ]; then
		info="not available"
		if [ -f "${!installpath}/.build-info" ]; then
		   info=$(cat "${!installpath}/.build-info")
		fi
		echo "$project"
		echo "build info: $info"
		for spath in lib lib64 lib32 share; do
		    ldp="${!installpath}$spath/"
		    pcp="${ldp}pkgconfig"
		    if [ -d $pcp ]; then
			pkgver=$(PKG_CONFIG_PATH=$pcp pkg-config --modversion "${!pkgcfg}")
			if [ "$?" = "0" ]; then
			    echo "${!pkgcfg} version: $pkgver"
			    found="yes"
			    break
			fi
		    fi
		done
	    fi

	    if [ -n "$found" ]; then
		if [ -z "$EXTERNAL_CONFIG_PATH" ]; then
		    EXTERNAL_CONFIG_PATH="$pcp"
		else
		    EXTERNAL_CONFIG_PATH="$EXTERNAL_CONFIG_PATH:$pcp"
		fi
		if [ -z "$EXTERNAL_LD_LIBRARY_PATH" ]; then
		    EXTERNAL_LD_LIBRARY_PATH="$ldp"
		else
		    EXTERNAL_LD_LIBRARY_PATH="$EXTERNAL_LD_LIBRARY_PATH:$ldp"
		fi
	    else
		echo "pkg-config ${!pkgcfg} not found."
	    fi
	done
	if [ -n "$EXTERNAL_CONFIG_PATH" ]; then
	    echo EXTERNAL_CONFIG_PATH=$EXTERNAL_CONFIG_PATH
	fi
	if [ -n "$EXTERNAL_LD_LIBRARY_PATH" ]; then
	    echo EXTERNAL_LD_LIBRARY_PATH=$EXTERNAL_LD_LIBRARY_PATH
	fi
    ''', returnStdout: true, label: 'Collect node build info, pkg-config and ldpath')

    println(build_info)

    return parse_ld_add(build_info)
}

def call(Map localinfo, String agentName)
{
    def cienv = [:]

    // Disable 'make check' if we are bootstrapping
    if (localinfo['bootstrap'] == 1) {
	cienv['CHECKS'] = 'nochecks'
    }

    cienv['build'] = ''
    if (localinfo['stageName'].endsWith('covscan')) {
	cienv['build'] = 'coverity'
    }
    if (localinfo['stageName'].endsWith('buildrpms')) {
	cienv['build'] = 'rpm'
    }
    if (localinfo['stageName'].endsWith('crosscompile')) {
	cienv['build'] = 'crosscompile'
    }

    if (!localinfo.containsKey('compiler')) {
	cienv['compiler'] = 'gcc'
	cienv['CC'] = cienv['compiler']
    } else {
	cienv['CC'] = localinfo['compiler']
    }

    if (!localinfo.containsKey('MAKE')) {
	cienv['MAKE'] = 'make'
    } else {
	// this is only necessary to simply paralleloutput check
	cienv['MAKE'] = localinfo['MAKE']
    }

    if (localinfo.containsKey('depbuildname')) {
	cienv["${localinfo['depbuildname']}ver"] = localinfo['depbuildversion']
	cienv['extraver'] = localinfo['depbuildname']+'-'+localinfo['depbuildversion']
    } else {
	cienv['extraver'] = ''
    }

    def path = sh(script: "#!/bin/sh -e\necho \$PATH", returnStdout: true, label: 'Collect node PATH').trim()
    cienv['PATH'] = "/opt/coverity/bin:${path}"

    def numcpu = sh(script: "#!/bin/sh -e\nnproc", returnStdout: true, label: 'Collect node nproc').trim()
    cienv['PARALLELMAKE'] = "-j ${numcpu}"

    def paralleloutput = sh(script: """#!/bin/sh -e
	    rm -f Makefile.stub
	    echo "all:" > Makefile.stub
	    PARALLELOUTPUT=""
	    if ${cienv['MAKE']} -f Makefile.stub ${cienv['PARALLELMAKE']} -O >/dev/null 2>&1; then
		PARALLELOUTPUT="-O"
	    fi
	    if ${cienv['MAKE']} -f Makefile.stub ${cienv['PARALLELMAKE']} -Orecurse >/dev/null 2>&1; then
		PARALLELOUTPUT="-Orecurse"
	    fi
	    rm -f Makefile.stub
	    echo \$PARALLELOUTPUT
	    """, returnStdout: true, label: 'Collect node make parallel output support').trim()

    cienv['PARALLELMAKE'] = "-j ${numcpu} ${paralleloutput}"

    // pacemaker version handling
    // Latest Pacemaker release branch
    cienv['PACEMAKER_RELEASE'] = '2.1'

    if (!cienv.containsKey('pacemakerver')) {
	if (localinfo['target'] == 'main') {
	    cienv['pacemakerver'] = 'main'
	} else {
	    cienv['pacemakerver'] = cienv['PACEMAKER_RELEASE']
	}
    }

    // build / test matrix

    // rpm builds should use standard packages
    if (cienv['build'] != 'rpm') {
	def ldmap = [:]
	// set all defaults to build against main branches
	// apply stable overrides below

	ldmap['LIBQB_PKGCFG'] = 'libqb'
	ldmap['KRONOSNET_PKGCFG'] = 'libknet'
	ldmap['COROSYNC_PKGCFG'] = 'corosync'
	ldmap['COROSYNC_QDEVICE_PKGCFG'] = 'corosync-qdevice'
	ldmap['FENCE_AGENTS_PKGCFG'] = 'fence-agents'
	ldmap['RESOURCE_AGENTS_PKGCFG'] = 'resource-agents'
	ldmap['PACEMAKER_PKGCFG'] = 'pacemaker'
	ldmap['BOOTH_PKGCFG'] = 'booth'
	ldmap['SBD_PKGCFG'] = 'sbd'

	ldmap['LIBQB_INSTALL_PATH'] = '/srv/libqb/origin/main/'
	ldmap['KRONOSNET_INSTALL_PATH'] = '/srv/kronosnet/origin/main/'
	ldmap['COROSYNC_INSTALL_PATH'] = '/srv/corosync/origin/main/'
	ldmap['COROSYNC_QDEVICE_INSTALL_PATH'] = '/srv/corosync-qdevice/origin/main/'
	ldmap['FENCE_AGENTS_INSTALL_PATH'] = '/srv/fence-agents/origin/main/'
	ldmap['RESOURCE_AGENTS_INSTALL_PATH'] = '/srv/resource-agents/origin/main/'
	ldmap['PACEMAKER_INSTALL_PATH'] = "/srv/pacemaker/origin/" + cienv['pacemakerver'] + "/"
	ldmap['BOOTH_INSTALL_PATH'] = "/srv/booth/origin/main-pacemaker-" + cienv['pacemakerver'] + "/"
	ldmap['SBD_INSTALL_PATH'] = "/srv/sbd/origin/main-pacemaker-" + cienv['pacemakerver'] + "/"

	if ((localinfo['target'] != 'main') ||
	    (cienv['pacemakerver'] != 'main')) {
	    ldmap['KRONOSNET_INSTALL_PATH'] = '/srv/kronosnet/origin/stable1-proposed/'
	    ldmap['COROSYNC_INSTALL_PATH'] = '/srv/corosync/origin/camelback/'
	}

	// corosync supports both kronosnet stable and main
	// we need to test build both
	if ((localinfo['project'] == 'corosync') &&
	    (cienv.containsKey('kronosnetver'))) {
	    ldmap['KRONOSNET_INSTALL_PATH'] = "/srv/kronosnet/origin/" + cienv['kronosnetver'] + "/"
	}

	// Generate the project list from the _PKGCFG entries above:
	def projects = ''
	for (l in ldmap) {
	    if (l.key.endsWith('PKGCFG')) {
		projects += "${l.key.substring(0, l.key.size()-7)} "
	    }
	}
	ldmap['PROJECTS'] = projects

	// generate ld library path and pkgconfig path
	cienv['EXTERNAL_LD_LIBRARY_PATH'] = ''
	cienv['EXTERNAL_CONFIG_PATH']= ''
	cienv += get_build_info(ldmap, cienv)
    } else {
	// same logic as above, for rpm builds
	cienv['LIBQB_REPO'] = "https://ci.kronosnet.org/builds/libqb-main-" + agentName + ".repo"
	cienv['LIBQB_REPO_PATH'] = "https://ci.kronosnet.org/builds/libqb/" + agentName + "/main/latest/"
	cienv['KRONOSNET_REPO'] = "https://ci.kronosnet.org/builds/kronosnet-main-" + agentName + ".repo"
	cienv['KRONOSNET_REPO_PATH'] = "https://ci.kronosnet.org/builds/kronosnet/" + agentName + "/main/latest/"
	cienv['COROSYNC_REPO'] = "https://ci.kronosnet.org/builds/corosync-main-kronosnet-main-" + agentName + ".repo"
	cienv['COROSYNC_REPO_PATH'] = "https://ci.kronosnet.org/builds/corosync/" + agentName + "/main-kronosnet-main/latest/"
	cienv['COROSYNC_QDEVICE_REPO'] = "https://ci.kronosnet.org/builds/corosync-qdevice-main-" + agentName + ".repo"
	cienv['COROSYNC_QDEVICE_REPO_PATH'] = "https://ci.kronosnet.org/builds/corosync-qdevice/" + agentName + "/main/latest/"
	cienv['FENCE_AGENTS_REPO'] = "https://ci.kronosnet.org/builds/fence-agents-main-" + agentName + ".repo"
	cienv['FENCE_AGENTS_REPO_PATH'] = "https://ci.kronosnet.org/builds/fence-agents/" + agentName + "/main/latest/"
	cienv['RESOURCE_AGENTS_REPO'] = "https://ci.kronosnet.org/builds/resource-agents-main-" + agentName + ".repo"
	cienv['RESOURCE_AGENTS_REPO_PATH'] = "https://ci.kronosnet.org/builds/resource-agents/" + agentName + "/main/latest/"
	cienv['PACEMAKER_REPO'] = "https://ci.kronosnet.org/builds/pacemaker-" + cienv['pacemakerver'] + "-" + agentName + ".repo"
	cienv['PACEMAKER_REPO_PATH'] = "https://ci.kronosnet.org/builds/pacemaker/" + agentName + "/" + cienv['pacemakerver'] + "/latest/"
	cienv['BOOTH_REPO'] = "https://ci.kronosnet.org/builds/booth-main-pacemaker-" + cienv['pacemakerver'] + "-" + agentName + ".repo"
	cienv['BOOTH_REPO_PATH'] = "https://ci.kronosnet.org/builds/booth/" + agentName + "/main-pacemaker-" + cienv['pacemakerver'] + "/latest/"
	cienv['SBD_REPO'] = "https://ci.kronosnet.org/builds/sbd-main-pacemaker-" + cienv['pacemakerver'] + "-" + agentName + ".repo"
	cienv['SBD_REPO_PATH'] = "https://ci.kronosnet.org/builds/sbd/" + agentName + "/main-pacemaker-" + cienv['pacemakerver'] + "/latest/"
	cienv['DLM_REPO'] = "https://ci.kronosnet.org/builds/dlm-main-" + agentName + ".repo"
	cienv['DLM_REPO_PATH'] = "https://ci.kronosnet.org/builds/dlm/" + agentName + "/main/latest/"
	cienv['GFS2UTILS_REPO'] = "https://ci.kronosnet.org/builds/gfs2-utils-main-" + agentName + ".repo"
	cienv['GFS2UTILS_REPO_PATH'] = "https://ci.kronosnet.org/builds/gfs2-utils/" + agentName + "/main/latest/"

	if ((localinfo['target'] != 'main') ||
	    (cienv['pacemakerver'] != 'main')) {
	    cienv['KRONOSNET_REPO'] = "https://ci.kronosnet.org/builds/kronosnet-stable1-proposed-" + agentName + ".repo"
	    cienv['KRONOSNET_REPO_PATH'] = "https://ci.kronosnet.org/builds/kronosnet/" + agentName + "/stable1-proposed/latest/"
	    cienv['COROSYNC_REPO'] = "https://ci.kronosnet.org/builds/corosync-camelback-kronosnet-stable1-proposed-" + agentName + ".repo"
	    cienv['COROSYNC_REPO_PATH'] = "https://ci.kronosnet.org/builds/corosync/" + agentName + "/camelback-kronosnet-stable1-proposed/latest/"
	}

	// corosync supports both kronosnet stable and main
	// we need to test build both
	if ((localinfo['project'] == 'corosync') &&
	    (cienv.containsKey('kronosnetver'))) {
	    cienv['KRONOSNET_REPO'] = "https://ci.kronosnet.org/builds/kronosnet-" + cienv['kronosnetver'] + "-" + agentName + ".repo"
	    cienv['KRONOSNET_REPO_PATH'] = "https://ci.kronosnet.org/builds/kronosnet/" + agentName + "/" + cienv['kronosnetver'] + "/latest/"
	}
    }

    // Global things
    cienv['PIPELINE_VER'] = '1'

    return cienv
}
