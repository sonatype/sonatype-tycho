package org.sonatype.tycho.p2.maven.repository;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator
    implements BundleActivator
{
    
    public static final String ID = "org.sonatype.tycho.p2.maven.repository";
    
    private static BundleContext context;

    public void start( BundleContext context )
        throws Exception
    {
        Activator.context = context;
    }

    public void stop( BundleContext context )
        throws Exception
    {
        Activator.context = null;
    }

    public static BundleContext getContext()
    {
        return context;
    }
}
