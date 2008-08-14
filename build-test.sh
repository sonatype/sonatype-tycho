#!/bin/bash
info() {
cat <<EOF
# $0
# Build tycho (if needed), fixture pom's, and fixture update site
#
# Requires from environment: 
# - \$JAVA_HOME (1.5 or later, with tools.jar)
# - \$builderMavenDir bootstraps the tycho build
# - \$targetPlatformDir contains eclipse 3.4 classic, for building or using tycho
# - settings.xml set up (http://docs.codehaus.org/display/M2ECLIPSE/Building+tycho)
#
# Reads from environment, if available: 
# \$buildTychoDir is the result of the tycho build (created by building if undefined)
#
# Current limitations: 
# - does not clean up build results
# - temp files go to $sandboxDir
# - merely requests missing target platform (\$targetPlatformDir) 
#   or bootstrap builder (\$builderMavenDir)
# - have to manually clean out interim work product from sandbox
# - no spaces in directory variable values 
#   (mvn gacks when reading quoted parameter values?)
EOF
}

javaPath() {
	echo "$1" | sed 's|/cygdrive/\([a-zA-Z]\)/|\1:/|'
}

errExit() {
	[ -n "$2" ] && echo "## $scriptName: $2"
	[ -n "$1" ] && exit $1
}

[ -n "$DEBUG" ] && set -vx

scriptName=`basename "$0"`
scriptDir=`dirname "$0"`
scriptDir=`cd "$scriptDir"; pwd`
scriptDir=`javaPath "$scriptDir"`
sandboxDir="$scriptDir/build-test-workspace"
builtTychoDir="${builtTychoDir:-$sandboxDir/builtTychoDir}"

if [ "$1" == "-info" ] ; then
	info
	exit 0
fi

targetPlatformDir="${targetPlatformDir:-$sandboxDir/target-platform/eclipse}"
testDir="${testDir:-$scriptDir/tycho-test-fixture}"

[ -d "$testDir" ] || errExit 23 "no testDir: $testDir" 
[ -f "$JAVA_HOME"/lib/tools.jar ] || errExit 22 "Setup JAVA_HOME with tools.jar: $JAVA_HOME/lib/tools.jar "
if [ ! -d "$targetPlatformDir" ] ; then
	cat<<EOF
* Download Eclipse 3.4 "classic" package for your platform from http://www.eclipse.org/downloads/

* unzip  Eclipse 3.4 "classic" package into 
	$targetPlatformDir
EOF
	errExit 2 "additional setup required"
fi	
	
if [ ! -d "$builtTychoDir" ] ; then
	builderMavenDir="${builderMavenDir:-$sandboxDir/bootstrap-tycho}"
	
	if [ ! -d "$builderMavenDir" ] ; then
		cat<<EOF
	* Download pre-built tycho distribution  v0.3.0-SNAPSHOT or better from
	  http://repository.sonatype.org/service/local/repositories/eclipse-snapshots/content/org/codehaus/tycho/tycho-distribution
	  
	* unzip tycho-distribution-${version}.jar into 
	 $builderMavenDir
EOF
		errExit 2 "additional setup required"
	fi
	
	cd "$scriptDir"
	## todo: settings.xml required?
	#"$builderMavenDir"/bin/mvn clean package -Dtycho.targetPlatform="$targetPlatformDir"
	zipFile=`ls "$scriptDir"/tycho-distribution/target/tycho-distribution*-bin.zip`
	## todo: zip file not here (installed)
	[ -f "$zipFile" ] || errExit 4 "## $0 FAILED to create zip in tycho-distribution"
	mkdir -p "$builtTychoDir" 
	pushd "$builtTychoDir" > /dev/null 2>&1
	"$JAVA_HOME"/bin/jar xf "$zipFile" || errExit 43 "failed to unzip \"$zipFile\""
	mv tycho-distribution*/* .
	popd > /dev/null 2>&1
	[ -d "$builtTychoDir" ] || errExit 4 "## failed to create \"$builtTychoDir\" "
fi ## builtTychoDir

## build the poms
cd "$testDir"

command=org.codehaus.tycho:maven-tycho-plugin:generate-poms
# todo bug: parameters can't handle quotes?
#parameters="-DbaseDir=\"$testDir\" -DgroupId=tycho.testArtifacts.group -Daggregator=true -Dtycho.targetPlatform=$targetPlatformDir"
parameters="-DbaseDir=$testDir -DgroupId=tycho.testArtifacts.group -Daggregator=true -Dtycho.targetPlatform=$targetPlatformDir"
"$builtTychoDir"/bin/mvn $command $parameters

## build everything, check for the update site
"$builtTychoDir"/bin/mvn package -Dtycho.targetPlatform=$targetPlatformDir

updateSiteXml=`ls -d *updatesite/target/site/site.xml`
[ -f "$updateSiteXml" ] || errExit 99 "update site.xml not found?"

