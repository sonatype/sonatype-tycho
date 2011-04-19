package org.eclipse.tycho.test.TYCHO570pomDependencyOnTychoArtifact;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class PomDependencyOnTychoArtifactTest extends AbstractTychoIntegrationTest {
    private static final String ITS_PROJECT_SUBPATH = "/TYCHO570pomDependencyOnTychoArtifact";

    /**
     * Checks whether the tests in this class should be executed or not. The tests require the local
     * Maven repository not to have specific artifacts already present, because the downloading and
     * adding into Tycho metadata is the actual test case.
     * 
     * If the external artifact is present, all tests will be skipped.
     * 
     * @throws Exception
     * 
     */
    @BeforeClass
    public static void checkTestPreconditions() throws Exception {
        // We use the Verifier to look into the local repo
        Verifier verifier = getVerifier(ITS_PROJECT_SUBPATH);

        String groupId = "org.sonatype.tycho";
        String artifactId = "org.sonatype.tycho.p2";
        String version = "0.11.0";

        String artifactMetadataPath = verifier.getArtifactMetadataPath(groupId, artifactId, version);
        File artifactFolder = new File(artifactMetadataPath).getParentFile();
        File p2ArtifactsXml = new File(artifactFolder, artifactId + "-" + version + "-" + "p2artifacts" + ".xml");

        // Skips all tests, if the p2-artifacts.xml was already downloaded into local Maven repository
        Assume.assumeTrue(!p2ArtifactsXml.exists());
    }

    @Test
    public void testPomDependenciesConsider() throws Exception {

        Verifier verifier = getVerifier(ITS_PROJECT_SUBPATH, false);
        verifier.setMavenDebug(true);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());

        Assert.assertTrue(new File(basedir, "repository/target/repository/plugins/org.sonatype.tycho.p2_0.11.0.jar")
                .canRead());
    }
}
