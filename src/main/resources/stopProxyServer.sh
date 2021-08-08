#!/bin/bash
kill -9 $(ps -ef | grep "java -jar HttpProxyServer.jar"| grep -v grep | awk '{print $2}')