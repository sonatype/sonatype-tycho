package org.sonatype.tycho.test.TYCHO331synchronizeVersions;

import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

import de.schlichtherle.io.File;

public class SynchronizeVersionsTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void install()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO331synchronizeVersions" );
        final String timestamp = "20022002-2002";
        verifier.getCliOptions().add("-DforceContextQualifier=" + timestamp);
        verifier.executeGoals( Arrays.asList( "clean", "install" ) );

        Assert.assertTrue( new File(verifier.localRepo, "TYCHO331synchronizeVersions/bundle/0.0.1.20022002-2002/bundle-0.0.1.20022002-2002.jar").canRead() );
        Assert.assertTrue( new File(verifier.localRepo, "TYCHO331synchronizeVersions/feature/0.0.1.20022002-2002/feature-0.0.1.20022002-2002.jar").canRead() );
    }

}
