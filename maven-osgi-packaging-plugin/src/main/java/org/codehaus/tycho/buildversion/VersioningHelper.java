package org.codehaus.tycho.buildversion;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.TychoConstants;
import org.osgi.framework.Version;

public class VersioningHelper
{
    public static final String QUALIFIER = "qualifier";

    public static String getExpandedVersion( MavenProject project, String originalVersion )
    {
        if ( project == null || originalVersion == null )
        {
            throw new IllegalArgumentException();
        }

        String version = (String) project.getContextValue( TychoConstants.CTX_EXPANDED_VERSION );
        if ( version != null )
        {
            return version;
        }

        return originalVersion;
    }

    public static void setExpandedVersion( MavenProject project, String originalVersion, String qualifier )
    {
        Version version = Version.parseVersion( originalVersion );

        String expandedVersion =
            new Version( version.getMajor(), version.getMinor(), version.getMicro(), qualifier ).toString();

        String oldVersion = (String) project.getContextValue( TychoConstants.CTX_EXPANDED_VERSION );

        if ( oldVersion != null && !oldVersion.equals( expandedVersion ) )
        {
            throw new IllegalStateException( "Cannot redefine expanded version" );
        }

        project.setContextValue( TychoConstants.CTX_EXPANDED_VERSION, expandedVersion );
    }
}
