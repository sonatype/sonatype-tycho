package org.codehaus.tycho.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.tycho.model.Feature.FeatureRef;

public class ProductConfiguration {

	@SuppressWarnings("deprecation")
	public static ProductConfiguration read(File file) throws IOException,
			XmlPullParserException {
		XmlStreamReader reader = ReaderFactory.newXmlReader(file);
		try {
			return new ProductConfiguration(Xpp3DomBuilder.build(reader));
		} finally {
			reader.close();
		}
	}

	@SuppressWarnings("deprecation")
	public static ProductConfiguration read(InputStream inputStream)
			throws IOException, XmlPullParserException {
		XmlStreamReader reader = ReaderFactory.newXmlReader(inputStream);
		try {
			return new ProductConfiguration(Xpp3DomBuilder.build(reader));
		} finally {
			reader.close();
		}
	}

	public static void write(ProductConfiguration product, File file) throws IOException {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        try {
            Xpp3DomWriter.write(writer, product.dom);
        } finally {
            writer.close();
        }
    }

	private Xpp3Dom dom;

	public ProductConfiguration(Xpp3Dom dom) {
		this.dom = dom;
	}

	public String getApplication() {
		return dom.getAttribute("application");
	}

	public List<FeatureRef> getFeatures() {
		Xpp3Dom featuresDom = dom.getChild("features");
		if (featuresDom == null) {
			return Collections.emptyList();
		}

		ArrayList<FeatureRef> features = new ArrayList<FeatureRef>();
		for (Xpp3Dom pluginDom : featuresDom.getChildren("feature")) {
			features.add(new FeatureRef(pluginDom));
		}
		return Collections.unmodifiableList(features);
	}

	public String getId() {
		return dom.getAttribute("id");
	}

	public Launcher getLauncher() {
		Xpp3Dom domLauncher = dom.getChild("launcher");
		if (domLauncher == null) {
			return null;
		}
		return new Launcher(domLauncher);
	}

	public String getName() {
		return dom.getAttribute("name");
	}

	public List<PluginRef> getPlugins() {
		Xpp3Dom pluginsDom = dom.getChild("plugins");
		if (pluginsDom == null) {
			return Collections.emptyList();
		}

		ArrayList<PluginRef> plugins = new ArrayList<PluginRef>();
		for (Xpp3Dom pluginDom : pluginsDom.getChildren("plugin")) {
			plugins.add(new PluginRef(pluginDom));
		}
		return Collections.unmodifiableList(plugins);
	}

	public boolean useFeatures() {
		return Boolean.parseBoolean(dom.getAttribute("useFeatures"));
	}

	public boolean includeLaunchers() {
        String attribute = dom.getAttribute("includeLaunchers");
        return attribute == null? true: Boolean.parseBoolean(attribute);
	}

	public String getVersion() {
		return dom.getAttribute("version");
	}

	public void setVersion(String version) {
	    dom.setAttribute("version", version);
	}
}
