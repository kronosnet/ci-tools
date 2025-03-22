def call() {
    sh (label: 'Remove all kcli clusters',
	script: 'su - kubesan /bin/bash -c "kcli start plan kubesan-test && sleep 2 && kcli delete plan --all --yes"')
    sh (label: 'Remove all old podman images',
	script: 'su - kubesan /bin/bash -c "podman image rm --force --all"')
    return ''
}
