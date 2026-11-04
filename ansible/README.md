# Ansible playbooks

## Sample directory layout

This layout organizes most tasks in roles, with a single inventory file for each environment and a few playbooks in the top-level directory:

```
group_vars/
   apk.yml               # here we assign variables to particular groups
   openindiana.yml
host_vars/
   hostname1.yml         # here we assign variables to particular systems
   hostname2.yml

library/                 # if any custom modules, put them here (optional)
module_utils/            # if any custom module_utils to support modules, put them here (optional)
filter_plugins/          # if any custom filter plugins, put them here (optional)

requirements.yml         # required collections
site.yml                 # main playbook
bootstrap.yml            # playbook for bootstrapping the servers
update.yml               # playbook for updateing the servers
tasks/                   # task files included from playbooks
    update-alpine.yml    # <-- avoids confusing playbook with task files. Use ansible_distribution name.
templates/               # jinja template files used in playbooks. Use ansible_distribution name as prefix.
```

## Examples

__Please note that we do not ship an inventory, as one is generated dynamically by the Jenkins pipelines.__

* Install required collections with `ansible-galaxy collection install -r requirements.yml`
* Run all playbooks: `ansible-playbook -i my-manual-inventory site.yml`
* Run the `update.yml` playbook on a specific server: `ansible-playbook update.yml -i my-manual-inventory --limit alpine-x86-64`
