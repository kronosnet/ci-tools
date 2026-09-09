// Return list of branches, which will have their RPMs and Coverity scan results published
def call()
{
    return ['main', 'pcs-0.10', 'pcs-0.11', 'pcs-0.12', 'pcs-1.x']
}
