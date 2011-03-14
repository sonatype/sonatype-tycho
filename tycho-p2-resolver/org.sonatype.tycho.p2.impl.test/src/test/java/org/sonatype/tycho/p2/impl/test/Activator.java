package org.sonatype.tycho.p2.impl.test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator
    implements BundleActivator
{

    private static BundleContext context;

    public void start( BundleContext context )
        throws Exception
    {
        this.context = context;
        for ( Bundle bundle : context.getBundles() )
        {
            if ( "org.eclipse.equinox.p2.exemplarysetup".equals( bundle.getSymbolicName() ) )
            {
                bundle.start( Bundle.START_TRANSIENT );
            }
        }
    }

    public void stop( BundleContext context )
        throws Exception
    {
    }

    public static BundleContext getContext()
    {
        return context;
    }
}
