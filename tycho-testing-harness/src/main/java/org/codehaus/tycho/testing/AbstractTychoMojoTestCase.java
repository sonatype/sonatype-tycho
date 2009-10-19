package org.codehaus.tycho.testing;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.repository.RepositorySystem;
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

//	protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
//		super.customizeContainerConfiguration(containerConfiguration);
//		containerConfiguration.addComponentDiscoverer( new MavenPluginDiscoverer() );
//		containerConfiguration.addComponentDiscoveryListener( new MavenPluginCollector() );
//	}

	@Override
    protected String getCustomConfigurationName()
    {
        String name = AbstractTychoMojoTestCase.class.getName().replace( '.', '/' ) + ".xml";
        return name;
    }

	protected ArtifactRepository getLocalRepository() throws Exception {
	    RepositorySystem repoSystem = lookup(RepositorySystem.class);
		
		File path = new File("target/local-repo").getCanonicalFile();

		ArtifactRepository r = repoSystem.createLocalRepository( path );

		return r;
	}

	protected MavenExecutionRequest newMavenExecutionRequest(File pom) throws Exception {
		Properties props = new Properties();
		props.putAll(System.getProperties());

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
		request.setBaseDirectory(pom.getParentFile());
		request.setPom(pom);
		request.setSystemProperties(props);
		request.setLocalRepository(getLocalRepository());
		return request;
	}

}
