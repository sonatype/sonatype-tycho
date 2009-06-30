package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.ArchiveEntryUtils;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.tycho.MavenSessionUtils;
import org.codehaus.tycho.PlatformPropertiesUtils;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.maven.DependenciesReader;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.ProductConfiguration;
import org.codehaus.tycho.model.Feature.FeatureRef;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Version;

/**
 * @goal product-export
 */
public class ProductExportMojo
    extends AbstractTychoPackagingMojo
{

    /** @parameter expression="${project.build.directory}/product" */
    private File target;

    /** @parameter expression="${project.build.directory}/product/plugins" */
    private File pluginsFolder;

    /** @parameter expression="${project.build.directory}/product/features" */
    private File featuresFolder;

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
     * Build qualifier. Recommended way to set this parameter is using
     * build-qualifier goal. 
     * 
     * @parameter expression="${buildQualifier}"
     */
    protected String qualifier;

    /**
     * @component roleHint="eclipse-application"
     */
    private DependenciesReader rcpDependencyReader;

    private TargetPlatform platform;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        initializeProjectContext();
        platform = (TargetPlatform) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM );

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

        generateEclipseProduct();
        generateConfigIni();
        includeRootFiles();

        pluginsFolder.mkdirs();
        if ( productConfiguration.getUseFeatures() )
        {
            featuresFolder.mkdirs();
            copyFeatures( productConfiguration.getFeatures() );
        }
        else
        {
            copyPlugins( productConfiguration.getPlugins() );
        }
        
        copyExecutable();

        packProduct();

        Version productVersion = Version.parseVersion( productConfiguration.getVersion() );
        productVersion = VersioningHelper.expandVersion( productVersion, qualifier );
        productConfiguration.setVersion( productVersion.toString() );

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
    }

    @Override
    protected void initializeProjectContext()
    {
        bundleResolutionState = rcpDependencyReader.getBundleResolutionState( session, project );
        featureResolutionState = rcpDependencyReader.getFeatureResolutionState( session, project );
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
     * 
     * @throws MojoExecutionException
     */
    private void includeRootFiles()
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
                    String config = getConfig();
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
                            handleRootEntry( entry.getValue(), null );
                        }
                        // root.xxx=
                        else if ( rootConfig.equals( key ) )
                        {
                            handleRootEntry( entry.getValue(), null );
                        }
                        // root.folder.yyy=
                        else if ( key.startsWith( rootFolder ) )
                        {
                            String subFolder = entry.getKey().substring( ( rootFolder.length() ) );
                            handleRootEntry( entry.getValue(), subFolder );
                        }
                        // root.xxx.folder.yyy=
                        else if ( key.startsWith( rootConfigFolder ) )
                        {
                            String subFolder = entry.getKey().substring( ( rootConfigFolder.length() ) );
                            handleRootEntry( entry.getValue(), subFolder );
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
    private void handleRootEntry( String rootFileEntries, String subFolder )
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

    private String getConfig()
    {
        String ws = platform.getProperty( PlatformPropertiesUtils.OSGI_WS );
        String os = platform.getProperty( PlatformPropertiesUtils.OSGI_OS );
        String arch = platform.getProperty( PlatformPropertiesUtils.OSGI_ARCH );
        StringBuffer config = new StringBuffer( ws ).append( "." ).append( os ).append( "." ).append( arch );
        return config.toString();
    }

    private void packProduct()
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

        File destFile = new File( project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".zip" );

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

        project.getArtifact().setFile( destFile );
    }

    private boolean isEclipse32Platform()
        throws MojoFailureException
    {
        BundleDescription runtime = bundleResolutionState.getSystemBundle();

        if ( !"org.eclipse.osgi".equals( runtime.getSymbolicName() ) )
        {
            throw new MojoFailureException( "Unsupported OSGi platform " + runtime.getSymbolicName() );
        }

        Version osgiVersion = runtime.getVersion();
        return osgiVersion.getMajor() == 3 && osgiVersion.getMinor() == 2;
    }

    private void generateEclipseProduct()
        throws MojoExecutionException
    {
        getLog().debug( "Generating .eclipseproduct" );
        Properties props = new Properties();
        setPropertyIfNotNull( props, "version", productConfiguration.getVersion() );
        setPropertyIfNotNull( props, "name", productConfiguration.getName() );
        setPropertyIfNotNull( props, "id", productConfiguration.getId() );

        target.mkdirs();

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

    private void generateConfigIni()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().debug( "Generating config.ini" );
        Properties props = new Properties();
        String splash = productConfiguration.getId().split( "\\." )[0];
        setPropertyIfNotNull( props, "osgi.splashPath", "platform:/base/plugins/" + splash );

        setPropertyIfNotNull( props, "eclipse.product", productConfiguration.getId() );
        // TODO check if there are any other levels
        setPropertyIfNotNull( props, "osgi.bundles.defaultStartLevel", "4" );

        if ( productConfiguration.getUseFeatures() )
        {
            setPropertyIfNotNull( props, "osgi.bundles", getFeaturesOsgiBundles() );
        }
        else
        {
            setPropertyIfNotNull( props, "osgi.bundles", getPluginsOsgiBundles() );
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

    private String getPluginsOsgiBundles()
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

            if ( isEclipse32Platform() && "org.eclipse.equinox.common".equals( plugin.getId() ) )
            {
                buf.append( "@2:start" );
            }
        }

        // required plugins, RCP didn't start without both
        if ( !isEclipse32Platform() )
        {
            if ( buf.length() != 0 )
            {
                buf.append( ',' );
            }
            String ws = platform.getProperty( PlatformPropertiesUtils.OSGI_WS );
            String os = platform.getProperty( PlatformPropertiesUtils.OSGI_OS );
            String arch = platform.getProperty( PlatformPropertiesUtils.OSGI_ARCH );

            buf.append( "org.eclipse.equinox.launcher," );
            buf.append( "org.eclipse.equinox.launcher." + ws + "." + os + "." + arch );
        }

        return buf.toString();
    }

    private void copyFeatures( List<FeatureRef> features )
        throws MojoExecutionException
    {
        getLog().debug( "copying " + features.size() + " features " );

        for ( FeatureRef feature : features )
        {
            copyFeature( feature );
        }
    }

    private void copyFeature( FeatureRef feature )
        throws MojoExecutionException
    {
        String featureId = feature.getId();
        String featureVersion = feature.getVersion();

        FeatureDescription featureDescription = featureResolutionState.getFeature( featureId, featureVersion );
        if ( featureDescription == null )
        {
            throw new MojoExecutionException( "Unable to resolve feature " + featureId + "_" + featureVersion );
        }

        featureVersion = VersioningHelper.getExpandedVersion( session, featureDescription );

        feature.setVersion( featureVersion );

        Feature featureRef = featureDescription.getFeature();

        MavenProject project = MavenSessionUtils.getMavenProject( session, featureDescription.getLocation() );

        de.schlichtherle.io.File source;
        if ( project == null )
        {
            getLog().debug( "feature = bundle: " + featureDescription.getLocation() );
            source = new de.schlichtherle.io.File( featureDescription.getLocation() );
        }
        else
        {
            getLog().debug( "feature = project: " + project.getArtifact() );
            source = new de.schlichtherle.io.File( project.getArtifact().getFile() );
        }

        File targetFolder = new File( featuresFolder, featureRef.getId() + "_" + featureVersion );
        targetFolder.mkdirs();

        source.copyAllTo( targetFolder );

        List<FeatureRef> featureRefs = featureRef.getIncludedFeatures();
        for ( FeatureRef fRef : featureRefs )
        {
            copyFeature( fRef );
        }

        // copy all plugins from all features
        List<PluginRef> pluginRefs = featureRef.getPlugins();
        for ( PluginRef pluginRef : pluginRefs )
        {
            if ( matchCurrentPlataform( pluginRef ) )
            {
                copyPlugin( pluginRef );
            }
        }

    }

    private boolean matchCurrentPlataform( PluginRef pluginRef )
    {
        String ws = platform.getProperty( PlatformPropertiesUtils.OSGI_WS );
        String os = platform.getProperty( PlatformPropertiesUtils.OSGI_OS );
        String arch = platform.getProperty( PlatformPropertiesUtils.OSGI_ARCH );

        String pluginWs = pluginRef.getWs();
        String pluginOs = pluginRef.getOs();
        String pluginArch = pluginRef.getArch();

        return ( pluginWs == null || ws.equals( pluginWs ) ) && //
            ( pluginOs == null || os.equals( pluginOs ) ) && //
            ( pluginArch == null || arch.equals( pluginArch ) );
    }

    private void copyPlugins( Collection<PluginRef> plugins )
        throws MojoExecutionException, MojoFailureException
    {
        getLog().debug( "copying " + plugins.size() + " plugins " );

        for ( PluginRef plugin : plugins )
        {
            copyPlugin( plugin );
        }

        // required plugins, RCP didn't start without both
        if ( !isEclipse32Platform() )
        {
            String ws = platform.getProperty( PlatformPropertiesUtils.OSGI_WS );
            String os = platform.getProperty( PlatformPropertiesUtils.OSGI_OS );
            String arch = platform.getProperty( PlatformPropertiesUtils.OSGI_ARCH );

            copyPlugin( newPluginRef( "org.eclipse.equinox.launcher", null, false ) );
            // for Mac OS X there is no org.eclipse.equinox.launcher.carbon.macosx.x86 folder,
            // only a org.eclipse.equinox.launcher.carbon.macosx folder.
            // see http://jira.codehaus.org/browse/MNGECLIPSE-1075
            if ( PlatformPropertiesUtils.OS_MACOSX.equals( os ) )
            {
                copyPlugin( newPluginRef( "org.eclipse.equinox.launcher." + ws + "." + os, null, false ) );
            }
            else
            {
                copyPlugin( newPluginRef( "org.eclipse.equinox.launcher." + ws + "." + os + "." + arch, null, false ) );
            }
        }
    }

    private PluginRef newPluginRef( String id, String version, boolean unpack )
    {
        PluginRef plugin = new PluginRef( "plugin" );
        plugin.setId( id );
        if ( version != null )
        {
            plugin.setVersion( version );
        }
        plugin.setUnpack( unpack );
        return plugin;
    }

    private String copyPlugin( PluginRef plugin )
        throws MojoExecutionException
    {
        String bundleId = plugin.getId();
        String bundleVersion = plugin.getVersion();
        
        getLog().debug( "Copying plugin " + bundleId + "_" + bundleVersion );

        if ( bundleVersion == null || "0.0.0".equals( bundleVersion ) )
        {
            bundleVersion = TychoConstants.HIGHEST_VERSION;
        }

        BundleDescription bundle = bundleResolutionState.getBundle( bundleId, bundleVersion );
        if ( bundle == null )
        {
            throw new MojoExecutionException( "Plugin '" + bundleId + "_" + bundleVersion + "' not found!" );
        }

        MavenProject bundleProject = MavenSessionUtils.getMavenProject( session, bundle.getLocation() );
        File source;
        if ( bundleProject != null )
        {
            source = bundleProject.getArtifact().getFile();
        }
        else
        {
            source = new File( bundle.getLocation() );
        }

        bundleVersion = VersioningHelper.getExpandedVersion( session, bundle );
        plugin.setVersion( bundleVersion );

        File target = new File( pluginsFolder, bundleId + "_" + bundleVersion + ".jar" );
        if ( source.isDirectory() )
        {
            copyToDirectory( source, pluginsFolder );
        }
        else if ( plugin.isUnpack() )
        {
            // when unpacked doesn't have .jar extension
            String path = target.getAbsolutePath();
            path = path.substring( 0, path.length() - 4 );
            target = new File( path );
            new de.schlichtherle.io.File( source ).copyAllTo( target );
            // unpackToDirectory(source, target);
        }
        else
        {
            copyToFile( source, target );
        }

        return bundleVersion;
    }

    private void copyToFile( File source, File target )
        throws MojoExecutionException
    {
        try
        {
            target.getParentFile().mkdirs();

            if ( source.isFile() )
            {
                FileUtils.copyFile( source, target );
            }
            else if ( source.isDirectory() )
            {
                FileUtils.copyDirectory( source, target );
            }
            else
            {
                getLog().warn( "Skipping bundle " + source.getAbsolutePath() );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to copy " + source.getName(), e );
        }
    }

    private void copyToDirectory( File source, File targetFolder )
        throws MojoExecutionException
    {
        try
        {
            if ( source.isFile() )
            {
                FileUtils.copyFileToDirectory( source, targetFolder );
            }
            else if ( source.isDirectory() )
            {
                FileUtils.copyDirectoryToDirectory( source, targetFolder );
            }
            else
            {
                getLog().warn( "Skipping bundle " + source.getAbsolutePath() );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to copy " + source.getName(), e );
        }
    }

    private void copyExecutable()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().debug( "Creating launcher" );

        FeatureDescription feature;
        // eclipse 3.2
        if ( isEclipse32Platform() )
        {
            feature = featureResolutionState.getFeature( "org.eclipse.platform.launchers", null );
        }
        else
        {
            feature = featureResolutionState.getFeature( "org.eclipse.equinox.executable", null );
        }

        if ( feature == null )
        {
            throw new MojoExecutionException( "RPC delta feature not found!" );
        }

        File location = feature.getLocation();

        String ws = platform.getProperty( PlatformPropertiesUtils.OSGI_WS );
        String os = platform.getProperty( PlatformPropertiesUtils.OSGI_OS );
        String arch = platform.getProperty( PlatformPropertiesUtils.OSGI_ARCH );

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

        File launcher = getLauncher();

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
            launcher.renameTo( new File( launcher.getParentFile(), newName ) );

            // macosx: the *.app directory is renamed to the
            // product configuration launcher name
            // see http://jira.codehaus.org/browse/MNGECLIPSE-1087
            if ( PlatformPropertiesUtils.OS_MACOSX.equals( os ) )
            {
                newName = launcherName + ".app";
                getLog().debug( "Renaming Eclipse.app to " + newName );
                File eclipseApp = new File( target, "Eclipse.app" );
                File renamedEclipseApp = new File( eclipseApp.getParentFile(), newName );
                eclipseApp.renameTo( renamedEclipseApp );
                // ToDo: the "Info.plist" file must be patched, so that the
                // property "CFBundleName" has the value of the
                // launcherName variable
            }
        }

        // eclipse 3.2
        if ( isEclipse32Platform() )
        {
            File startUpJar = new File( location, "bin/startup.jar" );
            try
            {
                FileUtils.copyFileToDirectory( startUpJar, target );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Unable to copy startup.jar executable", e );
            }
        }

    }

    private File getLauncher()
        throws MojoExecutionException
    {
        String os = platform.getProperty( PlatformPropertiesUtils.OSGI_OS );

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
