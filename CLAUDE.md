# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This repository contains the CI/CD infrastructure for ci.kronosnet.org, which builds and tests various High Availability (HA) clustering projects including kronosnet, corosync, pacemaker, pcs, fence-agents, resource-agents, booth, sbd, dlm, gfs2-utils, libqb, corosync-qdevice, and others.

The infrastructure consists of:
- **Shell scripts** (ci-*) for build/test automation on CI nodes
- **Jenkins pipelines** (Groovy) orchestrating builds across multiple platforms
- **Pipeline libraries** providing reusable functionality for different source control systems and cloud providers

## Repository Structure

```
.
├── ci-*                              # Shell scripts for CI operations (setup, build, test, update)
├── pipelines/
│   ├── global/                       # Global maintenance pipelines (update-all, reinstall, etc.)
│   ├── libs/                         # Shared pipeline libraries
│   │   ├── global/vars/              # Core Jenkins utilities (RWLock, shNoTrace, etc.)
│   │   ├── vapor/vars/               # Cloud cluster testing (create, deploy, test)
│   │   ├── pagure/vars/              # Pagure integration (PR comments, auth, SCM)
│   │   └── github/vars/              # GitHub integration
│   └── projects/                     # Per-project Jenkinsfiles and libraries
│       ├── kronosnet/
│       │   ├── Jenkinsfile           # Project pipeline definition
│       │   └── vars/                 # Project-specific library functions
│       ├── corosync/
│       ├── pacemaker/
│       └── ...
├── anvil-config-templates/           # Config files for Anvil bare-metal testing
├── bsd-update/                       # FreeBSD update scripts
├── flock                             # Python flock wrapper for macOS/Solaris compatibility
└── start_node.in                     # Template for node startup scripts
```

## Pipeline Library Architecture

Understanding the library structure is crucial to working with this codebase. There are distinct library types with different scopes and execution contexts:

### Global Library (`pipelines/libs/global/vars/`)

The global library contains utilities shared across **all** projects and pipelines:
- **Core utilities**: `RWLock`, `shNoTrace`, `cmdWithTimeout`, `runWithArtifacts`
- **Build orchestration**: `buildRunMap`, `runStage`, `buildGenericRunMap`
- **Node management**: `getNodes`, `getUpNodesByLabel`, `getLabelledNodes`, `getNodeProperties`
- **Environment setup**: `ci_set_env`, `ci_os_info`, `getShellVariables`
- **Publishing**: `postStage`, `sendEmails`, `projectFinishUp`, `stagingCleanAndCopy`
- **Common functions**: `getBuildInfoCommon`, `isThisAnInstallBranch`, `buildPRRPMs`

Imported in Jenkinsfiles as: `@Library('GlobalLib') _`

### Project-Specific Libraries (`pipelines/projects/*/vars/`)

Each project can define its own library functions in its `vars/` directory. These are project-specific and not shared:
- **Configuration**: `getProjectProperties` - build flags, dependencies, configure options
- **Timeouts**: `getBuildTimeout` - project-specific timeout values
- **Email settings**: `getEmails`, `getEmailOptions`, `getEmailReplyTo`
- **Branch policies**: `getInstallBranches`, `getPRBranchRPM`, `getValidPRUsers`
- **Coverity options**: `getCovOpts` - static analysis configuration

Imported in Jenkinsfiles as: `@Library('ProjectLib') _`

**Important**: Project libraries can only be called from that project's pipeline. They are not available to other projects.

### Vapor Library (`pipelines/libs/vapor/vars/`)

Cloud cluster testing library used by HA functional testing pipelines:
- **Cluster lifecycle**: `create_cluster`, `deploy_cluster`, `delete_cluster`, `recover_cluster`
- **Testing**: `run_functional_tests`, `get_cluster_tests`, `run_cluster_test`
- **Post-processing**: `post_functional_tests`, `sortTestReports`
- **Wrapper**: `vapor_wrapper` - unified interface to vapor CLI
- **Configuration**: `getProviderProperties`, `getBuildInfo`

