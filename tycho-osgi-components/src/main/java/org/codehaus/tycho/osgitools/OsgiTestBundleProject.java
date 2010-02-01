package org.codehaus.tycho.osgitools;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.TychoProject;

@Component( role = TychoProject.class, hint = TychoProject.ECLIPSE_TEST_PLUGIN )
public class OsgiTestBundleProject
    extends OsgiBundleProject
{
}
