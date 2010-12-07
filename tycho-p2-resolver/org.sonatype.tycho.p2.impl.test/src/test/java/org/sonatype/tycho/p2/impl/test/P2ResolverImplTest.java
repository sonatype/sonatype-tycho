package org.sonatype.tycho.p2.impl.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.p2.DependencyMetadataGenerator;
import org.sonatype.tycho.p2.impl.publisher.DefaultDependencyMetadataGenerator;
import org.sonatype.tycho.p2.impl.publisher.SourcesBundleDependencyMetadataGenerator;
import org.sonatype.tycho.p2.impl.resolver.DuplicateReactorIUsException;
import org.sonatype.tycho.p2.impl.resolver.P2ResolverImpl;
import org.sonatype.tycho.p2.resolver.P2ResolutionResult;
import org.sonatype.tycho.p2.resolver.P2Resolver;
import org.sonatype.tycho.test.util.HttpServer;

public class P2ResolverImplTest
{
    private HttpServer server;

    private DependencyMetadataGenerator generator = new DefaultDependencyMetadataGenerator();

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

    private void addMavenProject( P2ResolverImpl impl, File basedir, String packaging, String id )
        throws IOException
    {
        String version = "1.0.0-SNAPSHOT";

        impl.addMavenArtifact( new ArtifactMock( basedir.getCanonicalFile(), id, id, version, packaging ) );
    }

