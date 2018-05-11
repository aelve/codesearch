#!/bin/bash

ssh root@167.99.88.190 "
  set -x
  cd /root/aelve/codesearch
  git pull
  sbt web-server/assembly
  systemctl restart codesearch.service
"
