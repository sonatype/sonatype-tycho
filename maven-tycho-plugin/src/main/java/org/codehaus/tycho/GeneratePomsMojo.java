package org.codehaus.tycho;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.osgitools.OsgiState;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;

/**
 * @goal generate-poms
 * @requiresProject false
 */
public class GeneratePomsMojo extends AbstractMojo {

	/** @component */
	private OsgiState state;
	
	/**
	 * @parameter expression="${baseDir}" default-value="."
	 * @required
	 */
	private File baseDir;

	/** @parameter */
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
	 * @parameter expression="${aggregator}" default-value="true"
	 */
	private boolean aggregator;

	MavenXpp3Reader modelReader = new MavenXpp3Reader();
	MavenXpp3Writer modelWriter = new MavenXpp3Writer();
	
	private Set<File> updateSites = new LinkedHashSet<File>();

	public void execute() throws MojoExecutionException, MojoFailureException {
		File parentBasedir = null;
		Model parent = null;
		File[] baseDirs = getBaseDirs();
		if (getLog().isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append("baseDir=").append(toString(baseDir)).append('\n');
			sb.append("extraDirs=").append(extraDirs).append('\n');
			for (int i = 0; i < baseDirs.length; i++) {
				sb.append("dir[").append(i).append("]=").append(toString(baseDirs[i])).append('\n');
			}
			getLog().debug(sb.toString());
		}
		for (File basedir : baseDirs) {
			getLog().info("Scanning " + toString(baseDir) + " baseDir");
			if (!generatePom(null, basedir)) {
				if (groupId == null) {
					throw new MojoExecutionException("groupId is required");
				}
				if (parent == null) {
					parent = readPom("templates/parent-pom.xml");
					parentBasedir = basedir;
				}
				parent.setGroupId(groupId);
				parent.setArtifactId(basedir.getName());
				parent.setVersion(version);
				Set<File> dirs = new TreeSet<File>(Arrays.asList(basedir.listFiles()));
				if (parent != null && parentBasedir != null && dirs != null) {
					for (File dir : dirs) {
						if (generatePom(parent, dir)) {
							parent.addModule(getModuleName(parentBasedir, dir, null));
						}
					}
				}
			}
		}

		if (parentBasedir != null && parent != null) {
			writePom(parentBasedir, parent);
		}

		generateAggregatorPoms(parent);
	}

	private String toString(File file) {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			return file.getAbsolutePath();
		}
	}

	private File[] getBaseDirs() {
		ArrayList<File> dirs = new ArrayList<File>();
		dirs.add(baseDir);
		if (extraDirs != null) {
			StringTokenizer st = new StringTokenizer(extraDirs, ",");
			while (st.hasMoreTokens()) {
				try {
					dirs.add(new File(st.nextToken()).getCanonicalFile());
				} catch (IOException e) {
					getLog().warn("Can't parse extraDirs", e);
				}
			}
		}
		return dirs.toArray(new File[dirs.size()]);
	}

	private String getModuleName(File basedir, File dir, String relative) throws MojoExecutionException {
		try {
			if (isModule(basedir, dir)) {
				return relative != null? relative + dir.getName(): dir.getName(); 
			} else {
				return dir.getCanonicalPath();
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Can't determine module directory name", e);
		}
	}

	private void generateAggregatorPoms(Model parent) throws MojoExecutionException {
		state.resolveState();
		for (File basedir : updateSites) {
			Set<File> modules = getSiteFeaturesAndPlugins(basedir);
			if (aggregator && modules.size() > 0) {
				Model modela = readPom("templates/update-site-poma.xml");
				setParent(modela, parent);
				modela.setGroupId(groupId);
				modela.setArtifactId(basedir.getName() + ".aggregator");
				modela.setVersion(version);
				for (File module : modules) {
					modela.addModule(getModuleName(basedir.getParentFile(), module, "../"));
				}
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
			throw new MojoExecutionException("goupId parameter is required to generate pom.xml for Update Site project " + basedir.getName());
		}
		if (version == null) {
			throw new MojoExecutionException("version parameter is required to generate pom.xml for Update Site project " + basedir.getName());
		}
		
		Model model = readPom("templates/update-site-pom.xml");
		setParent(model, parent);
		model.setGroupId(groupId);
		model.setArtifactId(basedir.getName());
		model.setVersion(version);
		writePom(basedir, model);
		
		updateSites.add(basedir);
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
			if (dir != null) {
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
			File dir = getModuleDir(name);
			if (dir != null) {
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
		Model model = readPom("templates/feature-pom.xml");
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
		Model model = readPom("templates/plugin-pom.xml");
		setParent(model, parent);
		try {
			BundleDescription bundleDescription = state.addBundle(dir);
			String groupId = state.getGroupId(bundleDescription);
			if (groupId == null) {
				groupId = this.groupId;
			}
			if (groupId == null) {
				groupId = bundleDescription.getSymbolicName();
			}
			model.setGroupId(groupId);
			model.setArtifactId(bundleDescription.getSymbolicName());
			model.setVersion(bundleDescription.getVersion().toString());
		} catch (BundleException e) {
			throw new MojoExecutionException("Can't generate pom.xml", e);
		}
		writePom(dir, model);
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

	private Model readPom(String name) throws MojoExecutionException {
		ClassLoader cl = GeneratePomsMojo.class.getClassLoader();
		try {
			InputStream is = cl.getResourceAsStream(name);
			if (is != null) {
				try {
					return modelReader.read(is);
				} finally {
					is.close();
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
