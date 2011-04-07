package org.sonatype.tycho.p2.maven.repository;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator
    implements BundleActivator
{
    public static final String ID = "org.sonatype.tycho.p2.maven.repository";

    private static BundleContext context;

    private static IProvisioningAgent agent;

    public void start( BundleContext context )
        throws Exception
    {
        Activator.context = context;

        ServiceReference providerRef = context.getServiceReference( IProvisioningAgentProvider.SERVICE_NAME );
        IProvisioningAgentProvider provider = (IProvisioningAgentProvider) context.getService( providerRef );
        // TODO this doesn't return the running agent; is this intended?
        agent = provider.createAgent( null ); // null == currently running system
        context.ungetService( providerRef );
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

    // TODO repositories should not make assumptions on the agent they are loaded by (see callers)
    public static IProvisioningAgent getProvisioningAgent()
    {
        return agent;
    }
}
