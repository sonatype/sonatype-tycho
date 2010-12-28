package org.sonatype.tycho.test.TYCHO465RootFiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipException;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;
import org.sonatype.tycho.test.TYCHO188P2EnabledRcp.Util;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;

public class Tycho465RootFilesTest
    extends AbstractTychoIntegrationTest
{

    static final String QUALIFIER = "forced";
    static final String MODULE = "eclipse-repository";

    @SuppressWarnings( "unchecked" )
    @Test
    public void testProductBuild()
        throws Exception
    {
        Verifier verifier = new Tycho465RootFilesTest().getVerifier( "/TYCHO465RootFiles", false );

        verifier.getCliOptions().add( "-DforceContextQualifier=" + Tycho465RootFilesTest.QUALIFIER );
        verifier.getCliOptions().add( "-Dp2.repo="
                                          + new File( "repositories/e342" ).getCanonicalFile().toURI().normalize().toString() );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        File targetDir = new File( verifier.getBasedir(), Tycho465RootFilesTest.MODULE + "/target" );
        File repositoryTargetDirectory = new File( targetDir, "repository" );

        Document contentXml = openMetadataRepositoryDocument( repositoryTargetDirectory );

        assertBuildProductAndRepository( targetDir, repositoryTargetDirectory, contentXml );

        // clean the local build results
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();

        // re-build the repository project only (incl. products) to ensure that the created root file zips were attached
        // to the project and are available from the local repository
        Verifier eclipseRepoProjectVerifier = getVerifier( "/TYCHO465RootFiles/eclipse-repository", false );

        eclipseRepoProjectVerifier.getCliOptions().add( "-DforceContextQualifier=" + Tycho465RootFilesTest.QUALIFIER );
        eclipseRepoProjectVerifier.getCliOptions().add( "-Dp2.repo="
                                                            + new File( "repositories/e342" ).getCanonicalFile().toURI().normalize().toString() );

        eclipseRepoProjectVerifier.executeGoal( "verify" );
        eclipseRepoProjectVerifier.verifyErrorFreeLog();

        Document updatedContentXml = openMetadataRepositoryDocument( repositoryTargetDirectory );

        assertBuildProductAndRepository( targetDir, repositoryTargetDirectory, updatedContentXml );
    }

    private void assertBuildProductAndRepository( File targetDir, File repositoryTargetDirectory, Document contentXml )
        throws IOException
    {
        Tycho465RootFilesTest.assertCategoryIU( contentXml, Tycho465RootFilesTest.QUALIFIER + ".category.id",
                                        "tycho465.feature.feature.group" );
        Tycho465RootFilesTest.assertFeatureIU( contentXml, repositoryTargetDirectory, "tycho465.feature" );

        Tycho465RootFilesTest.assertRootIuMetaData( contentXml );
        Tycho465RootFilesTest.assertInstalledWinConfigRootFile( targetDir );
        Tycho465RootFilesTest.assertInstalledLinuxConfigRootFile( targetDir );
    }

    static String getFileNotExistsInDirMsg( String fileRootRelPath, File dir )
    {
        return ( "Expected root file '" + fileRootRelPath + "' does not exist in directory " + dir.toURI() );
    }

    static void assertContainsEntry( File dir, String prefix )
        throws IOException
    {
        File[] listFiles = dir.listFiles();
    
        for ( File file : listFiles )
        {
            if ( file.getName().startsWith( prefix ) )
            {
                if ( file.getName().endsWith( ".qualifier.jar" ) )
                {
                    Assert.fail( "replacement of build qualifier missing in file " + file + ", name: " + file.getName() );
                }
                return;
            }
        }
        Assert.fail( "Missing entry " + prefix + "* in assembled repository directory " + dir );
    }

    static void assertFeatureIU( Document contentXml, File assembledRepoDir, String featureId, String... requiredIus )
        throws IOException
    {
        String featureIuId = featureId + ".feature.group";
        Set<Element> featureIus = Util.findIU( contentXml, featureIuId );
        assertEquals( "Feature iu with id = '" + featureIuId + "' does not occur exactly once in content.xml", 1,
                      featureIus.size() );
    
        Element featureIu = featureIus.iterator().next();
    
        assertTrue( Util.containsIUWithProperty( contentXml, featureIuId, "org.eclipse.equinox.p2.type.group", "true" ) );
        assertTrue( Util.iuHasAllRequirements( featureIu, requiredIus ) );
    
        String featureArtifactPrefix = featureId + "_1.0.0";
        Tycho465RootFilesTest.assertContainsEntry( new File( assembledRepoDir, "features/" ), featureArtifactPrefix );
    }

    static void assertCategoryIU( Document contentXml, String categoryIuId, String featureIuId )
    {
        Set<Element> categoryIus = Util.findIU( contentXml, categoryIuId );
        assertEquals( "Unique category iu not found", 1, categoryIus.size() );
        Element categoryIu = categoryIus.iterator().next();
    
        assertTrue( "IU not typed as category",
                    Util.iuHasProperty( categoryIu, "org.eclipse.equinox.p2.type.category", "true" ) );
        assertTrue( "Category name missing",
                    Util.iuHasProperty( categoryIu, "org.eclipse.equinox.p2.name", "A Category" ) );
        assertTrue( Util.iuHasAllRequirements( categoryIu, featureIuId ) );
    }

    static void assertAddedRootFile( File targetDir )
    {
        String relRootFilePath = "addedFile.txt";
    
        File mainWinConfigProductDir = new File( targetDir, "products/main.product.id/win32/win32/x86" );
        File rootFile = new File( mainWinConfigProductDir, relRootFilePath );
    
        assertTrue( Tycho465RootFilesTest.getFileNotExistsInDirMsg( relRootFilePath, rootFile ), rootFile.exists() );
    
        File mainLinuxConfigProductDir = new File( targetDir, "products/main.product.id/linux/gtk/x86_64" );
        rootFile = new File( mainLinuxConfigProductDir, relRootFilePath );
    
        assertTrue( Tycho465RootFilesTest.getFileNotExistsInDirMsg( relRootFilePath, rootFile ), rootFile.exists() );
    }

    static void assertConfigIndependentRootFiles( File mainProductDir )
    {
        String relRootFilePath = "rootFile.txt";
        File rootFile = new File( mainProductDir, relRootFilePath );
    
        assertTrue( Tycho465RootFilesTest.getFileNotExistsInDirMsg( relRootFilePath, rootFile ), rootFile.exists() );
    
        relRootFilePath = "file5.txt";
        rootFile = new File( mainProductDir, relRootFilePath );
    
        assertTrue( Tycho465RootFilesTest.getFileNotExistsInDirMsg( relRootFilePath, rootFile ), rootFile.exists() );
    
        relRootFilePath = "dir/file6.txt";
        rootFile = new File( mainProductDir, relRootFilePath );
    
        assertTrue( Tycho465RootFilesTest.getFileNotExistsInDirMsg( relRootFilePath, rootFile ), rootFile.exists() );
    }

    static void assertInstalledLinuxConfigRootFile( File targetDir )
    {
        File mainProductDir = new File( targetDir, "products/main.product.id/linux/gtk/x86_64" );
        String relRootFilePath = "file1.txt";
        File rootFile = new File( mainProductDir, relRootFilePath );
    
        assertTrue( Tycho465RootFilesTest.getFileNotExistsInDirMsg( relRootFilePath, rootFile ), rootFile.exists() );
    
        relRootFilePath = "dir/file2.txt";
        rootFile = new File( mainProductDir, relRootFilePath );
    
        assertTrue( Tycho465RootFilesTest.getFileNotExistsInDirMsg( relRootFilePath, rootFile ), rootFile.exists() );
    
        // without config specified root files => included all config specific products
        Tycho465RootFilesTest.assertConfigIndependentRootFiles( mainProductDir );
    }

    static void assertInstalledWinConfigRootFile( File targetDir )
    {
        File mainProductDir = new File( targetDir, "products/main.product.id/win32/win32/x86" );
        String relRootFilePath = "file1.txt";
        File rootFile = new File( mainProductDir, relRootFilePath );
    
        assertTrue( Tycho465RootFilesTest.getFileNotExistsInDirMsg( relRootFilePath, rootFile ), rootFile.exists() );
    
        relRootFilePath = "file2.txt";
        rootFile = new File( mainProductDir, relRootFilePath );
    
        assertTrue( Tycho465RootFilesTest.getFileNotExistsInDirMsg( relRootFilePath, rootFile ), rootFile.exists() );
    
        relRootFilePath = "dir1/file3.txt";
        rootFile = new File( mainProductDir, relRootFilePath );
    
        assertTrue( Tycho465RootFilesTest.getFileNotExistsInDirMsg( relRootFilePath, rootFile ), rootFile.exists() );
    
        relRootFilePath = "dir1/dir2/file4.txt";
        rootFile = new File( mainProductDir, relRootFilePath );
    
        assertTrue( Tycho465RootFilesTest.getFileNotExistsInDirMsg( relRootFilePath, rootFile ), rootFile.exists() );
    
        // without config specified root files => included all config specific products
        Tycho465RootFilesTest.assertConfigIndependentRootFiles( mainProductDir );
    }

    static void assertRootIuMetaData( Document contentXml )
    {
        String featureId = "tycho465.feature";
        String featureIuId = featureId + ".feature.group";
        Set<Element> featureIus = Util.findIU( contentXml, featureIuId );
    
        assertEquals( "Feature iu with id = '" + featureIuId + "' does not occur exactly once in content.xml", 1,
                      featureIus.size() );
    
        Element featureIu = featureIus.iterator().next();
        String rootWinConfigFeatureIuId = featureId + "_root.win32.win32.x86";
    
        assertTrue( "Verifying content.xml failed because feature iu with id = '" + featureIuId
                        + "' does not contain required root iu with id = '" + rootWinConfigFeatureIuId + "'",
                    Util.iuHasAllRequirements( featureIu, rootWinConfigFeatureIuId ) );
    
        String rootLinuxConfigFeatureIuId = featureId + "_root.gtk.linux.x86_64";
    
        assertTrue( "Verifying content.xml failed because feature iu with id = '" + featureIuId
                        + "' does not contain required root iu with id = '" + rootLinuxConfigFeatureIuId + "'",
                    Util.iuHasAllRequirements( featureIu, rootLinuxConfigFeatureIuId ) );
    }

    private static Document openMetadataRepositoryDocument( File repositoryTargetDirectory )
        throws IOException, ZipException
    {

        File contentJar = new File( repositoryTargetDirectory, "content.jar" );
        assertTrue( "content.jar not found \n" + contentJar.getAbsolutePath(), contentJar.isFile() );

        return Util.openXmlFromZip( contentJar, "content.xml" );
    }
}
