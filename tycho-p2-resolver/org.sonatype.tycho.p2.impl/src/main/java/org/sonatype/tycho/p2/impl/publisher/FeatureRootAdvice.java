package org.sonatype.tycho.p2.impl.publisher;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.sonatype.tycho.p2.IArtifactFacade;
import org.sonatype.tycho.p2.resolver.P2Resolver;

/**
 * This class handles definitions of root files in build.properties according to
 * http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/pde_rootfiles.htm <br>
 * Currently <b>not supported</b> build.property key for root files
 * <ul>
 * <li>root.folder.&lt;subfolder&gt;
 * <li>root.&lt;config&gt;.folder.&lt;subfolder&gt;
 * <li>root.permissions.&lt;rights&gt;
 * <li>root.&lt;config&gt;.permissions.&lt;rights&gt;
 * <li>root.link
 * <li>root.&lt;config&gt;.link
 * </ul>
 * Also patterns (*, ** and ?) as values for root files are not yet supported.
 */
@SuppressWarnings( "restriction" )
public class FeatureRootAdvice
    implements IFeatureRootAdvice
{

    private static final String ROOT_DOT = "root.";

    private static final String ROOT = "root";

    private final String artifactId;

    protected final Map<String, Map<File, IPath>> configToRootFilesMapping;

    public FeatureRootAdvice( Map<String, Map<File, IPath>> configToRootFilesMapping, String artifactId )
    {
        if ( configToRootFilesMapping == null || configToRootFilesMapping.size() == 0 )
        {
            throw new IllegalArgumentException( "Passed configToRootFilesMapping must not be null or empty" );
        }
        this.configToRootFilesMapping = configToRootFilesMapping;
        this.artifactId = artifactId;
    }

    /**
     * @param featureArtifact
     * @return IFeatureRootAdvice if root file configuration in build properties exists otherwise return null
     */
    public static IFeatureRootAdvice createRootFileAdvice( IArtifactFacade featureArtifact )
    {
        File projectDir = getProjectBaseDir( featureArtifact );

        if ( projectDir != null )
        {

            Properties buildProperties = AbstractMetadataGenerator.loadProperties( projectDir );

            Map<String, Map<File, IPath>> rootFilesMap = getRootFilesFromBuildProperties( buildProperties, projectDir );

            if ( rootFilesMap != null && rootFilesMap.size() > 0 )
            {
                return new FeatureRootAdvice( rootFilesMap, featureArtifact.getArtifactId() );
            }
        }
        return null;
    }

    /**
     * The returned object maps <ws.os.arch> configurations to corresponding root files map. The root files map itself
     * maps the absolute source location of a root file to the relative path that describes the location of the root
     * file in the installed product. The returned map is used for creating the structure of the root file artifacts.
     * 
     * @param buildProperties loaded Properties object
     * @param baseDir base directory for resolution of relative paths in the buildProperties
     * @return the root files information parsed from the <code>Properties buildProperties</code> parameter. Returns
     *         null if buildProperties or baseDir is null.
     */
    public static Map<String, Map<File, IPath>> getRootFilesFromBuildProperties( Properties buildProperties,
                                                                                 File baseDir )
    {
        if ( buildProperties == null || baseDir == null )
            return null;

        Map<String, Map<File, IPath>> rootFileConfigsMap = new HashMap<String, Map<File, IPath>>();

        for ( Entry<Object, Object> pair : buildProperties.entrySet() )
        {
            if ( pair.getValue() instanceof String && pair.getKey() instanceof String )
            {
                String buildPropertyKey = (String) pair.getKey();

                if ( ROOT.equals( buildPropertyKey ) )
                {
                    // no specified os.ws.arch configuration gets the empty key.
                    rootFileConfigsMap.put( "", createRootFilesMap( baseDir, (String) pair.getValue() ) );
                }
                else if ( buildPropertyKey.startsWith( ROOT_DOT ) )
                {
                    // check not yet supported root files use case according to
                    // http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/pde_rootfiles.htm
                    if ( buildPropertyKey.contains( ".folder." ) )
                    {
                        throw new UnsupportedOperationException(
                                                                 "root.folder.<subfolder> and root.<config>.folder.<subfolder> are not yet supported in build.properties" );
                    }
                    else if ( buildPropertyKey.contains( ".permissions." ) )
                    {
                        throw new UnsupportedOperationException(
                                                                 "root.permissions.<rights> and root.<config>.permissions.<rights> are not yet supported in build.properties" );
                    }
                    else if ( buildPropertyKey.endsWith( ".link" ) )
                    {
                        throw new UnsupportedOperationException(
                                                                 "root.link and root.<config>.link are not yet supported in build.properties" );
                    }
                    else
                    {
                        String config = convertOsWsArchToWsOsArch( buildPropertyKey.substring( ROOT_DOT.length() ) );

                        if ( config != null )
                        {
                            rootFileConfigsMap.put( config, createRootFilesMap( baseDir, (String) pair.getValue() ) );
                        }
                    }
                }
            }
        }
        return rootFileConfigsMap;
    }

    /**
     * According to Eclipse Help > Help Contents: Plug-in Development Environment Guide > Tasks > PDE Build Advanced
     * Topics > Adding Files to the Root of a Build, value(s) of root are a comma separated list of relative paths to
     * folder(s). The contents of the folder are included as root files to the installation. Exception are if a list
     * value starts with: 'file:', 'absolute:' or 'absolute:file:'. 'file:' indicates that the included content is a
     * file only. 'absolute:' indicates that the path is absolute. Examples:
     * <ul>
     * <li>root=rootfiles1, rootfiles2, license.html
     * <li>root=file:license.html
     * <li>root=absolute:/rootfiles1
     * <li>root=absolute:file:/eclipse/about.html
     * </ul>
     * Configurations like root.<os.ws.arch> is also supported here but patterns, subfolder and permissions so far are
     * not supported. <br>
     * Following wrongly specified cases are simply ignored when trying to find root files<br>
     * <ol>
     * <li>root = license.html -> licence.html exists but is not a directory (contrary to PDE product export where build
     * fails )
     * <li>root = file:not_existing_file.txt, not_existing_dir -> specified file or directory does not exist
     * <li>root = file:C:/_tmp/file_absolute.txt -> existing file with absolute path;but not specified as absolute
     * <li>root = file:absolute:C:/_tmp/file_absolute.txt -> Using 'file:absolute:' (instead of correct
     * 'absolute:file:')
     * </ol>
     * 
     * @param baseDir base directory for resolution of relative paths in the buildProperties
     * @param rootFileEntryValue specified comma separated root files
     * @return the root files information parsed from the <code>rootFileEntryValue</code> parameter. If parsing lead to
     *         non valid root files cases then an empty Map is returned.
     */
    private static Map<File, IPath> createRootFilesMap( File baseDir, String rootFileEntryValue )
    {
        HashMap<File, IPath> rootFilesMap = new HashMap<File, IPath>();

        String[] rootFilePaths = rootFileEntryValue.split( "," );

        for ( String path : rootFilePaths )
        {
            path = path.trim();

            rootFilesMap.putAll( collectRootFilesMap( parseRootFilePath( path, baseDir ) ) );

        }
        return rootFilesMap;
    }

    private static File parseRootFilePath( String path, File baseDir )
    {
        boolean absolute = false;
        final String ABSOLUTE_STRING = "absolute:";
        if ( path.startsWith( ABSOLUTE_STRING ) )
        {
            absolute = true;
            path = path.substring( ABSOLUTE_STRING.length() );
        }

        String FILE_STRING = "file:";
        if ( path.startsWith( FILE_STRING ) )
        {
            path = path.substring( FILE_STRING.length() );
        }

        return ( absolute ? new File( path ) : new File( baseDir, path ) );

    }

    /**
     * Assumptions for resolving the project base directory of the given artifact:
     * <ul>
     * <li>packaging type of the artifact:"eclipse-feature"</li>
     * <li>the location of the feature artifact is absolute and points to the built feature.jar</li>
     * <li>the build output folder is located in a subfolder of the project base directory</li>
     * </ul>
     * 
     * @return the project base directory of the given artifact if found under the above assumptions, otherwise null
     */
    public static File getProjectBaseDir( IArtifactFacade featureArtifact )
    {
        if ( !P2Resolver.TYPE_ECLIPSE_FEATURE.equals( featureArtifact.getPackagingType() ) )
        {
            return null;
        }

        File featureJar = featureArtifact.getLocation();
        if ( featureJar != null && featureJar.isFile() && featureJar.isAbsolute() )
        {
            File targetDir = featureJar.getParentFile();
            if ( targetDir != null )
            {
                File projectLocation = targetDir.getParentFile();
                if ( projectLocation != null )
                {
                    return projectLocation;
                }
            }
        }
        return null;
    }

    private static Map<File, IPath> collectRootFilesMap( File rootFile )
    {
        if ( rootFile.isFile() )
        {
            return Collections.singletonMap( rootFile, Path.fromOSString( rootFile.getName() ) );
        }
        return collectRootFilesMap( rootFile, Path.fromOSString( rootFile.getAbsolutePath() ) );
    }

    private static Map<File, IPath> collectRootFilesMap( File file, IPath basePath )
    {
        Map<File, IPath> files = new HashMap<File, IPath>();

        if ( !file.exists() )
            return Collections.emptyMap();
        File[] dirFiles = file.listFiles();
        for ( File dirFile : dirFiles )
        {
            files.put( dirFile, Path.fromOSString( dirFile.getAbsolutePath() ).makeRelativeTo( basePath ) );
            if ( dirFile.isDirectory() )
            {
                files.putAll( collectRootFilesMap( dirFile, basePath ) );
            }
        }
        return files;
    }

    public boolean isApplicable( String configSpec, boolean includeDefault, String id, Version version )
    {
        if ( id != null && !id.equals( this.artifactId ) )
        {
            return false;
        }

        if ( configSpec != null && this.configToRootFilesMapping.get( configSpec ) != null )
        {
            return false;
        }

        return true;
    }

    public String[] getConfigurations()
    {
        Set<String> configurations = configToRootFilesMapping.keySet();
        return configurations.toArray( new String[configurations.size()] );
    }

    public IPathComputer getRootFileComputer( final String configSpec )
    {
        return new IPathComputer()
        {
            public void reset()
            {
                // do nothing
            }

            public IPath computePath( File source )
            {
                return configToRootFilesMapping.get( configSpec ).get( source );
            }
        };
    }

    public FileSetDescriptor getDescriptor( String configSpec )
    {
        if ( configSpec == null )
        {
            return null;
        }

        Map<File, IPath> rootFilesMap = this.configToRootFilesMapping.get( configSpec );

        if ( rootFilesMap != null )
        {
            String fileSetDescriptorKey = ( "".equals( configSpec ) ) ? ROOT : ROOT_DOT + configSpec;
            FileSetDescriptor fileSetDescriptor = new FileSetDescriptor( fileSetDescriptorKey, configSpec );

            Set<File> rootFileSet = rootFilesMap.keySet();
            fileSetDescriptor.addFiles( rootFileSet.toArray( new File[rootFileSet.size()] ) );

            return fileSetDescriptor;
        }

        return null;
    }

    // This conversion is needed as root files configurations are specified as os.ws.arch but publisher uses a
    // ws.os.arch configuration format
    private static String convertOsWsArchToWsOsArch( String osWsArchConfig )
    {
        String[] osWsArch = osWsArchConfig.split( "\\." );

        if ( osWsArch.length == 3 )
        {
            return new StringBuffer().append( osWsArch[1] ).append( "." ).append( osWsArch[0] ).append( "." ).append( osWsArch[2] ).toString();
        }
        else
        {
            throw new IllegalArgumentException( "Wrong os.ws.arch format specified for root files: '" + osWsArchConfig
                + "'" );
        }
    }
}
