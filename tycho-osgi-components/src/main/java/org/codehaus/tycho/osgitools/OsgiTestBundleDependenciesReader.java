package org.codehaus.tycho.osgitools;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.maven.DependenciesReader;
import org.sonatype.tycho.ProjectType;
import org.sonatype.tycho.TargetPlatformResolver;

@Component( role = DependenciesReader.class, hint = ProjectType.ECLIPSE_TEST_PLUGIN )
public class OsgiTestBundleDependenciesReader
    extends OsgiBundleDependenciesReader
{
    public void addProject( TargetPlatformResolver resolver, MavenProject project )
    {
        resolver.addMavenProject( project.getBasedir(), ProjectType.ECLIPSE_TEST_PLUGIN, project.getGroupId(),
                                  project.getArtifactId(), project.getVersion() );
    }
}
