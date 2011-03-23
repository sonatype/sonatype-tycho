package org.sonatype.tycho.p2.impl.test;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.junit.Test;
import org.sonatype.tycho.p2.impl.publisher.P2GeneratorImpl;
import org.sonatype.tycho.p2.repository.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.resolver.P2Resolver;

public class P2MetadataGeneratorImplTest
{
    @Test
    public void gav()
        throws Exception
    {
        P2GeneratorImpl impl = new P2GeneratorImpl( false );

        File location = new File( "resources/generator/bundle" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.test";
        String artifactId = "bundle";
        String version = "1.0.0-SNAPSHOT";
        List<Map<String, String>> environments = new ArrayList<Map<String, String>>();
        Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        Set<IArtifactDescriptor> artifacts = new LinkedHashSet<IArtifactDescriptor>();
		impl.generateMetadata(new ArtifactMock(location, groupId, artifactId,
				version, P2Resolver.TYPE_ECLIPSE_PLUGIN), environments, units,
				artifacts);

        Assert.assertEquals( 1, units.size() );
        IInstallableUnit unit = units.iterator().next();

        Assert.assertEquals( "org.sonatype.tycho.p2.impl.test.bundle", unit.getId() );
        Assert.assertEquals( "1.0.0.qualifier", unit.getVersion().toString() );
        Assert.assertEquals( 2, unit.getRequirements().size() );

        Assert.assertEquals( 1, artifacts.size() );
        IArtifactDescriptor ad = artifacts.iterator().next();
        Assert.assertEquals( "org.sonatype.tycho.p2.impl.test.bundle", ad.getArtifactKey().getId() );
        Assert.assertEquals( "1.0.0.qualifier", ad.getArtifactKey().getVersion().toString() );

        Assert.assertEquals( groupId, ad.getProperties().get( RepositoryLayoutHelper.PROP_GROUP_ID ) );
        Assert.assertEquals( artifactId, ad.getProperties().get( RepositoryLayoutHelper.PROP_ARTIFACT_ID ) );
        Assert.assertEquals( version, ad.getProperties().get( RepositoryLayoutHelper.PROP_VERSION ) );
    }

}
