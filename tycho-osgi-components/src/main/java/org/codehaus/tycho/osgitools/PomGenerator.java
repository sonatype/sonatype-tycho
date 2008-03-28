package org.codehaus.tycho.osgitools;

import java.util.List;

import org.apache.maven.model.Model;
import org.eclipse.osgi.service.resolver.BundleDescription;

public interface PomGenerator
{
	
	static final String ROLE = PomGenerator.class.getName();
	
	Model createBundlePom(GroupMapper groupMapper, List/*<BundleDescription>*/ deployedArtifacts, String versionQualifier, BundleDescription bundle);
}
