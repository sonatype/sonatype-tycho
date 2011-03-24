package org.sonatype.tycho.p2.impl.test;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.junit.Test;
import org.sonatype.tycho.p2.impl.publisher.P2GeneratorImpl;
import org.sonatype.tycho.p2.resolver.P2Resolver;

@SuppressWarnings( "restriction" )
public class P2DependencyGeneratorImplTest
{
    @Test
    public void bundle()
        throws Exception
    {
        P2GeneratorImpl impl = new P2GeneratorImpl( true );

        File location = new File( "resources/generator/bundle" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.test";
        String artifactId = "bundle";
        String version = "1.0.0-SNAPSHOT";
        List<Map<String, String>> environments = new ArrayList<Map<String, String>>();
        Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        Set<IArtifactDescriptor> artifacts = new LinkedHashSet<IArtifactDescriptor>();
        impl.generateMetadata( new ArtifactMock(location, groupId, artifactId, version, P2Resolver.TYPE_ECLIPSE_PLUGIN), environments,
                               units, artifacts );

        Assert.assertEquals( 1, units.size() );
        IInstallableUnit unit = units.iterator().next();

        Assert.assertEquals( "org.sonatype.tycho.p2.impl.test.bundle", unit.getId() );
        Assert.assertEquals( "1.0.0.qualifier", unit.getVersion().toString() );
        Assert.assertEquals( 2, unit.getRequirements().size() );

        // not really necessary, but we get this because we reuse standard p2 implementation
        Assert.assertEquals( 1, artifacts.size() );
    }

    @Test
    public void feature()
        throws Exception
    {
        P2GeneratorImpl impl = new P2GeneratorImpl( true );

        File location = new File( "resources/generator/feature" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.test";
        String artifactId = "feature";
        String version = "1.0.0-SNAPSHOT";
        List<Map<String, String>> environments = new ArrayList<Map<String, String>>();
        Set<IInstallableUnit> units = new TreeSet<IInstallableUnit>();
        Set<IArtifactDescriptor> artifacts = new LinkedHashSet<IArtifactDescriptor>();
        impl.generateMetadata( new ArtifactMock(location, groupId, artifactId, version, P2Resolver.TYPE_ECLIPSE_FEATURE), environments,
                               units, artifacts );

        // no feature.jar IU because dependencyOnly=true
        Assert.assertEquals( 1, units.size() );
        IInstallableUnit unit = units.iterator().next();

        Assert.assertEquals( "org.sonatype.tycho.p2.impl.test.feature.feature.group", unit.getId() );
        Assert.assertEquals( "1.0.0.qualifier", unit.getVersion().toString() );
        Assert.assertEquals( 4, unit.getRequirements().size() );

        Assert.assertEquals( 0, artifacts.size() );
    }

    @Test
    public void site()
        throws Exception
    {
        P2GeneratorImpl impl = new P2GeneratorImpl( true );

        File location = new File( "resources/generator/site" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.test";
        String artifactId = "site";
        String version = "1.0.0-SNAPSHOT";
        List<Map<String, String>> environments = new ArrayList<Map<String, String>>();
        Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        Set<IArtifactDescriptor> artifacts = new LinkedHashSet<IArtifactDescriptor>();
		impl.generateMetadata(new ArtifactMock(location, groupId, artifactId,
				version, P2Resolver.TYPE_ECLIPSE_UPDATE_SITE), environments,
				units, artifacts);

        Assert.assertEquals( 1, units.size() );
        IInstallableUnit unit = units.iterator().next();

        Assert.assertEquals( "site", unit.getId() );
        Assert.assertEquals( "raw:1.0.0.'SNAPSHOT'/format(n[.n=0;[.n=0;[-S]]]):1.0.0-SNAPSHOT",
                             unit.getVersion().toString() );
        Assert.assertEquals( 1, unit.getRequirements().size() );

        Assert.assertEquals( 0, artifacts.size() );
    }

    @Test
    public void rcpBundle()
        throws Exception
    {
        P2GeneratorImpl impl = new P2GeneratorImpl( true );

        File location = new File( "resources/generator/rcp-bundle" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.test";
        String artifactId = "rcp-bundle";
        String version = "1.0.0-SNAPSHOT";
        List<Map<String, String>> environments = new ArrayList<Map<String, String>>();
        Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        Set<IArtifactDescriptor> artifacts = new LinkedHashSet<IArtifactDescriptor>();
		impl.generateMetadata(new ArtifactMock(location, groupId, artifactId,
				version, P2Resolver.TYPE_ECLIPSE_APPLICATION), environments,
				units, artifacts);

        Assert.assertEquals( 1, units.size() );
        IInstallableUnit unit = units.iterator().next();

        Assert.assertEquals( "org.sonatype.tycho.p2.impl.test.rcp-bundle", unit.getId() );
        Assert.assertEquals( "1.0.0.qualifier", unit.getVersion().toString() );

        List<IRequirement> requirement = new ArrayList<IRequirement>( unit.getRequirements() );

        Assert.assertEquals( 3, requirement.size() );
        Assert.assertEquals( "included.bundle", ( (IRequiredCapability) requirement.get( 0 ) ).getName() );

        // implicit dependencies because includeLaunchers="true"
        Assert.assertEquals( "org.eclipse.equinox.executable.feature.group",
                             ( (IRequiredCapability) requirement.get( 1 ) ).getName() );
        Assert.assertEquals( "org.eclipse.equinox.launcher", ( (IRequiredCapability) requirement.get( 2 ) ).getName() );

        Assert.assertEquals( 0, artifacts.size() );
    }

    @Test
    public void rcpFeature()
        throws Exception
    {
        P2GeneratorImpl impl = new P2GeneratorImpl( true );

        File location = new File( "resources/generator/rcp-feature" ).getCanonicalFile();
        String groupId = "org.sonatype.tycho.p2.impl.test";
        String artifactId = "rcp-feature";
        String version = "1.0.0-SNAPSHOT";
        List<Map<String, String>> environments = new ArrayList<Map<String, String>>();
        Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        Set<IArtifactDescriptor> artifacts = new LinkedHashSet<IArtifactDescriptor>();
        impl.generateMetadata( new ArtifactMock(location, groupId, artifactId, version, P2Resolver.TYPE_ECLIPSE_APPLICATION), 
                               environments, units, artifacts );

        Assert.assertEquals( 1, units.size() );
        IInstallableUnit unit = units.iterator().next();

        Assert.assertEquals( "org.sonatype.tycho.p2.impl.test.rcp-feature", unit.getId() );
        Assert.assertEquals( "1.0.0.qualifier", unit.getVersion().toString() );

        Assert.assertEquals( 3, unit.getRequirements().size() );

        Assert.assertEquals( 0, artifacts.size() );

    }

    // TODO version ranges in feature, site and rcp apps
}
