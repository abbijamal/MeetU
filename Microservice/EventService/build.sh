#!/bin/sh
mvn clean package
docker build -t linxianer12/eventservice:latest . 
docker push linxianer12/eventservice:latest 
docker run -p 3002:3002 linxianer12/eventservice:latest  
