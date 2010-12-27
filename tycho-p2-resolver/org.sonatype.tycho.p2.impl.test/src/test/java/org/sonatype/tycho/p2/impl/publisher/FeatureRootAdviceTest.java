package org.sonatype.tycho.p2.impl.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.junit.Test;
import org.sonatype.tycho.p2.impl.test.ArtifactMock;

@SuppressWarnings( "restriction" )
public class FeatureRootAdviceTest
{
    private static final String WIN32_WIN32_X86 = "win32.win32.x86";

    private static final String LINUX_GTK_X86 = "linux.gtk.x86";

    private static final String GTK_LINUX_X86 = "gtk.linux.x86";

    private static final String RESOURCES_ROOTFILES_REL_PATH = "resources/rootfiles";

    private static final String RESOURCES_FEATURE_PROJ_REL_PATH = RESOURCES_ROOTFILES_REL_PATH + "/feature-project";

    private static final String FEATURE_JAR_REL_PATH = RESOURCES_ROOTFILES_REL_PATH
        + "/feature-project/target/feature-0.0.1-SNAPSHOT.jar";

    private static final String GROUP_ID = "group";

    private static final String ARTIFACT_ID = "feature";

    private static final String VERSION = "0.0.1-SNAPSHOT";

    private static final String PACKAGING_TYPE = "eclipse-feature";

    private static final String DEFAULT_CONFIG_SPEC = "";

    // files and directories used in build.properties
    private static final String ROOT_FILE_NAME = "rootFile.txt";

    private static final String ROOT_FILE2_NAME = "file1.txt";

    private static final String ROOT_FILE2_REL_PATH = "rootfiles/" + ROOT_FILE2_NAME;

    @Test
    public void testFeatureRootAdviceComputePath()
        throws Exception
    {
        IFeatureRootAdvice rootFileAdvice = FeatureRootAdvice.createRootFileAdvice( createDefaultArtifactMock() );

        File file1 = new File( RESOURCES_FEATURE_PROJ_REL_PATH, ROOT_FILE_NAME ).getCanonicalFile();
        IPath expectedPathFile1 = new Path( ROOT_FILE_NAME );
        IPath actualPathFile1 = rootFileAdvice.getRootFileComputer( DEFAULT_CONFIG_SPEC ).computePath( file1 );

        assertEquals( expectedPathFile1, actualPathFile1 );

        File file2 = new File( RESOURCES_FEATURE_PROJ_REL_PATH, ROOT_FILE2_REL_PATH ).getCanonicalFile();
        IPath expectedPathFile2 = new Path( ROOT_FILE2_NAME );
        IPath actualPathFile2 = rootFileAdvice.getRootFileComputer( DEFAULT_CONFIG_SPEC ).computePath( file2 );

        assertEquals( expectedPathFile2, actualPathFile2 );
    }

    @Test
    public void testGetProjectBaseDir()
        throws Exception
    {
        ArtifactMock defaultArtifactMock = createDefaultArtifactMock();
        assertEquals( new File( RESOURCES_FEATURE_PROJ_REL_PATH ).getCanonicalFile(),
                      FeatureRootAdvice.getProjectBaseDir( defaultArtifactMock ).getCanonicalFile() );

        // null checks
        ArtifactMock wrongTypeArtifactMock =
            new ArtifactMock( new File( FEATURE_JAR_REL_PATH ).getCanonicalFile(), GROUP_ID, ARTIFACT_ID, VERSION,
                              "eclipse-plugin" );
        assertNull( FeatureRootAdvice.getProjectBaseDir( wrongTypeArtifactMock ) );

        ArtifactMock invalidLocationArtifactMock =
            new ArtifactMock( new File( "resources/rootfiles/feature-project/target/feature.jar" ).getCanonicalFile(),
                              GROUP_ID, ARTIFACT_ID, VERSION, PACKAGING_TYPE );
        assertNull( FeatureRootAdvice.getProjectBaseDir( invalidLocationArtifactMock ) );

        ArtifactMock invalidRelativeLocationArtifactMock =
            new ArtifactMock( new File( FEATURE_JAR_REL_PATH ), GROUP_ID, ARTIFACT_ID, VERSION, PACKAGING_TYPE );
        assertNull( FeatureRootAdvice.getProjectBaseDir( invalidRelativeLocationArtifactMock ) );

    }

