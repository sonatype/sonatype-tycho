#!/bin/bash
# called to verify results of build

scriptDir=`dirname "$0"`
# site.xml:
#   update site works
# com.company.tools.core.feature_1.0.0.jar
#   indirectly-reference feature
# com.company.tools.packageService.user_1.0.0.jar
#   if compiles ok, host fragment is properly visible to client of host

#files="site.xml features/com.company.tools.core.feature_1.0.0.jar"
files="site.xml features/com.company.tools.core.feature_1.0.0.jar plugins/com.company.packageService.provider.fragment"
for e in $files; do
	expectedFile="$scriptDir/target/site/$e"
	[ -f  "$expectedFile" ] || echo "## $0 FAIL: expected \"$expectedFile\" "
done
