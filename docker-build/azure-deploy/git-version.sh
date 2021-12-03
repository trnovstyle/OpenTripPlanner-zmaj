#!/bin/bash

versionTag="$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout).RELEASE"
echo "New version : $versionTag"

gitUser=$(git config user.name)
gitEmail=$(git config user.email)
[[ -z "$gitUser" ]] && git config user.name =  "resesok-adapter Azure Pipelines"
[[ -z "$gitEmail" ]] && git config user.email = "noreply@resesok.adapter.se"

# Get rid of all local tags
git -c http.extraheader="AUTHORIZATION: bearer $DEVOPS_ACCESSTOKEN" fetch --prune  origin --tags -f

export MAVEN_OPTS="-Xmx2G"

mvn clean package deploy -Pjunit-report --settings docker-build/azure-deploy/settings.xml -Dsettings.security=/tmp/settings-security.xml -B -U

if [ $(git tag -l $versionTag) ]; then
  echo "Tag already exists, skipping."
  exit 0
fi

# tag the master branch
echo "# Create local tag $versionTag"
if ! git tag -a $versionTag -m "New RELEASE $new_version"; then
  echo "# Error while creating tag $versionTag"
  exit 1
fi

echo "# Push tag to origin"
if ! git -c http.extraheader="AUTHORIZATION: bearer $DEVOPS_ACCESSTOKEN" push origin "$versionTag"; then
  echo "# Error push tag $versionTag to origin"
  exit 1
fi
