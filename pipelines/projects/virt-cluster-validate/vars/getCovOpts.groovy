// Return params for covscan
def call(String branch)
{
    // We can make decisions based on the branch if needed
    return '--all --disable STACK_USE --disable-parse-warnings'
}
