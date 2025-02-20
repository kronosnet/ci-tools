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

def libvirt10_setup()
{
    sh """
         rm -f /var/lib/libvirt/images/centos-10-stream.qcow2 && wget -qO /var/lib/libvirt/images/centos-10-stream.qcow2 https://cloud.centos.org/centos/10-stream/x86_64/images/CentOS-Stream-GenericCloud-10-latest.x86_64.qcow2 && virsh pool-refresh default
       """
}

// Return a map of cloud providers and their possibilities/limitations
def call()
{
    // Cloud providers and their limits & params.
    def providers = [:]

    providers['libvirt'] = ['maxjobs': 4, 'testlevel': 'all', 'vers': ['rhel8', 'rhel9', 'centos10'],
			    'has_watchdog': true, 'has_storage': true, 'weekly': true,
			    'defaultiscsi': '10',
			    'defaultuseiscsi': 'no',
			    'defaultblocksize': '200',
			    'authopts': '--libvirtd-ip localhost',
			    'rhel8': ['createopts': "--image rhel-8.10.0.x86_64.qcow2 --flavor-workstation rhelha-vapor-workstation-medium --flavor rhelha-vapor-node-medium",
				      'deployopts':  '',
				      'testopts': '',
				      'setup_fn': {}],
			    'rhel9': ['createopts': "--image rhel-9.5.0.x86_64.qcow2 --flavor-workstation rhelha-vapor-workstation-medium --flavor rhelha-vapor-node-medium",
				      'deployopts':  '',
				      'testopts': '',
				      'setup_fn': {}],
			    'centos10': ['createopts': "--image centos-10-stream.qcow2 --flavor-workstation rhelha-vapor-workstation-medium --flavor rhelha-vapor-node-medium",
					 'deployopts':  '',
					 'testopts': '',
					 'setup_fn': {libvirt10_setup()} ]]

    providers['osp'] = ['maxjobs': 4, 'testlevel': 'all', 'vers': ['rhel8', 'rhel9'],
			'has_watchdog': true, 'has_storage': true, 'weekly': true,
			'defaultiscsi': '200',
			'defaultuseiscsi': 'yes',
			'defaultblocksize': '10',
			'authopts': '--cloud rhelha-ci',
			'rhel8': ['createopts': '--image rhel-8.10.0.x86_64',
				  'deployopts':  '',
				  'testopts': '',
				  'setup_fn': {}],
			'rhel9': ['createopts': '--image rhel-9.5.0.x86_64',
				  'deployopts':  '',
				  'testopts': '',
				  'setup_fn': {}]]

    providers['azure'] = ['maxjobs': 4, 'testlevel': 'smoke', 'vers': ['rhel8', 'rhel9'],
			  'has_watchdog': false, 'has_storage': true, 'weekly': true,
			  'defaultiscsi': '',
			  'defaultuseiscsi': 'no',
			  'defaultblocksize': '1024',
			  'authopts': '--region eastus',
			  'rhel8': ['createopts': '--image $(az vm image list --all --publisher RedHat  --output table | grep "RedHat:RHEL:8_" | awk \'{print $NF}\' | sort -V | tail -n 1)',
				    'deployopts':  '',
				    'testopts': '',
				    'setup_fn': {}],
			  'rhel9': ['createopts': '--image $(az vm image list --all --publisher RedHat  --output table | grep "RedHat:RHEL:9_" | awk \'{print $NF}\' | sort -V | tail -n 1)',
				    'deployopts':  '',
				    'testopts': '',
				    'setup_fn': {}]]

    providers['aws'] = ['maxjobs': 4, 'testlevel': 'smoke', 'vers': ['rhel8', 'rhel9'],
			'has_watchdog': false, 'has_storage': true, 'weekly': true,
			'defaultiscsi': '',
			'defaultuseiscsi': 'no',
			'defaultblocksize': '300',
			'authopts': '--region us-east-1',
			'rhel8': ['createopts': '--image $(vapor get-images aws --region us-east-1 | grep "RHEL-8.10.0_HVM-" | grep x86_64 | sed -e \'s#",##g\' -e \'s#.*"##g\' | sort -u | tail -n 1)',
				  'deployopts':  '',
				  'testopts': '',
				  'setup_fn': {}],
			'rhel9': ['createopts': '--image $(vapor get-images aws --region us-east-1 | grep "RHEL-9.5.0_HVM_GA" | grep x86_64 | sed -e \'s#",##g\' -e \'s#.*"##g\' | sort -u | tail -n 1)',
				  'deployopts':  '',
				  'testopts': '',
				  'setup_fn': {}]]

    providers['gcp'] = ['maxjobs': 4, 'testlevel': 'all', 'vers': ['rhel8', 'rhel9'],
			'has_watchdog': true, 'has_storage': true, 'weekly': false,
			'defaultiscsi': '',
			'defaultuseiscsi': 'yes',
			'defaultblocksize': '',
			'authopts': '--region us-east1',
			'rhel8': ['createopts': '--flavor-workstation rhelha-vapor-workstation8 --flavor rhelha-vapor-node8 --image rhel-8',
				  'deployopts':  '',
				  'testopts': '',
				  'setup_fn': {gcp_setup('8')}],
			'rhel9': ['createopts': '--flavor-workstation rhelha-vapor-workstation9 --flavor rhelha-vapor-node9 --image rhel-9',
				  'deployopts':  '',
				  'testopts': '',
				  'setup_fn': {gcp_setup('9')}]]

    providers['ibmvpc'] = ['maxjobs': 0, 'testlevel': 'all', 'vers': ['rhel8', 'rhel9'],
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

    providers['ocpv'] = ['maxjobs': 4, 'testlevel': 'all', 'vers': ['rhel8', 'rhel9', 'centos10'],
			 'has_watchdog': true, 'has_storage': true, 'weekly': true,
			 'defaultiscsi': '10',
			 'defaultuseiscsi': 'no',
			 'defaultblocksize': '200',
			 'authopts': '',
			 'rhel8': ['createopts': '--flavor-workstation gn1.xlarge --flavor gn1.xlarge --image rhel-8.10.0',
				   'deployopts':  '',
				   'testopts': '',
				   'setup_fn': {}],
			 'rhel9': ['createopts': '--flavor-workstation gn1.xlarge --flavor gn1.xlarge --image rhel-9.5.0',
				   'deployopts':  '',
				   'testopts': '',
				   'setup_fn': {}],
			 'centos10': ['createopts': '--flavor-workstation gn1.xlarge --flavor gn1.xlarge --image centos-10-stream',
				      'deployopts':  '',
				      'testopts': '',
				      'setup_fn': {}]]


    return providers
}
