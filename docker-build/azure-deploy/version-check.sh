#!/bin/bash

# This script will work on triggered branch from Azure Pipelines and will fetch release version and push new git git.
# Then it will project and push it to Skanetrafiken Maven release repository.


# This script must have correct permissions to push tag to Git repository. In Azure DevOps in the project settings for repositories change permissions for user Tokyo Build Service set e.g. these policies to "Allow"
# * Contribute
# * Contribute to pull requests
# * Create branch
# * Create tag
# * Read

# Compare versions. Return 1 if the first version is bigger, return 2 if the second version is bigger
# otherwise return 0
# Does not require any external utilities
vercomp () {
    if [[ $1 == $2 ]]
    then
        return 0
    fi
    local IFS=.
    local i ver1=($1) ver2=($2)
    # fill empty fields in ver1 with zeros
    for ((i=${#ver1[@]}; i<${#ver2[@]}; i++))
    do
        ver1[i]=0
    done
    for ((i=0; i<${#ver1[@]}; i++))
    do
        if [[ -z ${ver2[i]} ]]
        then
            # fill empty fields in ver2 with zeros
            ver2[i]=0
        fi
        if ((10#${ver1[i]} > 10#${ver2[i]}))
        then
            return 1
        fi
        if ((10#${ver1[i]} < 10#${ver2[i]}))
        then
            return 2
        fi
    done
    return 0
}

# Get version from pom.xml
new_version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

if ! [[ $new_version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "New Version $new_version is badly formatted"
  exit 1
fi

old_version=$(git describe --match=[0-9]*)

if ! [[ $old_version =~ ^[0-9]+\.[0-9]+\.[0-9]+(-RC|\.RELEASE){1}.* ]]; then
	echo "Version $old_version is bad. The last version is not correct formatted. Coder needs to create tag on earlier commit of correct format"
	exit 1
fi

old_version=$(echo $old_version | sed -e 's/^\([0-9]\+\.[0-9]\+\.[0-9]\+\).*/\1/')

vercomp $old_version $new_version
compare_result=$?

# Set DevOps Pipeline variable otpVersion to new version
echo "##vso[task.setvariable variable=otpVersion]$new_version"
