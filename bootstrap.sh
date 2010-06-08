#!/bin/bash

BASDIR=$(pwd)

# Location of Eclipse 3.6M7 with RCP Delta pack
TYCHO_TARGET_PLATFORM=/opt/eclipse-3.6-rcp/eclipse

# location of maven used to build bootstrap tycho distribution
TYCHO_M2_HOME=/opt/maven

export MAVEN_OPTS=-Xmx512m

#export MAVEN_PARAMS="-Dmaven.repo.local=/tmp/localrepo -Dit.cliOptions=-U"

$TYCHO_M2_HOME/bin/mvn clean install -e -V -Pbootstrap-1 || exit

$TYCHO_M2_HOME/bin/mvn clean install -e -V -Pbootstrap-2 -Dtycho.targetPlatform=$TYCHO_TARGET_PLATFORM || exit

$TYCHO_M2_HOME/bin/mvn clean install -U -up -e -V -Pits -Dtycho.targetPlatform=$TYCHO_TARGET_PLATFORM ${MAVEN_PARAMS} || exit
