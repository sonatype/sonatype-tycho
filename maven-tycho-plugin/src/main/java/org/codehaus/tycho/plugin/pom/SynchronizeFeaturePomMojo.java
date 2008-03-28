package org.codehaus.tycho.plugin.pom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @goal synchronize-feature-pom
 * @requiresProject true
 */
public class SynchronizeFeaturePomMojo extends AbstractMojo {
	/**
	 * Remote repositories which will be searched for source attachments.
	 * 
	 * @parameter expression="${project.remoteArtifactRepositories}"
	 * @required
	 * @readonly
	 */
	protected List remoteRepositories;

    /**
	 * @parameter expression="${project.basedir}/feature.xml"
	 */
	private File featureFile;

	/**
	 * @parameter expression="${project.basedir}/pom.xml"
	 * 
	 */
	private File pomFile;

	/**
	 * @parameter default-value="${project.groupId}"
	 */
	private String groupId;

	/**
	 * @parameter expression="${version}" default-value="${project.version}"
	 * @required true
	 */
	private String version;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Model model = readPom(pomFile);

			Iterator it = model.getDependencies().iterator();
			while (it.hasNext()) {
				String scope = ((Dependency) it.next()).getScope();
				if (scope == null
						|| scope.equals(DefaultArtifact.SCOPE_COMPILE)) {
					it.remove();
				}
			}

			Dependency[] dependencies = getPluginIDs(featureFile);

			getLog().info("Dependent bundles:");
			for (int i = 0; i < dependencies.length; i++) {
				Dependency d = dependencies[i];
				model.getDependencies().add(d);

				getLog().info(groupId + ":" + d.getArtifactId() + ":" + version);
			}

			new MavenXpp3Writer().write(new FileWriter(pomFile), model);

		} catch (IOException e) {
			throw new MojoExecutionException("", e);
		} catch (XmlPullParserException e) {
			throw new MojoExecutionException("", e);
		}
	}

	private Model readPom(File pom) throws IOException, XmlPullParserException {
		return new MavenXpp3Reader().read(new FileReader(pom));
	}

	private Dependency[] getPluginIDs(File pomFile) throws MojoExecutionException {
		try {
			XPath path = XPathFactory.newInstance().newXPath();
			NodeList l1 = (NodeList) path.evaluate("/feature/plugin",
					new InputSource(new FileInputStream(featureFile)),
					XPathConstants.NODESET);
			List result = new ArrayList();
			for (int i = 0; i < l1.getLength(); i++) {
				Node node = l1.item(i);
				String version = node.getAttributes().getNamedItem("version").getTextContent();
				String id = node.getAttributes().getNamedItem("id").getTextContent();
				Dependency d = new Dependency();
				d.setGroupId(groupId);
				d.setVersion(version);
				d.setArtifactId(id);
				result.add(d);
			}
			return (Dependency[]) result.toArray(new Dependency[result.size()]);
		} catch (XPathExpressionException e) {
			throw new MojoExecutionException("", e);
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException("", e);
		}
	}
}
