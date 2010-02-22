package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.ArchiveEntryUtils;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.PluginDescription;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.ProductConfiguration;
import org.codehaus.tycho.osgitools.BundleManifestReader;
import org.codehaus.tycho.osgitools.DefaultPluginDescription;
import org.codehaus.tycho.osgitools.EquinoxBundleResolutionState;
import org.codehaus.tycho.utils.PlatformPropertiesUtils;
import org.eclipse.pde.internal.swt.tools.IconExe;

/**
 * @goal product-export
 */
public class ProductExportMojo
    extends AbstractTychoPackagingMojo
{
    /**
     * The product configuration, a .product file. This file manages all aspects of a product definition from its
     * constituent plug-ins to configuration files to branding.
     * 
     * @parameter expression="${productConfiguration}"
     */
    private File productConfigurationFile;

    /**
     * @parameter expression="${productConfiguration}/../p2.inf"
     */
    private File p2inf;

    /**
     * Location of generated .product file with all versions replaced with their expanded values.
     * 
     * @parameter expression="${project.build.directory}/${project.artifactId}.product"
     */
    private File expandedProductFile;

    /**
     * Parsed product configuration file
     */
    private ProductConfiguration productConfiguration;

    /**
     * @parameter
     */
    private TargetEnvironment[] environments;

    /**
     * @parameter expression="${tycho.product.createArchive}" default-value="true"
     */
    private boolean createProductArchive;

    /**
     * @parameter default-value="false"
     */
    private boolean includeSources;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( productConfigurationFile == null )
        {
            File basedir = project.getBasedir();
            File productCfg = new File( basedir, project.getArtifactId() + ".product" );
            if ( productCfg.exists() )
            {
                productConfigurationFile = productCfg;
            }
        }

        if ( productConfigurationFile == null )
        {
            throw new MojoExecutionException( "Product configuration file not expecified" );
        }
        if ( !productConfigurationFile.exists() )
        {
            throw new MojoExecutionException( "Product configuration file not found "
                + productConfigurationFile.getAbsolutePath() );
        }

        try
        {
            getLog().debug( "Parsing productConfiguration" );
            productConfiguration = ProductConfiguration.read( productConfigurationFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error reading product configuration file", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error parsing product configuration file", e );
        }

        // build results will vary from system to system without explicit target environment configuration
        if ( productConfiguration.includeLaunchers() && environments == null )
        {
            throw new MojoFailureException( "Product includes native launcher but no target environment was specified" );
        }

        // expandVersion();
        
        BundleManifestReader manifestReader = EquinoxBundleResolutionState.newManifestReader( plexus, project );

        for ( TargetEnvironment environment : getEnvironments() )
        {
            File target = getTarget( environment );
            File targetEclipse = new File( target, "eclipse" );
            targetEclipse.mkdirs();

            generateDotEclipseProduct( targetEclipse );
            generateConfigIni( environment, targetEclipse );
            includeRootFiles( environment, targetEclipse );
            
            ProductAssembler assembler = new ProductAssembler( session, manifestReader, targetEclipse, environment );
            assembler.setIncludeSources( includeSources );
            getDependencyWalker( environment ).walk( assembler );

            copyImplicitDependencies( environment, assembler );

            if ( productConfiguration.includeLaunchers() )
            {
                copyExecutable( environment, targetEclipse );
            }

            if ( createProductArchive )
            {
                createProductArchive( environment, target );
            }
        }

        // String version = getTychoProjectFacet().getArtifactKey( project ).getVersion();
        // String productVersion = VersioningHelper.getExpandedVersion( project, version );
        // productConfiguration.setVersion( productVersion.toString() );

        try
        {
            ProductConfiguration.write( productConfiguration, expandedProductFile );

            if ( p2inf.canRead() )
            {
                FileUtils.copyFile( p2inf, new File( expandedProductFile.getParentFile(), p2inf.getName() ) );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing expanded product configuration file", e );
        }

        if ( !createProductArchive || environments != null )
        {
            project.getArtifact().setFile( expandedProductFile );
        }
    }

    private ArtifactDependencyWalker getDependencyWalker( TargetEnvironment environment )
    {
        return getTychoProjectFacet( TychoProject.ECLIPSE_APPLICATION ).getDependencyWalker( project, environment );
    }

    private TargetEnvironment[] getEnvironments()
    {
        if ( environments != null )
        {
            return environments;
        }

        TargetPlatformConfiguration configuration =
            (TargetPlatformConfiguration) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION );
        return new TargetEnvironment[] { configuration.getEnvironment() };
    }

    private File getTarget( TargetEnvironment environment )
    {
        File target;

        if ( environments == null )
        {
            target = new File( project.getBuild().getDirectory(), "product" );
        }
        else
        {
            target = new File( project.getBuild().getDirectory(), toString( environment ) );
        }

        target.mkdirs();

        return target;
    }

    private String toString( TargetEnvironment environment )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( environment.getOs() ).append( '.' ).append( environment.getWs() ).append( '.' ).append(
                                                                                                           environment.getArch() );
        if ( environment.getNl() != null )
        {
            sb.append( '.' ).append( environment.getNl() );
        }
        return sb.toString();
    }

    /**
     * Root files are files that must be packaged with an Eclipse install but are not features or plug-ins. These files
     * are added to the root or to a specified sub folder of the build.
     * 
     * <pre>
     * root=
     * root.<confi>=
     * root.folder.<subfolder>=
     * root.<config>.folder.<subfolder>=
     * </pre>
     * 
     * Not supported are the properties root.permissions and root.link.
     * 
     * @see http://help.eclipse.org/ganymede/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/pde_rootfiles.htm
     * @throws MojoExecutionException
     */
    private void includeRootFiles( TargetEnvironment environment, File target )
        throws MojoExecutionException
    {
        Properties properties = project.getProperties();
        String generatedBuildProperties = properties.getProperty( "generatedBuildProperties" );
        getLog().debug( "includeRootFiles from " + generatedBuildProperties );
        if ( generatedBuildProperties != null )
        {
            Properties rootProperties = new Properties();
            try
            {
                rootProperties.load( new FileInputStream( new File( project.getBasedir(), generatedBuildProperties ) ) );
                if ( !rootProperties.isEmpty() )
                {
                    String config = getConfig( environment );
                    String root = "root";
                    String rootConfig = "root." + config;
                    String rootFolder = "root.folder.";
                    String rootConfigFolder = "root." + config + ".folder.";
                    Set<Entry<Object, Object>> entrySet = rootProperties.entrySet();
                    for ( Iterator iterator = entrySet.iterator(); iterator.hasNext(); )
                    {
                        Entry<String, String> entry = (Entry<String, String>) iterator.next();
                        String key = entry.getKey().trim();
                        // root=
                        if ( root.equals( key ) )
                        {
                            handleRootEntry( target, entry.getValue(), null );
                        }
                        // root.xxx=
                        else if ( rootConfig.equals( key ) )
                        {
                            handleRootEntry( target, entry.getValue(), null );
                        }
                        // root.folder.yyy=
                        else if ( key.startsWith( rootFolder ) )
                        {
                            String subFolder = entry.getKey().substring( ( rootFolder.length() ) );
                            handleRootEntry( target, entry.getValue(), subFolder );
                        }
                        // root.xxx.folder.yyy=
                        else if ( key.startsWith( rootConfigFolder ) )
                        {
                            String subFolder = entry.getKey().substring( ( rootConfigFolder.length() ) );
                            handleRootEntry( target, entry.getValue(), subFolder );
                        }
                        else
                        {
                            getLog().debug( "ignoring property " + entry.getKey() + "=" + entry.getValue() );
                        }
                    }
                }
            }
            catch ( FileNotFoundException e )
            {
                throw new MojoExecutionException( "Error including root files for product", e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error including root files for product", e );
            }
        }
    }

    /**
     * @param rootFileEntry files and directories seperated by semicolons, the syntax is:
     *            <ul>
     *            <li>for a relative file: file:license.html,...</li>
     *            <li>for a absolute file: absolute:file:/eclipse/about.html,...</li>
     *            <li>for a relative folder: rootfiles,...</li>
     *            <li>for a absolute folder: absolute:/eclipse/rootfiles,...</li>
     *            </ul>
     * @param subFolder the sub folder to which the root file entries are copied to
     */
    private void handleRootEntry( File target, String rootFileEntries, String subFolder )
    {
        StringTokenizer t = new StringTokenizer( rootFileEntries, "," );
        File destination = target;
        if ( subFolder != null )
        {
            destination = new File( target, subFolder );
        }
        while ( t.hasMoreTokens() )
        {
            String rootFileEntry = t.nextToken();
            String fileName = rootFileEntry.trim();
            boolean isAbsolute = false;
            if ( fileName.startsWith( "absolute:" ) )
            {
                isAbsolute = true;
                fileName = fileName.substring( "absolute:".length() );
            }
            if ( fileName.startsWith( "file" ) )
            {
                fileName = fileName.substring( "file:".length() );
            }
            File source = null;
            if ( !isAbsolute )
            {
                source = new File( project.getBasedir(), fileName );
            }
            else
            {
                source = new File( fileName );
            }
            if ( source.isFile() )
            {
                try
                {
                    FileUtils.copyFileToDirectory( source, destination );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
            else if ( source.isDirectory() )
            {
                try
                {
                    FileUtils.copyDirectoryToDirectory( source, destination );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
            else
            {
                getLog().warn( "Skipping root entry " + rootFileEntry );
            }
        }
    }

    private String getConfig( TargetEnvironment environment )
    {
        String os = environment.getOs();
        String ws = environment.getWs();
        String arch = environment.getArch();
        StringBuffer config = new StringBuffer( ws ).append( "." ).append( os ).append( "." ).append( arch );
        return config.toString();
    }

    private void createProductArchive( TargetEnvironment environment, File target )
        throws MojoExecutionException
    {
        ZipArchiver zipper;
        try
        {
            zipper = (ZipArchiver) plexus.lookup( ZipArchiver.ROLE, "zip" );
        }
        catch ( ComponentLookupException e )
        {
            throw new MojoExecutionException( "Unable to resolve ZipArchiver", e );
        }

        String classifier = toString( environment );

        StringBuilder filename = new StringBuilder( project.getBuild().getFinalName() );
        if ( environments != null )
        {
            filename.append( '-' ).append( classifier );
        }
        filename.append( ".zip" );

        File destFile = new File( project.getBuild().getDirectory(), filename.toString() );

        try
        {
            zipper.addDirectory( target );
            zipper.setDestFile( destFile );
            zipper.createArchive();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error packing product", e );
        }

        if ( environments == null )
        {
            // main artifact
            project.getArtifact().setFile( destFile );
        }
        else
        {
            projectHelper.attachArtifact( project, destFile, classifier );
        }
    }

    private void generateDotEclipseProduct( File target )
        throws MojoExecutionException
    {
        getLog().debug( "Generating .eclipseproduct" );
        Properties props = new Properties();
        setPropertyIfNotNull( props, "version", productConfiguration.getVersion() );
        setPropertyIfNotNull( props, "name", productConfiguration.getName() );
        setPropertyIfNotNull( props, "id", productConfiguration.getId() );

        File eclipseproduct = new File( target, ".eclipseproduct" );
        try
        {
            FileOutputStream fos = new FileOutputStream( eclipseproduct );
            props.store( fos, "Eclipse Product File" );
            fos.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating .eclipseproduct file.", e );
        }
    }

    private void generateConfigIni( TargetEnvironment environment, File target )
        throws MojoExecutionException, MojoFailureException
    {
        getLog().debug( "Generating config.ini" );
        Properties props = new Properties();
        String id = productConfiguration.getId();
        if ( id != null )
        {
            String splash = id.split( "\\." )[0];
            setPropertyIfNotNull( props, "osgi.splashPath", "platform:/base/plugins/" + splash );
        }

        setPropertyIfNotNull( props, "eclipse.product", id );
        // TODO check if there are any other levels
        setPropertyIfNotNull( props, "osgi.bundles.defaultStartLevel", "4" );

        if ( productConfiguration.useFeatures() )
        {
            setPropertyIfNotNull( props, "osgi.bundles", getFeaturesOsgiBundles() );
        }
        else
        {
            setPropertyIfNotNull( props, "osgi.bundles", getPluginsOsgiBundles( environment ) );
        }

        File configsFolder = new File( target, "configuration" );
        configsFolder.mkdirs();

        File configIni = new File( configsFolder, "config.ini" );
        try
        {
            FileOutputStream fos = new FileOutputStream( configIni );
            props.store( fos, "Product Runtime Configuration File" );
            fos.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating .eclipseproduct file.", e );
        }
    }

    private String getFeaturesOsgiBundles()
    {
        // TODO how does eclipse know this?
        return "org.eclipse.equinox.common@2:start,org.eclipse.update.configurator@3:start,org.eclipse.core.runtime@start";
    }

    private String getPluginsOsgiBundles( TargetEnvironment environment )
        throws MojoFailureException
    {
        List<PluginRef> plugins = productConfiguration.getPlugins();
        StringBuilder buf = new StringBuilder( plugins.size() * 10 );
        for ( PluginRef plugin : plugins )
        {
            // reverse engineering discovered
            // this plugin is not present on config.ini, and if so nothing
            // starts
            if ( "org.eclipse.osgi".equals( plugin.getId() ) )
            {
                continue;
            }

            if ( buf.length() != 0 )
            {
                buf.append( ',' );
            }

            buf.append( plugin.getId() );

            // reverse engineering discovered
            // the final bundle has @start after runtime
            if ( "org.eclipse.core.runtime".equals( plugin.getId() ) )
            {
                buf.append( "@start" );
            }
        }

        // required plugins, RCP didn't start without both
        if ( buf.length() != 0 )
        {
            buf.append( ',' );
        }
        String os = environment.getOs();
        String ws = environment.getWs();
        String arch = environment.getArch();

        buf.append( "org.eclipse.equinox.launcher," );
        buf.append( "org.eclipse.equinox.launcher." + ws + "." + os + "." + arch );

        return buf.toString();
    }

    private void copyImplicitDependencies( TargetEnvironment environment, ProductAssembler assembler )
        throws MojoExecutionException, MojoFailureException
    {
        // required plugins, RCP didn't start without both
        assembler.visitPlugin( newPluginDescription( "org.eclipse.equinox.launcher" ) );

        String os = environment.getOs();
        String ws = environment.getWs();
        String arch = environment.getArch();

        // for Mac OS X there is no org.eclipse.equinox.launcher.carbon.macosx.x86 folder,
        // only a org.eclipse.equinox.launcher.carbon.macosx folder.
        // see http://jira.codehaus.org/browse/MNGECLIPSE-1075
        if ( PlatformPropertiesUtils.OS_MACOSX.equals( os ) && PlatformPropertiesUtils.WS_CARBON.equals( os ))
        {
            assembler.visitPlugin( newPluginDescription( "org.eclipse.equinox.launcher." + ws + "." + os ) );
        }
        else
        {
            assembler.visitPlugin( newPluginDescription( "org.eclipse.equinox.launcher." + ws + "." + os + "." + arch ) );
        }
    }

    private PluginDescription newPluginDescription( String id )
    {
        TargetPlatform platform = getTargetPlatform();
        ArtifactKey key = platform.getArtifactKey( TychoProject.ECLIPSE_PLUGIN, id, null );

        if ( key == null )
        {
            throw new IllegalArgumentException( "Could not resolve required bundle " + id );
        }

        File location = platform.getArtifact( key );

        return new DefaultPluginDescription( key, location, null, null );
    }

    private void copyExecutable( TargetEnvironment environment, File target )
        throws MojoExecutionException, MojoFailureException
    {
        getLog().debug( "Creating launcher" );

        File location =
            getTargetPlatform().getArtifact( TychoProject.ECLIPSE_FEATURE, "org.eclipse.equinox.executable", null );

        if ( location == null )
        {
            throw new MojoExecutionException( "RPC delta feature not found!" );
        }

        String os = environment.getOs();
        String ws = environment.getWs();
        String arch = environment.getArch();

        File osLauncher = new File( location, "bin/" + ws + "/" + os + "/" + arch );

        try
        {
            // Don't copy eclipsec file
            IOFileFilter eclipsecFilter =
                FileFilterUtils.notFileFilter( FileFilterUtils.prefixFileFilter( "eclipsec" ) );
            FileUtils.copyDirectory( osLauncher, target, eclipsecFilter );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to copy launcher executable", e );
        }

        File launcher = getLauncher( environment, target );

        // make launcher executable
        try
        {
            getLog().debug( "running chmod" );
            ArchiveEntryUtils.chmod( launcher, 0755, null );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Unable to make launcher being executable", e );
        }

        File osxEclipseApp = null;

        // Rename launcher
        if ( productConfiguration.getLauncher() != null && productConfiguration.getLauncher().getName() != null )
        {
            String launcherName = productConfiguration.getLauncher().getName();
            String newName = launcherName;

            // win32 has extensions
            if ( PlatformPropertiesUtils.OS_WIN32.equals( os ) )
            {
                String extension = FilenameUtils.getExtension( launcher.getAbsolutePath() );
                newName = launcherName + "." + extension;
            }
            else if ( PlatformPropertiesUtils.OS_MACOSX.equals( os ) )
            {
                // the launcher is renamed to "eclipse", because
                // this is the value of the CFBundleExecutable
                // property within the Info.plist file.
                // see http://jira.codehaus.org/browse/MNGECLIPSE-1087
                newName = "eclipse";
            }

            getLog().debug( "Renaming launcher to " + newName );
            File newLauncher = new File( launcher.getParentFile(), newName );
            if ( !launcher.renameTo( newLauncher ) )
            {
                throw new MojoExecutionException( "Could not rename native launcher to " + newName );
            }
            launcher = newLauncher;

            // macosx: the *.app directory is renamed to the
            // product configuration launcher name
            // see http://jira.codehaus.org/browse/MNGECLIPSE-1087
            if ( PlatformPropertiesUtils.OS_MACOSX.equals( os ) )
            {
                newName = launcherName + ".app";
                getLog().debug( "Renaming Eclipse.app to " + newName );
                File eclipseApp = new File( target, "Eclipse.app" );
                osxEclipseApp = new File( eclipseApp.getParentFile(), newName );
                eclipseApp.renameTo( osxEclipseApp );
                // ToDo: the "Info.plist" file must be patched, so that the
                // property "CFBundleName" has the value of the
                // launcherName variable
            }
        }

        // icons
        if ( productConfiguration.getLauncher() != null )
        {
            if ( PlatformPropertiesUtils.OS_WIN32.equals( os ) )
            {
                getLog().debug( "win32 icons" );
                List<String> icons = productConfiguration.getW32Icons();

                if ( icons != null )
                {
                    getLog().debug( icons.toString() );
                    try
                    {
                        String[] args = new String[icons.size() + 1];
                        args[0] = launcher.getAbsolutePath();

                        int pos = 1;
                        for ( String string : icons )
                        {
                            args[pos] = string;
                            pos++;
                        }

                        IconExe.main( args );
                    }
                    catch ( Exception e )
                    {
                        throw new MojoExecutionException( "Unable to replace icons", e );
                    }
                }
                else
                {
                    getLog().debug( "icons is null" );
                }
            }
            else if ( PlatformPropertiesUtils.OS_LINUX.equals( os ) )
            {
                String icon = productConfiguration.getLinuxIcon();
                if ( icon != null )
                {
                    try
                    {
                        File sourceXPM = new File( project.getBasedir(), removeFirstSegment( icon ) );
                        File targetXPM = new File( launcher.getParentFile(), "icon.xpm" );
                        FileUtils.copyFile( sourceXPM, targetXPM );
                    }
                    catch ( IOException e )
                    {
                        throw new MojoExecutionException( "Unable to create ico.xpm", e );
                    }
                }
            }
            else if ( PlatformPropertiesUtils.OS_MACOSX.equals( os ) )
            {
                String icon = productConfiguration.getMacIcon();
                if ( icon != null )
                {
                    try
                    {
                        if ( osxEclipseApp == null )
                        {
                            osxEclipseApp = new File( target, "Eclipse.app" );
                        }

                        File source = new File( project.getBasedir(), removeFirstSegment( icon ) );
                        File targetFolder = new File( osxEclipseApp, "/Resources/" + source.getName() );

                        FileUtils.copyFile( source, targetFolder );
                        // Modify eclipse.ini
                        File iniFile = new File( osxEclipseApp + "/Contents/MacOS/eclipse.ini" );
                        if ( iniFile.exists() && iniFile.canWrite() )
                        {
                            StringBuffer buf = new StringBuffer( FileUtils.readFileToString( iniFile ) );
                            int pos = buf.indexOf( "Eclipse.icns" );
                            buf.replace( pos, pos + 12, source.getName() );
                            FileUtils.writeStringToFile( iniFile, buf.toString() );
                        }
                    }
                    catch ( Exception e )
                    {
                        throw new MojoExecutionException( "Unable to create macosx icon", e );
                    }
                }
            }
        }
    }

    private String removeFirstSegment( String path )
    {
        int idx = path.indexOf( '/' );
        if ( idx < 0 )
        {
            return null;
        }

        if ( idx == 0 )
        {
            idx = path.indexOf( '/', 1 );
        }

        if ( idx < 0 )
        {
            return null;
        }

        return path.substring( idx );
    }

    private File getLauncher( TargetEnvironment environment, File target )
        throws MojoExecutionException
    {
        String os = environment.getOs();

        if ( PlatformPropertiesUtils.OS_WIN32.equals( os ) )
        {
            return new File( target, "launcher.exe" );
        }

        if ( PlatformPropertiesUtils.OS_LINUX.equals( os ) || PlatformPropertiesUtils.OS_SOLARIS.equals( os )
            || PlatformPropertiesUtils.OS_HPUX.equals( os ) || PlatformPropertiesUtils.OS_AIX.equals( os ) )
        {
            return new File( target, "launcher" );
        }

        if ( PlatformPropertiesUtils.OS_MACOSX.equals( os ) )
        {
            // TODO need to check this at macos
            return new File( target, "Eclipse.app/Contents/MacOS/launcher" );
        }

        throw new MojoExecutionException( "Unexpected OS: " + os );
    }

    private void setPropertyIfNotNull( Properties properties, String key, String value )
    {
        if ( value != null )
        {
            properties.setProperty( key, value );
        }
    }
}
