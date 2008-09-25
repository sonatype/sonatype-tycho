package org.codehaus.tycho;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.osgitools.OsgiState;
import org.eclipse.osgi.framework.adaptor.FilePath;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;

/**
 * @goal generate-poms
 * @requiresProject false
 */
public class GeneratePomsMojo extends AbstractMojo {

	/** reference to real pom.xml in aggregator poma.xml */
	private static final String THIS_MODULE = ".";

	/** @component */
	private OsgiState state;

	/**
	 * @parameter expression="${baseDir}" default-value="${basedir}"
	 * @required
	 */
	private File baseDir;

	/** @parameter expression="${extraDirs}*/
	private String extraDirs;

	/**
	 * @parameter expression="${groupId}"
	 */
	private String groupId;

	/**
	 * @parameter expression="${version}" default-value="0.0.1-SNAPSHOT"
	 */
	private String version;

	/**
	 * If true (the default), additional aggregator poma.xml pom file will 
	 * be generated for update site projects. This poma.xml file can be used
	 * to build update site and all its dependencies.
	 * 
	 * @parameter expression="${aggregator}" default-value="true"
	 */
	private boolean aggregator;

	/**
	 * Suffix used to determine test bundles to add to update site 
	 * aggregator pom.
	 * 
	 * @parameter expression="${testSuffix}" default-value=".tests"
	 */
	private String testSuffix;

	/**
	 * Bundle-SymbolicName of the test suite, a special bundle that knows
	 * how to locate and execute all relevant tests.
	 * 
	 * @parameter expression="${testSuite}"
	 */
	private String testSuite;

	/**
	 * Location of directory with template pom.xml file. pom.xml templates will
	 * be looked at this directory first, default templates will be used if 
	 * template directory and the template itself does not exist.
	 * 
	 * See src/main/resources/templates for the list of supported template files.
	 * 
	 * @parameter expression="${templatesDir}" default-value="${basedir}/pom-templates"
	 */
	private File templatesDir;
	
	/**
	 * Comma separated list of root project folders. If specified, generated
	 * pom.xml files will only include root projects and projects directly
	 * and indirectly referecned by the root projects.
	 * 
	 * @parameter expression="${rootProjects}" default-value=".tests"
	 */
	private String rootProjects;

	MavenXpp3Reader modelReader = new MavenXpp3Reader();
	MavenXpp3Writer modelWriter = new MavenXpp3Writer();

	private Map<File, Model> updateSites = new LinkedHashMap<File, Model>();

	public void execute() throws MojoExecutionException, MojoFailureException {
		List<File> baseDirs = getBaseDirs();
		if (getLog().isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append("baseDir=").append(toString(baseDir)).append('\n');
			sb.append("extraDirs=").append(extraDirs).append('\n');
			for (int i = 0; i < baseDirs.size(); i++) {
				sb.append("dir[").append(i).append("]=").append(toString(baseDirs.get(i))).append('\n');
			}
			getLog().debug(sb.toString());
		}

		List<File> rootProjects = getRootProjects();
		if (getLog().isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append("rootProjects=").append(this.rootProjects);
			for (int i = 0; i < rootProjects.size(); i++) {
				sb.append("rootProject[").append(i).append("]=").append(toString(rootProjects.get(i))).append('\n');
			}
			getLog().debug(sb.toString());
		}
		
		// add all bundles to osgi state
		for (File basedir : baseDirs) {
			if (isProjectDir(basedir)) {
				if (isPluginProject(basedir)) {
					try {
						state.addBundle(basedir);
					} catch (BundleException e) {
						getLog().debug("Exception prescanning OSGi bundles", e);
					}
				}
			} else {
				File[] listFiles = basedir.listFiles();
				if (listFiles != null) {
					for (File file : listFiles) {
						if (isPluginProject(file)) {
							try {
								state.addBundle(file);
							} catch (BundleException e) {
								getLog().debug("Exception prescanning OSGi bundles", e);
							}
						}
					}
				}
				
			}
		}
		state.resolveState();

		Set<File> projects = new LinkedHashSet<File>();
		
		// always add baseDir
		projects.add(baseDirs.get(0));

		if (rootProjects.size() > 0) {
			for (File rootProject : rootProjects) {
				getLog().info("Resolving root project " + toString(rootProject));
				if (isUpdateSiteProject(rootProject)) {
					projects.add(rootProject);
					projects.addAll(getSiteFeaturesAndPlugins(rootProject));
				} else {
					getLog().warn("Unsupported root project " + toString(rootProject));
				}
			}
		} else {
			for (File basedir : baseDirs) {
				getLog().info("Scanning " + toString(basedir) + " basedir");
				if (isProjectDir(basedir)) {
					projects.add(basedir);
				} else {
					File[] listFiles = basedir.listFiles();
					if (listFiles != null) {
						for (File file : listFiles) {
							if (isProjectDir(file)) {
								projects.add(file);					
							}
						}
					}
				}
			}
		}

		if (getLog().isDebugEnabled()) {
			getLog().debug("Collected " + projects.size() + " projects");
			for (File dir : projects) {
				getLog().debug("\t" + toString(dir));
			}
		}

		// write poms
		Iterator<File> projectIter = projects.iterator();
		File parentDir = projectIter.next();
		if (!projectIter.hasNext()) {
			if (isProjectDir(parentDir)) {
				generatePom(null, parentDir);
			} else {
				throw new MojoExecutionException("Could not find any valid projects");
			}
		} else {
			Model parent = readPomTemplate("parent-pom.xml");
			parent.setGroupId(groupId);
			parent.setArtifactId(parentDir.getName());
			parent.setVersion(version);
			while (projectIter.hasNext()) {
				File projectDir = projectIter.next();
				generatePom(parent, projectDir);
				parent.addModule(getModuleName(parentDir, projectDir));
			}
			File testSuiteLocation = null;
			if (testSuite != null) {
				BundleDescription bundle = state.getBundleDescription(testSuite, OsgiState.HIGHEST_VERSION);
				if (bundle != null) {
					testSuiteLocation = new File(bundle.getLocation());
				}
			}
			reorderModules(parent, parentDir, testSuiteLocation);
			writePom(parentDir, parent);
			generateAggregatorPoms(testSuiteLocation);
		}
	}

