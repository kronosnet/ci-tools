def call() {
    sh (label: 'Remove all minikube clusters',
	script: 'su - kubesan /bin/bash -c "minikube delete --all"')
    sh (label: 'Remove all old podman images',
	script: 'su - kubesan /bin/bash -c "podman image rm --force --all"')
    return ''
}
