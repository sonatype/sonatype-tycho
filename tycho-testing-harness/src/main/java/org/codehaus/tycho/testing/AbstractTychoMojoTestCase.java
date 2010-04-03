package org.codehaus.tycho.testing;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.Maven;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.FileUtils;

public class AbstractTychoMojoTestCase extends AbstractMojoTestCase {

    protected Maven maven;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        maven = lookup(Maven.class);
    }

    @Override
    protected void tearDown() throws Exception {
        maven = null;
        super.tearDown();
    }

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
		Properties systemProps = new Properties();
		systemProps.putAll(System.getProperties());

		Properties userProps = new Properties();
		userProps.put("tycho-version", "0.0.0");

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
		request.setBaseDirectory(pom.getParentFile());
		request.setPom(pom);
		request.setSystemProperties(systemProps);
		request.setUserProperties(userProps);
		request.setLocalRepository(getLocalRepository());
		return request;
	}

    protected List<MavenProject> getSortedProjects(File basedir, File platform) throws Exception {
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);
        request.setLocalRepository(getLocalRepository());
        if (platform != null) {
            request.getUserProperties().put("tycho.targetPlatform", platform.getCanonicalPath());
        }
        MavenExecutionResult result = maven.execute( request );
        if (result.hasExceptions()) {
            throw new CompoundRuntimeException(result.getExceptions());
        }
        return result.getTopologicallySortedProjects();
    }

    protected MavenSession newMavenSession( MavenProject project, List<MavenProject> projects ) throws Exception
    {
        MavenExecutionRequest request = newMavenExecutionRequest( new File( project.getBasedir(), "pom.xml" ) );
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        MavenSession session = new MavenSession(getContainer(), request, result);
        session.setCurrentProject( project );
        session.setProjects( projects );
        return session;
    }
    
}
