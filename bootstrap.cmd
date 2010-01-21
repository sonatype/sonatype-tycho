rem Location of Eclipse 3.5 with RCP Delta pack
set TYCHO_TARGET_PLATFORM=c:/temp/eclipse

rem location of maven used to build bootstrap tycho distribution
set TYCHO_M2_HOME=%M2_HOME%

rem Stage 0, update versions
rem %TYCHO_M2_HOME%/bin/mvn -e -U -up -Ppseudo-release,full \
rem  org.codehaus.mojo:buildnumber-maven-plugin:create org.sonatype.plugins:maven-version-plugin:set-version \
rem  -Dmaven.repo.local=%M2_LOCAL_REPO%

call %TYCHO_M2_HOME%/bin/mvn clean install -e -V -Pbootstrap-1

call %TYCHO_M2_HOME%/bin/mvn clean install -e -V -Pbootstrap-2 -Dtycho.targetPlatform=%TYCHO_TARGET_PLATFORM%