    @Test
    public void testParseBuildPropertiesNullChecks()
    {
        assertNull( FeatureRootAdvice.getRootFilesFromBuildProperties( null, null ) );

        assertNull( FeatureRootAdvice.getRootFilesFromBuildProperties( null, new File( RESOURCES_FEATURE_PROJ_REL_PATH ) ) );

        assertNull( FeatureRootAdvice.getRootFilesFromBuildProperties( new Properties(), null ) );
    }

    @Test
    public void testParseBuildPropertiesRelativeFile()
    {
        Properties buildProperties = new Properties();
        buildProperties.put( "root", "file:rootfiles/file1.txt" );

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties =
            FeatureRootAdvice.getRootFilesFromBuildProperties( buildProperties,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );

        assertNotNull( rootFilesFromBuildProperties );

        Map<File, IPath> defaultRootFilesMap = rootFilesFromBuildProperties.get( DEFAULT_CONFIG_SPEC );

        assertEquals( 1, defaultRootFilesMap.size() );

        IPath entryPath = defaultRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt" ) );
        assertFalse( entryPath.isAbsolute() );

        assertEquals( new Path( "file1.txt" ), entryPath );
    }

    @Test
    public void testParseBuildPropertiesRelativeFiles()
    {
        Properties buildProperties = new Properties();
        buildProperties.put( "root", "file:rootfiles/file1.txt,file:rootfiles/dir/file3.txt" );

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties =
            FeatureRootAdvice.getRootFilesFromBuildProperties( buildProperties,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );

        assertNotNull( rootFilesFromBuildProperties );

        Map<File, IPath> defaultRootFilesMap = rootFilesFromBuildProperties.get( DEFAULT_CONFIG_SPEC );
        assertNotNull( defaultRootFilesMap );
        assertEquals( 2, defaultRootFilesMap.size() );

        IPath entryPathFile1 =
            defaultRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt" ) );
        assertFalse( entryPathFile1.isAbsolute() );
        assertEquals( new Path( "file1.txt" ), entryPathFile1 );

        IPath entryPathFile3 =
            defaultRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/dir/file3.txt" ) );
        assertFalse( entryPathFile3.isAbsolute() );

        assertEquals( new Path( "file3.txt" ), entryPathFile3 );
    }

    @Test
    public void testParseBuildPropertiesRelativeFolder()
    {
        Properties buildProperties = new Properties();
        buildProperties.put( "root", "rootfiles" );

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties =
            FeatureRootAdvice.getRootFilesFromBuildProperties( buildProperties,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );

        assertNotNull( rootFilesFromBuildProperties );

        Map<File, IPath> defaultRootFilesMap = rootFilesFromBuildProperties.get( DEFAULT_CONFIG_SPEC );

        assertEquals( 4, defaultRootFilesMap.size() );

        IPath entryPathFile1 =
            defaultRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt" ) );
        assertFalse( entryPathFile1.isAbsolute() );
        assertEquals( new Path( "file1.txt" ), entryPathFile1 );

        IPath entryPathFile2 =
            defaultRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file2.txt" ) );
        assertFalse( entryPathFile2.isAbsolute() );
        assertEquals( new Path( "file2.txt" ), entryPathFile2 );

        IPath entryPathDir = defaultRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/dir" ) );
        assertFalse( entryPathDir.isAbsolute() );
        assertEquals( new Path( "dir" ), entryPathDir );

        IPath entryPathFile3 =
            defaultRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/dir/file3.txt" ) );
        assertFalse( entryPathFile3.isAbsolute() );
        assertEquals( new Path( "dir/file3.txt" ), entryPathFile3 );
    }

