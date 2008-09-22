BASDIR=$(pwd)
TYCHO_BOOTSTRAP=$BASDIR/tycho-bootstrap
TARGET_PLATFORM=/opt/eclipse-3.4/eclipse
M2_HOME=/opt/maven


$M2_HOME/bin/mvn clean install -B -U -e

# Build for real using bootstrap tycho from above
rm -rf $TYCHO_BOOTSTRAP; mkdir -p `dirname $TYCHO_BOOTSTRAP`
cp -R $BASDIR/tycho-distribution/target/tycho-distribution-*-bin.dir/tycho-distribution-* $TYCHO_BOOTSTRAP
chmod +x $TYCHO_BOOTSTRAP/bin/mvn
env M2_HOME=$TYCHO_BOOTSTRAP \
  $TYCHO_BOOTSTRAP/bin/mvn -Pdeploy clean install -B -U -e \
  -Dtycho.targetPlatform=$TARGET_PLATFORM

rm -rf $TYCHO_BOOTSTRAP
