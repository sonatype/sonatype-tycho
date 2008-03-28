package org.codehaus.tycho.targetplatform;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.tycho.CLITools;
import org.codehaus.tycho.TychoException;
import org.codehaus.tycho.osgitools.GroupMapper;
import org.codehaus.tycho.osgitools.OsgiStateController;
import org.codehaus.tycho.osgitools.PomGenerator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.osgi.framework.BundleException;

/**
 * Installs or deploys a group of bundles from a a valid Eclipse install to the
 * Maven repository
 * 
 * @requiresProject false
 */
public abstract class AbstractDeployBundlesMojo extends AbstractMojo {

	/**
	 * @parameter expression="${baseDir}" default-value="."
	 * @required
	 */
	private File baseDir;

	/**
	 * a comma separated list of regular expressions to select the bundles that
	 * will be deployed
	 * 
	 * @parameter expression="${include}" default-value=".*"
	 * @required
	 */
	private String include;

	/**
	 * File that contains a list of symbolic-name-pattern=groupId
	 * 
	 * @parameter expression="${groupList}" default-value="group.list"
	 */
	private File groupList;

	/**
	 * String that will be appended to the version
	 * 
	 * @parameter expression="${versionClassifier}"
	 */
	private String versionQualifier;

	/**
	 * Component used to create an artifact
	 * 
	 * @component
	 */
	private ArtifactFactory artifactFactory;

	/**
	 * @component
	 */
	private PomGenerator pomGenerator;

	/**
	 * @component
	 */
	protected CLITools cliTools;

	private SourceManager sourceManager;

	private Model targetPlatformPom;

	private List noSourceFound = new ArrayList();

	/** @component */
	private org.apache.maven.artifact.resolver.ArtifactResolver resolver;

	/** @component */
	private ArtifactMetadataSource metadataSource;

	/** @component */
	private MavenProjectBuilder mavenProjectBuilder;

	/**
	 * @parameter expression="${targetArtifactId}"
	 */
	private String targetArtifactId;

	/**
	 * @parameter expression="${sourceArtifactId}"
	 */
	private String sourceArtifactId;

	/** @parameter expression="${localRepository}" */
	ArtifactRepository localRepository;

	/** @parameter expression="${project.remoteArtifactRepositories}" */
	private List remoteRepositories;

	private Artifact targetPlatformArtifact;

	private List sourceArtifacts = new ArrayList();

	public void execute() throws MojoExecutionException {
		try {

			if (!StringUtils.isEmpty(targetArtifactId)) {
				targetPlatformArtifact = cliTools.createArtifact(
						targetArtifactId, "pom");
				targetPlatformPom = new Model();
				targetPlatformPom.setModelVersion("4.0.0");
				targetPlatformPom.setArtifactId(targetPlatformArtifact
						.getArtifactId());
				targetPlatformPom.setGroupId(targetPlatformArtifact
						.getGroupId());
				targetPlatformPom.setVersion(targetPlatformArtifact
						.getVersion());
				targetPlatformPom.setPackaging("eclipse-feature");
			}

			if (!StringUtils.isEmpty(sourceArtifactId)) {
				String[] split = sourceArtifactId.split(",");
				for (int i = 0; i < split.length; i++) {
					Set dependencies = new HashSet();
					Artifact artifact = cliTools
							.createArtifact(split[i], "pom");
					sourceArtifacts.addAll(getTransitiveDependencies(artifact));
				}
			}

			OsgiStateController state = createOsgiState();

			String filters[] = include.split(",");
			Pattern[] patterns = new Pattern[filters.length];
			for (int i = 0; i < patterns.length; i++) {
				patterns[i] = Pattern.compile(filters[i]);
			}

			GroupMapper groupMapper = new GroupMapper(groupList);
			BundleDescription[] bundles = state.getState().getBundles();
			List deployed = filterDeployed(patterns, bundles);

			sourceManager = new SourceManager(baseDir, state);

			for (Iterator iterator = deployed.iterator(); iterator.hasNext();) {
				BundleDescription bundle = (BundleDescription) iterator.next();
				if (state.getState().getResolverErrors(bundle).length == 0) {
					Model model = pomGenerator.createBundlePom(groupMapper,
							deployed, versionQualifier, bundle);
					deploy(bundle, model);
				} else {
					getLog().error(
							"Not deploying " + bundle
									+ " because of resolving errrors");
				}
			}

			if (targetPlatformPom != null) {
				File pom = writePom(targetPlatformPom);
				pom.deleteOnExit();
				targetPlatformArtifact.setFile(createEmptyJar());
				ArtifactMetadata metadata = new ProjectArtifactMetadata(
						targetPlatformArtifact, pom);
				targetPlatformArtifact.addMetadata(metadata);
				deployArtifact(targetPlatformArtifact);
			}

			if (!noSourceFound.isEmpty()) {
				getLog().info("No source found for: ");
				for (Iterator iter = noSourceFound.iterator(); iter.hasNext();) {
					String element = (String) iter.next();
					getLog().info("\t" + element);

				}
			}
			
			File featureDir = new File(baseDir, "features");
			if (featureDir.exists()) {
				File[] featureDirs = featureDir.listFiles();
				for (int i = 0; i < featureDirs.length; i++) {
					File f = featureDirs[i];
					File featureXML = new File(f, "feature.xml");
					if (featureXML.exists()) {
						deployFeature(f, bundles);
					}
				}
			}

		} catch (Exception e) {
			throw new MojoExecutionException("Error", e);
		}
	}

