package org.codehaus.tycho;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Set;

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
		Model parent = null;
		if (!generatePom(null, baseDir)) {
			parent = readPom("templates/parent-pom.xml");
			if (groupId == null) {
				throw new MojoExecutionException("groupId is required");
			}
			parent.setGroupId(groupId);
			parent.setArtifactId(baseDir.getName());
			parent.setVersion(version);
			File[] dirs = baseDir.listFiles();
			if (dirs != null) {
				for (File dir : dirs) {
					if (generatePom(parent, dir)) {
						parent.addModule(dir.getName());
					}
				}
			}
			writePom(baseDir, parent);
		}

		generateAggregatorPoms(parent);
	}

	private void generateAggregatorPoms(Model parent) throws MojoExecutionException {
		state.resolveState();
		for (File basedir : updateSites) {
			Set<String> modules = getSiteFeaturesAndPlugins(basedir);
			if (aggregator && modules.size() > 0) {
				Model modela = readPom("templates/update-site-poma.xml");
				setParent(modela, parent);
				modela.setGroupId(groupId);
				modela.setArtifactId(basedir.getName() + ".aggregator");
				modela.setVersion(version);
				for (String module : modules) {
					modela.addModule("../" + module);
				}
				writePom(basedir, "poma.xml", modela);
			}
		}
	}

	private boolean generatePom(Model parent, File basedir) throws MojoExecutionException {
		if (isPluginProject(basedir)) {
			generatePluginPom(parent, basedir);
		} else if (isFeatureProject(basedir)) {
			generateFeaturePom(parent, basedir);
		} else if (isUpdateSiteProject(basedir)) {
			generateUpdateSitePom(parent, basedir);
		} else {
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

	private Set<String> getSiteFeaturesAndPlugins(File basedir) throws MojoExecutionException {
		try {
			Set<String> result = new LinkedHashSet<String>();

			UpdateSite site = UpdateSite.read(new File(basedir, "site.xml"));

			for (UpdateSite.FeatureRef feature : site.getFeatures()) {
				addFeature(result, basedir, feature.getId());
			}

			return result;
		} catch (Exception e) {
			throw new MojoExecutionException("Could not collect update site features and plugins", e);
		}
	}

	private void addFeature(Set<String> result, File basedir, String name) throws IOException, XmlPullParserException {
		if (name != null) {
			File dir = new File(basedir.getParent(), name);
			if (dir.exists() && dir.isDirectory()) {
				result.add(name);
				result.addAll(getFeatureFeaturesAndPlugins(dir));
			}
		}
	}

	private Set<String> getFeatureFeaturesAndPlugins(File basedir) throws IOException, XmlPullParserException {
		Set<String> result = new LinkedHashSet<String>(); 

		Feature feature = Feature.read(new File(basedir, "feature.xml"));

		for (Feature.PluginRef plugin : feature.getPlugins()) {
			addPlugin(result, basedir, plugin.getId());
		}

		for (Feature.FeatureRef includedFeature : feature.getIncludedFeatures()) {
			addFeature(result, basedir, includedFeature.getId());
		}

		for (Feature.RequiresRef require : feature.getRequires()) {
			for (Feature.ImportRef imp : require.getImports()) {
				addPlugin(result, basedir, imp.getPlugin());
				addFeature(result, basedir, imp.getFeature());
			}
		}
		
		return result;
	}

	private void addPlugin(Set<String> result, File basedir, String name) {
		if (name != null) {
			File dir = new File(basedir.getParent(), name);
			if (dir.exists() && dir.isDirectory()) {
				if (result.add(name)) {
					BundleDescription bundle = state.getBundleDescription(dir);
					if (bundle != null) { 
						try {
							state.assertResolved(bundle);
							BundleDescription[] requiredBundles = state.getDependencies(bundle);
							for (int i = 0; i < requiredBundles.length; i++) {
								BundleDescription supplier = requiredBundles[i].getSupplier().getSupplier();
								File suppliedDir = new File(supplier.getLocation());
								if (suppliedDir.isDirectory() && isParent(basedir.getParentFile(), suppliedDir)) {
									addPlugin(result, suppliedDir, suppliedDir.getName());
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

	private boolean isParent(File parent, File file) {
		try {
			return file.getCanonicalFile().equals(new File(parent, file.getName()).getCanonicalFile());
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
