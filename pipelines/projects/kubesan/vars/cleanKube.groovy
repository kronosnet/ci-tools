def call() {
    sh (label: 'Remove all kcli clusters',
	script: 'su - kubesan /bin/bash -c "kcli delete plan --all --yes"')
    sh (label: 'Remove all minikube clusters',
	script: 'su - kubesan /bin/bash -c "minikube delete --all"')
    sh (label: 'Remove all old podman images',
	script: 'su - kubesan /bin/bash -c "podman image rm --force --all"')
    sh (label: 'Remove minikube temp files',
	script: 'rm -rf /tmp/minikube* /tmp/juju*')
    return ''
}
