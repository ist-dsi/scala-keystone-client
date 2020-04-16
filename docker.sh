#!/bin/bash
sudo docker stop dev-keystone
sudo docker rm dev-keystone
sudo systemctl start docker
sudo docker run -d -e IPADDR=0.0.0.0 -p 5000:5000 -p 35357:35357 --name=dev-keystone openio/openstack-keystone:queens
