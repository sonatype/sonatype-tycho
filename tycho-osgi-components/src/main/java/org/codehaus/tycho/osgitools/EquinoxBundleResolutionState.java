package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.ProjectType;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoConstants;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import copy.org.eclipse.core.runtime.internal.adaptor.PluginConverterImpl;

@Component( role = BundleResolutionState.class, instantiationStrategy = "per-lookup" )
public class EquinoxBundleResolutionState
    extends AbstractLogEnabled
    implements BundleResolutionState
{
    private static final String PROP_MANIFEST = "BundleManifest";

    private static StateObjectFactory factory = StateObjectFactory.defaultFactory;

    private final State state = factory.createState( true );

    private long nextBundleId;

    private File manifestsDir;

    public BundleDescription addBundle( File bundleLocation, boolean override )
        throws BundleException
    {
        if ( bundleLocation == null || !bundleLocation.exists() )
        {
            throw new IllegalArgumentException( "bundleLocation not found: " + bundleLocation );
        }
        Dictionary manifest = loadBundleManifest( bundleLocation );
        if ( manifest == null )
        {
            throw new BundleException( "Manifest not found in " + bundleLocation );
        }
        return addBundle( manifest, bundleLocation, override );
    }

    private BundleDescription addBundle( Dictionary enhancedManifest, File bundleLocation, boolean override )
        throws BundleException
    {
        BundleDescription descriptor;
        descriptor =
            factory.createBundleDescription( state, enhancedManifest, bundleLocation.getAbsolutePath(),
                                             getNextBundleId() );

        setUserProperty( descriptor, PROP_MANIFEST, enhancedManifest );

        if ( override )
        {
            BundleDescription[] conflicts = state.getBundles( descriptor.getSymbolicName() );
            if ( conflicts != null )
            {
                for ( BundleDescription conflict : conflicts )
                {
                    state.removeBundle( conflict );
                    getLogger().warn(
                                      conflict.toString()
                                          + " has been replaced by another bundle with the same symbolic name "
                                          + descriptor.toString() );
                }
            }
        }

        state.addBundle( descriptor );
        return descriptor;
    }

    private long getNextBundleId()
    {
        return nextBundleId++;
    }

    public void assertResolved( BundleDescription desc )
        throws BundleException
    {
        if ( !desc.isResolved() )
        {
            StringBuffer msg = new StringBuffer();
            msg.append( "Bundle " ).append( desc.getSymbolicName() ).append( " cannot be resolved\n" );
            msg.append( "Resolution errors:\n" );
            ResolverError[] errors = getResolverErrors( desc );
            for ( int i = 0; i < errors.length; i++ )
            {
                ResolverError error = errors[i];
                msg.append( "   Bundle " ).append( error.getBundle().getSymbolicName() ).append( " - " ).append(
                                                                                                                 error.toString() ).append(
                                                                                                                                            "\n" );
            }

            throw new BundleException( msg.toString() );
        }
    }

    public ResolverError[] getResolverErrors( BundleDescription bundle )
    {
        Set<ResolverError> errors = new LinkedHashSet<ResolverError>();
        getRelevantErrors( errors, bundle );
        return (ResolverError[]) errors.toArray( new ResolverError[errors.size()] );
    }

    private void getRelevantErrors( Set<ResolverError> errors, BundleDescription bundle )
    {
        ResolverError[] bundleErrors = state.getResolverErrors( bundle );
        for ( int j = 0; j < bundleErrors.length; j++ )
        {
            ResolverError error = bundleErrors[j];
            errors.add( error );

            VersionConstraint constraint = error.getUnsatisfiedConstraint();
            if ( constraint instanceof BundleSpecification || constraint instanceof HostSpecification )
            {
                BundleDescription[] requiredBundles = state.getBundles( constraint.getName() );
                for ( int i = 0; i < requiredBundles.length; i++ )
                {
                    getRelevantErrors( errors, requiredBundles[i] );
                }
            }
        }
    }

    public BundleDescription getBundle( String symbolicName, String version )
    {
        try
        {
            if ( TychoConstants.HIGHEST_VERSION == version )
            {
                return getLatestBundle( symbolicName );
            }
            return state.getBundle( symbolicName, new Version( version ) );
        }
        catch ( NumberFormatException e )
        {
            return null;
        }
        catch ( IllegalArgumentException e )
        {
            return null;
        }
    }

    private BundleDescription getLatestBundle( String symbolicName )
    {
        BundleDescription[] bundles = state.getBundles( symbolicName );
        BundleDescription highest = null;
        if ( bundles != null )
        {
            for ( BundleDescription desc : bundles )
            {
                if ( highest == null || highest.getVersion().compareTo( desc.getVersion() ) < 0 )
                {
                    highest = desc;
                }
            }
        }
        return highest;
    }

    public BundleDescription getBundleByLocation( File location )
    {
        String absolutePath = location.getAbsolutePath();
        return state.getBundleByLocation( absolutePath );
    }

    public List<BundleDescription> getDependencies( BundleDescription bundle )
    {
        Set<Long> bundleIds = new LinkedHashSet<Long>();
        addBundleAndDependencies( bundle, bundleIds, true );
        ArrayList<BundleDescription> dependencies = new ArrayList<BundleDescription>();
        for ( long bundleId : bundleIds )
        {
            if ( bundle.getBundleId() != bundleId )
            {
                BundleDescription dependency = state.getBundle( bundleId );
                if ( dependency != null )
                {
                    dependencies.add( dependency );
                }
            }
        }
        return dependencies;
    }

    /**
     * Code below is copy&paste from org.eclipse.pde.internal.core.DependencyManager which seems to calculate runtime
     * dependencies. In particular, it adds fragments' dependencies to the host bundle (see TychoTest#testFragment unit
     * test). This may or may not cause problems... RequiredPluginsClasspathContainer#computePluginEntries has the logic
     * to calculate compile-time dependencies in IDE. TODO find the code used by PDE/Build
     */
    private static void addBundleAndDependencies( BundleDescription desc, Set<Long> bundleIds, boolean includeOptional )
    {
        if ( desc != null && bundleIds.add( new Long( desc.getBundleId() ) ) )
        {
            BundleSpecification[] required = desc.getRequiredBundles();
            for ( int i = 0; i < required.length; i++ )
            {
                if ( includeOptional || !required[i].isOptional() )
                {
                    addBundleAndDependencies( (BundleDescription) required[i].getSupplier(), bundleIds, includeOptional );
                }
            }
            ImportPackageSpecification[] importedPkgs = desc.getImportPackages();
            for ( int i = 0; i < importedPkgs.length; i++ )
            {
                ExportPackageDescription exporter = (ExportPackageDescription) importedPkgs[i].getSupplier();
                // Continue if the Imported Package is unresolved of the package is optional and don't want optional
                // packages
                if ( exporter == null
                    || ( !includeOptional && Constants.RESOLUTION_OPTIONAL.equals( importedPkgs[i].getDirective( Constants.RESOLUTION_DIRECTIVE ) ) ) )
                {
                    continue;
                }
                addBundleAndDependencies( exporter.getExporter(), bundleIds, includeOptional );
            }
            BundleDescription[] fragments = desc.getFragments();
            for ( int i = 0; i < fragments.length; i++ )
            {
                if ( !fragments[i].isResolved() )
                {
                    continue;
                }
                String id = fragments[i].getSymbolicName();
                if ( !"org.eclipse.ui.workbench.compatibility".equals( id ) ) //$NON-NLS-1$
                {
                    addBundleAndDependencies( fragments[i], bundleIds, includeOptional );
                }
            }
            HostSpecification host = desc.getHost();
            if ( host != null )
            {
                addBundleAndDependencies( (BundleDescription) host.getSupplier(), bundleIds, includeOptional );
            }
        }
    }

    public StateHelper getStateHelper()
    {
        return state.getStateHelper();
    }

    public BundleDescription getSystemBundle()
    {
        return getLatestBundle( "org.eclipse.osgi" );
    }

    public List<BundleDescription> getBundles()
    {
        return Arrays.asList( state.getBundles() );
    }

    public Manifest loadManifest( File bundleLocation )
    {
        try
        {
            if ( bundleLocation.isDirectory() )
            {
                File m = new File( bundleLocation, JarFile.MANIFEST_NAME );
                if ( m.canRead() )
                {
                    return loadManifestFile( m );
                }
                m = convertPluginManifest( bundleLocation );
                if ( m != null && m.canRead() )
                {
                    return loadManifestFile( m );
                }
                return null;
            }

            // it's a file, make sure we can read it
            if ( !bundleLocation.canRead() )
            {
                return null;
            }

            // file but not a jar, assume it is MANIFEST.MF
            if ( !bundleLocation.getName().toLowerCase().endsWith( ".jar" ) )
            {
                return loadManifestFile( bundleLocation );
            }

            // it is a jar, lets see if it has OSGi bundle manifest
            ZipFile jar = new ZipFile( bundleLocation, ZipFile.OPEN_READ );
            try
            {
                ZipEntry me = jar.getEntry( JarFile.MANIFEST_NAME );
                if ( me != null )
                {
                    InputStream is = jar.getInputStream( me );
                    try
                    {
                        Manifest mf = new Manifest( is );
                        if ( mf.getMainAttributes().getValue( "Bundle-SymbolicName" ) != null )
                        {
                            return mf;
                        }
                    }
                    finally
                    {
                        is.close();
                    }
                }
            }
            finally
            {
                jar.close();
            }

            // it is a jar, does not have OSGi bundle manifest, lets try plugin.xml/fragment.xml
            File m = convertPluginManifest( bundleLocation );
            if ( m != null && m.canRead() )
            {
                return loadManifestFile( m );
            }
        }
        catch ( IOException e )
        {
            getLogger().warn( "Exception reading bundle manifest", e );
        }
        catch ( PluginConversionException e )
        {
            getLogger().warn( "Exception reading bundle manifest: " + e.getMessage() );
        }

        // not a bundle
        return null;
    }

    public String getManifestAttribute( BundleDescription bundle, String name )
    {
        Dictionary mf = (Dictionary) getUserProperty( bundle, PROP_MANIFEST );
        if ( mf != null )
        {
            return (String) mf.get( name );
        }
        return null;
    }

    public Dictionary loadBundleManifest( File bundleLocation )
    {
        Manifest m = loadManifest( bundleLocation );
        if ( m == null )
        {
            return null;
        }

        Dictionary manifest = manifestToProperties( m.getMainAttributes() );

        // enforce symbolic name
        if ( manifest.get( Constants.BUNDLE_SYMBOLICNAME ) == null )
        {
            // TODO maybe derive symbolic name from artifactId/groupId if we
            // have them?
            return null;
        }

        // enforce bundle classpath
        if ( manifest.get( Constants.BUNDLE_CLASSPATH ) == null )
        {
            manifest.put( Constants.BUNDLE_CLASSPATH, "." ); //$NON-NLS-1$
        }

        return manifest;
    }

    private static Properties manifestToProperties( Attributes d )
    {
        Iterator iter = d.keySet().iterator();
        Properties result = new Properties();
        while ( iter.hasNext() )
        {
            Attributes.Name key = (Attributes.Name) iter.next();
            result.put( key.toString(), d.get( key ) );
        }
        return result;
    }

    private Manifest loadManifestFile( File m )
        throws IOException
    {
        if ( !m.canRead() )
        {
            return null;
        }
        InputStream is = new FileInputStream( m );
        try
        {
            return new Manifest( is );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    private File convertPluginManifest( File bundleLocation )
        throws PluginConversionException
    {
        PluginConverterImpl converter = new PluginConverterImpl( null, null );
        String name = bundleLocation.getName();
        if ( name.endsWith( ".jar" ) )
        {
            name = name.substring( 0, name.length() - 4 );
        }
        File manifestFile = new File( manifestsDir, name + "/META-INF/MANIFEST.MF" );
        manifestFile.getParentFile().mkdirs();
        converter.convertManifest( bundleLocation, manifestFile, false /* compatibility */, "3.2" /* target version */,
                                   true /* analyse jars to set export-package */, null /* devProperties */);
        if ( manifestFile.exists() )
        {
            return manifestFile;
        }
        return null;
    }

    public static EquinoxBundleResolutionState newInstance( PlexusContainer plexus, MavenSession session,
                                                            MavenProject project )
    {
        try
        {
            EquinoxBundleResolutionState resolver =
                (EquinoxBundleResolutionState) plexus.lookup( BundleResolutionState.class );

            Set<File> basedirs = new HashSet<File>();
            for ( MavenProject sessionProject : session.getProjects() )
            {
                basedirs.add( sessionProject.getBasedir() );
            }

            TargetPlatform platform = (TargetPlatform) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM );

            File manifestsDir = new File( project.getBuild().getDirectory(), "manifests" );
            manifestsDir.mkdirs();

            resolver.setManifestsDir( manifestsDir );

            for ( File file : platform.getArtifactFiles( ProjectType.OSGI_BUNDLE ) )
            {
                boolean isProject = basedirs.contains( file );
                resolver.addBundle( file, isProject );
            }

            for ( File file : platform.getArtifactFiles( ProjectType.ECLIPSE_TEST_PLUGIN ) )
            {
                boolean isProject = basedirs.contains( file );
                resolver.addBundle( file, isProject );
            }

            resolver.setPlatformProperties( platform.getProperties() );

            resolver.resolve( project );

            return resolver;
        }
        catch ( ComponentLookupException e )
        {
            throw new RuntimeException( "Could not lookup required component", e );
        }
        catch ( BundleException e )
        {
            throw new RuntimeException( "Unable to initialize BundleResolutionState", e );
        }
    }

    public void setManifestsDir( File manifestsDir )
    {
        this.manifestsDir = manifestsDir;
    }

    public File getManifestsDir()
    {
        return manifestsDir;
    }

    public void resolve( MavenProject project )
    {
        state.resolve( false );

        if ( getLogger().isDebugEnabled() )
        {
            StringBuilder sb = new StringBuilder( "Resolved OSGi state\n" );
            for ( BundleDescription bundle : state.getBundles() )
            {
                if ( !bundle.isResolved() )
                {
                    sb.append( "NOT " );
                }
                sb.append( "RESOLVED " );
                sb.append( bundle.toString() ).append( " : " ).append( bundle.getLocation() );
                sb.append( '\n' );
                for ( ResolverError error : state.getResolverErrors( bundle ) )
                {
                    sb.append( '\t' ).append( error.toString() ).append( '\n' );
                }
            }

            getLogger().debug( sb.toString() );
        }

    }

    public void setPlatformProperties( Properties properties )
    {
        Properties effective = new Properties();
        effective.putAll( properties );
        // Put Equinox OSGi resolver into development mode.
        // See http://www.nabble.com/Re:-resolving-partially-p18449054.html
        effective.put( org.eclipse.osgi.framework.internal.core.Constants.OSGI_RESOLVER_MODE,
                       org.eclipse.osgi.framework.internal.core.Constants.DEVELOPMENT_MODE );
        state.setPlatformProperties( effective );
    }

    private static void setUserProperty( BundleDescription desc, String name, Object value )
    {
        Object userObject = desc.getUserObject();

        if ( userObject != null && !( userObject instanceof Map ) )
        {
            throw new IllegalStateException( "Unexpected user object " + desc.toString() );
        }

        Map props = (Map) userObject;
        if ( props == null )
        {
            props = new HashMap();
            desc.setUserObject( props );
        }

        props.put( name, value );
    }

    private static Object getUserProperty( BundleDescription desc, String name )
    {
        if ( desc == null )
        {
            return null;
        }
        Object userObject = desc.getUserObject();
        if ( userObject instanceof Map )
        {
            return ( (Map) userObject ).get( name );
        }
        return null;
    }

}