	private void deployFeature(File f, BundleDescription[] bundles) {
		System.out.println("TODO deploy feature " + f);
	}

	private List filterDeployed(Pattern[] patterns, BundleDescription[] bundles) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription b = bundles[i];
			getLog().info("checking " + b.getSymbolicName());
			for (int j = 0; j < patterns.length; j++) {
				Pattern p = patterns[j];
				if (p.matcher(b.getSymbolicName()).matches()) {
					result.add(b);
				}
			}
		}
		return result;
	}

	private OsgiStateController createOsgiState()
			throws MojoExecutionException, TychoException {
		OsgiStateController state = new OsgiStateController();
		File[] bundles = new File(baseDir, "plugins")
				.listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						return pathname.getName().endsWith(".jar")
								|| (pathname.isDirectory() && (new File(
										pathname, JarFile.MANIFEST_NAME)
										.exists() || new File(pathname,
										"plugin.xml").exists()));
					}
				});

		if (bundles == null) {
			throw new MojoExecutionException("No bundles found!");
		}

		Set deployed = new HashSet();

		for (int i = 0; i < bundles.length; i++) {
			File bundle = bundles[i];
			try {
				BundleDescription bd = state.addBundle(bundle);
				bd.setUserObject(bundle);
				deployed.add(bd.getSymbolicName());
			} catch (BundleException e) {
				getLog().info("Could not add bundle: " + bundle);
			}
		}
		Iterator it = sourceArtifacts.iterator();
		while (it.hasNext()) {
			Artifact a = (Artifact) it.next();
			try {
				BundleDescription bd = state.addBundle(a.getFile());
				if (!deployed.contains(bd.getSymbolicName())) {
					bd.setUserObject(a.getFile());
					deployed.add(bd.getSymbolicName());
				} else {
					state.getState().removeBundle(bd);
					getLog().info(
							"Duplicate bundles - ignoring in target platform:"
									+ bd.getSymbolicName());
				}
			} catch (BundleException e) {
				getLog().info("Could not add bundle: " + a.getFile());
			}
		}

		state.resolveState();

		BundleDescription[] bds = state.getState().getBundles();
		for (int i = 0; i < bds.length; i++) {
			BundleDescription bundle = bds[i];
			ResolverError[] errors = state.getState().getResolverErrors(bundle);
			if (errors.length > 0) {
				getLog()
						.error("Errors for bundle: " + bundle.getSymbolicName());
				for (int j = 0; j < errors.length; j++) {
					ResolverError error = errors[j];
					getLog().error(error.toString());
				}
			}
		}

		return state;
	}

	private void deploy(BundleDescription bundle, Model model)
			throws MojoExecutionException {

		File location = new File(bundle.getLocation());
		if (!SourceManager.getSourceDirs(location).isEmpty()) {
			return;
		}

		if (location.isDirectory()) {
			location = createJar(location);
			location.deleteOnExit();
		}

		Artifact artifact = artifactFactory.createArtifactWithClassifier(model
				.getGroupId(), model.getArtifactId(), model.getVersion(),
				"jar", null);
		artifact.setFile(location);

		try {
			File pom = writePom(model);
			pom.deleteOnExit();

			ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact,
					pom);
			artifact.addMetadata(metadata);

			deployArtifact(artifact);

			if (targetPlatformPom != null) {
				Dependency dep = new Dependency();
				dep.setGroupId(model.getGroupId());
				dep.setArtifactId(model.getArtifactId());
				dep.setVersion(model.getVersion());
				dep.setScope(Artifact.SCOPE_COMPILE);
				targetPlatformPom.addDependency(dep);
			}

			File sourceFile = sourceManager.getSourceZip(model.getArtifactId(),
					model.getVersion());

			if (sourceFile != null) {
				Artifact sourceArtifact = artifactFactory
						.createArtifactWithClassifier(model.getGroupId(), model
								.getArtifactId(), model.getVersion(), "jar",
								"sources");
				sourceArtifact.setFile(sourceFile);
				deployArtifact(sourceArtifact);
			} else {
				noSourceFound.add(model.getArtifactId());
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Could not write POM", e);
		} catch (TychoException e) {
			throw new MojoExecutionException("Could not install/deploy "
					+ artifact, e);
		}
	}

	public abstract void deployArtifact(Artifact artifact)
			throws TychoException;

	private File writePom(Model model) throws IOException {
		File pom = File.createTempFile("deploy", ".pom");
		FileWriter fw = new FileWriter(pom);
		try {
			new MavenXpp3Writer().write(fw, model);
			return pom;
		} finally {
			fw.close();
		}
	}

	private File createJar(File location) {
		try {
			File temp = File.createTempFile("deploy", ".jar");
			JarArchiver archiver = new JarArchiver();
			archiver.addDirectory(location, new String[] { "**/*" },
					new String[] { "**/.svn", "**/.svn/**" });
			archiver.setDestFile(temp);
			File manifest = new File(location, JarFile.MANIFEST_NAME);
			if (manifest.exists()) {
				archiver.setManifest(manifest);
			}
			archiver.createArchive();
			return temp;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private File createEmptyJar() {
		try {
			File temp = File.createTempFile("deploy", ".jar");
			JarArchiver archiver = new JarArchiver();
			archiver.setDestFile(temp);
			archiver.createArchive();
			return temp;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Set getTransitiveDependencies(Artifact artifact)
			throws TychoException {
		String groupId = artifact.getGroupId();
		String artifactId = artifact.getArtifactId();
		String version = artifact.getVersion();

		try {
			org.apache.maven.artifact.Artifact pomArtifact = artifactFactory
					.createArtifact(groupId, artifactId, version, null, "pom");
			MavenProject pomProject = mavenProjectBuilder.buildFromRepository(
					pomArtifact, remoteRepositories, localRepository);
			String scope = org.apache.maven.artifact.Artifact.SCOPE_RUNTIME;
			ArtifactFilter filter = new ScopeArtifactFilter(scope);

			Set artifacts = pomProject.createArtifacts(artifactFactory, null,
					null);
			ArtifactResolutionResult arr = resolver.resolveTransitively(
					artifacts, pomArtifact, localRepository,
					remoteRepositories, metadataSource, filter);
			artifacts = arr.getArtifacts();
			for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
				org.apache.maven.artifact.Artifact a = (org.apache.maven.artifact.Artifact) iter
						.next();
				resolver.resolve(a, remoteRepositories, localRepository);
			}
			return artifacts;
		} catch (ProjectBuildingException e) {
			throw new TychoException("Error resolving transitively for "
					+ artifact, e);
		} catch (ArtifactResolutionException e) {
			throw new TychoException("Error resolving transitively for "
					+ artifact, e);
		} catch (ArtifactNotFoundException e) {
			throw new TychoException("Error resolving transitively for "
					+ artifact, e);
		} catch (Exception e) {
			throw new TychoException("Error resolving transitively for "
					+ artifact, e);
		}
	}
}