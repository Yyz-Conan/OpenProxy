#!/bin/bash
pid=`pidof "java"`
if [ -n "$pid" ]; then
	kill -9 $pid
fi
nohup java -jar HttpProxyServer.jar > server.log 2>&1 &