# Ansible playbooks

## Sample directory layout

This layout organizes most tasks in roles, with a single inventory file for each environment and a few playbooks in the top-level directory:

```
production               # inventory file for production servers
testing                  # inventory file for testing servers

group_vars/
   alpine.yml            # here we assign variables to particular groups
   freebsd.yml
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
    update-alpine.yml    # <-- avoids confusing playbook with task files. Use inventory group name.
```

## Examples

* Install required collections with `ansible-galaxy collection install -r requirements.yml`
* Run all playbooks on all **production** servers: `ansible-playbook site.yml`
* Run all playbooks on all **testing** servers: `ansible-playbook -i testing site.yml`
* Run the `update.yml` playbook on a specific **production** server: `ansible-playbook update.yml --limit alpine-x86-64`
