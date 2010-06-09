package org.sonatype.tycho.p2.impl.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.tycho.utils.PlatformPropertiesUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.p2.facade.internal.P2RepositoryCache;
import org.sonatype.tycho.p2.facade.internal.P2ResolutionResult;
import org.sonatype.tycho.p2.facade.internal.P2Resolver;
import org.sonatype.tycho.p2.resolver.P2ResolverImpl;
import org.sonatype.tycho.test.util.HttpServer;

public class P2ResolverImplTest
{
    private HttpServer server;

    @After
    public void stopHttpServer()
        throws Exception
    {
        HttpServer _server = server;
        server = null;
        if ( _server != null )
        {
            _server.stop();
        }
    }

    @Test
    public void basic()
        throws Exception
    {
        P2ResolverImpl impl = new P2ResolverImpl();
        impl.setRepositoryCache( new P2RepositoryCache() );
        impl.setLocalRepositoryLocation( getLocalRepositoryLocation() );
        impl.addP2Repository( new File( "resources/repositories/e342" ).getCanonicalFile().toURI() );

        File bundle = new File( "resources/resolver/bundle01" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.resolver.test.bundle01";
        String artifactId = "org.sonatype.tycho.p2.impl.resolver.test.bundle01";
        String version = "1.0.0-SNAPSHOT";

        impl.setEnvironments( getEnvironments() );
        impl.addMavenArtifact( new ArtifactMock( bundle, groupId, artifactId, version, P2Resolver.TYPE_ECLIPSE_PLUGIN ) );

        List<P2ResolutionResult> results = impl.resolveProject( bundle );

        impl.stop();

        Assert.assertEquals( 1, results.size() );
        P2ResolutionResult result = results.get( 0 );

        Assert.assertEquals( 2, result.getArtifacts().size() );
    }

    @Test
    public void offline()
        throws Exception
    {
        server = HttpServer.startServer();
        String url = server.addServer( "e342", new File( "resources/repositories/e342" ) );

        // prime local repository
        P2ResolverImpl impl = new P2ResolverImpl();
        resolveFromHttp( impl, url );

        // now go offline and resolve again
        impl = new P2ResolverImpl();
        impl.setOffline( true );
        List<P2ResolutionResult> results = resolveFromHttp( impl, url );

        Assert.assertEquals( 1, results.size() );
        P2ResolutionResult result = results.get( 0 );

        Assert.assertEquals( 2, result.getArtifacts().size() );
    }

    @Test
    public void offlineNoLocalCache()
        throws Exception
    {
        server = HttpServer.startServer();
        String url = server.addServer( "e342", new File( "resources/repositories/e342" ) );

        delete( getLocalRepositoryLocation() );

        P2ResolverImpl impl = new P2ResolverImpl();
        impl.setOffline( true );

        try
        {
            resolveFromHttp( impl, url );
            Assert.fail();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            // TODO better assertion
        }
    }

    protected List<P2ResolutionResult> resolveFromHttp( P2ResolverImpl impl, String url )
        throws IOException, URISyntaxException
    {
        impl.setRepositoryCache( new P2RepositoryCache() );
        impl.setLocalRepositoryLocation( getLocalRepositoryLocation() );
        impl.addP2Repository( new URI( url ) );

        File bundle = new File( "resources/resolver/bundle01" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.resolver.test.bundle01";
        String artifactId = "org.sonatype.tycho.p2.impl.resolver.test.bundle01";
        String version = "1.0.0-SNAPSHOT";

        impl.setEnvironments( getEnvironments() );
        impl.addMavenArtifact( new ArtifactMock( bundle, groupId, artifactId, version, P2Resolver.TYPE_ECLIPSE_PLUGIN ) );

        List<P2ResolutionResult> results = impl.resolveProject( bundle );
        return results;
    }

    protected File getLocalRepositoryLocation()
        throws IOException
    {
        return new File( "target/localrepo" ).getCanonicalFile();
    }

    private List<Map<String, String>> getEnvironments()
    {
        ArrayList<Map<String, String>> environments = new ArrayList<Map<String, String>>();

        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put( PlatformPropertiesUtils.OSGI_OS, "linux" );
        properties.put( PlatformPropertiesUtils.OSGI_WS, "gtk" );
        properties.put( PlatformPropertiesUtils.OSGI_ARCH, "x86_64" );

        // TODO does not belong here
        properties.put( "org.eclipse.update.install.features", "true" );

        environments.add( properties );

        return environments;
    }

    static void delete( File dir )
    {
        if ( dir == null || !dir.exists() )
        {
            return;
        }

        if ( dir.isDirectory() )
        {
            File[] members = dir.listFiles();
            if ( members != null )
            {
                for ( File member : members )
                {
                    delete( member );
                }
            }
        }

        Assert.assertTrue( "Delete " + dir.getAbsolutePath(), dir.delete() );
    }
}
