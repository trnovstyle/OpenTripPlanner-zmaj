#!/bin/bash

# This script will work on triggered branch from Azure Pipelines and will create a new release version and push it to Skanetrafiken Maven release repository.
# The release will always increment the patch number. This means that if the last release was 3.0.8 next will be 3.0.9, etc. The strategy for making a new major or minor is to create
# a release candidate tag on any commit after last release. A release candidate will have the format x.y.z-RCm where x.y.z is the new release number. The instead of incrementing the patch
# This script will create a new release x.y.z.

# This script must have correct permissions to push tag to Git repository. In Azure DevOps in the project settings for repositories change permissions for user Tokyo Build Service set e.g. these policies to "Allow"
# * Contribute
# * Contribute to pull requests
# * Create branch
# * Create tag
# * Read

# Possible version tags to consider
# a.b.c.RELEASE - Release version with major(a), minor(b), patch(c)
# a.b.c-RCm     - Release candidate for version a.b.c. m = 1, 2, 3, ...
#
# Major changes specially for incompatible API changes
# Minor changes for added functionality in a backwards compatible manner
# Patch changes for backwards compatible bug fixes


last_version=$(git describe --match=[0-9]*)

if ! [[ $last_version =~ ^[0-9]+\.[0-9]+\.[0-9]+(-RC|\.RELEASE){1}.* ]]; then
	echo "version $last_version is bad the last version is not correct format exit with error and coder needs to create tag on earlier commit of correct format"
	exit 1
fi

if [[ $last_version =~ .*RC.* ]]; then
  echo "Creating release from release candidate: $last_version"
  version=$(echo $last_version  | sed -e  's/^\([0-9]\+\.[0-9]\+\.[0-9]\+\).*/\1/')
else
  echo "Creating new release with incremented patch from version: $last_version"
  patch=$(echo $last_version | sed -e 's/^[0-9]\+\.[0-9]\+\.\([0-9]\+\).*/\1/')
  # Bump patch
  patch=$(($patch+1))
  # major.minor. (note the '.' in the end)
  majorMinor=$(echo $last_version  | sed -e  's/^\([0-9]\+\.[0-9]\+\.\).*/\1/')
  version="$majorMinor$patch"
fi

gitUser=$(git config user.name)
gitEmail=$(git config user.email)
[[ -z "$gitUser" ]] && git config user.name =  "resesok-adapter Azure Pipelines"
[[ -z "$gitEmail" ]] && git config user.email = "noreply@resesok.adapter.se"

echo "Original version: $last_version"
echo "New version : $version"
versionTag="$version.RELEASE"

currentBranch=$(echo $SOURCE_BRANCH | sed 's:.*/::')

git -c http.extraheader="AUTHORIZATION: bearer $DEVOPS_ACCESSTOKEN" checkout -b $currentBranch
git -c http.extraheader="AUTHORIZATION: bearer $DEVOPS_ACCESSTOKEN" pull
git -c http.extraheader="AUTHORIZATION: bearer $DEVOPS_ACCESSTOKEN" fetch --prune --prune-tags

echo "# Update pom.xml with new version and commit changes"
mvn versions:set -DnewVersion=$version
mvn versions:commit
git commit pom.xml -m "New Release $version"
if ! git -c http.extraheader="AUTHORIZATION: bearer $DEVOPS_ACCESSTOKEN" push -u origin $currentBranch; then
  echo "# Error committing new version $version"
  exit 1
fi

# tag the master branch
echo "# Create local tag $versionTag"
git tag -a $versionTag -m "New RELEASE $versionTag"

echo "# Push tag to origin"
if ! git -c http.extraheader="AUTHORIZATION: bearer $DEVOPS_ACCESSTOKEN" push origin "$versionTag"; then
  echo "# Error push tag $versionTag to origin"
  exit 1
fi

# Set DevOps Pipeline variable otpVersion to new version
echo "##vso[task.setvariable variable=otpVersion]$version"
