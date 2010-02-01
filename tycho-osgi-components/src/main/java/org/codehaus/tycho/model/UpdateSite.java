package org.codehaus.tycho.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

/**
 * http://help.eclipse.org/ganymede/topic/org.eclipse.platform.doc.isv/reference/misc/update_sitemap.html
 */
public class UpdateSite {

    public static final String SITE_XML = "site.xml";

	final Xpp3Dom dom;

	public UpdateSite(Xpp3Dom dom) {
		this.dom = dom;
	}

	public List<SiteFeatureRef> getFeatures() {
		ArrayList<SiteFeatureRef> features = new ArrayList<SiteFeatureRef>();
		for (Xpp3Dom featureDom : dom.getChildren("feature")) {
			features.add(new SiteFeatureRef(featureDom));
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

	public static class SiteFeatureRef extends FeatureRef {

		public SiteFeatureRef(Xpp3Dom dom) {
			super(dom);
		}

		public void setUrl(String url) {
			dom.setAttribute("url", url);
		}

		public String getUrl()
		{
		    return dom.getAttribute("url");
		}

	}

	public static UpdateSite read(File file) throws IOException, XmlPullParserException {
	    return read(new FileInputStream(file));
	}

    @SuppressWarnings("deprecation")
    public static UpdateSite read(InputStream is)
        throws IOException, XmlPullParserException
    {
        XmlStreamReader reader = ReaderFactory.newXmlReader(is);
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
