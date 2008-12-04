package org.codehaus.tycho.osgitools;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.maven.DependenciesReader;

@Component( role = DependenciesReader.class, hint = "eclipse-test-plugin" )
public class OsgiTestBundleDependenciesReader extends OsgiBundleDependenciesReader {

}
