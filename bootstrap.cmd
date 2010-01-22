rem Location of Eclipse 3.5 with RCP Delta pack
set TYCHO_TARGET_PLATFORM=c:\Users\Seven\Desktop\eclipse

rem location of maven used to build bootstrap tycho distribution
set TYCHO_M2_HOME=c:\java\maven

rem Stage 0, update versions
rem %TYCHO_M2_HOME%/bin/mvn -e -U -up -Ppseudo-release,full \
rem  org.codehaus.mojo:buildnumber-maven-plugin:create org.sonatype.plugins:maven-version-plugin:set-version \
rem  -Dmaven.repo.local=%M2_LOCAL_REPO%

call %TYCHO_M2_HOME%/bin/m32 clean install -e -V -Pbootstrap-1

call %TYCHO_M2_HOME%/bin/m32 clean install -e -V -Pbootstrap-2,its -Dtycho.targetPlatform=%TYCHO_TARGET_PLATFORM%
