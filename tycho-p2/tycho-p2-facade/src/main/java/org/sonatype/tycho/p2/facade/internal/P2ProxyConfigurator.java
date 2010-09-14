package org.sonatype.tycho.p2.facade.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.tycho.maven.MultiplexExecutionListener;
import org.codehaus.tycho.maven.TychoLifecycleParticipant;
import org.sonatype.tycho.osgi.EquinoxEmbedder;
import org.sonatype.tycho.p2.ProxyServiceFacade;

@Component( role = TychoLifecycleParticipant.class, hint = "P2ProxyConfigurator" )
public class P2ProxyConfigurator
    implements TychoLifecycleParticipant
{

    @Requirement
    private Logger logger;

    @Requirement
    private EquinoxEmbedder equinoxEmbedder;

    private ProxyServiceFacade proxyService;

    public void configure( MavenSession session )
    {
        configureProxy( session );
    }

    private void configureProxy( MavenSession session )
    {
        final List<Proxy> activeProxies = new ArrayList<Proxy>();
        for ( Proxy proxy : session.getSettings().getProxies() )
        {
            if ( proxy.isActive() )
            {
                activeProxies.add( proxy );
            }
        }
        equinoxEmbedder.registerAfterStartCallback( new Runnable()
        {

            public void run()
            {
                proxyService = equinoxEmbedder.getService( ProxyServiceFacade.class );
                // make sure there is no old state from previous aborted builds
                logger.debug( "clear OSGi proxy settings" );
                proxyService.clearPersistentProxySettings();
                for ( Proxy proxy : activeProxies )
                {
                    logger.debug( "Configure OSGi proxy for protocol " + proxy.getProtocol() + ", host: "
                        + proxy.getHost() + ", port: " + proxy.getPort() + ", nonProxyHosts: "
                        + proxy.getNonProxyHosts() );
                    proxyService.configureProxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(),
                                                 proxy.getUsername(), proxy.getPassword(), proxy.getNonProxyHosts() );
                }
            }
        } );
        MavenExecutionRequest request = session.getRequest();
        MultiplexExecutionListener listener = new MultiplexExecutionListener( request.getExecutionListener() );
        request.setExecutionListener( listener );
        listener.addListener( new AbstractExecutionListener()
        {
            @Override
            public void sessionEnded( ExecutionEvent event )
            {
                if ( proxyService != null )
                {
                    logger.debug( "clear OSGi proxy settings" );
                    proxyService.clearPersistentProxySettings();
                }
            }
        } );
    }

}
