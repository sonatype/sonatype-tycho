package org.sonatype.tycho.p2.publisher;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.utils.SourceBundleUtils;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.junit.Test;
import org.sonatype.tycho.p2.impl.test.ArtifactMock;
import org.sonatype.tycho.p2.publisher.P2GeneratorImpl;

public class P2GeneratorImplTest {

	@Test
	public void testCreateId() throws Exception {
		P2GeneratorImpl p2GeneratorImpl = new P2GeneratorImpl(true);
		long id = p2GeneratorImpl.createId("test", "1.0.0");
		assertEquals("1.0.0".hashCode(), (int) (id >> 32));
		assertEquals("test".hashCode(), (int) (id));
	}

	@Test
	public void testGenerateSourceBundleMetadata() throws Exception {
		P2GeneratorImpl p2GeneratorImpl = new P2GeneratorImpl(true);
		LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
		LinkedHashSet<IArtifactDescriptor> artifactDescriptors = new LinkedHashSet<IArtifactDescriptor>();
		File location = new File("resources/generator/bundle")
				.getCanonicalFile();
		ArtifactMock artifactMock = new ArtifactMock(location, "org.acme",
				"foo", "0.0.1", TychoProject.ECLIPSE_PLUGIN, ".suffix", true);
		p2GeneratorImpl.generateMetadata(artifactMock, null, units,
				artifactDescriptors);
		assertEquals(2, units.size());
		IInstallableUnit sourceBundleUnit = getUnit("foo.suffix", units);
		assertNotNull(sourceBundleUnit);
		assertEquals(Version.create("0.0.1"), sourceBundleUnit.getVersion());
		assertEquals(SourceBundleUtils.ARTIFACT_CLASSIFIER, sourceBundleUnit
				.getProperties().get("maven-classifier"));
		ITouchpointData touchPointData = sourceBundleUnit.getTouchpointData()
				.iterator().next();
		String manifestContent = touchPointData.getInstruction("manifest")
				.getBody();
		Manifest manifest = new Manifest(new ByteArrayInputStream(
				manifestContent.getBytes("UTF-8")));
		Attributes attributes = manifest.getMainAttributes();
		assertEquals("foo.suffix",
				attributes.getValue("Bundle-SymbolicName"));
		assertEquals("foo;version=0.0.1;roots:=\".\"",
				attributes.getValue("Eclipse-SourceBundle"));
	}

	@Test
	public void testNoSourceBundleMetadata() throws Exception {
		P2GeneratorImpl p2GeneratorImpl = new P2GeneratorImpl(true);
		LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
		LinkedHashSet<IArtifactDescriptor> artifactDescriptors = new LinkedHashSet<IArtifactDescriptor>();
		File location = new File("resources/generator/bundle")
				.getCanonicalFile();
		boolean generateSourceBundle = false;
		ArtifactMock artifactMock = new ArtifactMock(location, "org.acme",
				"foo", "0.0.1", TychoProject.ECLIPSE_PLUGIN, ".suffix", generateSourceBundle);
		p2GeneratorImpl.generateMetadata(artifactMock, null, units,
				artifactDescriptors);
		assertEquals(1, units.size());
		IInstallableUnit unit = units.iterator().next();
		assertEquals("org.sonatype.tycho.p2.impl.test.bundle", unit.getId());
	}

	private IInstallableUnit getUnit(String id, Set<IInstallableUnit> units) {
		for (IInstallableUnit unit : units) {
			if (id.equals(unit.getId())) {
				return unit;
			}
		}
		return null;
	}

}
