package bundle.test;

import java.net.URI;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class BundleShapeTest
{
    @Test
    public void test()
        throws Exception
    {
        Bundle bundle = getBundle( "bundle" );

        URI entry = URIUtil.toURI( FileLocator.resolve( bundle.getEntry( "data/file.txt" ) ) );

        Assert.assertEquals( "file", entry.getScheme() );
    }

    public Bundle getBundle( String id )
    {
        for ( Bundle bundle : Activator.context.getBundles() )
        {
            if ( id.equals( bundle.getSymbolicName() ) )
            {
                return bundle;
            }
        }

        throw new IllegalStateException();
    }
}