Used primarily by `pipelines/global/ha-functional-testing*` pipelines.

### Source Control Libraries

**GitHub Library (`pipelines/libs/github/vars/`):**
- `getBuildInfo` - determines build parameters from GitHub webhooks
- `getAuthCheck` - verifies PR author permissions
- `postPRcomment` - posts comments on pull requests
- `getSCM` - configures git checkout from GitHub
- `getCollaborators`, `getGlobalAdminUsers` - authorization lists
- Used by most projects in this infrastructure (GitHub is the primary SCM)

**Pagure Library (`pipelines/libs/pagure/vars/`):**
- Similar functions adapted for Pagure-hosted repositories
- `getCredUUID` - credential lookup (Pagure-specific)

Imported as: `@Library('GithubLib') _` or `@Library('PagureLib') _`

### Groovy Sandbox and Library Execution

**Critical distinction**: Which library a function belongs to affects its execution context:

- **Global library functions** run with **full permissions** - NOT sandboxed
- **Project library functions** run in the Jenkins Groovy sandbox with restricted permissions
- **Vapor library functions** run in the Jenkins Groovy sandbox with restricted permissions
- **Source control libraries** run in the Jenkins Groovy sandbox with restricted permissions

All libraries **except GlobalLib** run sandboxed. Understanding which library provides a function is essential for debugging and extending functionality. The sandbox restricts certain operations and may require script approval for new methods.

**Important Policy**: This infrastructure maintains a strict **no script approvals** policy. All privileged code that requires permissions beyond the sandbox must live in GlobalLib, where it can be properly controlled and reviewed. This ensures security and maintainability.

## Build Workflow Architecture

### CI Script Phases

Source builds follow a multi-phase workflow executed by shell scripts and Groovy functions:

1. **OS Info** (`ci_os_info`): Groovy script that detects OS information
2. **Setup** (`ci-setup-src`): Runs `autogen.sh` and `./configure` with project-specific options
3. **Build** (`ci-build-src`): Executes `make` for standard builds, or specialized coverity/rpm builds
4. **Test** (`ci-tests-src`): Runs `make check` and `make distcheck`, handles coverity scan reporting

Shell scripts are invoked via `ci-wrap`, which handles branch switching and environment setup.

### The ci-wrap Mechanism

`ci-wrap` is a critical wrapper that:
- Clones ci-tools to a temporary workspace if `CITBRANCH` is not `main`
- Checks out the specified branch for testing ci-tool changes
- Exports `CITHOME` and `CITBRANCH` environment variables
- Prevents recursive calls and validates commands exist

Usage: `ci-wrap <ci-command> [args...]`

### Build Types

Controlled via the `$build` variable:
- **standard**: Normal compilation and testing
- **coverity**: Static analysis with Coverity scan
- **rpm**: RPM package builds
- **crosscompile**: Cross-compilation verification

### Pipeline Execution Model

Jenkinsfiles use a common pattern:

1. **Library imports**: `@Library(['GlobalLib', 'GithubLib', 'ProjectLib']) _`
2. **Auth & setup**: `getBuildInfo()` checks permissions and gathers build metadata
3. **Parallel builds**: `buildRunMap()` creates build stages distributed across labeled nodes
4. **Post-processing**: `postStage()` publishes RPMs, processes coverity results
5. **Notifications**: `projectFinishUp()` sends email/PR comments

## Global Pipeline Components

These components from `pipelines/libs/global/vars/` are used across all project pipelines:

### RWLock (Read-Write Locking)

Prevents race conditions in parallel builds using file-based locking via JNA flock:

```groovy
RWLock(info, 'ci-rpm-repos', 'READ', 'vapor_deploy', {
    // Code that needs lock protection
})
```

- Locks are stored in `$JENKINS_HOME/locks/`
- Supports READ (shared) and WRITE (exclusive) modes
- Automatically unlocks after closure execution
- Stage name must be unique within a job

### vapor_wrapper (Vapor Library)

