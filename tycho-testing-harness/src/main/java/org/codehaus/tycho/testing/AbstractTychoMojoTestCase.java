package org.codehaus.tycho.testing;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MavenPluginCollector;
import org.apache.maven.plugin.MavenPluginDiscoverer;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.util.FileUtils;

public class AbstractTychoMojoTestCase extends AbstractMojoTestCase {

	protected File getBasedir(String name) throws IOException {
		File src = new File( getBasedir(), "src/test/resources/" + name );
		File dst = new File( getBasedir(), "target/" + name);
	
		if (dst.isDirectory()) {
			FileUtils.deleteDirectory(dst);
		} else if (dst.isFile()) {
			if (!dst.delete()) {
				throw new IOException("Can't delete file " + dst.toString());
			}
		}
	
		FileUtils.copyDirectoryStructure(src, dst);
		
		return dst;
	}

	protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
		super.customizeContainerConfiguration(containerConfiguration);
		containerConfiguration.addComponentDiscoverer( new MavenPluginDiscoverer() );
		containerConfiguration.addComponentDiscoveryListener( new MavenPluginCollector() );
	}

	protected ArtifactRepository getLocalRepository() throws Exception {
		ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup(ArtifactRepositoryLayout.ROLE, "legacy");
		
		File path = new File("target/local-repo").getCanonicalFile();

		ArtifactRepository r = new DefaultArtifactRepository("local", "file://"	+ path, repoLayout);

		return r;
	}

	protected MavenExecutionRequest newMavenExecutionRequest(File pom) {
		Properties props = System.getProperties();
        ProfileActivationContext ctx = new DefaultProfileActivationContext( props, false );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
		request.setBaseDirectory(pom.getParentFile());
		request.setPom(pom);
		request.setProfileManager(new DefaultProfileManager( getContainer(), ctx ));
		request.setProperties(props);
		request.setUserProperties(props);
		return request;
	}

}
