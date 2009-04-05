package org.codehaus.tycho.osgitools;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.maven.DependenciesReader;
import org.sonatype.tycho.ProjectType;

@Component( role = DependenciesReader.class, hint = ProjectType.ECLIPSE_TEST_PLUGIN )
public class OsgiTestBundleDependenciesReader
    extends OsgiBundleDependenciesReader
{

}
