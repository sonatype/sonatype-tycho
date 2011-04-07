package org.sonatype.tycho.p2.tools.impl;

import java.io.File;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.sonatype.tycho.p2.tools.FacadeException;

public class Activator
    implements BundleActivator
{
    private static BundleContext context;

    public void start( BundleContext context )
        throws Exception
    {
        Activator.context = context;
    }

    public void stop( BundleContext context )
        throws Exception
    {
    }

    public static IProvisioningAgent createProvisioningAgent( final File targetDirectory )
        throws FacadeException
    {
        ServiceReference serviceReference = context.getServiceReference( IProvisioningAgentProvider.SERVICE_NAME );
        IProvisioningAgentProvider agentFactory = (IProvisioningAgentProvider) context.getService( serviceReference );
        try
        {
            return agentFactory.createAgent( new File( targetDirectory, "p2agent" ).toURI() );
        }
        catch ( ProvisionException e )
        {
            throw new FacadeException( e );
        }
        finally
        {
            context.ungetService( serviceReference );
        }
    }
}
