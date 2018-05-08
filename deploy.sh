#!/bin/bash

set -x

ssh root@167.99.88.190 "
  cd /root/aelve/codesearch
  git pull
  sbt web-server/clean
  sbt web-server/assembly
  mv codesearch-server.jar ../
  systemctl restart codesearch.service
"