    @Test
    public void testParseBuildPropertiesAbsoluteFile()
        throws Exception
    {
        Properties buildProperties = new Properties();
        File file = new File( RESOURCES_FEATURE_PROJ_REL_PATH + "/rootfiles/file1.txt" );
        buildProperties.put( "root", "absolute:file:" + file.getAbsolutePath() );

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties =
            FeatureRootAdvice.getRootFilesFromBuildProperties( buildProperties,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );

        assertNotNull( rootFilesFromBuildProperties );

        Map<File, IPath> defaultRootFilesMap = rootFilesFromBuildProperties.get( DEFAULT_CONFIG_SPEC );
        assertEquals( 1, defaultRootFilesMap.size() );

        IPath entryPath =
            defaultRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt" ).getCanonicalFile() );
        assertFalse( entryPath.isAbsolute() );

        assertEquals( new Path( "file1.txt" ), entryPath );
    }

    @Test
    public void testParseBuildPropertiesRelativeFileWithLinuxConfig()
    {
        Properties buildProperties = new Properties();
        buildProperties.put( "root." + LINUX_GTK_X86, "file:rootfiles/file1.txt" );

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties =
            FeatureRootAdvice.getRootFilesFromBuildProperties( buildProperties,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );

        assertNotNull( rootFilesFromBuildProperties );

        Map<File, IPath> defaultRootFilesMap = rootFilesFromBuildProperties.get( GTK_LINUX_X86 );

        assertEquals( 1, defaultRootFilesMap.size() );

        IPath entryPath = defaultRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt" ) );
        assertFalse( entryPath.isAbsolute() );

        assertEquals( new Path( "file1.txt" ), entryPath );
    }

