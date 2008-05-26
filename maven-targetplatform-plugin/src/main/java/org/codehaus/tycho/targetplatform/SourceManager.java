package org.codehaus.tycho.targetplatform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class SourceManager {

	private File base;

	private File[] sourceDirs;

	public SourceManager(File base) {
		this.base = base;

		sourceDirs = findSourcePlugins();
	}

	private File[] findSourcePlugins() {
		File[] plugins = new File(base, "plugins").listFiles();

		List sourceDirs = new ArrayList();
		for (int i = 0; i < plugins.length; i++) {
			File plugin = plugins[i];
			sourceDirs.addAll(getSourceDirs(plugin));
		}

		return (File[]) sourceDirs.toArray(new File[sourceDirs.size()]);
	}

	public static List getSourceDirs(File plugin) {
		List sourceDirs = new ArrayList();
		File pluginXML = new File(plugin, "plugin.xml");
		if (!pluginXML.exists()) {
			pluginXML = new File(plugin, "fragment.xml");
			if (!pluginXML.exists()) {
				return sourceDirs;
			}
		}

		try {
			Reader r = new FileReader(pluginXML);
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression pdeSourcePath = xpath
					.compile("//extension[@point='org.eclipse.pde.core.source']/location/@path");

			Node node = (Node) pdeSourcePath.evaluate(new InputSource(r),
					XPathConstants.NODE);
			if (node != null) {
				String srcValue = node.getTextContent();

				File f = new File(plugin, srcValue);
				if (f.exists()) {
					sourceDirs.add(f);
				}
			}

		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sourceDirs;
	}

	public File getSourceZip(String symbolicName, String version) {
		for (int i = 0; i < sourceDirs.length; i++) {
			File dir = new File(sourceDirs[i], symbolicName + "_" + version);
			File result = new File(dir, "src.zip");
			if (result.exists()) {
				return result;
			}
		}
		return null;
	}

}
