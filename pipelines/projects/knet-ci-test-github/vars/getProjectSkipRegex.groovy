// Java pattern matching style
// Commits whose messages match this pattern won't trigger the build pipeline

def call()
{
    return '.*\\[ci\\-skip\\].*'
}
