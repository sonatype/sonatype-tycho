package org.sonatype.tycho.test.TYCHO0439repositoryCategories;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

public class RepositoryCategoriesTest
    extends AbstractTychoIntegrationTest
{

    @Test
    public void testDeployableFeature()
        throws Exception
    {
        Verifier v01 = getVerifier( "TYCHO0439repositoryCategories" );
        v01.executeGoal( "install" );
        v01.verifyErrorFreeLog();

        File site = new File( v01.getBasedir(), "target/site" );
        Assert.assertTrue( site.isDirectory() );

        File content = new File( site, "content.jar" );
        Assert.assertTrue( content.isFile() );

        boolean found = false;

        XMLParser parser = new XMLParser();
        Document document = null;
        ZipFile contentJar = new ZipFile(content);
        try {
        	ZipEntry contentXmlEntry = contentJar.getEntry("content.xml");
        	document = parser.parse( new XMLIOSource( contentJar.getInputStream(contentXmlEntry) ) );
        } finally {
        	contentJar.close();
        }
        Element repository = document.getRootElement();
        all_units: for ( Element unit : repository.getChild( "units" ).getChildren( "unit" ) )
        {
            for ( Element property : unit.getChild( "properties" ).getChildren( "property" ) )
            {
                if ( "org.eclipse.equinox.p2.type.category".equals( property.getAttributeValue( "name" ) )
                    && Boolean.parseBoolean( property.getAttributeValue( "value" ) ) )
                {
                    found = true;
                    break all_units;
                }
            }
        }

        Assert.assertTrue( "Custom category", found );
    }

}
