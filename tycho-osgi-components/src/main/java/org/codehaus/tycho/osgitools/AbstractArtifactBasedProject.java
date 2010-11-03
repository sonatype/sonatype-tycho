package org.codehaus.tycho.osgitools;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TychoConstants;
import org.sonatype.tycho.ReactorProject;

public abstract class AbstractArtifactBasedProject
    extends AbstractTychoProject
{
    // this is stricter than Artifact.SNAPSHOT_VERSION
    public static final String SNAPSHOT_VERSION = "-SNAPSHOT";

    // requires resolved target platform
    public ArtifactDependencyWalker getDependencyWalker( MavenProject project )
    {
        return getDependencyWalker( project, null );
    }

    public ArtifactDependencyWalker getDependencyWalker( MavenProject project, TargetEnvironment environment )
    {
        return newDependencyWalker( project, environment );
    }

    protected abstract ArtifactDependencyWalker newDependencyWalker( MavenProject project, TargetEnvironment environment );

    @Override
    public void resolve( MavenSession session, MavenProject project )
    {
        TargetPlatformConfiguration configuration =
            (TargetPlatformConfiguration) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION );

        if ( configuration == null )
        {
            throw new IllegalStateException( "Target platform configuration has not been initialized for project "
                + project.toString() );
        }

        // this throws exceptions when dependencies are missing
        getDependencyWalker( project ).walk( new ArtifactDependencyVisitor()
        {
        } );
    }

    protected String getOsgiVersion( ReactorProject project )
    {
        String version = project.getVersion();
        if ( version.endsWith( SNAPSHOT_VERSION ) )
        {
            version = version.substring( 0, version.length() - SNAPSHOT_VERSION.length() ) + ".qualifier";
        }
        return version;
    }
}
