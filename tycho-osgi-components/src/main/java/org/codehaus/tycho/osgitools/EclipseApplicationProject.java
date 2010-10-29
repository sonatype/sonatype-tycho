package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.ProductConfiguration;
import org.sonatype.tycho.ArtifactKey;

@Component( role = TychoProject.class, hint = org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_APPLICATION )
public class EclipseApplicationProject
    extends AbstractArtifactBasedProject
{
    @Override
    protected ArtifactDependencyWalker newDependencyWalker( MavenProject project, TargetEnvironment environment )
    {
        final ProductConfiguration product = loadProduct( project );
        return new AbstractArtifactDependencyWalker( getTargetPlatform( project, environment ),
                                                     getEnvironments( project, environment ) )
        {
            public void walk( ArtifactDependencyVisitor visitor )
            {
                traverseProduct( product, visitor );
            }
        };
    }

    protected ProductConfiguration loadProduct( final MavenProject project )
    {
        File file = new File( project.getBasedir(), project.getArtifactId() + ".product" );
        try
        {
            return ProductConfiguration.read( file );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Could not read product configuration file " + file.getAbsolutePath(), e );
        }
    }

    public ArtifactKey getArtifactKey( MavenProject project )
    {
        ProductConfiguration product = loadProduct( project );
        String id = product.getId() != null ? product.getId() : project.getArtifactId();
        String version = product.getVersion() != null ? product.getVersion() : getOsgiVersion( project );

        return new DefaultArtifactKey( org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_APPLICATION, id, version );
    }
}
