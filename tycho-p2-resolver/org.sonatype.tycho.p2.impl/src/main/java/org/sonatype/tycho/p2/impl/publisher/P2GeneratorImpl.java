package org.sonatype.tycho.p2.impl.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.updatesite.CategoryParser;
import org.eclipse.equinox.internal.p2.updatesite.SiteModel;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.osgi.framework.BundleException;
import org.sonatype.tycho.p2.IArtifactFacade;
import org.sonatype.tycho.p2.IProjectArtifactFacade;
import org.sonatype.tycho.p2.P2Generator;
import org.sonatype.tycho.p2.impl.publisher.model.ProductFile2;
import org.sonatype.tycho.p2.impl.publisher.repo.TransientArtifactRepository;
import org.sonatype.tycho.p2.maven.repository.xmlio.ArtifactsIO;
import org.sonatype.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.sonatype.tycho.p2.resolver.P2Resolver;

@SuppressWarnings( "restriction" )
public class P2GeneratorImpl
    implements P2Generator
{
    private static final String SUFFIX_QUALIFIER = ".qualifier";
    private static final String SUFFIX_SNAPSHOT = "-SNAPSHOT";

    private static final String[] SUPPORTED_TYPES = { P2Resolver.TYPE_ECLIPSE_PLUGIN,
        P2Resolver.TYPE_ECLIPSE_TEST_PLUGIN, P2Resolver.TYPE_ECLIPSE_FEATURE, P2Resolver.TYPE_ECLIPSE_UPDATE_SITE,
        P2Resolver.TYPE_ECLIPSE_APPLICATION, P2Resolver.TYPE_ECLIPSE_REPOSITORY };

    /**
     * Whether we need full p2 metadata (false) or just required capabilities.
     */
    private boolean dependenciesOnly;

    private IProgressMonitor monitor = new NullProgressMonitor();

    public P2GeneratorImpl( boolean dependenciesOnly )
    {
        this.dependenciesOnly = dependenciesOnly;
    }

    public P2GeneratorImpl()
    {
        this( false );
    }

    public void generateMetadata( IArtifactFacade artifact, File content, File artifacts )
        throws IOException
    {
        LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        LinkedHashSet<IArtifactDescriptor> artifactDescriptors = new LinkedHashSet<IArtifactDescriptor>();

        generateMetadata( artifact, null, units, artifactDescriptors );

        new MetadataIO().writeXML( units, content );
        new ArtifactsIO().writeXML( artifactDescriptors, artifacts );
    }

    private IRequirement[] extractExtraEntriesAsIURequirement( File location )
    {
        Properties buildProperties = loadProperties( location );
        if ( buildProperties == null || buildProperties.size() == 0 )
            return null;
        ArrayList<IRequirement> result = new ArrayList<IRequirement>();
        Set<Entry<Object, Object>> pairs = buildProperties.entrySet();
        for ( Entry<Object, Object> pair : pairs )
        {
            if ( !( pair.getValue() instanceof String ) )
                continue;
            String buildPropertyKey = (String) pair.getKey();
            if ( buildPropertyKey.startsWith( "extra." ) )
            {
                createRequirementFromExtraClasspathProperty( result, ( (String) pair.getValue() ).split( "," ) );
            }
        }

        String extra = buildProperties.getProperty( "jars.extra.classpath" );
        if ( extra != null )
        {
            createRequirementFromExtraClasspathProperty( result, extra.split( "," ) );
        }
        if ( result.isEmpty() )
            return null;
        return result.toArray( new IRequirement[result.size()] );
    }

    private void createRequirementFromExtraClasspathProperty( ArrayList<IRequirement> result, String[] urls )
    {
        for ( int i = 0; i < urls.length; i++ )
        {
            createRequirementFromPlatformURL( result, urls[i].trim() );
        }
    }

    private void createRequirementFromPlatformURL( ArrayList<IRequirement> result, String url )
    {
        Pattern platformURL = Pattern.compile( "platform:/(plugin|fragment)/([^/]*)(/)*.*" );
        Matcher m = platformURL.matcher( url );
        if ( m.matches() )
            result.add( MetadataFactory.createRequirement( IInstallableUnit.NAMESPACE_IU_ID, m.group( 2 ),
                                                                  VersionRange.emptyRange, null, false, false ) );
    }

    private static Properties loadProperties( File project )
    {
        File file = new File( project, "build.properties" );

        Properties buildProperties = new Properties();
        if ( file.canRead() )
        {
            InputStream is = null;
            try
            {
                try
                {
                    is = new FileInputStream( file );
                    buildProperties.load( is );
                }
                finally
                {
                    if ( is != null )
                        is.close();
                }
            }
            catch ( Exception e )
            {
                // ignore
            }
        }

        return buildProperties;
    }

    public void generateMetadata( IArtifactFacade artifact, List<Map<String,String>> environments, Set<IInstallableUnit> units,
                                  Set<IArtifactDescriptor> artifacts)
    {
        TransientArtifactRepository artifactsRepository = new TransientArtifactRepository();
        PublisherInfo publisherInfo = createPublisherInfo(artifact,
				artifactsRepository);
        publisherInfo.addAdvice( new MavenPropertiesAdvice( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()) );
        IPublisherAction[] actions = getPublisherActions( artifact, environments , false);
        publish(units, artifacts, artifactsRepository, publisherInfo, actions);
        if (artifact.hasSourceBundle()) {
            publisherInfo = createPublisherInfo(artifact,
    				artifactsRepository);
			publisherInfo.addAdvice(new MavenPropertiesAdvice(artifact
					.getGroupId(), artifact.getArtifactId(), artifact
					.getVersion(), "sources"));
            actions = getPublisherActions( artifact, environments, true );
            publish(units, artifacts, artifactsRepository, publisherInfo, actions);
        }
    }

	private void publish(Set<IInstallableUnit> units,
			Set<IArtifactDescriptor> artifacts,
			TransientArtifactRepository artifactsRepository,
			PublisherInfo publisherInfo, IPublisherAction[] actions) {
		PublisherResult result = new PublisherResult();

        IStatus status = new Publisher( publisherInfo, result ).publish( actions, monitor );

        if ( !status.isOK() )
        {
            throw new RuntimeException( new CoreException( status ) );
        }

        if ( units != null )
        {
            units.addAll( result.getIUs( null, null ) );
        }

        if ( artifacts != null )
        {
            artifacts.addAll( artifactsRepository.getArtifactDescriptors() );
        }
	}

	private PublisherInfo createPublisherInfo(IArtifactFacade artifact,
			TransientArtifactRepository artifactsRepository) {
		PublisherInfo request = new PublisherInfo();
        request.setArtifactRepository( artifactsRepository );

        final IRequirement[] extraRequirements = extractExtraEntriesAsIURequirement( artifact.getLocation());
        request.addAdvice( new ICapabilityAdvice()
        {

            public boolean isApplicable( String configSpec, boolean includeDefault, String id, Version version )
            {
                return true;
            }

            public IRequirement[] getRequiredCapabilities( InstallableUnitDescription iu )
            {
                return extraRequirements;
            }

            public IProvidedCapability[] getProvidedCapabilities( InstallableUnitDescription iu )
            {
                return null;
            }

            public IRequirement[] getMetaRequiredCapabilities( InstallableUnitDescription iu )
            {
                return null;
            }
        } );
		return request;
	}

    private IPublisherAction[] getPublisherActions( IArtifactFacade artifact, List<Map<String,String>> environments, boolean isSourceBundle )
    {
    	String packaging = artifact.getPackagingType();
    	File location = artifact.getLocation();
        if ( P2Resolver.TYPE_ECLIPSE_PLUGIN.equals( packaging )
            || P2Resolver.TYPE_ECLIPSE_TEST_PLUGIN.equals( packaging ) )
        {
			if (isSourceBundle) {
				return new IPublisherAction[] { createSourceBundleAction(artifact) };
			} else {
				return new IPublisherAction[] { new BundlesAction(
						new File[] { location }) };
			}
        }
        else if ( P2Resolver.TYPE_ECLIPSE_FEATURE.equals( packaging ) )
        {
            Feature feature = new FeatureParser().parse( location );
            feature.setLocation( location.getAbsolutePath() );
            if ( dependenciesOnly )
            {
                return new IPublisherAction[] { new FeatureDependenciesAction( feature ) };
            }
            else
            {
                return new IPublisherAction[] { new FeaturesAction( new Feature[] { feature } ) };
            }
        }
        else if ( P2Resolver.TYPE_ECLIPSE_APPLICATION.equals( packaging ) )
        {
            String product = new File( location, artifact.getArtifactId() + ".product" ).getAbsolutePath();
            try
            {
                IProductDescriptor productDescriptor = new ProductFile2( product );
                if ( dependenciesOnly )
                {
                    return new IPublisherAction[] { new ProductDependenciesAction( productDescriptor, environments ) };
                }
                else
                {
                    return new IPublisherAction[] { new ProductAction( product, productDescriptor, null, null ) };
                }
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
        }
        else if ( P2Resolver.TYPE_ECLIPSE_UPDATE_SITE.equals( packaging ) )
        {
            if ( dependenciesOnly )
            {
                return new IPublisherAction[] { new SiteDependenciesAction( location, artifact.getArtifactId(), artifact.getVersion()) };
            }
            else
            {
                return new IPublisherAction[] { new SiteXMLAction( location.toURI(), null ) };
            }
        }
        else if ( P2Resolver.TYPE_ECLIPSE_REPOSITORY.equals( packaging ) )
        {
            List<IPublisherAction> actions = new ArrayList<IPublisherAction>();
            for ( File productFile : getProductFiles( location ) )
            {
                String product = productFile.getAbsolutePath();
                IProductDescriptor productDescriptor;
                try
                {
                    productDescriptor = new ProductFile2( product );
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( "Unable to parse the product file " + product, e );
                }
                if ( dependenciesOnly )
                {
                    actions.add( new ProductDependenciesAction( productDescriptor, environments ) );
                }
            }
            for ( File categoryFile : getCategoryFiles( location ) )
            {
                CategoryParser cp = new CategoryParser( null );
                FileInputStream ins = null;
                try
                {
                    try
                    {
                        ins = new FileInputStream( categoryFile );
                        SiteModel siteModel = cp.parse( ins );
                        actions.add( new CategoryDependenciesAction( siteModel, artifact.getArtifactId(),
                                                                     artifact.getVersion() ) );
                    }
                    finally
                    {
                        if ( ins != null )
                        {
                            ins.close();
                        }
                    }
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( "Unable to read category File", e );
                }
            }
            return actions.toArray( new IPublisherAction[actions.size()] );
        }
        else if ( location.isFile() && location.getName().endsWith( ".jar" ) )
        {
            return new IPublisherAction[] { new BundlesAction( new File[] { location } ) };
        }

        throw new IllegalArgumentException("Unknown type of packaging " + packaging);
    }



	private IPublisherAction createSourceBundleAction(IArtifactFacade artifact) {
		String id = artifact.getArtifactId();
		String version = toCanonicalVersion(artifact.getVersion());
		try {
			if (artifact instanceof IProjectArtifactFacade) {
				return new BundlesAction(
						new File[] { ((IProjectArtifactFacade) artifact)
								.getSourceArtifactLocation() });
			} else {
				// generated source bundle is not available at this point in filesystem yet, need to create 
				// in-memory BundleDescription instead
				Dictionary<String, String> manifest = new Hashtable<String, String>();
				manifest.put("Manifest-Version", "1.0");
				manifest.put("Bundle-ManifestVersion", "2");
				String sourceBundleSymbolicName = id
						+ artifact.getSourceBundleSuffix();
				manifest.put("Bundle-SymbolicName", sourceBundleSymbolicName);
				manifest.put("Bundle-Version", version);
				manifest.put("Eclipse-SourceBundle", id + ";version=" + version
						+ ";roots:=\".\"");
				StateObjectFactory factory = StateObjectFactory.defaultFactory;
				BundleDescription bundleDescription = factory
						.createBundleDescription(factory.createState(false),
								manifest, artifact.getLocation().getAbsolutePath(),
								createId(sourceBundleSymbolicName, version));
				bundleDescription.setUserObject(manifest);
				return new BundlesAction(
						new BundleDescription[] { bundleDescription });
			}
		} catch (BundleException e) {
			throw new RuntimeException(e);
		}
	}

	public long createId(String sourceBundleSymbolicName, String version) {
		return (long)sourceBundleSymbolicName.hashCode() | (((long)version.hashCode()) << 32);
	}

	private static String toCanonicalVersion(String version) {
		if (version == null) {
			return null;
		}
		if (version.endsWith(SUFFIX_SNAPSHOT)) {
			return version.substring(0,
					version.length() - SUFFIX_SNAPSHOT.length())
					+ SUFFIX_QUALIFIER;
		}
		return version;
	}

    public boolean isSupported( String type )
    {
        return Arrays.asList( SUPPORTED_TYPES ).contains( type );
    }
    
    /**
     * Looks for all files at the base of the project that extension is ".product"
     * Duplicated in the EclipseRepositoryProject
     * @param projectLocation
     * @return The list of product files to parse for an eclipse-repository project
     */
    private List<File> getProductFiles( File projectLocation)
    {
    	List<File> res = new ArrayList<File>();
    	for (File f : projectLocation.listFiles())
    	{
    		if (f.isFile() && f.getName().endsWith(".product"))
    		{
    			res.add(f);
    		}
    	}
    	return res;
    }
    
	private List<File> getCategoryFiles(File projectLocation)
	{
		List<File> res = new ArrayList<File>();
		File categoryFile = new File(projectLocation, "category.xml");
		if (categoryFile.exists())
		{
			res.add(categoryFile);
		}
		return res;
	}
}
