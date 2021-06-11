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

version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

gitUser=$(git config user.name)
gitEmail=$(git config user.email)
[[ -z "$gitUser" ]] && git config user.name =  "resesok-adapter Azure Pipelines"
[[ -z "$gitEmail" ]] && git config user.email = "noreply@resesok.adapter.se"

echo "Original version: $last_version"
echo "New version : $version"
versionTag="$version.RELEASE.TEST"

# tag the master branch
echo "# Create local tag $versionTag"
git tag -a $versionTag -m "New RELEASE $version"

echo "# Push tag to origin"
if ! git -c http.extraheader="AUTHORIZATION: bearer $DEVOPS_ACCESSTOKEN" push origin "$version.RELEASE"; then
  echo "# Error push tag $version.RELEASE to origin"
  exit 1
fi

# Set DevOps Pipeline variable otpVersion to new version
echo "##vso[task.setvariable variable=otpVersion]$version"
