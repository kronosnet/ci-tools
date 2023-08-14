def call(String cmd, String visible) {
    echo("${visible}")
    def a = sh (script: '#!/bin/sh -e\n'+cmd)
}
