package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.sonatype.tycho.DefaultTargetPlatform;
import org.sonatype.tycho.ProjectType;
import org.sonatype.tycho.TargetPlatform;
import org.sonatype.tycho.TargetPlatformResolver;

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

    private Properties properties = new Properties();

    private Map<File, String> projects = new LinkedHashMap<File, String>();

    public void addMavenProject( File location, String type, String groupId, String artifactId, String version )
    {
        projects.put( location, type );
    }

    public void addRepository( URI location )
    {
        File basedir = new File( location );
        layout.setLocation( basedir );
    }

    public TargetPlatform resolvePlatform( File projectLocation )
    {
        if ( !projects.keySet().contains( projectLocation ) )
        {
            return null;
        }

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

        for ( Map.Entry<File, String> entry : projects.entrySet() )
        {
            platform.addArtifactFile( entry.getValue(), entry.getKey() );

            if ( parentDir == null || isSubdir( entry.getKey(), parentDir ) )
            {
                parentDir = entry.getKey();
            }
        }
        
        platform.addSite( parentDir );

        platform.setProperties( properties );

        return platform;
    }

    private boolean isSubdir( File parent, File child )
    {
        return child.getAbsolutePath().startsWith( parent.getAbsolutePath() );
    }

    public void setLocalRepositoryLocation( File lcoation )
    {
        // ignore, we are not going to copy anything to the local repository
    }

    public void setProperties( Properties properties )
    {
        this.properties = new Properties();
        this.properties.putAll( properties );
    }

}