From `pipelines/libs/vapor/vars/vapor_wrapper.groovy` - manages cloud-based cluster testing (AWS, Azure, GCP, Aliyun, OpenStack):

```groovy
vapor_wrapper([
    command: 'create',      // create|deploy|test|delete|reboot
    provider: 'aws',        // Cloud provider
    osver: 'rhel9',         // OS version
    nodes: '4',             // Cluster size
    project: 'kronosnet',   // Project name
    buildnum: env.BUILD_NUMBER
], info, timeout_minutes)
```

The wrapper constructs vapor CLI commands with appropriate options for cluster management. Used by `pipelines/global/ha-functional-testing*` pipelines.

### buildRunMap

Generates parallel build stages for different node types:

```groovy
def voting = buildRunMap('voting', info, [
    'voting': true,           // Whether failures block merge
    'nodelabel': 'voting',    // Jenkins node label to match
    'extravars': [:]          // Additional env variables
])
```

Returns a map of closures that can be passed to `parallel`.

## Common Development Tasks

### Testing CI Tool Changes

To test changes to ci-tools scripts without affecting production:

**Note**: `CITBRANCH` is automatically set if the pipeline is called with GlobalLib from another branch. Manual setting is rarely needed.

```bash
# Manual override (rare cases only):
export CITBRANCH=your-feature-branch
ci-wrap ci-setup-src  # Will use your branch instead of main
```

### Adding a New Build Type

1. Create node configuration in project's `vars/getProjectProperties.groovy`
2. Add `buildRunMap()` call in project's `Jenkinsfile`
3. May need to extend build scripts (ci-setup-src, ci-build-src, ci-tests-src) with project-specific logic

### Modifying Package Dependencies

Edit `ci-rpm-common`:
- Update `REMOVERPMS` to add packages to clean before builds
- Modify `installrpmdeps()` to handle new repository requirements

### Updating Global Node Maintenance

Global pipelines in `pipelines/global/`:
- `update-all-yum`, `update-all-apt`, `update-all-freebsd`: Update packages on CI nodes
- `reinstall-all`: Reinstall nodes using kcli/kubesan
- `all-weekly`: Scheduled maintenance tasks

## Environment Variables Used by CI Scripts

Set by Jenkins before invoking ci-* scripts:

- `WORKSPACE`: Build directory
- `NODE_NAME`: Jenkins node name (e.g., `rhel-9-x86-64`)
- `BUILD_NUMBER`: Jenkins build number
- `project`: Project name (kronosnet, corosync, pacemaker, etc.)
- `target`: Git branch/tag being built
- `build`: Build type (standard, coverity, rpm, crosscompile)
- `CITHOME`: Path to ci-tools (set by ci-wrap)
- `CITBRANCH`: Branch of ci-tools being used (set by ci-wrap)
- `DISTROCONFOPTS`: Distribution-specific configure flags
- `EXTERNAL_CONFIG_PATH`: PKG_CONFIG_PATH for external dependencies
- `PARALLELMAKE`: Parallel make flags (e.g., `-j8`)

Project-specific variables:
- `pacemakerver`: Pacemaker version for dependent builds
- `RPMDEPS`: RPM build dependencies to install
- `CHECKS`: Override default test targets
- `RUSTBINDINGS`: Enable Rust bindings for kronosnet/corosync

## Testing Infrastructure

### Anvil Bare-Metal Testing

Scripts prefixed `ci-setup-anvil-*` configure Alteeve Anvil! systems for HA cluster testing:
- `ci-setup-anvil-bm`: Basic bare-metal setup
- `ci-setup-anvil-bm-vm`: Configure VMs on bare-metal
- `ci-setup-anvil-simengine`: Setup simulated hardware environment

### Cloud Testing (vapor)

The vapor tool creates ephemeral test clusters in cloud providers:
1. **create**: Provision VMs with specified nodes/storage
2. **deploy**: Install packages and configure cluster
3. **test**: Run functional test suites
4. **delete**: Cleanup resources

