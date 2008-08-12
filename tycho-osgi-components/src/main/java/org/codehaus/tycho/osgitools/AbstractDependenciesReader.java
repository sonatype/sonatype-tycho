package org.codehaus.tycho.osgitools;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.maven.DependenciesReader;

public abstract class AbstractDependenciesReader extends AbstractLogEnabled implements DependenciesReader {
	protected static final List<Dependency> NO_DEPENDENCIES = new ArrayList<Dependency>();

}
