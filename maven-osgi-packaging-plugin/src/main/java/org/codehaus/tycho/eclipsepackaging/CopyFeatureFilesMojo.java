package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.ArrayUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.osgitools.BundleFile;
import org.codehaus.tycho.osgitools.OsgiStateController;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * This goal will create a feature with all the dependencies listed in the POM.
 * Optionally, it can generate a feature.xml with exactly those dependencies.
 * Alternatively you can use an existing feature.xml which can be checked for
 * synchronization with the POM. The rest of the contents of the feature is
 * determined by the build.properties file.
 * 
 * @goal copy-feature-files
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class CopyFeatureFilesMojo extends AbstractMojo
{
	/**
	 * @parameter expression="${project}"
	 * @required
	 */
	protected MavenProject project;

	/**
	 * Temporary assembly directory
	 * 
	 * @parameter expression="${project.build.directory}/deploy"
	 */
	private File deployDirectory;

	/**
	 * Generate the feature.xml based on the POM ?
	 * 
	 * @parameter expression="${generateFeatureXML}" default-value="false"
	 */
	private boolean generateFeatureXML;

	/**
	 * @parameter expression="${project.basedir}/feature.xml"
	 */
	private File featureXML;

	/**
	 * Fail if pom and feature.xml are not in sync (only when not generated)
	 * 
	 * @parameter default-value="false"
	 */
	private boolean failOnError;

	/** @parameter expression="${project.build.directory}" */
	private File outputDir;

	public void execute() throws MojoExecutionException, MojoFailureException
	{
		if (!generateFeatureXML)
		{
			validate();
		}

		File destination = new File(deployDirectory, "plugins");
		if (!destination.exists())
		{
			destination.mkdirs();
		}

		createFeatureDir();
		copyRootFiles();

		List files = new ArrayList();
		for (Iterator iterator = project.getDependencyArtifacts().iterator(); iterator.hasNext();) {
			Artifact a= (Artifact) iterator.next();
			if (DefaultArtifact.SCOPE_COMPILE.equals(a.getScope()))
			{
				try
				{
					File bundleLocation = a.getFile();
					OsgiStateController state = new OsgiStateController(outputDir);
					BundleFile b = new BundleFile(state.loadManifest(bundleLocation), bundleLocation);
					String bundleFileName = b.getSymbolicName() + "_"
							+ b.getVersion() + ".jar";
					files.add(b);
					FileUtils.copyFile(bundleLocation, new File(destination,
							bundleFileName));
				}
				catch (IOException e)
				{
					throw new MojoExecutionException("Error copying "
							+ a.getFile() + " to " + destination, e);
				}
			}
		}

		if (generateFeatureXML)
		{
			try
			{
				VelocityEngine ve = new VelocityEngine();
				ve.setProperty("resource.loader", "class");
				ve.setProperty("class.resource.loader.class",
						ClasspathResourceLoader.class.getName());
				ve.init();
				VelocityContext context = new VelocityContext();
				context.put("project", project);
				context.put("bundles", files);
				File featureDir = new File(deployDirectory, "features/"
						+ project.getArtifactId() + "_" + project.getVersion());
				File output = new File(featureDir, "feature.xml");
				FileWriter w = new FileWriter(output);
				Template t = ve.getTemplate("/feature.xml.vm");
				t.merge(context, w);
				w.close();
			}
			catch (Exception e)
			{
				throw new MojoExecutionException(
						"Error while creating feature.xml", e);
			}
		}
	}

	private void createFeatureDir() throws MojoExecutionException
	{
		try
		{
			File featureDir = new File(deployDirectory, "features/"
					+ project.getArtifactId() + "_" + project.getVersion());
			Properties props = new Properties();
			props.load(new FileInputStream(new File(project.getBasedir(),
					"build.properties")));
			String[] binIncludes = props.getProperty("bin.includes").split(",");
			String files[] = Util.getIncludedFiles(project.getBasedir(),
					binIncludes);
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i];
				File f = new File(project.getBasedir(), fileName);
				File target = new File(featureDir, fileName);
				if (f.isDirectory())
				{
					target.mkdirs();
				} else
				{
					FileUtils.copyFile(f, target);
				}
			}
		}
		catch (Exception e)
		{
			throw new MojoExecutionException("", e);
		}

	}

	private void copyRootFiles() throws MojoExecutionException
	{
		Properties props = new Properties();
		try
		{
			props.load(new FileInputStream(new File(project.getBasedir(),
					"build.properties")));
		}
		catch (FileNotFoundException e1)
		{
			throw new MojoExecutionException("Could not find build.properties");
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("Could not read build.properties",
					e);
		}
		String root = props.getProperty("root");
		if (root != null)
		{
			String[] includes = root.split(",");

			for (int i = 0; i < includes.length; i++) {
				String include = includes[i];
				String[] files = Util.getIncludedFiles(project.getBasedir(),
						new String[]
						{ include });

				for (int j = 0; j < files.length; j++) {
					String file = files[j];
					File f = new File(project.getBasedir(), file);
					if (include.endsWith("/"))
					{
						file = file.substring(include.length());
					}
					try
					{
						FileUtils.copyFile(f, new File(deployDirectory, file));
					}
					catch (IOException e)
					{
						throw new MojoExecutionException("Error copying file "
								+ f);
					}
				}
			}
		}
	}

	private String[] getFeaturePluginIDs() throws MojoExecutionException
	{
		String xpath = "/feature//plugin/@id";
		try
		{
			XPath path = XPathFactory.newInstance().newXPath();
			FileInputStream fis = new FileInputStream(featureXML);
			InputSource is = new InputSource(fis);
			NodeList list = (NodeList) path.evaluate(xpath, is,
					XPathConstants.NODESET);
			String[] result = new String[list.getLength()];
			for (int i = 0; i < list.getLength(); i++)
			{
				Node node = list.item(i);
				result[i] = node.getTextContent();
			}
			return result;

		}
		catch (XPathExpressionException e)
		{
			throw new MojoExecutionException("XPath error: " + xpath, e);
		}
		catch (FileNotFoundException e)
		{
			throw new MojoExecutionException("File not found: " + featureXML, e);
		}

	}

	private void validate() throws MojoFailureException, MojoExecutionException
	{
		Set pomPlugins = project.getDependencyArtifacts();
		String[] featurePlugins = getFeaturePluginIDs();

		boolean fail = false;

		for (Iterator iterator = pomPlugins.iterator(); iterator.hasNext();) {
			Artifact a = (Artifact) iterator.next();
			String id = a.getArtifactId();
			if (!ArrayUtils.contains(featurePlugins, id))
			{
				getLog().error("Plugin found in POM but not in feature: " + id);
				fail = true;
			}
		}

		for (int i = 0; i < featurePlugins.length; i++) {
			String id= featurePlugins[i];
			boolean found = false;
			for (Iterator iterator2 = pomPlugins.iterator(); iterator2.hasNext();) {
				Artifact a = (Artifact) iterator2.next();
				if (id.equals(a.getArtifactId()))
				{
					found = true;
				}
			}
			if (found == false)
			{
				fail = true;
				getLog().error("Plugin found in feature but not in POM: " + id);
			}
		}

		if (failOnError && fail)
		{
			throw new MojoFailureException("validation failed");
		}

	}

}