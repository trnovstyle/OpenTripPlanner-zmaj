# Introduction
TODO: Write some Skanetrafken specifics on project

# Build
TODO: Write something

# Release
Deploy new version to [Skanetrfafiken Nexus](https://nexus-dev.skanetrafiken.se) server consists on first changing version number in POM to desired next version and
making the deploy to Nexus from local computer.
``
$ mvn clean deploy
``
Then set the version to next planned SNAPSHOT version, e.g. if the version was *1.0.0* then set version to *1.1.0-SNAPSHOT* always set patch to *0*.

Then tag new version number on corresponding GIT commit on *resesok-otp* branch and push to central GIT repository. 