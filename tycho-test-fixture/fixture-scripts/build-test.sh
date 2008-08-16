#!/bin/bash
info() {
cat <<EOF
# $0
# Build tycho (if needed), fixture pom's, and fixture update site
#
# Requires from environment (can define variables in $0.local): 
# - \$JAVA_HOME (1.5 or later, with tools.jar)
# - \$builderMavenDir bootstraps the tycho build
# - \$targetPlatformDir contains eclipse 3.4 classic, for building or using tycho
# - settings.xml set up (http://docs.codehaus.org/display/M2ECLIPSE/Building+tycho)
# - build targets in \$testDir should contain build-[pass|fail|tbd].txt file
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
if [ "$1" == "-info" ] ; then
	info
	exit 0
fi

javaPath() {
    local sedJavaPath='s|/cygdrive/\([a-zA-Z]\)/|\1:/|'
    if [ -d "$1" ] ; then
	    local jpResult=`cd "$1"; pwd | sed "$sedJavaPath"`
	    echo "$jpResult"
    elif [ -f "$1" ] ; then
    	local jpDir=`dirname "$1"`
    	local jpName=`basename "$1"`
    	[ -z "$jpDir" ] && jpDir=.
    	echo `javaPath "$jpDir"`"/$jpName"
    else
		echo "$1" | sed "$sedJavaPath"
    fi 
}

errExit() {
	[ 0 = $# ] && exit 1 "## $scriptName: Unknown error"
	[ 0 = $# ] && exit 1 "## $scriptName: Unknown error"
	# todo: handle message-only exits?
	[ -n "$2" ] && echo "## $scriptName: $2"
	[ -n "$1" ] && exit $1
}

outFileMessage() {
	[ -n "$1" ] && echo "############# $1" >> "$outFile"
}

###########################################################
[ -n "$DEBUG" ] && set -vx

skipTychoTests=-Dmaven.test.skip=true

scriptName=`basename "$0"`
scriptDir=`dirname "$0"`
scriptDir=`javaPath "$scriptDir"`
trunkDir=`javaPath "$scriptDir/../.."`
sandboxDir="$scriptDir/temp-workspace"
rm -rf "$sandboxDir"
mkdir -p "$sandboxDir"

[ -f "${0}.local" ] && . "${0}.local" 
outFile="${outFile:-${sandboxDir}/${scriptName}.out.txt}"
rm -f "$outFile" && echo "" > "$outFile"
builtTychoDir="${builtTychoDir:-$sandboxDir/builtTychoDir}"
targetPlatformDir="${targetPlatformDir:-$sandboxDir/target-platform/eclipse}"
testDir="${testDir:-$scriptDir/../../tycho-test-fixture}"

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
	
	cd "$trunkDir"
	## todo: settings.xml required?
	outFileMessage "Building tycho"
	"$builderMavenDir"/bin/mvn clean package $skipTychoTests -Dtycho.targetPlatform="$targetPlatformDir" >> "$outFile" 2>&1
	zipFile=`ls "$trunkDir"/tycho-distribution/target/tycho-distribution*-bin.zip 2>/dev/null`
	## todo: zip file not here (installed)
	[ -f "$zipFile" ] || errExit 4 "## $0 FAILED to create zip in tycho-distribution"
	mkdir -p "$builtTychoDir" 
	pushd "$builtTychoDir" > /dev/null 2>&1
	"$JAVA_HOME"/bin/jar xf "$zipFile" || errExit 43 "failed to unzip \"$zipFile\""
	mv tycho-distribution*/* .
	popd > /dev/null 2>&1
	[ -d "$builtTychoDir" ] || errExit 4 "## failed to create \"$builtTychoDir\" "
fi ## builtTychoDir

[ -f "$builtTychoDir/bin/mvn" ] || errExit 4 "## invalid \"$builtTychoDir\" "

## run tests
cd "$testDir"

## build the poms
if [ -z "$skipPoms" ] ; then
	command=org.codehaus.tycho:maven-tycho-plugin:generate-poms
	# todo bug: parameters can't handle quotes?
	#parameters="-DbaseDir=\"$testDir\" -DgroupId=tycho.testArtifacts.group -Daggregator=true -Dtycho.targetPlatform=$targetPlatformDir"
	parameters="-DbaseDir=$testDir -DgroupId=tycho.testArtifacts.group -Daggregator=true -Dtycho.targetPlatform=$targetPlatformDir"
	outFileMessage "Building poms"
	"$builtTychoDir"/bin/mvn $command $parameters >> "$outFile" 2>&1
fi

## build targets (identified by "checkResult.sh" file)
rm -rf */target
for i in `ls */checkResult.sh 2>/dev/null`; do
	buildDir=`dirname "$i"`
	if [ -f "$buildDir/poma.xml" ] ; then
		pomArg="-f $buildDir/poma.xml" 
	elif [ -f "$buildDir/pom.xml" ] ; then
		pomArg="-f $buildDir/pom.xml" 
	else
		pomArg="" 
	fi
	outFileMessage "Building $buildDir (pomArg=${pomArg})"
	"$builtTychoDir"/bin/mvn package $pomArg -Dtycho.targetPlatform=$targetPlatformDir >> "$outFile" 2>&1
	"$i"
done

tail -20 "$outFile"
echo "$outFile"