Used primarily for HA functional testing pipelines in `pipelines/global/ha-functional-testing*`.

## Source Control Integration

### GitHub

Libraries in `pipelines/libs/github/vars/`:
- `getAuthCheck`: Verify PR author permissions
- `postPRcomment`: Add comments to pull requests
- `getSCM`: Configure git checkout

Most projects in this infrastructure are hosted on GitHub.

### Pagure (pagure.io, src.fedoraproject.org)

Libraries in `pipelines/libs/pagure/vars/` provide similar functionality for Pagure-hosted repositories.

## Notable Patterns

### Project-Specific Build Logic

Many scripts contain case statements keying off `${project}` to handle per-project requirements:
- pcs requires Python virtual environment setup
- pacemaker sets `SPECVERSION` for RPM builds
- booth requires `CONFIG_SHELL=bash`
- kronosnet/corosync support optional Rust bindings

### Error Handling

- Shell scripts use `set -e` to exit on first error
- Groovy pipelines catch exceptions and store in `info['exception_text']`
- Failed builds still proceed to post-stage for reporting
- Test logs are dumped on failure: `find . -name "*test*suite.log" -exec cat {} \;`

## Detailed Pipeline Execution

### runStage: The Core Build Orchestrator

`runStage` (in `pipelines/libs/global/vars/runStage.groovy`) is where builds actually execute. For each node/stage combination:

1. **Clean workspace**: Removes all previous build artifacts
2. **Checkout source**: Calls `getSCM(info)` to clone the repository
3. **Load configurations**:
   - `getNodeProperties(agentName)`: Node-specific environment variables
   - `getProjectProperties(localinfo, agentName)`: Project-specific build options
4. **Execute build phases sequentially**:
   - Get OS Info (`ci_os_info`)
   - Setup RPMs (`ci-setup-rpm`)
   - Setup source (`ci-setup-src`)
   - Build source (`ci-build-src`)
   - Run tests (`ci-tests-src`)
   - Install (`ci-install-src` - only if `install=1`)
5. **Collect artifacts**: Build logs saved to `${stageName}-${agentName}.log`
6. **Record results**: Updates `info[]` with success/failure stats

Each phase runs with timeout protection via `runWithTimeout()`. Failures are caught and logged but don't crash the pipeline - stats are collected in `info[]` for final reporting.

### Bootstrap Mode Optimization

When `params.bootstrap == 1` (full rebuild), `buildRunMap` optimizes which stages run:

- **buildrpms stages**: Only run if `publishrpm == 1`
- **voting stages**: Only run if `install == 1`
- **covscan stages**: Only run if `covinstall == 1`
- All enabled stages become `voting: true` to catch failures

This avoids redundant work when doing a complete rebuild of all branches/projects. Bootstrap is used when:
- Updating build infrastructure (new compiler, new dependencies)
- Rebuilding after major toolchain changes
- Initial setup of CI infrastructure

### External Dependency Resolution

Projects often depend on each other (e.g., corosync depends on kronosnet, which depends on libqb). The `ci_set_env` function handles this:

1. For each project in `PROJECTS` (in dependency order):
   - Check if `/srv/${project}/origin/${target}/` exists
   - Read `.build-info` for version tracking
   - Scan for `pkgconfig/` directories in `lib/`, `lib64/`, `lib32/`, `share/`
   - Build `EXTERNAL_CONFIG_PATH` with all found pkgconfig paths
   - Build `EXTERNAL_LD_LIBRARY_PATH` with corresponding library paths

2. Export to build environment:
   - `PKG_CONFIG_PATH=$EXTERNAL_CONFIG_PATH` (for ./configure)
   - `LD_LIBRARY_PATH=$EXTERNAL_LD_LIBRARY_PATH` (for test execution)

This allows builds to link against locally-built dependencies instead of only system packages.

## Node Configuration Details

### Node Discovery and Selection

**`getNodes(label)`**: Internal function in `pipelines/libs/global/vars/getNodes.groovy` that queries Jenkins for all nodes matching a specific label. Returns a list of node names.

