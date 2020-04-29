#!/bin/bash

bashdir=$(dirname $(readlink -f $0))
DOCKER_IMG="tokyo/otp"

cd $bashdir

docker run --rm --entrypoint java --tmpfs /graph -v $bashdir/data:/graph/data $DOCKER_IMG:latest -Xmx5G -jar /code/otp-shaded.jar --build /graph/data
