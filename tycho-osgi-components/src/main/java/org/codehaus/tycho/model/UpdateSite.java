package org.codehaus.tycho.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class UpdateSite {
	
	final Xpp3Dom dom;
	
	public UpdateSite(Xpp3Dom dom) {
		this.dom = dom;
	}

	public List<FeatureRef> getFeatures() {
		ArrayList<FeatureRef> features = new ArrayList<FeatureRef>();
		for (Xpp3Dom featureDom : dom.getChildren("feature")) {
			features.add(new FeatureRef(featureDom));
		}
		return Collections.unmodifiableList(features);
	}

	public Map<String, String> getArchives() {
		Map<String, String> archives = new HashMap<String, String>();
		for (Xpp3Dom archiveDom : dom.getChildren("archive")) {
			String path = archiveDom.getAttribute("path");
			String url = archiveDom.getAttribute("url");
			archives.put(path, url);
		}
		return Collections.unmodifiableMap(archives);
	}

	public void removeArchives() {
		int i = 0;
		while (i < dom.getChildCount()) {
			Xpp3Dom child = dom.getChild(i);
			if ("archive".equals(child.getName())) {
				dom.removeChild(i);
			} else {
				i++;
			}
		}
	}

	public static class FeatureRef implements IFeatureRef {

		private final Xpp3Dom dom;

		public FeatureRef(Xpp3Dom dom) {
			this.dom = dom;
		}

		public String getId() {
			return dom.getAttribute("id");
		}

		public String getVersion() {
			return dom.getAttribute("version");
		}

		public void setUrl(String url) {
			dom.setAttribute("url", url);
		}

		public void setVersion(String version) {
			dom.setAttribute("version", version);
		}

	}
	
	@SuppressWarnings("deprecation")
	public static UpdateSite read(File file) throws IOException, XmlPullParserException {
		XmlStreamReader reader = ReaderFactory.newXmlReader(file);
		try {
			return new UpdateSite(Xpp3DomBuilder.build(reader));
		} finally {
			reader.close();
		}
	}

	public static void write(UpdateSite site, File file) throws IOException {
		Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		try {
			Xpp3DomWriter.write(writer, site.dom);
		} finally {
			writer.close();
		}
	}
	
	public boolean isPack200() {
	    String pack200 = dom.getAttribute( "pack200" );
	    return "true".equals(pack200);
	}
}
