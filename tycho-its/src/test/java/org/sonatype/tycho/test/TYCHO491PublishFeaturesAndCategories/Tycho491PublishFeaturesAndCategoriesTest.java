package org.sonatype.tycho.test.TYCHO491PublishFeaturesAndCategories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipException;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;
import org.sonatype.tycho.test.TYCHO188P2EnabledRcp.Util;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;

public class Tycho491PublishFeaturesAndCategoriesTest
    extends AbstractTychoIntegrationTest
{

    private static final String MODULE = "eclipse-repository";

    private static final String GROUP_ID = "org.sonatype.tycho.tychoits.TYCHO491PublishFeaturesAndCategories";

    private static final String ARTIFACT_ID = "example-eclipse-repository";

    private static final String VERSION = "1.0.0-SNAPSHOT";

    private static final String QUALIFIER = "12345-forcedQualifier";

    @Test
    public void testProductPublisher()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO491PublishFeaturesAndCategories", false );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        assertRepositoryArtifacts( verifier );

        File repoDir = new File( verifier.getBasedir(), MODULE + "/target/repository" );
        File contentJar = new File( repoDir, "content.jar" );
        assertTrue( "content.jar not found \n" + contentJar.getAbsolutePath(), contentJar.isFile() );

        Document contentXml = Util.openXmlFromZip( contentJar, "content.xml" );

        assertCategoryIU( contentXml, QUALIFIER + ".example.category", "example.feature.feature.group" );

        assertFeatureIU( verifier, contentXml, "example.feature", "example.included.feature.feature.group",
                         "example.bundle" );
        assertBundleIU( verifier, contentXml, "example.bundle" );

        assertFeatureIU( verifier, contentXml, "example.included.feature", "example.included.bundle" );
        assertBundleIU( verifier, contentXml, "example.included.bundle" );

    }

    static private void assertRepositoryArtifacts( Verifier verifier )
    {
        verifier.assertArtifactPresent( GROUP_ID, ARTIFACT_ID, VERSION, "zip" );

    }

    static private void assertCategoryIU( Document contentXml, String categoryIuId, String featureIuId )
    {
        Set<Element> categoryIus = Util.findIU( contentXml, categoryIuId );
        assertEquals( "Unique category iu not found", 1, categoryIus.size() );
        Element categoryIu = categoryIus.iterator().next();

        assertTrue( "iu not typed as category",
                    Util.iuHasProperty( categoryIu, "org.eclipse.equinox.p2.type.category", "true" ) );
        assertTrue( "category name missing",
                    Util.iuHasProperty( categoryIu, "org.eclipse.equinox.p2.name", "Example Category" ) );
        assertTrue( Util.iuHasAllRequirements( categoryIu, featureIuId ) );
    }

    static private void assertFeatureIU( Verifier verifier, Document contentXml, String featureId,
                                         String... requiredIus )
        throws IOException
    {
        String featureIuId = featureId + ".feature.group";
        Set<Element> featureIus = Util.findIU( contentXml, featureIuId );
        assertEquals( "Unique feature iu not found", 1, featureIus.size() );
        Element featureIu = featureIus.iterator().next();

        assertTrue( Util.containsIUWithProperty( contentXml, featureIuId, "org.eclipse.equinox.p2.type.group", "true" ) );
        assertTrue( Util.iuHasAllRequirements( featureIu, requiredIus ) );

        String featureArtifactPath = "features/" + featureId + "_1.0.0";
        assertArtifact( verifier, featureArtifactPath );
    }

    static private void assertBundleIU( Verifier verifier, Document contentXml, String bundleId )
        throws IOException
    {
        assertTrue( "bundle not found", Util.containsIU( contentXml, bundleId ) );

        String bundleArtifactPath = "plugins/" + bundleId + "_1.0.0";
        assertArtifact( verifier, bundleArtifactPath );
    }

    static private void assertArtifact( Verifier verifier, String artifactPath )
        throws IOException, ZipException
    {
        File repositoryArtifact = new File( verifier.getArtifactPath( GROUP_ID, ARTIFACT_ID, VERSION, "zip" ) );
        assertContainsEntry( repositoryArtifact, artifactPath );
    }

    private static void assertContainsEntry( File file, String prefix )
        throws IOException
    {
        ZipFile zipFile = new ZipFile( file );

        for ( final Enumeration<?> entries = zipFile.getEntries(); entries.hasMoreElements(); )
        {
            final ZipEntry entry = (ZipEntry) entries.nextElement();
            if ( entry.getName().startsWith( prefix ) )
            {
                if ( entry.getName().endsWith( ".qualifier.jar" ) )
                {
                    Assert.fail( "replacement of build qualifier missing in " + file + ", zip entry: "
                        + entry.getName() );
                }
                return;
            }
        }
        Assert.fail( "missing entry " + prefix + "* in repository archive " + file );
    }
}