    @Test
    public void testParseBuildPropertiesRelativeFileWithAndWithoutConfigs()
    {
        Properties buildProperties = new Properties();
        buildProperties.put( "root." + WIN32_WIN32_X86, "file:rootfiles/file1.txt" );
        buildProperties.put( "root." + LINUX_GTK_X86, "file:rootfiles/file2.txt" );
        buildProperties.put( "root", "rootfiles/dir" );

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties =
            FeatureRootAdvice.getRootFilesFromBuildProperties( buildProperties,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );

        assertNotNull( rootFilesFromBuildProperties );

        // win config
        Map<File, IPath> winConfigRootFilesMap = rootFilesFromBuildProperties.get( WIN32_WIN32_X86 );

        assertEquals( 1, winConfigRootFilesMap.size() );

        IPath winEntryPath =
            winConfigRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt" ) );
        assertFalse( winEntryPath.isAbsolute() );

        assertEquals( new Path( "file1.txt" ), winEntryPath );

        // linux config
        Map<File, IPath> linuxConfigRootFilesMap = rootFilesFromBuildProperties.get( GTK_LINUX_X86 );

        assertEquals( 1, linuxConfigRootFilesMap.size() );

        IPath linuxEntryPath =
            linuxConfigRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file2.txt" ) );
        assertFalse( linuxEntryPath.isAbsolute() );

        assertEquals( new Path( "file2.txt" ), linuxEntryPath );

        // without config
        Map<File, IPath> defaultConfigRootFilesMap = rootFilesFromBuildProperties.get( "" );

        assertEquals( 1, defaultConfigRootFilesMap.size() );

        IPath entryPath =
            defaultConfigRootFilesMap.get( new File( RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/dir/file3.txt" ) );
        assertFalse( entryPath.isAbsolute() );

        assertEquals( new Path( "file3.txt" ), entryPath );
    }

    @Test
    public void testParseBuildPropertiesInvalidConfigs()
    {
        Properties invalidBuildProperties1 = new Properties();
        invalidBuildProperties1.put( "root.invalid.config", "file:rootfiles/file1.txt" );

        try
        {
            FeatureRootAdvice.getRootFilesFromBuildProperties( invalidBuildProperties1,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );
            fail( "IllegalArgumentException expected: 'Wrong os.ws.arch format...' " );
        }
        catch ( IllegalArgumentException e )
        {
            // this exception is expected
        }

        Properties invalidBuildProperties2 = new Properties();
        invalidBuildProperties2.put( "root...", "file:rootfiles/file2.txt" );

        try
        {
            FeatureRootAdvice.getRootFilesFromBuildProperties( invalidBuildProperties2,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );
            fail( "IllegalArgumentException expected: 'Wrong os.ws.arch format...' " );
        }
        catch ( IllegalArgumentException e )
        {
            // this exception is expected
        }
    }

    @Test
    public void testUnsupportedFolderBuildProperties()
    {
        Properties unsupportedBuildProperties1 = new Properties();
        unsupportedBuildProperties1.put( "root.folder.dir", "file:rootfiles/file1.txt" );

        try
        {
            FeatureRootAdvice.getRootFilesFromBuildProperties( unsupportedBuildProperties1,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );
            fail( "UnsupportedOperationException expected" );
        }
        catch ( UnsupportedOperationException e )
        {
            // this exception is expected
        }

        Properties unsupportedBuildProperties2 = new Properties();
        unsupportedBuildProperties2.put( "root.win32.win32.x86.folder.dir", "file:rootfiles/file1.txt" );

        try
        {
            FeatureRootAdvice.getRootFilesFromBuildProperties( unsupportedBuildProperties2,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );
            fail( "UnsupportedOperationException expected" );
        }
        catch ( UnsupportedOperationException e )
        {
            // this exception is expected
        }
    }

    @Test
    public void testUnsupportedPermissionsBuildProperties()
    {
        Properties unsupportedBuildProperties1 = new Properties();
        unsupportedBuildProperties1.put( "root.permissions.755", "file:rootfiles/file1.txt" );

        try
        {
            FeatureRootAdvice.getRootFilesFromBuildProperties( unsupportedBuildProperties1,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );
            fail( "UnsupportedOperationException expected" );
        }
        catch ( UnsupportedOperationException e )
        {
            // this exception is expected
        }

        Properties unsupportedBuildProperties2 = new Properties();
        unsupportedBuildProperties2.put( "root.win32.win32.x86.permissions.755", "file:rootfiles/file1.txt" );

        try
        {
            FeatureRootAdvice.getRootFilesFromBuildProperties( unsupportedBuildProperties2,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );
            fail( "UnsupportedOperationException expected" );
        }
        catch ( UnsupportedOperationException e )
        {
            // this exception is expected
        }
    }

    @Test
    public void testUnsupportedLinkBuildProperties()
    {
        Properties unsupportedBuildProperties1 = new Properties();
        unsupportedBuildProperties1.put( "root.link", "file:rootfiles/file1.txt" );

        try
        {
            FeatureRootAdvice.getRootFilesFromBuildProperties( unsupportedBuildProperties1,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );
            fail( "UnsupportedOperationException expected" );
        }
        catch ( UnsupportedOperationException e )
        {
            // this exception is expected
        }

        Properties unsupportedBuildProperties2 = new Properties();
        unsupportedBuildProperties2.put( "root.win32.win32.x86.link", "file:rootfiles/file1.txt" );

        try
        {
            FeatureRootAdvice.getRootFilesFromBuildProperties( unsupportedBuildProperties2,
                                                               new File( RESOURCES_FEATURE_PROJ_REL_PATH ) );
            fail( "UnsupportedOperationException expected" );
        }
        catch ( UnsupportedOperationException e )
        {
            // this exception is expected
        }
    }

    private ArtifactMock createDefaultArtifactMock()
        throws IOException
    {
        return ( new ArtifactMock( new File( FEATURE_JAR_REL_PATH ).getCanonicalFile(), GROUP_ID, ARTIFACT_ID, VERSION,
                                   PACKAGING_TYPE ) );
    }
}
