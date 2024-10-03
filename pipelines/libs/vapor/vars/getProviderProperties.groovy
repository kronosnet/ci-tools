import java.nio.file.Files
import java.nio.file.Paths

// Setup routines called by setup_fn() entries
def gcp_setup(String ver)
{
    sh """
	gcloud --quiet compute instance-templates delete rhelha-vapor-workstation${ver} rhelha-vapor-node${ver} || true
	gcloud --quiet compute images delete rhel-${ver} || true
	gcloud --quiet compute images create rhel-${ver} --source-image-family rhel-${ver} --source-image-project rhel-cloud
	gcloud --quiet compute instance-templates create rhelha-vapor-node${ver} --boot-disk-size=40GB --machine-type=e2-custom-4-8192 --image-family=rhel-${ver} --image-project=rhel-cloud
	gcloud --quiet compute instance-templates create rhelha-vapor-workstation${ver} --boot-disk-size=100GB --machine-type=e2-custom-4-8192 --image-family=rhel-${ver} --image-project=rhel-cloud

       """
}

// Return a map of cloud providers and their possibilities/limitaions
def call()
{
    // Cloud providers and their limits & params.
    def providers = [:]

    providers['libvirt'] = ['maxjobs': 4, 'testlevel': 'all', 'rhelvers': ['7', '8', '9'],
			    'has_watchdog': true, 'has_storage': true, 'weekly': true,
			    'defaultiscsi': '10',
			    'defaultuseiscsi': 'no',
			    'defaultblocksize': '350',
			    'authopts': '--libvirtd-ip localhost',
			    'rhel7': ['createopts': "--image rhel-7.9.0.x86_64.qcow2 --flavor-workstation rhelha-vapor-workstation-extra-large --flavor rhelha-vapor-node-extra-large --block-device /dev/vapor/shared",
				      'deployopts':  '',
				      'testopts': '',
				      'setup_fn': {}],
			    'rhel8': ['createopts': "--image rhel-8.10.0.x86_64.qcow2 --flavor-workstation rhelha-vapor-workstation-extra-large --flavor rhelha-vapor-node-extra-large --block-device /dev/vapor/shared",
				      'deployopts':  '',
				      'testopts': '',
				      'setup_fn': {}],
			    'rhel9': ['createopts': "--image rhel-9.4.0.x86_64.qcow2 --flavor-workstation rhelha-vapor-workstation-extra-large --flavor rhelha-vapor-node-extra-large  --block-device /dev/vapor/shared",
				      'deployopts':  '',
				      'testopts': '',
				      'setup_fn': {}]]

    providers['osp'] = ['maxjobs': 4, 'testlevel': 'all', 'rhelvers': ['8', '9'],
			'has_watchdog': true, 'has_storage': true, 'weekly': true,
			'defaultiscsi': '200',
			'defaultuseiscsi': 'yes',
			'defaultblocksize': '10',
			'authopts': '--cloud rhelha-ci',
			'rhel8': ['createopts': '--image rhel-8.10.0.x86_64',
				  'deployopts':  '',
				  'testopts': '',
				  'setup_fn': {}],
			'rhel9': ['createopts': '--image rhel-9.4.0.x86_64',
				  'deployopts':  '',
				  'testopts': '',
				  'setup_fn': {}]]

    providers['azure'] = ['maxjobs': 4, 'testlevel': 'smoke', 'rhelvers': ['7', '8', '9'],
			  'has_watchdog': false, 'has_storage': true, 'weekly': true,
			  'defaultiscsi': '',
			  'defaultuseiscsi': 'no',
			  'defaultblocksize': '1024',
			  'authopts': '--region eastus',
			  'rhel7': ['createopts': '--image $(az vm image list --all --publisher RedHat  --output table | grep "RedHat:RHEL:7_" | tail -n 1 | awk \'{print $NF}\')',
				    'deployopts':  '',
				    'testopts': '',
				    'setup_fn': {}],
			  'rhel8': ['createopts': '--image $(az vm image list --all --publisher RedHat  --output table | grep "RedHat:RHEL:8_" | tail -n 1 | awk \'{print $NF}\')',
				    'deployopts':  '',
				    'testopts': '',
				    'setup_fn': {}],
			  'rhel9': ['createopts': '--image $(az vm image list --all --publisher RedHat  --output table | grep "RedHat:RHEL:9_" | tail -n 1 | awk \'{print $NF}\')',
				    'deployopts':  '',
				    'testopts': '',
				    'setup_fn': {}]]

    providers['gcp'] = ['maxjobs': 4, 'testlevel': 'all', 'rhelvers': ['7', '8', '9'],
			'has_watchdog': true, 'has_storage': true, 'weekly': false,
			'defaultiscsi': '',
			'defaultuseiscsi': 'yes',
			'defaultblocksize': '',
			'authopts': '--region us-east1',
			'rhel7': ['createopts': '--flavor-workstation rhelha-vapor-workstation7 --flavor rhelha-vapor-node7 --image rhel-7',
				  'deployopts':  '',
				  'testopts': '',
				  'setup_fn': {gcp_setup('7')}],
			'rhel8': ['createopts': '--flavor-workstation rhelha-vapor-workstation8 --flavor rhelha-vapor-node8 --image rhel-8',
				  'deployopts':  '',
				  'testopts': '',
				  'setup_fn': {gcp_setup('8')}],
			'rhel9': ['createopts': '--flavor-workstation rhelha-vapor-workstation9 --flavor rhelha-vapor-node9 --image rhel-9',
				  'deployopts':  '',
				  'testopts': '',
				  'setup_fn': {gcp_setup('9')}]]

    providers['ibmvpc'] = ['maxjobs': 0, 'testlevel': 'all', 'rhelvers': ['8','9'],
			   'has_watchdog': false, 'weekly': false,
			   'defaultiscsi': '350',
			   'defaultuseiscsi': 'yes',
			   'defaultblocksize': '',
			   'authopts': '--region us-east',
			   'rhel8': ['createopts': '--image vapor-rhel8-9-x86-64',
				     'deployopts':  '',
				     'testopts': '',
				     'setup_fn': {}],
			   'rhel9': ['createopts': '--image vapor-rhel9-3-x86-64',
				     'deployopts':  '',
				     'testopts': '',
				     'setup_fn': {}]]

    providers['ocpv'] = ['maxjobs': 3, 'testlevel': 'all', 'rhelvers': ['8', '9'],
			 'has_watchdog': true, 'has_storage': false, 'weekly': false,
			 'defaultiscsi': '10',
			 'defaultuseiscsi': 'yes',
			 'defaultblocksize': '10',
			 'authopts': '--region tlv',
			 'rhel8': ['createopts': '--image 8.9.0',
				   'deployopts':  '',
				   'testopts': '',
				   'setup_fn': {}],
			 'rhel9': ['createopts': '--image 9.3.0',
				   'deployopts':  '',
				   'testopts': '',
				   'setup_fn': {}]]

    //    providers['aws'] = ['maxjobs': 1, 'testlevel': 'smoke', 'rhelvers': ['8', '9'], 'has_watchdog': true, 'weekly': false]

    return providers
}
