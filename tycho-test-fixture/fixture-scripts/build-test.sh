#!/bin/bash
set -vx
info() {
cat <<EOF
# $0
# Build tycho (if needed), fixture pom's, and buildable projects in fixture
#
# Requires from environment (can define variables in $0.local): 
# - \$JAVA_HOME (1.5 or later, with tools.jar)
# - \$builderMavenDir bootstraps the tycho build
# - \$tychoTargetPlatformDir contains eclipse 3.4 classic, for building or using tycho
# - settings.xml set up (http://docs.codehaus.org/display/M2ECLIPSE/Building+tycho)
# - build targets in \$testDir should contain build-[pass|fail|tbd].txt file
#
# Reads from environment, if available: 
# \$buildTychoDir is the result of the tycho build (created by building if undefined)
#
# Current limitations: 
# - does not clean up build results
# - temp files go to $sandboxDir
# - merely requests missing target platform (\$tychoTargetPlatformDir) 
#   or bootstrap builder (\$builderMavenDir)
# - have to manually clean out interim work product from sandbox
# - no spaces in directory variable values 
#   (mvn gacks when reading quoted parameter values?)
EOF
}


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

asNumber() {
	[ -n "$1" ] && echo "$1" | grep '[^-0-9]' >/dev/null 2>&1 || echo "$1"
}
errExit() {
	# errExit {n} <message>
	local exitCode=`asNumber "$1"`
	[ -n "$exitCode" ] && shift
	[ -n "$1" ] && echo "## $scriptName: $1"
	[ -z "$exitCode" ] && exitCode=2
	[ -f "$outFile" ] && echo "## $scriptName output file: $outFile"
	exit "$exitCode"
}

outFileMessage() {
	[ -n "$1" ] && echo "############# $1" >> "$outFile"
}

getExtraDirsArg() {
	## extradirs: is this single-level or magic grandparent directory?
	# was just for targets, but need to build poms
	# TODO: assumes we are grandparent and all are roots
	# find -maxdepth 1 -mindepth 1 -type d
	# find -iname manifest.mf | sed 's|/meta-inf.*||;s|/META-INF.*||;s|^./||'
	local extraDirsArgTemp=""
	local extraDirsSep=""
	local prefixDir=`javaPath .`
	for magicDirName in plugins features updatesites tests; do # TODO magic dirs for now
		if [ -d "${magicDirName}" ] ; then
			extraDirsArgTemp="${extraDirsArgTemp}${extraDirsSep}${prefixDir}/${magicDirName}"
			extraDirsSep=","		
		fi 
	done
	[ -n "$extraDirsArgTemp" ] && extraDirsArgTemp="-DextraDirs=${extraDirsArgTemp}"
	echo "$extraDirsArgTemp"
}

###########################################################
[ -n "$DEBUG" ] && set -vx

if [ "$1" == "-info" ] ; then
	info
	exit 0
elif [ -d "$1" ] ; then
	testDir=`javaPath "$1"`
fi

## read local variable definitions, if any
[ -f "${0}.local" ] && . "${0}.local" 

## set default values 
scriptName=`basename "$0"`
scriptDir=`dirname "$0"`
scriptDir=`javaPath "$scriptDir"`
trunkDir=`javaPath "$scriptDir/../.."` # note: location
sandboxDir="${sandboxDir:-${scriptDir}/temp-workspace}"
builtTychoDir="${builtTychoDir:-$sandboxDir/builtTychoDir}"
tychoTargetPlatformDir="${tychoTargetPlatformDir:-$sandboxDir/target-platform/eclipse}"
testTargetPlatformDir="${testTargetPlatformDir:-$tychoTargetPlatformDir}"
testDir="${testDir:-$scriptDir/../../tycho-test-fixture}"
outFile="${outFile:-${sandboxDir}/${scriptName}.out.txt}"
settingsFile="${settingsFile:-${scriptDir}/settings.xml}"
skipTychoTests="${skipTychoTests:--Dmaven.test.skip=true}"
checkScriptName="${checkScriptName:-checkResult.sh}"

## check input values
[ -d "$testDir" ] || errExit 23 "no testDir: $testDir" 
[ -f "$JAVA_HOME"/lib/tools.jar ] || errExit 22 "Setup JAVA_HOME with tools.jar: $JAVA_HOME/lib/tools.jar "
[ -d "$testTargetPlatformDir" ] || errExit 23 "no testTargetPlatformDir: $testTargetPlatformDir" 
[ -f "$settingsFile" ] || errExit 22 "Need maven settings \${settingsFile}: $settingsFile"

