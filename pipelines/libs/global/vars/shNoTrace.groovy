// Run a shell command without showing the actual command itself in the logs
def call(String cmd, String visible) {
    echo("${visible}")
    def a = sh (script: '#!/bin/sh -e\n'+cmd)
}
