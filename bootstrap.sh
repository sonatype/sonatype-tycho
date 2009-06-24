#!/bin/bash

BASDIR=$(pwd)


# Location of Eclipse 3.4.1 with RCP Delta pack
TYCHO_TARGET_PLATFORM=/opt/eclipse-3.5/eclipse


# location of maven used to build bootstrap tycho distribution
M2_HOME=/opt/maven

# Tycho bootstrap distribution location
TYCHO_BOOTSTRAP=$BASDIR/tycho-bootstrap



# Stage 1, build bootstrap tycho distribution
$M2_HOME/bin/mvn clean install -e
rm -rf $TYCHO_BOOTSTRAP; mkdir -p `dirname $TYCHO_BOOTSTRAP`
cp -R $BASDIR/tycho-distribution/target/tycho-distribution-*-bin.dir/tycho-distribution-* $TYCHO_BOOTSTRAP
chmod +x $TYCHO_BOOTSTRAP/bin/mvn



# Stage 2, build full tycho distribution and run integration tests
env M2_HOME=$TYCHO_BOOTSTRAP \
  $TYCHO_BOOTSTRAP/bin/mvn -Pits clean install -e \
  -Dtycho.targetPlatform=$TYCHO_TARGET_PLATFORM -Dtycho.equinoxIgnore=true



# cleanup
#rm -rf $TYCHO_BOOTSTRAP
