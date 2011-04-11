package org.eclipse.tycho.p2.impl.test;

import org.eclipse.tycho.p2.impl.publisher.SiteDependenciesAction;
import org.junit.Test;

public class VersionCreateTest
{
    @Test
    public void test()
        throws Exception
    {
        SiteDependenciesAction.createSiteVersion( "0.10.0" ); // maven RELEASE version
        SiteDependenciesAction.createSiteVersion( "0.10.0-SNAPSHOT" ); // maven SNAPSHOT version
        SiteDependenciesAction.createSiteVersion( "0.10.0.20100205-2200" ); // maven RELEASE version
        SiteDependenciesAction.createSiteVersion( "0.10.0.qualifier" ); // maven RELEASE version
    }
}
