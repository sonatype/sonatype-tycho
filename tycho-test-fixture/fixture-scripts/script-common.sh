## expected to be sourced from other scripts to define common variables and functions

[ -n "$DEBUG" ] && set -vx

# requires $1 item to make path for 
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

# requires $1 number or text
asNumber() {
	[ -n "$1" ] && echo "$1" | grep '[^-0-9]' >/dev/null 2>&1 || echo "$1"
}

# requires $1 code and $2 message
errExit() {
	# errExit {n} <message>
	local exitCode=`asNumber "$1"`
	[ -n "$exitCode" ] && shift
	[ -n "$1" ] && echo "## $scriptName: $1"
	[ -z "$exitCode" ] && exitCode=2
	[ -f "$outFile" ] && echo "## $scriptName output file: $outFile"
	exit "$exitCode"
}

# requires $1 message and $outFile
outFileMessage() {
	[ -n "$1" ] && echo "############# $1 [`date +%Y%m%d-%H%M`]" >> "$outFile"
}

scriptDir=`dirname "$0"`
scriptDir=`javaPath "$scriptDir"`
scriptName=`basename "$0"`
outFile="${outFile:-${scriptDir}/${scriptName}.out.txt}" 
rm -f "$outFile"
[ -f "$outFile" ] && outFile="${scriptDir}/${scriptName}.$$.out.txt"