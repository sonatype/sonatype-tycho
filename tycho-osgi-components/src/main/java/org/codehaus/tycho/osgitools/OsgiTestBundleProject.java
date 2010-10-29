package org.codehaus.tycho.osgitools;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.TychoProject;
import org.sonatype.tycho.ArtifactKey;

@Component( role = TychoProject.class, hint = ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN )
public class OsgiTestBundleProject
    extends OsgiBundleProject
{
}
