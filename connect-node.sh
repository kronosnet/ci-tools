#!/bin/sh
# Copy the Jenkins jar file to the remote node in $1
# and run the start_node script

curl  https://ci.kronosnet.org/jnlpJars/agent.jar | ssh $1 "cat > /root/bin/agent.jar"
ssh $1 sh /root/bin/start_node
