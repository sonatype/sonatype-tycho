/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.TYCHO570pomDependencyOnTychoArtifact;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.LocalMavenRepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class PomDependencyOnTychoArtifactTest extends AbstractTychoIntegrationTest {
    private static final GAV ITS_PROJECT_POM_DEPENDENCY = new GAV("org.sonatype.tycho", "org.sonatype.tycho.p2",
            "0.11.0");

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
        LocalMavenRepositoryTool localRepo = new LocalMavenRepositoryTool();
        File p2ArtifactsXml = localRepo.getArtifactFile(ITS_PROJECT_POM_DEPENDENCY,
                RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS, RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS);

        // Skips all tests, if the p2-artifacts.xml was already downloaded into local Maven repository
        Assume.assumeTrue(!p2ArtifactsXml.exists());
    }

    @Test
    public void testPomDependenciesConsider() throws Exception {

        Verifier verifier = getVerifier("TYCHO570pomDependencyOnTychoArtifact", false);
        verifier.setMavenDebug(true);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        String testProjectRoot = verifier.getBasedir();
        File p2RepoModule = new File(testProjectRoot, "repository");
        P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(p2RepoModule);

        File expectedBundle = p2Repo.getBundleArtifact(ITS_PROJECT_POM_DEPENDENCY.getArtifactId(),
                ITS_PROJECT_POM_DEPENDENCY.getVersion());
        Assert.assertTrue(expectedBundle.canRead());
    }
}
