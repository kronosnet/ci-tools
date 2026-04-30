// Return params for covscan
def call(String branch)
{
    // We can make decisions based on the branch if needed

    /* @COMPAT Prior to GLib 2.58, the implementation of g_clear_pointer()
     * triggers the INCONSISTENT_UNION_ACCESS warning. Re-enable that warning
     * when Pacemaker requires GLib >= 2.58.
     */
    return '--all --disable STACK_USE --disable-parse-warnings --disable INCONSISTENT_UNION_ACCESS'
}
