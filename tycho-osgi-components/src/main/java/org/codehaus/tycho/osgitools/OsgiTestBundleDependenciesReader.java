package org.codehaus.tycho.osgitools;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.ProjectType;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.maven.DependenciesReader;

@Component( role = DependenciesReader.class, hint = ProjectType.ECLIPSE_TEST_PLUGIN )
public class OsgiTestBundleDependenciesReader
    extends OsgiBundleDependenciesReader
{

}