**`getUpNodesByLabel(label)`**: Production-safe wrapper in `pipelines/libs/global/vars/getUpNodesByLabel.groovy` that:
1. Calls `getNodes(label)` to get all matching nodes
2. Calls `getLabelledNodes('down')` to get offline nodes
3. Returns the difference - nodes that match the label AND are not marked 'down'

**Always use `getUpNodesByLabel()` in pipelines to avoid scheduling builds on 'down' nodes.**

### Node Label Strategy

**Important**: This infrastructure uses labels defined in `getNodes()` rather than Jenkins' native node labels (though they are kept in sync with git). The `getNodes()` function maintains its own label-to-node mapping.

Node labels used by `getNodes()`:
- **OS/version**: `rhel-9-x86-64`, `debian-unstable-x86-64`, `freebsd-14-x86-64`
- **Build capability**: `voting`, `nonvoting`, `buildrpms`, `covscan`
- **Special purposes**: `built-in` (Jenkins controller), `down` (offline nodes), `vapor-driver` (cloud testing)

### Node Properties (`getNodeProperties`)

Each Jenkins node can have custom environment variables defined in `pipelines/libs/global/vars/getNodeProperties.groovy`:

**Platform-specific settings:**
- **FreeBSD**: `MAKE=gmake`, `PYTHON=/usr/local/bin/python3.11`, `RUSTBINDINGS=yes`
- **Alpine**: `RUSTBINDINGS=yes`
- **Fedora/RHEL derivatives**: Various `RUSTBINDINGS` settings
- **Cross-compilation**: `EXTRA_ARCH=armhf` for Debian unstable cross-builds

**Special test nodes:**
- Nodes with `-ci-test-` in their name are used for testing CI infrastructure changes

### Project Properties Pattern

Each project has `vars/getProjectProperties.groovy` that returns build-specific configuration:

**Example from kronosnet:**
```groovy
props['PARALLELTEST'] = 'no'                    // Disable parallel testing
props['RPMDEPS'] = 'libqb-devel doxygen2man'    // Package dependencies for RPM build
props['DISTROCONFOPTS'] = '...'                 // ./configure flags
props['DEBUGJOBS'] = ['voting', 'nonvoting']    // Which job types get debug builds
props['DEBUGOPTS'] = '--enable-debug'           // Debug configure flag
```

Properties can be node-specific:
- Check `agentName.startsWith("debian-unstable-cross")` for cross-compile settings
- Query installed package versions to adjust build flags dynamically

### Node Restrictions

Some nodes are reserved for specific projects (defined in `buildRunMap.nodeRestrictions`):

```groovy
restrict['openindiana-x86-64'] = ['libqb', 'corosync', 'kronosnet', 'ci-test']
```

Other projects will skip these nodes even if they match the label.

## Publishing and Post-Processing

### postStage: Publishing Build Artifacts

`postStage` runs on the `built-in` node after all parallel builds complete:

**Coverity Results Publishing:**
- Collects all coverity scan results from `info['cov_results_urls']`
- Acquires WRITE lock on `ci-cov-repos`
- Runs `ci-cov-repos ${project} ${covtgtdir} ${version}` for each version
- Publishes to `/srv/covscan/${project}/origin/${target}/${extraver}/`

**RPM Publishing:**
- Only if `buildrpms_failed != 1` and `publishrpm == 1`
- Acquires WRITE lock on `ci-rpm-repos`
- Publishes to `origin/${target}` for branch builds, `pr/${pull_id}` for PRs
- Runs `ci-rpm-repos ${project} ${repopath} ${version}`

**Pipeline Result:**
- Sets `currentBuild.result = 'FAILURE'` if any voting stage failed
- `info['state']` determines email/PR comment content

Locks prevent corruption when multiple builds try to update repositories simultaneously.

## Node Update Mechanisms

### ci-update-* Scripts

Each OS family has dedicated update scripts:

