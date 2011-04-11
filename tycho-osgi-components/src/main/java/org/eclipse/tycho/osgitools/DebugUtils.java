package org.eclipse.tycho.osgitools;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public class DebugUtils
{
    public static boolean isDebugEnabled( MavenSession session, MavenProject project )
    {
        String config = session.getUserProperties().getProperty( "tycho.debug.resolver" );
        return config != null && config.trim().equals( project.getArtifactId() );
    }
}
