package org.codehaus.tycho.osgitools;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TychoConstants;

public abstract class AbstractArtifactBasedProject
    extends AbstractTychoProject
{

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
    public void resolve( MavenProject project )
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
}
