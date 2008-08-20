#!/bin/bash
# called to verify results of build

scriptDir=`dirname "$0"`
expectedFile="$scriptDir/target/site/site.xml"
[ -f  "$expectedFile" ] || echo "## $0 FAIL: expected \"$expectedFile\" "
expectedFile="$scriptDir/target/site/features/com.company.tools.core.feature_1.0.0.jar"
## TODO enable when included-features are supported
#[ -f  "$expectedFile" ] || echo "## $0 FAIL: expected \"$expectedFile\" "

