package org.sonatype.tycho.p2.test;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.junit.Test;
import org.sonatype.tycho.p2.P2ResolverImpl;
import org.sonatype.tycho.p2.facade.internal.P2ResolutionResult;
import org.sonatype.tycho.p2.facade.internal.P2Resolver;

public class P2ResolverTest
{

    @Test
    public void bundleProject()
        throws Exception
    {
        P2ResolverImpl resolver = new P2ResolverImpl();

        Properties newSelectionContext = new Properties();
        newSelectionContext.put( "osgi.arch", "x86_64" );
        newSelectionContext.put( "org.eclipse.equinox.p2.roaming", "true" );
        newSelectionContext.put( "osgi.ws", "gtk" );
        newSelectionContext.put( "org.eclipse.equinox.p2.cache", "/tmp/p2tmp/" );
        newSelectionContext.put( "org.eclipse.equinox.p2.installFolder", "/tmp/p2tmp/" );
        newSelectionContext.put( "org.eclipse.equinox.p2.environments", "osgi.ws=gtk,osgi.os=linux,osgi.arch=x86_64" );
        newSelectionContext.put( "org.eclipse.equinox.p2.flavor", "tooling" );
        newSelectionContext.put( "osgi.os", "linux" );
        newSelectionContext.put( "org.eclipse.update.install.features", "true" );

        resolver.setProperties( newSelectionContext );

        resolver.addRepository( new URL( "http://download.eclipse.org/releases/ganymede" ).toURI() );
        resolver.setLocalRepositoryLocation( new File( "target/localrepo" ) );

        File location = new File( "/var/tmp/m2e/org.maven.ide.components.maven-model-edit" );
        resolver.addMavenProject( location, P2Resolver.TYPE_OSGI_BUNDLE, "groupId", "maven-model-edit", "1.0.0" );

        resolver.addMavenProject(
            new File( "projects/bundle02" ),
            P2Resolver.TYPE_OSGI_BUNDLE,
            "groupId",
            "bundle02",
            "1.0.0" );
        
        resolver.addDependency( P2Resolver.TYPE_INSTALLABLE_UNIT, "org.eclipse.ui.ide.application", null );

        P2ResolutionResult platform = resolver.resolve( location );

        for ( File file : platform.getBundles() )
        {
            System.out.println( file );
        }
    }
}