	private List<File> getRootProjects() {
		return toFileList(rootProjects);
	}

	private boolean isProjectDir(File dir) {
		return isPluginProject(dir)
				|| isFeatureProject(dir)
				|| isUpdateSiteProject(dir);
	}

	private void reorderModules(Model parent, File basedir, File testSuiteLocation) throws MojoExecutionException {
		List<String> modules = parent.getModules();
		Collections.sort(modules);
		if (testSuiteLocation != null) {
			String moduleName = getModuleName(basedir, testSuiteLocation);
			modules.remove(moduleName);
			modules.add(moduleName);
		}
		if (modules.contains(THIS_MODULE)) {
			modules.remove(THIS_MODULE);
			modules.add(THIS_MODULE);
		}
	}

	private String toString(File file) {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			return file.getAbsolutePath();
		}
	}

	private List<File> getBaseDirs() {
		ArrayList<File> dirs = new ArrayList<File>();
		dirs.add(baseDir);
		if (extraDirs != null) {
			dirs.addAll(toFileList(extraDirs));
		}
		return dirs;
	}

	private List<File> toFileList(String str) {
		ArrayList<File> dirs = new ArrayList<File>();
		if (str != null) {
			StringTokenizer st = new StringTokenizer(str, ",");
			while (st.hasMoreTokens()) {
				try {
					File dir = new File(st.nextToken()).getCanonicalFile();
					if (dir.exists() && dir.isDirectory()) {
						dirs.add(dir);
					} else {
						getLog().warn("Not a directory " + dir.getAbsolutePath());
					}
				} catch (IOException e) {
					getLog().warn("Can't parse extraDirs", e);
				}
			}
		}
		return dirs;
	}

	private String getModuleName(File basedir, File dir) throws MojoExecutionException {
		// adding extra dependency for a single method is not nice
		// but I don't want to reimplement this tedious logic
		File relative = new File(new FilePath(basedir).makeRelative(new FilePath(dir)));
		return relative.getPath().replace('\\', '/');
	}

	private void generateAggregatorPoms(File testSuiteLocation) throws MojoExecutionException {
		state.resolveState();
		for (Entry<File, Model> updateSite : updateSites.entrySet()) {
			File basedir = updateSite.getKey();
			Model parent = updateSite.getValue();
			Set<File> modules = getSiteFeaturesAndPlugins(basedir);
			if (aggregator && modules.size() > 0) {
				Model modela = readPomTemplate("update-site-poma.xml");
				setParent(modela, parent);
				modela.setGroupId(groupId);
				modela.setArtifactId(basedir.getName() + ".aggregator");
				modela.setVersion(version);
				for (File module : modules) {
					modela.addModule(getModuleName(basedir, module));
				}
				reorderModules(modela, basedir, testSuiteLocation);
				writePom(basedir, "poma.xml", modela);
			}
		}
	}

	private boolean generatePom(Model parent, File basedir) throws MojoExecutionException {
		if (isPluginProject(basedir)) {
			getLog().debug("Found plugin PDE project " + toString(basedir));
			generatePluginPom(parent, basedir);
		} else if (isFeatureProject(basedir)) {
			getLog().debug("Found feature PDE project " + toString(basedir));
			generateFeaturePom(parent, basedir);
		} else if (isUpdateSiteProject(basedir)) {
			getLog().debug("Found update site PDE project " + toString(basedir));
			generateUpdateSitePom(parent, basedir);
		} else {
			getLog().debug("Not a PDE project " + toString(basedir));
			return false;
		}
		return true;
	}

	private boolean isUpdateSiteProject(File dir) {
		return new File(dir, "site.xml").canRead();
	}

	private boolean isFeatureProject(File dir) {
		return new File(dir, "feature.xml").canRead();
	}

	private boolean isPluginProject(File dir) {
		return new File(dir, "META-INF/MANIFEST.MF").canRead() /*|| new File(dir, "plugin.xml").canRead()*/;
	}

	private void generateUpdateSitePom(Model parent, File basedir) throws MojoExecutionException {
		if (groupId == null) {
			throw new MojoExecutionException("groupId parameter is required to generate pom.xml for Update Site project " + basedir.getName());
		}
		if (version == null) {
			throw new MojoExecutionException("version parameter is required to generate pom.xml for Update Site project " + basedir.getName());
		}
		
		Model model = readPomTemplate("update-site-pom.xml");
		setParent(model, parent);
		model.setGroupId(groupId);
		model.setArtifactId(basedir.getName());
		model.setVersion(version);
		writePom(basedir, model);
		
		updateSites.put(basedir, parent);
	}

	private Set<File> getSiteFeaturesAndPlugins(File basedir) throws MojoExecutionException {
		try {
			Set<File> result = new LinkedHashSet<File>();

			UpdateSite site = UpdateSite.read(new File(basedir, "site.xml"));

			for (UpdateSite.FeatureRef feature : site.getFeatures()) {
				addFeature(result, feature.getId());
			}

			return result;
		} catch (Exception e) {
			throw new MojoExecutionException("Could not collect update site features and plugins", e);
		}
	}

	private void addFeature(Set<File> result, String name) throws IOException, XmlPullParserException, MojoExecutionException {
		if (name != null) {
			File dir = getModuleDir(name);
			if (dir != null && isFeatureProject(dir)) {
				result.add(dir);
				result.addAll(getFeatureFeaturesAndPlugins(dir));
			}
		}
	}

	private File getModuleDir(String name) throws MojoExecutionException {
		File moduleDir = null;
		for (File basedir : getBaseDirs()) {
			File dir = new File(basedir, name);
			if (dir.exists() && dir.isDirectory()) {
				if (moduleDir != null) {
					throw new MojoExecutionException("Duplicate module directory name " + name);
				}
				moduleDir = dir;
			}
		}
		return moduleDir;
	}

	private Set<File> getFeatureFeaturesAndPlugins(File basedir) throws IOException, XmlPullParserException, MojoExecutionException {
		Set<File> result = new LinkedHashSet<File>(); 

		Feature feature = Feature.read(new File(basedir, "feature.xml"));

		for (Feature.PluginRef plugin : feature.getPlugins()) {
			addPlugin(result, plugin.getId());
		}

		for (Feature.FeatureRef includedFeature : feature.getIncludedFeatures()) {
			addFeature(result, includedFeature.getId());
		}

		for (Feature.RequiresRef require : feature.getRequires()) {
			for (Feature.ImportRef imp : require.getImports()) {
				addPlugin(result, imp.getPlugin());
				addFeature(result, imp.getFeature());
			}
		}
		
		return result;
	}

	private void addPlugin(Set<File> result, String name) throws MojoExecutionException {
		if (name != null) {
			addPluginImpl(result, name, true);
			addPluginImpl(result, name + testSuffix, false);
		}
	}

	private void addPluginImpl(Set<File> result, String name, boolean required) throws MojoExecutionException {
		if (name != null) {
			File dir = getModuleDir(name);
			if (dir != null && isPluginProject(dir)) {
				if (result.add(dir)) {
					BundleDescription bundle = state.getBundleDescription(dir);
					if (bundle != null) { 
						try {
							state.assertResolved(bundle);
							BundleDescription[] requiredBundles = state.getDependencies(bundle);
							for (int i = 0; i < requiredBundles.length; i++) {
								BundleDescription supplier = requiredBundles[i].getSupplier().getSupplier();
								File suppliedDir = new File(supplier.getLocation());
								if (isModuleDir(suppliedDir)) {
									addPlugin(result, suppliedDir.getName());
								}
							}
						} catch (BundleException e) {
							getLog().warn("Could not determine bundle dependencies", e);
						}
					} else {
						getLog().warn("Not an OSGi bundle " + dir.toString());
					}
				}
			} else {
				if (required) {
					// not really required, but lets warn anyways
					getLog().warn("Unknown bundle reference " + name);
				}
			}
		}
	}

	private boolean isModuleDir(File dir) {
		if (!dir.exists() || !dir.isDirectory()) {
			return false;
		}
		for (File basedir : getBaseDirs()) {
			if (isModule(basedir, dir)) {
				return true;
			}
		}
		return false;
	}

	private boolean isModule(File basedir, File dir) {
		try {
			if (basedir.getCanonicalFile().equals(dir.getParentFile().getCanonicalFile())) {
				return true;
			}
		} catch (IOException e) {
			getLog().warn("Totally unexpected IOException", e);
		}
		return false;
	}

	private void setParent(Model model, Model parentModel) {
		if (parentModel != null) {
			Parent parent = new Parent();
			parent.setGroupId(parentModel.getGroupId());
			parent.setArtifactId(parentModel.getArtifactId());
			parent.setVersion(parentModel.getVersion());
			
			model.setParent(parent);
		}
	}

	private void generateFeaturePom(Model parent, File basedir) throws MojoExecutionException {
		Model model = readPomTemplate("feature-pom.xml");
		setParent(model, parent);

		try {
			FileInputStream is = new FileInputStream(new File(basedir, "feature.xml"));
			try {
				XmlStreamReader reader = new XmlStreamReader(is);
				Xpp3Dom dom = Xpp3DomBuilder.build(reader);

				String groupId = this.groupId;
				if (groupId == null) {
					groupId = dom.getAttribute("id");
				}
				model.setGroupId(groupId);
				model.setArtifactId(dom.getAttribute("id"));
				model.setVersion(dom.getAttribute("version"));
				
			} finally {
				is.close();
			}
		} catch (XmlPullParserException e) {
			throw new MojoExecutionException("Can't create pom.xml file", e);
		} catch (IOException e) {
			throw new MojoExecutionException("Can't create pom.xml file", e);
		}
		
		writePom(basedir, model);
	}

	private void generatePluginPom(Model parent, File dir) throws MojoExecutionException {
		try {
			BundleDescription bundleDescription = state.addBundle(dir);
			String groupId = state.getGroupId(bundleDescription);

			Model model;
			if ( (testSuffix != null && dir.getName().endsWith(testSuffix)) 
					|| (testSuite != null && bundleDescription.getSymbolicName().equals(testSuite))) {
				model = readPomTemplate("test-plugin-pom.xml");
			} else {
				model = readPomTemplate("plugin-pom.xml");
			}
			setParent(model, parent);
			if (groupId == null) {
				groupId = this.groupId;
			}
			if (groupId == null) {
				groupId = bundleDescription.getSymbolicName();
			}
			model.setGroupId(groupId);
			model.setArtifactId(bundleDescription.getSymbolicName());
			model.setVersion(bundleDescription.getVersion().toString());

			writePom(dir, model);
		} catch (BundleException e) {
			throw new MojoExecutionException("Can't generate pom.xml", e);
		}
	}

	private void writePom(File dir, Model model) throws MojoExecutionException {
		writePom(dir, "pom.xml", model);
	}

	private void writePom(File dir, String filename, Model model) throws MojoExecutionException {
		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(new File(dir, filename)), "UTF-8");
			try {
				modelWriter.write(writer, model);
			} finally {
				writer.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Can't write pom.xml", e);
		}
	}

	private Model readPomTemplate(String name) throws MojoExecutionException {
		try {
			XmlStreamReader reader;

			File file = new File(templatesDir, name);
			if (file.canRead()) {
				// check custom templates dir first
				reader = ReaderFactory.newXmlReader(file);
			} else {
				// fall back to internal templates 
				ClassLoader cl = GeneratePomsMojo.class.getClassLoader();
				InputStream is = cl.getResourceAsStream("templates/" + name);
				reader = is != null? ReaderFactory.newXmlReader(is): null;
			}
			if (reader != null) {
				try {
					return modelReader.read(reader);
				} finally {
					reader.close();
				}
			} else {
				throw new MojoExecutionException("pom.xml template cannot be found " + name);
			}
		} catch (XmlPullParserException e) {
			throw new MojoExecutionException("Can't read pom.xml template " + name, e);
		} catch (IOException e) {
			throw new MojoExecutionException("Can't read pom.xml template " + name, e);
		}
	}

}
