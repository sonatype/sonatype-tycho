package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.DefaultTargetPlatform;
import org.codehaus.tycho.ProjectType;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformResolver;

/**
 * Creates target platform based on local eclipse installation.
 */
@Component( role = TargetPlatformResolver.class, hint = LocalTargetPlatformResolver.ROLE_HINT, instantiationStrategy = "per-lookup" )
public class LocalTargetPlatformResolver
    extends AbstractLogEnabled
    implements TargetPlatformResolver
{

    public static final String ROLE_HINT = "local";

    @Requirement
    private EclipseInstallationLayout layout;

    private List<MavenProject> projects;

    private Properties properties;

    private boolean isSubdir( File parent, File child )
    {
        return child.getAbsolutePath().startsWith( parent.getAbsolutePath() );
    }

    public TargetPlatform resolvePlatform( MavenProject project )
    {
        DefaultTargetPlatform platform = new DefaultTargetPlatform();

        for ( File site : layout.getSites() )
        {
            platform.addSite( site );

            for ( File plugin : layout.getPlugins( site ) )
            {
                platform.addArtifactFile( ProjectType.OSGI_BUNDLE, plugin );
            }

            for ( File feature : layout.getFeatures( site ) )
            {
                platform.addArtifactFile( ProjectType.ECLIPSE_FEATURE, feature );
            }
        }

        File parentDir = null;

        for ( MavenProject otherProject : projects )
        {
            File otherBasedir = otherProject.getBasedir();

            platform.addArtifactFile( otherProject.getPackaging(), otherBasedir );

            if ( parentDir == null || isSubdir( otherBasedir, parentDir ) )
            {
                parentDir = otherBasedir;
            }
        }

        platform.addSite( parentDir );

        platform.setProperties( properties );

        return platform;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        // ignore, we are not going to copy anything to the local repository
    }

    public void setMavenProjects( List<MavenProject> projects )
    {
        this.projects = projects;

    }

    public void setLocation( File location )
    {
        layout.setLocation( location );
    }

    public void setProperties( Properties properties )
    {
        this.properties = properties;
    }
}
