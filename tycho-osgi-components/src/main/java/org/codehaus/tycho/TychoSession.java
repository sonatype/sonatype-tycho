package org.codehaus.tycho;

import java.io.File;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.osgitools.project.EclipsePluginProject;
import org.sonatype.tycho.TargetPlatform;

public interface TychoSession
{

    public BundleResolutionState getBundleResolutionState( MavenProject project );

    public MavenProject getMavenProject( File location );

    public MavenProject getMavenProject( String location );

    public FeatureResolutionState getFeatureResolutionState( MavenProject project );

    public TargetPlatform getTargetPlatform( MavenProject project );

    public EclipsePluginProject getEclipsePluginProject( MavenProject project );

    public Map<String, Object> getSessionContext();
}
