#! /bin/sh

MVN_RELEASE_PLUGIN=org.apache.maven.plugins:maven-release-plugin:2.5.1

MAVEN=mvn
VERSION_BASE=3.5.3
VERSION_TAG=3.5.3.201412180710-r
RELEASE_VERSION=${VERSION_TAG}-atlassian-6

${MAVEN} $MVN_RELEASE_PLUGIN:clean
${MAVEN} $MVN_RELEASE_PLUGIN:prepare -e \
	-Darguments="-DskipTests=true" \
	-DdevelopmentVersion=${VERSION_BASE}-SNAPSHOT \
	-DreleaseVersion=${RELEASE_VERSION} \
	-Dtag=org.eclipse.jgit-parent-${RELEASE_VERSION}
