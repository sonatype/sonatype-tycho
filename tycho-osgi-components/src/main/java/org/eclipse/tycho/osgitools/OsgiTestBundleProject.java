package org.eclipse.tycho.osgitools;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.TychoProject;

@Component( role = TychoProject.class, hint = ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN )
public class OsgiTestBundleProject
    extends OsgiBundleProject
{
}
