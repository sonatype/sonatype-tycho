To build tycho source tree

* Download pre-built tycho distribution  v0.3.0-SNAPSHOT or better from
  http://repository.sonatype.org/service/local/repositories/eclipse-snapshots/content/org/codehaus/tycho/tycho-distribution
  
* unzip tycho-distribution-${version}.jar into an empty folder (TYCHO_HOME from here on)

* export M2_HOME=$TYCHO_HOME

* $M2_HOME/bin/mvn clean install