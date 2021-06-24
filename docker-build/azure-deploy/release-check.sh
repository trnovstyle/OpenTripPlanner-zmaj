#!/bin/bash

# Get version from pom.xml
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid version: $VERSION"
  exit 1
fi

if ! [[ "$SOURCE_BRANCH" =~ ^refs\/heads\/release\/[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "# Invalid branch name: $SOURCE_BRANCH"
  exit 1
fi

