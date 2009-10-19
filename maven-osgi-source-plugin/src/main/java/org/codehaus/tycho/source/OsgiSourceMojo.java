package org.codehaus.tycho.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.TychoConstants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Version;

/**
 * Goal to create a JAR-package containing all the source files of a osgi
 * project.
 * 
 * @extendsPlugin source
 * @extendsGoal jar
 * @goal plugin-source
 * @phase package
 */
public class OsgiSourceMojo extends AbstractSourceJarMojo {
    
    private static final String BUNDLE_SYMBOLIC_NAME_SUFFIX = ".source";
    private static final String MANIFEST_HEADER_BUNDLE_MANIFEST_VERSION = "Bundle-ManifestVersion";
    private static final String MANIFEST_HEADER_BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
    private static final String MANIFEST_HEADER_BUNDLE_VERSION = "Bundle-Version";
    private static final String MANIFEST_HEADER_ECLIPSE_SOURCE_BUNDLE = "Eclipse-SourceBundle";
    private static final String VERSION_QUALIFIER = "qualifier";

	/**
	 * If set to true, compiler will use source folders defined in
	 * build.properties file and will ignore
	 * ${project.compileSourceRoots}/${project.testCompileSourceRoots}.
	 * 
	 * Compilation will fail with an error, if this parameter is set to true but
	 * the project does not have valid build.properties file.
	 * 
	 * @parameter default-value="true"
	 */
	private boolean usePdeSourceRoots;

    /**
     * Whether the source jar should be an Eclipse source bundle.
     * 
     * @parameter default-value="true"
     */
    private boolean sourceBundle;

    /**
     * Build qualifier. Recommended way to set this parameter is using
     * build-qualifier goal. Only used when creating a source bundle. 
     * 
     * @parameter expression="${buildQualifier}"
     */
    private String qualifier;


	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	protected List<String> getSources(MavenProject p)
			throws MojoExecutionException {
		if (usePdeSourceRoots) {
			Properties props = getBuildProperties();
			if (props.containsKey("source..")) {
				String sourceRaw = props.getProperty("source..");
				List<String> sources = new ArrayList<String>();
				for (String source : sourceRaw.split(",")) {
					sources.add(new File(project.getBasedir(), source)
							.getAbsolutePath());
				}
				return sources;
			} else {
				throw new MojoExecutionException(
						"Source folder not found at build.properties");
			}
		} else {
			return p.getCompileSourceRoots();
		}
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	protected List getResources(MavenProject p) {
		if (excludeResources) {
			return Collections.EMPTY_LIST;
		}
		if (usePdeSourceRoots) {
			return Collections.EMPTY_LIST;
		}

		return p.getResources();
	}

	/** {@inheritDoc} */
	protected String getClassifier() {
		return "sources";
	}

	// TODO check how to fix this code duplicated
	private Properties getBuildProperties() throws MojoExecutionException {
		File file = new File(project.getBasedir(), "build.properties");
		if (!file.canRead()) {
			throw new MojoExecutionException(
					"Unable to read build.properties file");
		}

		Properties buildProperties = new Properties();
		try {
			InputStream is = new FileInputStream(file);
			try {
				buildProperties.load(is);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Exception reading build.properties file", e);
		}
		return buildProperties;
	}

    @Override
    protected void updateSourceManifest(MavenArchiveConfiguration mavenArchiveConfiguration) {
        super.updateSourceManifest(mavenArchiveConfiguration);

        if (sourceBundle)
        {
            addSourceBundleManifestEntries(mavenArchiveConfiguration);
        }
    }

    private void addSourceBundleManifestEntries(MavenArchiveConfiguration mavenArchiveConfiguration)
    {
        BundleDescription bundleDescription = getProjectBundleManifest();

        if ( bundleDescription != null
        	    && bundleDescription.getSymbolicName() != null
        	    && bundleDescription.getVersion() != null )
        {
            mavenArchiveConfiguration.addManifestEntry( MANIFEST_HEADER_BUNDLE_MANIFEST_VERSION, "2" );

            mavenArchiveConfiguration.addManifestEntry( MANIFEST_HEADER_BUNDLE_SYMBOLIC_NAME,
            	    bundleDescription.getSymbolicName() + BUNDLE_SYMBOLIC_NAME_SUFFIX );

            Version expandedVersion = getExpandedVersion(bundleDescription.getVersion());

            mavenArchiveConfiguration.addManifestEntry( MANIFEST_HEADER_BUNDLE_VERSION, expandedVersion.toString() );

            mavenArchiveConfiguration.addManifestEntry( MANIFEST_HEADER_ECLIPSE_SOURCE_BUNDLE, bundleDescription.getSymbolicName()
            	    + ";version=\"" + expandedVersion + '"' );
        }
        else
        {
            getLog().info("NOT adding source bundle manifest entries. Incomplete or no bundle information available.");
        }
    }

    private Version getExpandedVersion(Version version)
    {
        if ( VERSION_QUALIFIER.equals(version.getQualifier()) )
        {
            return new Version(version.getMajor(), version.getMinor(), version.getMicro(), qualifier);
        }
        return version;
    }

    private BundleDescription getProjectBundleManifest()
    {
        BundleResolutionState bundleResolutionState =
            (BundleResolutionState) project.getContextValue( TychoConstants.CTX_BUNDLE_RESOLUTION_STATE );

        if ( bundleResolutionState != null )
        {
            return bundleResolutionState.getBundleByLocation( project.getBasedir() );
        }

        return null;
    }

}
