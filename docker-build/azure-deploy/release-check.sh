#!/bin/bash

echo "JAVA VERSION: $JAVA_HOME"
echo "JAVA 11 VERSION: $JAVA_HOME_11_X64"
echo "MAVEN VERSION: $(mvn -version)"
# Get version from pom.xml
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid version: $VERSION"
  echo "Version needs to follow standard: x.x.x, for example 1.1.1"
  exit 1
fi

if ! [[ "$SOURCE_BRANCH" =~ ^refs\/heads\/release\/[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid branch name: $SOURCE_BRANCH"
  echo "Release branch needs to follow naming standard: refs/heads/release/1.1.1"
  exit 1
fi