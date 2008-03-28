
- Run 'mvn install' on the root dir.

This will build all projects except those for testing support.

- Take a clean Eclipse 3.2.1 install and deploy it into your Maven repository. From the Eclipse directory, do 'mvn targetplatform:install-bundles'. 
(or mvn org.codehaus.tycho:maven-targetplatform-plugin:1.0-SNAPSHOT:install-bundles)

To deploy to a remote repository, run:
mvn targetplatform:deploy-bundles -DremoteRepository=repositoryId::default::repositoryUrl

Deploy the startup.jar:
mvn install:install-file -Dfile=c:\eclipse-3.2.1\startup.jar -DgroupId=org.eclipse -DartifactId=startup -Dversion=3.2.1 -DgeneratePom=true -Dpackaging=jar

- Run 'mvn install -Dbootstrap=false' on the root dir
This will also build the maven-osgi-test-plugin and the org.codehaus.tycho.junit4.runner.


- To try it out: take the org.eclipse.core.variables plugin and import it as a source project. Put the following pom inside:

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>org.eclipse</groupId>
	<artifactId>org.eclipse.core.variables</artifactId>
	<packaging>osgi-bundle</packaging>
	<version>1.0-SNAPSHOT</version>
	<build>
		<sourceDirectory>src</sourceDirectory>
		<plugins>
			<plugin>
				<groupId>org.codehaus.tycho</groupId>
				<artifactId>maven-osgi-lifecycle-plugin</artifactId>
				<extensions>true</extensions>
			</plugin>
		</plugins>
	</build>
</project>

Run 'mvn org.codehaus.tycho:maven-tycho-plugin:1.0-SNAPSHOT:synchronize-plugin-pom -DtargetPlatform=c:\eclipse-321'
This will print out a pom with dependencies for the project.
Copy/paste the dependencies into the POM and do a 'mvn clean install'.

To enable testing (inside a OSGi framework) you need to create a src/test/configuration/config.ini file (like you would create one for a custom launch config in Eclipse)
More on that later....


Some quick rules on creating a osgi bundle project:
- packaging should be osgi-bundle
- add the maven-osgi-lifecycle-plugin as an extension plugin
- every require-bundle or import-package should be provided by one of the dependencies
- every dependency should be a valid osgi bundle (bundles with nested jars are supported)
- the poms of every dependency should also cover their transitive dependencies
(this is because I build a complete OSGi resolver state before compiling each project - this is necessary to calculate the correct classpath)
- if you depend on SWT (and use the sychronize-plugin-pom mojo), you will have to add the platform-dependent fragment yourself (e.g. org.eclipse.swt.win32.win32.x86)
