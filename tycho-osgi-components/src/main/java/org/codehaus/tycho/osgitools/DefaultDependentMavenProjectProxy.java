package org.codehaus.tycho.osgitools;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.sonatype.tycho.resolver.DependentMavenProjectProxy;

public class DefaultDependentMavenProjectProxy
    implements DependentMavenProjectProxy
{

    public DefaultDependentMavenProjectProxy( MavenProject project )
    {

    }

    public String getGroupId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getArtifactId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getVersion()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public File getBasedir()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPackaging()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getContextValue( String key )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setContextValue( String key, Object value )
    {
        // TODO Auto-generated method stub

    }

    public static DependentMavenProjectProxy adapt( MavenProject project )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public File getArtifact()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public File getArtifact( String artifactClassifier )
    {
        // TODO Auto-generated method stub
        return null;
    }

}
