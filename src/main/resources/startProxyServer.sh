#!/bin/bash
pid=`ps -ef | grep "java -jar HttpProxyServer.jar"| grep -v grep | awk '{print $2}'`
if [ -n "$pid" ]; then
	kill -9 $pid
fi
nohup java -jar HttpProxyServer.jar > server.log 2>&1 &