    protected List<P2ResolutionResult> resolveFromHttp( P2ResolverImpl impl, String url )
        throws IOException, URISyntaxException
    {
        impl.setRepositoryCache( new P2RepositoryCacheImpl() );
        impl.setLocalRepositoryLocation( getLocalRepositoryLocation() );
        impl.addP2Repository( new URI( url ) );

        impl.setEnvironments( getEnvironments() );

        String groupId = "org.sonatype.tycho.p2.impl.resolver.test.bundle01";
        File bundle = new File( "resources/resolver/bundle01" ).getCanonicalFile();

        addMavenProject( impl, bundle, P2Resolver.TYPE_ECLIPSE_PLUGIN, groupId );

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
        properties.put( "osgi.os", "linux" );
        properties.put( "osgi.ws", "gtk" );
        properties.put( "osgi.arch", "x86_64" );

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

    @Test
    public void basic()
        throws Exception
    {
        P2ResolverImpl impl = new P2ResolverImpl();
        impl.setRepositoryCache( new P2RepositoryCacheImpl() );
        impl.setLocalRepositoryLocation( getLocalRepositoryLocation() );
        impl.addP2Repository( new File( "resources/repositories/e342" ).getCanonicalFile().toURI() );
        impl.setLogger( new NullP2Logger() );

        File bundle = new File( "resources/resolver/bundle01" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.resolver.test.bundle01";
        String artifactId = "org.sonatype.tycho.p2.impl.resolver.test.bundle01";
        String version = "1.0.0-SNAPSHOT";

        impl.setEnvironments( getEnvironments() );

        ArtifactMock a = new ArtifactMock( bundle, groupId, artifactId, version, P2Resolver.TYPE_ECLIPSE_PLUGIN );
        a.setDependencyMetadata( generator.generateMetadata( a, getEnvironments() ) );

        impl.addReactorArtifact( a );

        List<P2ResolutionResult> results = impl.resolveProject( bundle );

        impl.stop();

        Assert.assertEquals( 1, results.size() );
        P2ResolutionResult result = results.get( 0 );

        Assert.assertEquals( 2, result.getArtifacts().size() );
        Assert.assertEquals( 1, result.getNonReactorUnits().size() );
    }

    @Test
    public void offline()
        throws Exception
    {
        server = HttpServer.startServer();
        String url = server.addServer( "e342", new File( "resources/repositories/e342" ) );

        // prime local repository
        P2ResolverImpl impl = new P2ResolverImpl();
        impl.setLogger( new NullP2Logger() );
        resolveFromHttp( impl, url );

        // now go offline and resolve again
        impl = new P2ResolverImpl();
        impl.setLogger( new NullP2Logger() );
        impl.setOffline( true );
        List<P2ResolutionResult> results = resolveFromHttp( impl, url );

        Assert.assertEquals( 1, results.size() );
        P2ResolutionResult result = results.get( 0 );

        Assert.assertEquals( 2, result.getArtifacts().size() );
        Assert.assertEquals( 1, result.getNonReactorUnits().size() );
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

    @Test
    public void siteConflictingDependenciesResolver()
        throws IOException
    {
        P2ResolverImpl impl = new P2ResolverImpl();
        impl.setRepositoryCache( new P2RepositoryCacheImpl() );
        impl.setLocalRepositoryLocation( getLocalRepositoryLocation() );
        impl.addP2Repository( new File( "resources/repositories/e342" ).getCanonicalFile().toURI() );
        impl.setLogger( new NullP2Logger() );

        impl.setEnvironments( getEnvironments() );

        addMavenProject( impl, new File( "resources/siteresolver/bundle342" ), P2Resolver.TYPE_ECLIPSE_PLUGIN,
                         "bundle342" );
        addMavenProject( impl, new File( "resources/siteresolver/bundle352" ), P2Resolver.TYPE_ECLIPSE_PLUGIN,
                         "bundle352" );
        addMavenProject( impl, new File( "resources/siteresolver/feature342" ), P2Resolver.TYPE_ECLIPSE_FEATURE,
                         "feature342" );
        addMavenProject( impl, new File( "resources/siteresolver/feature352" ), P2Resolver.TYPE_ECLIPSE_FEATURE,
                         "feature352" );

        File basedir = new File( "resources/siteresolver/site" ).getCanonicalFile();
        addMavenProject( impl, basedir, P2Resolver.TYPE_ECLIPSE_UPDATE_SITE, "site" );

        P2ResolutionResult result = impl.collectProjectDependencies( basedir );

        impl.stop();

        Assert.assertEquals( 4, result.getArtifacts().size() );
        // conflicting dependency mode only collects included artifacts - the referenced non-reactor unit org.eclipse.osgi is not included
        Assert.assertEquals( 0, result.getNonReactorUnits().size() );
    }

    @Test
    public void duplicateInstallableUnit()
        throws Exception
    {
        P2ResolverImpl impl = new P2ResolverImpl();
        impl.setLogger( new NullP2Logger() );
        impl.setRepositoryCache( new P2RepositoryCacheImpl() );
        impl.setLocalRepositoryLocation( getLocalRepositoryLocation() );
        impl.setEnvironments( getEnvironments() );

        File projectLocation = new File( "resources/duplicate-iu/featureA" ).getCanonicalFile();

        ArtifactMock a1 =
            new ArtifactMock( projectLocation, "groupId", "featureA", "1.0.0-SNAPSHOT", P2Resolver.TYPE_ECLIPSE_FEATURE );
        a1.setDependencyMetadata( generator.generateMetadata( a1, getEnvironments() ) );

        ArtifactMock a2 =
            new ArtifactMock( new File( "resources/duplicate-iu/featureA2" ).getCanonicalFile(), "groupId",
                              "featureA2", "1.0.0-SNAPSHOT", P2Resolver.TYPE_ECLIPSE_FEATURE );
        a2.setDependencyMetadata( generator.generateMetadata( a2, getEnvironments() ) );

        impl.addReactorArtifact( a1 );
        impl.addReactorArtifact( a2 );

        try
        {
            impl.resolveProject( projectLocation );
            fail();
        }
        catch ( DuplicateReactorIUsException e )
        {
            // TODO proper assertion
        }
    }

    @Test
    public void featureInstallableUnits()
        throws Exception
    {
        P2ResolverImpl impl = new P2ResolverImpl();
        impl.setRepositoryCache( new P2RepositoryCacheImpl() );
        impl.setLocalRepositoryLocation( getLocalRepositoryLocation() );
        impl.setLogger( new NullP2Logger() );

        File bundle = new File( "resources/resolver/feature01" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.resolver.test.feature01";
        String artifactId = "org.sonatype.tycho.p2.impl.resolver.test.feature01";
        String version = "1.0.0-SNAPSHOT";

        impl.setEnvironments( getEnvironments() );
        impl.addMavenArtifact( new ArtifactMock( bundle, groupId, artifactId, version, P2Resolver.TYPE_ECLIPSE_FEATURE ) );

        List<P2ResolutionResult> results = impl.resolveProject( bundle );

        impl.stop();

        Assert.assertEquals( 1, results.size() );
        P2ResolutionResult result = results.get( 0 );

        Assert.assertEquals( 1, result.getArtifacts().size() );
        Assert.assertEquals( 1, result.getArtifacts().iterator().next().getInstallableUnits().size() );
        Assert.assertEquals( 0, result.getNonReactorUnits().size() );
    }

    @Test
    public void sourceBundle()
        throws Exception
    {
        P2ResolverImpl impl = new P2ResolverImpl();
        impl.setRepositoryCache( new P2RepositoryCacheImpl() );
        impl.setLocalRepositoryLocation( getLocalRepositoryLocation() );
        impl.setLogger( new NullP2Logger() );

        File feature = new File( "resources/sourcebundles/feature01" ).getCanonicalFile();
        String featureId = "org.sonatype.tycho.p2.impl.resolver.test.feature01";
        String featureVersion = "1.0.0-SNAPSHOT";

        ArtifactMock f =
            new ArtifactMock( feature, featureId, featureId, featureVersion, P2Resolver.TYPE_ECLIPSE_FEATURE );
        f.setDependencyMetadata( generator.generateMetadata( f, getEnvironments() ) );
        impl.addReactorArtifact( f );

        File bundle = new File( "resources/sourcebundles/bundle01" ).getCanonicalFile();
        String bundleId = "org.sonatype.tycho.p2.impl.resolver.test.bundle01";
        String bundleVersion = "1.0.0-SNAPSHOT";
        ArtifactMock b = new ArtifactMock( bundle, bundleId, bundleId, bundleVersion, P2Resolver.TYPE_ECLIPSE_PLUGIN );
        b.setDependencyMetadata( generator.generateMetadata( b, getEnvironments() ) );
        impl.addReactorArtifact( b );

        ArtifactMock sb =
            new ArtifactMock( bundle, bundleId, bundleId, bundleVersion, P2Resolver.TYPE_ECLIPSE_PLUGIN, "sources" );
        sb.setDependencyMetadata( new SourcesBundleDependencyMetadataGenerator().generateMetadata( sb, getEnvironments() ) );
        impl.addReactorArtifact( sb );

        impl.setEnvironments( getEnvironments() );

        List<P2ResolutionResult> results = impl.resolveProject( feature );
        impl.stop();

        Assert.assertEquals( 1, results.size() );
        P2ResolutionResult result = results.get( 0 );

        Assert.assertEquals( 3, result.getArtifacts().size() );
        List<P2ResolutionResult.Entry> entries = new ArrayList<P2ResolutionResult.Entry>( result.getArtifacts() );

        Assert.assertEquals( "org.sonatype.tycho.p2.impl.resolver.test.feature01", entries.get( 0 ).getId() );
        Assert.assertEquals( "org.sonatype.tycho.p2.impl.resolver.test.bundle01", entries.get( 1 ).getId() );
        Assert.assertEquals( "org.sonatype.tycho.p2.impl.resolver.test.bundle01.source", entries.get( 2 ).getId() );
        Assert.assertEquals( bundle, entries.get( 1 ).getLocation() );
        Assert.assertEquals( bundle, entries.get( 2 ).getLocation() );
        Assert.assertEquals( "sources", entries.get( 2 ).getClassifier() );
    }

    @Test
    public void eclipseRepository()
        throws Exception
    {
        P2ResolverImpl impl = new P2ResolverImpl();
        impl.setRepositoryCache( new P2RepositoryCacheImpl() );
        impl.setLocalRepositoryLocation( getLocalRepositoryLocation() );
        impl.addP2Repository( new File( "resources/repositories/e342" ).getCanonicalFile().toURI() );
        // launchers currently cannot be disabled (see TYCHO-511/TYCHO-512)
        impl.addP2Repository( new File( "resources/repositories/launchers" ).getCanonicalFile().toURI() );
        impl.setLogger( new NullP2Logger() );

        File projectDir = new File( "resources/resolver/repository" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.resolver.test.repository";
        String artifactId = "org.sonatype.tycho.p2.impl.resolver.test.repository";
        String version = "1.0.0-SNAPSHOT";

        impl.setEnvironments( getEnvironments() );

        addMavenProject( impl, new File( "resources/resolver/bundle01" ), P2Resolver.TYPE_ECLIPSE_PLUGIN, "bundle01" );

        ArtifactMock module =
            new ArtifactMock( projectDir, groupId, artifactId, version, P2Resolver.TYPE_ECLIPSE_REPOSITORY );
        module.setDependencyMetadata( generator.generateMetadata( module, getEnvironments() ) );

        impl.addReactorArtifact( module );

        List<P2ResolutionResult> results = impl.resolveProject( projectDir );

        impl.stop();

        Assert.assertEquals( 1, results.size() );
        P2ResolutionResult result = results.get( 0 );

        Assert.assertEquals( 3, result.getArtifacts().size() ); // the product, bundle01, and the one dependency of bundle01
        Assert.assertEquals( 4, result.getNonReactorUnits().size() );

        assertContainsUnit( "org.eclipse.osgi", result.getNonReactorUnits() );
        assertContainsUnit( "org.eclipse.equinox.launcher", result.getNonReactorUnits() );
        assertContainsUnit( "org.eclipse.equinox.launcher.gtk.linux.x86_64", result.getNonReactorUnits() );
        assertContainsUnit( "org.eclipse.equinox.executable.feature.group", result.getNonReactorUnits() );
    }

    private void assertContainsUnit( String unitID, Set<?> units )
    {
        for ( Object unitObject : units )
        {
            IInstallableUnit unit = (IInstallableUnit) unitObject;
            if ( unitID.equals( unit.getId() ) )
                return;
        }
        fail( "Unit " + unitID + " not found" );
    }
}
