package org.sonatype.tycho.p2.impl.test;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.tycho.utils.PlatformPropertiesUtils;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.p2.facade.internal.P2RepositoryCache;
import org.sonatype.tycho.p2.facade.internal.P2ResolutionResult;
import org.sonatype.tycho.p2.facade.internal.P2Resolver;
import org.sonatype.tycho.p2.resolver.P2ResolverImpl;

public class P2ResolverImplTest
{
    @Test
    public void basic()
        throws Exception
    {
        P2ResolverImpl impl = new P2ResolverImpl();
        impl.setRepositoryCache( new P2RepositoryCache() );
        impl.addP2Repository( new File( "resources/repositories/e342" ).getCanonicalFile().toURI() );
        impl.setLocalRepositoryLocation( new File( "target/localrepo" ).getCanonicalFile() );

        File bundle = new File( "resources/resolver/bundle01" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.resolver.test.bundle01";
        String artifactId = "org.sonatype.tycho.p2.impl.resolver.test.bundle01";
        String version = "1.0.0-SNAPSHOT";

        impl.setEnvironments( getEnvironments() );
        impl.addMavenArtifact( bundle, P2Resolver.TYPE_ECLIPSE_PLUGIN, groupId, artifactId, version );

        List<P2ResolutionResult> results = impl.resolveProject( bundle );

        Assert.assertEquals( 1, results.size() );
        P2ResolutionResult result = results.get( 0 );

        Assert.assertEquals( 2, result.getArtifacts().size() );
    }

    private List<Map<String, String>> getEnvironments()
    {
        ArrayList<Map<String, String>> environments = new ArrayList<Map<String, String>>();

        Map<String,String> properties = new LinkedHashMap<String, String>();
        properties.put( PlatformPropertiesUtils.OSGI_OS, "linux" );
        properties.put( PlatformPropertiesUtils.OSGI_WS, "gtk" );
        properties.put( PlatformPropertiesUtils.OSGI_ARCH, "x86_64" );

        // TODO does not belong here
        properties.put( "org.eclipse.update.install.features", "true" );

        environments.add( properties );

        return environments;
    }

}
