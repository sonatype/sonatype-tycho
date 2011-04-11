package org.eclipse.tycho.p2.facade.test;

import junit.framework.Assert;

import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.junit.Test;

public class RepositoryLayoutHelperTest
{
    @Test
    public void testRelpath()
    {
        GAV gav = new GAV( "a.b.c", "d.e.f", "1.0.0" );
        Assert.assertEquals( "a/b/c/d.e.f/1.0.0/d.e.f-1.0.0-foo.bar", RepositoryLayoutHelper.getRelativePath(
            gav,
            "foo",
            "bar" ) );

        Assert.assertEquals( "a/b/c/d.e.f/1.0.0/d.e.f-1.0.0.jar", RepositoryLayoutHelper.getRelativePath(
            gav,
            null,
            null ) );

    }

    @Test
    public void testRelpathSimpleGroupId()
    {
        GAV gav = new GAV( "a", "b", "1.0.0" );
        Assert.assertEquals( "a/b/1.0.0/b-1.0.0.jar", RepositoryLayoutHelper.getRelativePath( gav, null, null ) );

    }
}
