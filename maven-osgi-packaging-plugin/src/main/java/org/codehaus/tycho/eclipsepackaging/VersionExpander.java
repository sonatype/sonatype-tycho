package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.TychoSession;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Version;

public class VersionExpander
{

    private static final String QUALIFIER = "qualifier";

    private static final String CTX_VERSIONS = null;

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

    public static void setExpandedVersion( TychoSession session, File location, String version )
    {
        getVersions( session ).put( location, version );
    }

    public static void setExpandedVersion( TychoSession session, String location, String version )
    {
        setExpandedVersion( session, new File( location ), version );
    }

    public static String getMavenBaseVersion( TychoSession session, BundleDescription bundle )
    {
        MavenProject mavenProject = session.getMavenProject( bundle.getLocation() );
        if ( mavenProject != null )
        {
            return mavenProject.getVersion(); // not expanded yet
        }
        return null;
    }

    public static String getExpandedVersion( TychoSession session, BundleDescription bundle )
    {
        String version = getVersions( session ).get( new File( bundle.getLocation() ) );
        if ( version != null )
        {
            return version;
        }
        
        return bundle.getVersion().toString();
    }

    public static String getExpandedVersion( TychoSession session, FeatureDescription feature )
    {
        String version = getVersions( session ).get( feature.getLocation() );
        if ( version != null )
        {
            return version;
        }
        
        return feature.getVersion().toString();
    }

    @SuppressWarnings( "unchecked" )
    private static Map<File, String> getVersions( TychoSession session )
    {
        Map<File, String> versions = (Map<File, String>) session.getSessionContext().get( CTX_VERSIONS );
        if ( versions == null )
        {
            versions = new HashMap<File, String>();
            session.getSessionContext().put( CTX_VERSIONS, versions );
        }
        return versions;
    }

}
