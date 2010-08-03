package org.sonatype.tycho.test.TYCHO188P2EnabledRcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipException;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

public class Tycho188P2EnabledRcpTest
    extends AbstractTychoIntegrationTest
{

    private static final String MODULE = "eclipse-repository";

    private static final String GROUP_ID = "org.sonatype.tycho.tychoits.TYCHO188";

    private static final String ARTIFACT_ID = "example-eclipse-repository";

    private static final String VERSION = "1.0.0-SNAPSHOT";

    private static final List<Product> TEST_PRODUCTS = Arrays.asList( new Product( "main.product.id", "", false ),
                                                                      new Product( "extra.product.id", "extra", true ),
                                                                      new Product( "repoonly.product.id", false ) );

    private static final List<Environment> TEST_ENVIRONMENTS =
        Arrays.asList( new Environment( "win32", "win32", "x86" ), new Environment( "linux", "gtk", "x86" ) );

    @Test
    public void testProductPublisher()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO188P2EnabledRcp", false );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        File repoDir = new File( verifier.getBasedir(), MODULE + "/target/repository" );
        File contentJar = new File( repoDir, "content.jar" );
        assertTrue( "content.jar not found \n" + contentJar.getAbsolutePath(), contentJar.isFile() );

        Document contentXml = openXmlFromZip( contentJar, "content.xml" );

        for ( Product product : TEST_PRODUCTS )
        {
            for ( Environment env : TEST_ENVIRONMENTS )
            {
                assertProductIUs( contentXml, product, env );

                if ( product.isMaterialized() )
                {
                    assertProductArtifacts( verifier, product, env );
                }
            }
        }
        assertRepositoryArtifacts( verifier );

        int materializedProducts = TEST_PRODUCTS.size() - 1;
        int environmentsPerProduct = TEST_ENVIRONMENTS.size();
        int repositoryArtifacts = 1;
        assertTotalZipArtifacts( verifier, materializedProducts * environmentsPerProduct + repositoryArtifacts );
    }

    static private void assertProductIUs( Document contentXml, Product product, Environment env )
    {
        assertTrue( product.unitId + " IU with lineUp property value true does not exist",
                    containsIUWithProperty( contentXml, product.unitId, "lineUp", "true" ) );

        final String p2InfAdded = "p2.inf.added-property";
        assertEquals( "Property " + p2InfAdded + " in " + product.unitId, product.p2InfProperty,
                      containsIUWithProperty( contentXml, product.unitId, p2InfAdded, "true" ) );

        /*
         * This only works if the context repositories are configured correctly. If the
         * simpleconfigurator bundle is not visible to the product publisher, this IU would not be
         * generated.
         */
        String simpleConfiguratorIU = "tooling" + env.toWsOsArch() + "org.eclipse.equinox.simpleconfigurator";
        assertTrue( simpleConfiguratorIU + " IU does not exist", containsIU( contentXml, simpleConfiguratorIU ) );
    }

    static private void assertProductArtifacts( Verifier verifier, Product product, Environment env )
        throws IOException, ZipException
    {
        File artifactDirectory =
            new File( verifier.getArtifactPath( GROUP_ID, ARTIFACT_ID, VERSION, "zip" ) ).getParentFile();
        File installedProductArchive =
            new File( artifactDirectory, ARTIFACT_ID + '-' + VERSION + product.getAttachIdSegment() + "-"
                + env.toOsWsArch() + ".zip" );
        assertTrue( "Product archive not found at: " + installedProductArchive, installedProductArchive.exists() );

        Properties configIni = openPropertiesFromZip( installedProductArchive, "configuration/config.ini" );
        String bundleConfiguration = configIni.getProperty( "osgi.bundles" );
        assertTrue( "Installation is not configured to use the simpleconfigurator",
                    bundleConfiguration.startsWith( "reference:file:org.eclipse.equinox.simpleconfigurator" ) );
    }

    static private void assertRepositoryArtifacts( Verifier verifier )
    {
        verifier.assertArtifactPresent( GROUP_ID, ARTIFACT_ID, VERSION, "zip" );
    }

    static private void assertTotalZipArtifacts( final Verifier verifier, final int expectedArtifacts )
    {
        final File artifactDirectory =
            new File( verifier.getArtifactPath( GROUP_ID, ARTIFACT_ID, VERSION, "zip" ) ).getParentFile();
        final String prefix = ARTIFACT_ID + '-' + VERSION;

        int zipArtifacts = 0;
        for ( final String fileName : artifactDirectory.list() )
        {
            if ( fileName.startsWith( prefix ) && fileName.endsWith( ".zip" ) )
            {
                zipArtifacts++;
            }
        }
        assertEquals( expectedArtifacts, zipArtifacts );
    }

    static Document openXmlFromZip( File zipFile, String xmlFile )
        throws IOException, ZipException
    {
        XMLParser parser = new XMLParser();
        ZipFile zip = new ZipFile( zipFile );
        try
        {
            ZipEntry contentXmlEntry = zip.getEntry( xmlFile );
            InputStream entryStream = zip.getInputStream( contentXmlEntry );
            try
            {
                return parser.parse( new XMLIOSource( entryStream ) );
            }
            finally
            {
                entryStream.close();
            }
        }
        finally
        {
            zip.close();
        }
    }

    static Properties openPropertiesFromZip( File zipFile, String propertyFile )
        throws IOException, ZipException
    {
        ZipFile zip = new ZipFile( zipFile );
        Properties configIni = new Properties();
        try
        {
            ZipEntry configIniEntry = zip.getEntry( propertyFile );
            InputStream entryStream = zip.getInputStream( configIniEntry );
            try
            {
                configIni.load( entryStream );
            }
            finally
            {
                entryStream.close();
            }
        }
        finally
        {
            zip.close();
        }
        return configIni;
    }

    static private boolean containsIU( Document contentXML, String iuId )
    {
        return containsIUWithProperty( contentXML, iuId, null, null );
    }

    static private boolean containsIUWithProperty( Document contentXML, String iuId, String propName, String propValue )
    {
        boolean foundIU = false;

        Element repository = contentXML.getRootElement();
        all_units: for ( Element unit : repository.getChild( "units" ).getChildren( "unit" ) )
        {
            if ( iuId.equals( unit.getAttributeValue( "id" ) ) )
            {
                if ( propName != null )
                {
                    for ( Element property : unit.getChild( "properties" ).getChildren( "property" ) )
                    {
                        if ( propName.equals( property.getAttributeValue( "name" ) )
                            && propValue.equals( ( property.getAttributeValue( "value" ) ) ) )
                        {
                            foundIU = true;
                            break all_units;
                        }
                    }
                }
                else
                {
                    foundIU = true;
                    break all_units;
                }
            }
        }
        return foundIU;
    }

    static class Environment
    {
        String os;

        String ws;

        String arch;

        Environment( String os, String ws, String arch )
        {
            this.os = os;
            this.ws = ws;
            this.arch = arch;
        }

        String toOsWsArch()
        {
            return os + '.' + ws + '.' + arch;
        }

        String toWsOsArch()
        {
            return ws + '.' + os + '.' + arch;
        }
    }

    static class Product
    {
        String unitId;

        String attachId;

        boolean p2InfProperty;

        Product( String unitId, String attachId, boolean p2InfProperty )
        {
            this.unitId = unitId;
            this.attachId = attachId;
            this.p2InfProperty = p2InfProperty;
        }

        Product( String unitId, boolean p2InfProperty )
        {
            this.unitId = unitId;
            this.attachId = null;
            this.p2InfProperty = p2InfProperty;
        }

        boolean isMaterialized()
        {
            return attachId != null;
        }

        String getAttachIdSegment()
        {
            if ( attachId == null )
            {
                throw new IllegalStateException();
            }
            return attachId.length() == 0 ? "" : "-" + attachId;
        }
    }
}
