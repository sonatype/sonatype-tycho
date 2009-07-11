package org.sonatype.tycho.test.TYCHO59targetDefinition;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.codehaus.tycho.model.Target;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class TargetDefinitionTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void testMultiplatformReactorBuild()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO59targetDefinition" );

        File platformFile = new File( verifier.getBasedir(), "target-platform/platform.target" );
        Target platform = Target.read( platformFile );

        for ( Target.Location location : platform.getLocations() )
        {
            File file = new File( location.getRepositoryLocation() );
            location.setRepositoryLocation( file.getCanonicalFile().toURI().toASCIIString() );
        }

        Target.write( platform, platformFile );

        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }

}