## setup sandbox, output sink
[ -n "$cleanSandbox" ] && rm -rf "$sandboxDir" && mkdir -p "$sandboxDir"
rm -f "$outFile" && echo "" > "$outFile"

	
#### build tycho if pre-built directory not available
if [ ! -d "$builtTychoDir" ] ; then
	## check targetplatform
	if [ ! -d "$tychoTargetPlatformDir" ] ; then
		cat<<EOF
	* Download Eclipse 3.4 "classic" package for your platform from http://www.eclipse.org/downloads/
	
	* unzip  Eclipse 3.4 "classic" package into 
		$tychoTargetPlatformDir
EOF
		errExit 2 "additional setup required"
	fi	
	
	builderMavenDir="${builderMavenDir:-$sandboxDir/bootstrap-tycho}"	
	if [ ! -d "$builderMavenDir/bin" ] ; then
		cat<<EOF
	* Download pre-built tycho distribution  v0.3.0-SNAPSHOT or better from
	  http://repository.sonatype.org/service/local/repositories/eclipse-snapshots/content/org/codehaus/tycho/tycho-distribution
	  
	* unzip tycho-distribution-${version}.jar into 
	 $builderMavenDir
EOF
		errExit 2 "additional setup required"
	fi
	
	cd "$trunkDir"
	## build tycho and unzip
	outFileMessage "Building tycho"
	"$builderMavenDir"/bin/mvn $debugMvn -s "$settingsFile"  clean -Dtycho.targetPlatform="$tychoTargetPlatformDir" \
		>> "$outFile" 2>&1 || errExit 87 "tycho clean failed"
	[ -d "$trunkDir/tycho-distribution/target" ] && errExit 89 "tycho clean failed - have target"
	"$builderMavenDir"/bin/mvn $debugMvn -s "$settingsFile" -Pdeploy install $skipTychoTests -Dtycho.targetPlatform="$tychoTargetPlatformDir" \
		>> "$outFile" 2>&1 || errExit 88 "tycho build failed"
	zipFile=`ls "$trunkDir"/tycho-distribution/target/tycho-distribution*-bin.zip 2>/dev/null` # note: location
	[ -f "$zipFile" ] || errExit 4 "## $0 FAILED to create zip in tycho-distribution"
	mkdir -p "$builtTychoDir" 
	pushd "$builtTychoDir" > /dev/null 2>&1
	"$JAVA_HOME"/bin/jar xf "$zipFile" || errExit 43 "failed to unzip \"$zipFile\""
	mv tycho-distribution*/* .
	popd > /dev/null 2>&1
	[ -f "$builtTychoDir/bin/mvn" ] || errExit 4 "failed to create builtTychoDir=\"$builtTychoDir\" "
fi ## builtTychoDir

[ -f "$builtTychoDir/bin/mvn" ] || errExit 4 "invalid builtTychoDir=\"$builtTychoDir\" "

## run tests
cd "$testDir"  || errExit 41 "unable to cd $testDir"

extraDirsArg=`getExtraDirsArg`

## build the poms
if [ -z "$skipPoms" ] ; then
	[ -d "$testTargetPlatformDir/plugins" ] || errExit 5 "invalid target platform: $testTargetPlatformDir"
	checkedTTP="testTargetPlatformDir"
	# todo: need to clean the poms?
	command=org.codehaus.tycho:maven-tycho-plugin:generate-poms
	# todo bug: parameters can't handle quotes?
	#parameters="-DbaseDir=\"$testDir\" -DgroupId=tycho.testArtifacts.group -Daggregator=true -Dtycho.targetPlatform=$testTargetPlatformDir"
	parameters="-DbaseDir=$testDir -DgroupId=tycho.testArtifacts.group -Daggregator=true -Dtycho.targetPlatform=$testTargetPlatformDir"
	outFileMessage "Building poms using flags: $mvnFlags -s \"$settingsFile\" ${extraDirsArg} $parameters "
	"$builtTychoDir"/bin/mvn $mvnFlags -s "$settingsFile"  $command ${extraDirsArg} $parameters >> "$outFile" 2>&1
fi

## build targets (identified by "${checkScriptName}" file)
rm -rf */target
for i in `ls */${checkScriptName} */*/${checkScriptName} 2>/dev/null`; do
	if [ -n "${checkedTTP}" ] ; then
		[ -d "$testTargetPlatformDir/plugins" ] || errExit 5 "invalid target platform: $testTargetPlatformDir"
		checkedTTP="testTargetPlatformDir"
	fi
	buildDir=`dirname "$i"`
	# which pom to build
	if [ -f "$buildDir/poma.xml" ] ; then
		pomArg="-f $buildDir/poma.xml" 
	elif [ -f "$buildDir/pom.xml" ] ; then
		pomArg="-f $buildDir/pom.xml" 
	else
		pomArg="" 
	fi
	# build
	"$builtTychoDir"/bin/mvn $mvnFlags -s "$settingsFile"  package $pomArg ${extraDirsArg} -Dtycho.targetPlatform=$testTargetPlatformDir >> "$outFile" 2>&1
	"$i"
done

tail -20 "$outFile"
echo "$outFile"