**`ci-update-yum` (RHEL/CentOS/Fedora):**
- Installs EPEL repositories for older RHEL versions
- Enables specific module streams (e.g., `ruby:3.1` on RHEL 9)
- Defines required/optional packages via `required_pkg()` and `optional_pkg()`
- Uses `--nobest` on newer platforms to handle dependency conflicts

**`ci-update-apt` (Debian/Ubuntu):**
- Manages apt repositories and sources
- Handles Debian experimental/testing/unstable specifics

**`ci-update-freebsd`:**
- Uses `pkg` for FreeBSD package management

**`ci-update-common`:**
- Sourced by all update scripts
- Provides common package installation logic

### BSD Update (Ansible)

The `bsd-update/` directory contains Ansible playbooks for FreeBSD nodes:

```
bsd-update/
├── ansible.cfg           # Ansible configuration
├── ansible-inventory     # Inventory of FreeBSD nodes
├── group_vars/           # Group variables
├── roles/                # Ansible roles for updates
├── run-update            # Wrapper script
└── site.yml              # Main playbook
```

Used for orchestrated updates across all FreeBSD CI nodes, managing pkg updates and system configurations.

## Global Utility Functions

These functions from `pipelines/libs/global/vars/` provide common utilities across all pipelines:

### shNoTrace

Runs shell commands without exposing the actual command in Jenkins logs:

```groovy
shNoTrace("exit 1", "Marking this stage as a failure")
```

The log shows "Marking this stage as a failure" instead of "exit 1". Useful for:
- Commands with sensitive data
- Exit commands where the exit code is what matters
- Cleaner log output

### cmdWithTimeout

Wraps shell commands with timeout protection:

```groovy
cmdWithTimeout(minutes, "long-running-command")
```

### runWithArtifacts

Runs a closure while collecting artifacts and handling logging:

```groovy
runWithArtifacts(info, logfile, {
    // Build or test operations
    // Logs are automatically captured to logfile
})
```

This function is used extensively in `runStage` to collect build logs for each stage.

## Source Control Integration Details

### GitHub getBuildInfo Flow

For GitHub-based projects (most projects in this infrastructure), `getBuildInfo` determines build parameters:

**Pull Request Build:**
- `target`: Target branch of the PR
- `pull_id`: PR number
- `checkout`: Target branch (to test merge compatibility)
- `install`: 0 (never install from PRs)
- `publishrpm`: Controlled by `buildPRRPMs()` - some branches allow PR RPMs
- `jobname`: "PR-${pr_number}"

**Branch Build (merge/push):**
- `target`: Branch name
- `pull_id`: 1 (placeholder)
- `checkout`: Commit SHA if provided, else branch name
- `install`: Determined by `isThisAnInstallBranch(target)`
  - `main` branch: `maininstall=1, stableinstall=0`
  - Stable branches: `maininstall=0, stableinstall=1`
- `publishrpm`: Always 1 for installable branches

### Pagure getBuildInfo Flow

For Pagure-based projects, the flow is similar with Pagure-specific webhook parameters (`BRANCH_TO != 'None'` indicates a PR).

### Authorization

`getAuthCheck` verifies PR authors against:
1. Project collaborators (`getCollaborators()`)
2. Global admin users (`getGlobalAdminUsers()`)

Unauthorized PRs are rejected before any build starts.

## Agent Node Startup

Nodes are started using the `connect-node.sh` script, which uses the `start_node.in` template to generate startup scripts for Jenkins agents:

- Cleans `~/.jenkins` on startup
- Finds the latest OpenJDK installation in `@JVMPATH@`
- Cleans old vapor cache files (>30 days)
- Launches Jenkins agent with `java -jar agent.jar`

Variables like `@HOME@`, `@JVMPATH@`, `@SHELL@` are replaced during node provisioning.

## Maintenance

### Updating CI Tools on Nodes

Use the `update-all-ci-tools` global pipeline, which runs `git pull` in `~/ci-tools` on all nodes.

### Node Reboot Procedures

The `maintenance-lockout` pipeline prevents new builds during maintenance windows.
