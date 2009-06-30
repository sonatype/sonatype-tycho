package org.codehaus.tycho.eclipsepackaging;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.MavenSessionUtils;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Version;

public class VersioningHelper
{

    private static final String QUALIFIER = "qualifier";

    /**
     * Returns true is version is a snapshot version, i.e. qualifier is ".qualifier"
     */
    public static boolean isSnapshotVersion( Version version )
    {
        return QUALIFIER.equals( version.getQualifier() );
    }

    public static Version expandVersion( Version version, String qualifier )
    {
        if ( isSnapshotVersion( version ) )
        {
            return new Version( version.getMajor(), version.getMinor(), version.getMicro(), qualifier );
        }

        return version;
    }

    public static void setExpandedVersion( MavenSession session, File location, String version )
    {
        MavenProject project = MavenSessionUtils.getMavenProject( session, location );
        if ( project == null )
        {
            throw new IllegalArgumentException( location.getAbsolutePath() + " does not correspond to a reactor project basedir." );
        }
        project.setContextValue( TychoConstants.CTX_EXPANDED_VERSION, version );
    }

    public static void setExpandedVersion( MavenSession session, String location, String version )
    {
        setExpandedVersion( session, new File( location ), version );
    }

    public static String getMavenBaseVersion( MavenSession session, BundleDescription bundle )
    {
        MavenProject mavenProject = MavenSessionUtils.getMavenProject( session, bundle.getLocation() );
        if ( mavenProject != null )
        {
            return mavenProject.getVersion(); // not expanded yet
        }
        return null;
    }

    public static String getExpandedVersion( MavenSession session, BundleDescription bundle )
    {
        MavenProject project = MavenSessionUtils.getMavenProject( session, bundle.getLocation() );
        if ( project != null )
        {
            String version = (String) project.getContextValue( TychoConstants.CTX_EXPANDED_VERSION );
            if ( version != null )
            {
                return version;
            }
        }

        return bundle.getVersion().toString();
    }

    public static String getExpandedVersion( MavenSession session, FeatureDescription feature )
    {
        MavenProject project = MavenSessionUtils.getMavenProject( session, feature.getLocation() );
        if ( project != null )
        {
            String version = (String) project.getContextValue( TychoConstants.CTX_EXPANDED_VERSION );
            if ( version != null )
            {
                return version;
            }
        }
        
        return feature.getVersion().toString();
    }

}
