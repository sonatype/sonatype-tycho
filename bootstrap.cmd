SET BASDIR=C:\TEMP\tycho


@rem Location of Eclipse 3.5.1 with RCP Delta pack
SET TYCHO_TARGET_PLATFORM=C:\eclipse-3.5\eclipse

@rem location of maven used to build bootstrap tycho distribution
SET TYCHO_M2_HOME=C:\maven

@rem Tycho bootstrap distribution location
SET TYCHO_BOOTSTRAP=%BASDIR%\tycho-bootstrap

SET TYCHO_VERSION=0.4.0-SNAPSHOT

@rem Stage 1, build bootstrap tycho distribution
call %TYCHO_M2_HOME%\bin\mvn clean install -e -V -P bootstrap-1
rmdir /s /q "%TYCHO_BOOTSTRAP%"
mkdir "%TYCHO_BOOTSTRAP%"
xcopy "%BASDIR%\tycho-distribution\target\tycho-distribution-%TYCHO_VERSION%-bin.dir\tycho-distribution-%TYCHO_VERSION%" "%TYCHO_BOOTSTRAP%" /E /Q



@rem Stage 2, build full tycho distribution and run integration tests
SET M2_HOME=%TYCHO_BOOTSTRAP%
call %TYCHO_BOOTSTRAP%\bin\mvn clean install -e -V -P bootstrap-2,its ^
  -Dtycho.targetPlatform=%TYCHO_TARGET_PLATFORM% -Dtycho.equinoxIgnore=true

