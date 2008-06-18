package org.codehaus.tycho;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

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

	MavenXpp3Reader modelReader = new MavenXpp3Reader();
	MavenXpp3Writer modelWriter = new MavenXpp3Writer();

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!generatePom(null, baseDir)) {
			Model parent = readPom("templates/parent-pom.xml");
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
	}

	private boolean generatePom(Model parent, File baseDir) throws MojoExecutionException {
		if (isPluginProject(baseDir)) {
			generatePluginPom(parent, baseDir);
		} else if (isFeatureProject(baseDir)) {
			generateFeaturePom(parent, baseDir);
		} else if (isUpdateSiteProject(baseDir)) {
			generateUpdateSitePom(parent, baseDir);
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

	private void generateUpdateSitePom(Model parent, File baseDir) throws MojoExecutionException {
		Model model = readPom("templates/update-site-pom.xml");
		setParent(model, parent);
		model.setGroupId(groupId);
		model.setArtifactId(baseDir.getName());
		model.setVersion(version);
		writePom(baseDir, model);
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

	private void generateFeaturePom(Model parent, File baseDir) throws MojoExecutionException {
		Model model = readPom("templates/feature-pom.xml");
		setParent(model, parent);

		try {
			FileInputStream is = new FileInputStream(new File(baseDir, "feature.xml"));
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
		
		writePom(baseDir, model);
	}

	private void generatePluginPom(Model parent, File dir) throws MojoExecutionException {
		Model model = readPom("templates/plugin-pom.xml");
		setParent(model, parent);
		try {
			BundleDescription bundleDescription = state.addBundle(new File(dir, "META-INF/MANIFEST.MF"));
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
		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(new File(dir, "pom.xml")), "UTF-8");
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
