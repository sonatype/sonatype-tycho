package org.codehaus.tycho.utils;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TychoConstants;

public class TychoProjectUtils
{
    private static final String TYCHO_NOT_CONFIGURED = "Tycho build extension not configured for ";

    /**
     * Returns the {@link TargetPlatform} instance associated with the given project.
     * 
     * @param project a Tycho project
     * @return the target platform for the given project; never <code>null</code>
     * @throws IllegalStateException if the given project does not have an associated target
     *             platform
     */
    public static TargetPlatform getTargetPlatform( MavenProject project )
        throws IllegalStateException
    {
        TargetPlatform targetPlatform = (TargetPlatform) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM );
        if ( targetPlatform == null )
        {
            throw new IllegalStateException( TYCHO_NOT_CONFIGURED + project.toString() );
        }
        return targetPlatform;
    }

    /**
     * Returns the {@link TargetPlatformConfiguration} instance associated with the given project.
     * 
     * @param project a Tycho project
     * @return the target platform configuration for the given project; never <code>null</code>
     * @throws IllegalStateException if the given project does not have an associated target
     *             platform configuration
     */
    public static TargetPlatformConfiguration getTargetPlatformConfiguration( MavenProject project )
        throws IllegalStateException
    {
        TargetPlatformConfiguration targetPlatformConfiguration =
            (TargetPlatformConfiguration) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION );
        if ( targetPlatformConfiguration == null )
        {
            throw new IllegalStateException( TYCHO_NOT_CONFIGURED + project.toString() );
        }
        return targetPlatformConfiguration;
    }
}
