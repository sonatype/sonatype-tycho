package org.sonatype.tycho.plugins.p2.director;

import static org.junit.Assert.assertEquals;

import org.codehaus.tycho.TargetEnvironment;
import org.junit.Test;
import org.sonatype.tycho.plugins.p2.director.Product;
import org.sonatype.tycho.plugins.p2.director.ProductArchiverMojo;

public class ProductArchiverMojoTest
{
    @Test
    public void testGetArtifactClassifier()
    {
        TargetEnvironment env = new TargetEnvironment( "os", "ws", "arch", null );
        Product product = new Product( "product.id" );
        String classifier = ProductArchiverMojo.getArtifactClassifier( product, env );
        assertEquals( "os.ws.arch", classifier );
    }
    @Test
    public void testGetArtifactClassifierWithAttachId()
    {
        TargetEnvironment env = new TargetEnvironment( "os", "ws", "arch", null );
        Product product = new Product( "product.id", "attachId" );
        String classifier = ProductArchiverMojo.getArtifactClassifier( product, env );
        assertEquals( "attachId-os.ws.arch", classifier );
    }
}
