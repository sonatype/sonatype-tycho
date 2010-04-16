package org.codehaus.tycho.maven.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.ArtifactDescription;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.osgitools.targetplatform.LocalTargetPlatformResolver;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;

public class LocalTargetPlatformResolverTest
    extends AbstractTychoMojoTestCase
{
    public void testBundleIdParsing()
        throws Exception
    {
        TargetPlatform platform = getTargetPlatform( new File( "src/test/resources/targetplatforms/basic" ) );

        ArtifactDescription artifact = platform.getArtifact( TychoProject.ECLIPSE_PLUGIN, "bundle01", null );
        ArtifactKey key = artifact.getKey();
        assertEquals( "bundle01", key.getId() );
        assertEquals( "0.0.1", key.getVersion() );

        File file = artifact.getLocation();
        assertEquals( "bundle01_0.0.1", file.getName() );
    }

    protected TargetPlatform getTargetPlatform( File location )
        throws Exception, IOException
    {
        LocalTargetPlatformResolver resolver = (LocalTargetPlatformResolver) lookup( TargetPlatformResolver.class );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        MavenSession session = new MavenSession( getContainer(), request, result );
        session.setProjects( new ArrayList<MavenProject>() );

        MavenProject project = new MavenProject();

        resolver.setLocation( location );

        TargetPlatform platform = resolver.resolvePlatform( session, project, null );
        return platform;
    }

    public void testPlatformRelativePath()
        throws Exception
    {
        File platformPath = new File( "src/test/resources/targetplatforms/basic" );
        TargetPlatform platform = getTargetPlatform( platformPath );

        // canonical path to a bundle
        File bundlePath =
            new File( platformPath, "plugins/org.eclipse.equinox.launcher_1.0.101.R34x_v20081125.jar" ).getCanonicalFile();

        ArtifactDescription artifact = platform.getArtifact( bundlePath );

        assertNotNull( artifact );
    }

    public void testBundleRelativePath()
        throws Exception
    {
        File platformPath = new File( "src/test/resources/targetplatforms/basic" ).getCanonicalFile();
        TargetPlatform platform = getTargetPlatform( platformPath );

        File bundlePath =
            new File(
                      "src/test/resources/targetplatforms/basic/plugins/org.eclipse.equinox.launcher_1.0.101.R34x_v20081125.jar" );

        ArtifactDescription artifact = platform.getArtifact( bundlePath );

        assertNotNull( artifact );
    }
}
