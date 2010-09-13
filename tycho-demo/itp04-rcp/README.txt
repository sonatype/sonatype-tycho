Demo RCP application - p2 updateable eclipse product (TYCHO-188,TYCHO-491)

Here we describe by means of a demo RCP application how Tycho (0.10.0-SHAPSHOT) supports RCP builds with the new packing type eclipse-repository. The demo RCP application adds several project modules (below called 'RCP reactor') at location ../sonatype-tycho/tycho-demo/itp04-rcp.

The demo RCP application demonstrates that Tycho supports the creation of p2 updateable eclipse products since the work done for TYCHO-188, TYCHO-491. The related technical details and discussions can be found at TYCHO-188 ( https://issues.sonatype.org/browse/TYCHO-188), TYCHO-491 (https://issues.sonatype.org/browse/TYCHO-491) and wiki (http://wiki.github.com/jlohre/sonatype-tycho/)

The RCP reactor contains a typical feature-based product ('main.product') located in project 'eclipse-repository'. In the same project a category definition file ('category.xml') specifies different categories for two features (example-feature and example-feature-2) both located in the RCP reactor. 

Executing ‘mvn clean install’ on RCP Reactor root location ../sonatype-tycho/tycho-demo/itp04-rcp will result in different repository contents in the build target. This repository contents depend on the contents of the product and category files but also on the configuration of the pom.xml of the eclipse repository project as will be explained in more detail below.

The demo RCP product file and category content and configuration leads e.g. to a ready to use ready-to-use window installation of the  ../itp04-rcp/eclipse-repository/target/products/example.product.id-win32.win32.×86.zip. 

The creation of a p2 repository at location ../itp04-rcp/eclipse-repository/target/repository containing the "p2 published" product main.product happens without any additional configuration of the eclipse-repository project. The fact that the p2 repository also contains the meta-data of the specified features and categories of the category.xml as well as the corresponding artefacts (features and plugins directories contents) is caused by the tycho-p2-publisher-plugin configuration where publishArtifacts is set to true in the pom.xml

{code:xml}
<plugin>
	<groupId>${tycho-groupid}</groupId>
	<artifactId>tycho-p2-publisher-plugin</artifactId>
	<version>${tycho-version}</version>
	<configuration>
		<publishArtifacts>true</publishArtifacts>
	</configuration>
</plugin>
{code}

The materialization of the product at location ../itp04-rcp/eclipse-repository/target/products and the creation of an archive (zip file) of this materialized product at the root of the same location works because of the following tycho-p2-director-plugin configuration: 

{code:xml}
<plugin>
	<groupId>${tycho-groupid}</groupId>
	<artifactId>tycho-p2-director-plugin</artifactId>
	<version>${tycho-version}</version>
	<executions>
		<execution>
			<id>materialize-products</id>
			<goals>
				<goal>materialize-products</goal>
			</goals>
		</execution>
		<execution>
			<id>archive-products</id>
			<goals>
				<goal>archive-products</goal>
			</goals>
		</execution>
	</executions>
	<configuration>
		<products>
			<product>
				<id>example.product.id</id>
			</product>
		</products>
	</configuration>
</plugin>
{code}

Unzipping the above mentioned product archive ../itp04-rcp/eclipse-repository/target/products/example.product.id-win32.win32.×86.zip and starting the contained eclipse.exe will open a very small application window. Because the feature “org.eclipse.equinox.p2.user.ui” was added to the mail.product file the application contains the p2 means to update the product: see menu 'Help' > 'Install new Software...' and/or > 'Check for Updates'.

A. The product update variant 'Install new Software...' via p2 can be confirmed by execute the following steps.
   1. Add the following url as ‘Available Software Sites’ in the preferences
         file:/<local path>/itp04-rcp/eclipse-repository/target/repository (published example product including published features, bundle and categories repo)
   2. Help > 'Install new Software...' and choose the added software site
   3. Confirmed that the 'Example Feature' contained in 'Example Category' has already been installed as part of the product (greyed out node)
   4. Select 'Install Feature 2 Category' which contained the not yet installed "Example Feature 2" and continue the Install wizard to finished the new feature installation
   
B. The product update variant 'Check for Updates' via p2 can be confirmed by execute the following steps.
   1. Increment the example product and feature version e.g. from 0.1.0 to 0.2.0 and build again the entire RCP.
   2. Add the following url as ‘Available Software Sites’ in the preferences (if not already added)
         file:/<local path>/itp04-rcp/eclipse-repository/target/repository (published example product including published features, bundle and categories repo with the incremented versions).
   3. Select the added or already existing software site and press 'Reload'.
   4. Use Help > Check for Updates to confirm that product and included feature with a new version can be updated to the new version.
