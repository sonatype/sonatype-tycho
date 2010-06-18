package org.sonatype.tycho.test.TYCHO0453expandReleaseVersion;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.UpdateSite;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class ExpandReleaseVersionTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void test()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0453expandReleaseVersion", false );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();

        File featureXml = new File( verifier.getBasedir(), "feature/target/feature.xml" );
        Feature feature = Feature.read( featureXml );
        Assert.assertEquals( "1.0.0.1234567890-bundle", feature.getPlugins().get( 0 ).getVersion() );
        // TODO included features

        File siteXml = new File( verifier.getBasedir(), "site/target/site/site.xml" );
        UpdateSite site = UpdateSite.read( siteXml );
        Assert.assertEquals( "1.0.0.1234567890-feature", site.getFeatures().get( 0 ).getVersion() );

        // TODO .product version expansion
    }

}
