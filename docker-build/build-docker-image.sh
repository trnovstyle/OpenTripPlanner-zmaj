#!/bin/bash

cleanTarget() {
	if [ -d docker-build/target ]; then
		rm -rf docker-build/target
	fi
}

commitId=`git rev-parse --short HEAD`
COMMITID=$commitId
bashdir=$(dirname "$(readlink -f "$0")")
#DOCKER_IMG="Tokyo/resesok-otp"
cd $bashdir/.. || exit

cleanTarget
mkdir docker-build/target
cp target/*.jar docker-build/target

#docker build --tag=$DOCKER_IMG:$commitId -f ./docker-build/Dockerfile .
#docker tag $DOCKER_IMG:$commitId $DOCKER_IMG:latest
#
#docker push $DOCKER_IMG:$commitId
#docker push $DOCKER_IMG:latest

#regexp=".*SNAPSHOT.*"
## Check if mvnversion is not a snapshot else just make a default tag
#mvnversion=$(mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout)
#if [[ $mvnversion =~ $regexp ]]; then
#	docker build --tag=$DOCKER_IMG -f ./docker-build/Dockerfile .
#else
#	docker build --tag=$DOCKER_IMG:$mvnversion -f ./docker-build/Dockerfile .
#fi

