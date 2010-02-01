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
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * http://help.eclipse.org/ganymede/topic/org.eclipse.platform.doc.isv/reference/misc/feature_manifest.html 
 */
public class Feature {
	
	public static final String FEATURE_XML = "feature.xml";

	private final Xpp3Dom dom;

    private ArrayList<PluginRef> plugins;

    private ArrayList<FeatureRef> features;

	public Feature(Xpp3Dom dom) {
		this.dom = dom;
	}

	/** copy constructor */
	public Feature(Feature other) {
		this.dom = new Xpp3Dom(other.dom);
	}

	public List<PluginRef> getPlugins() {
	    if ( plugins == null ) {
    		plugins = new ArrayList<PluginRef>();
    		for (Xpp3Dom pluginDom : dom.getChildren("plugin")) {
    			plugins.add(new PluginRef(pluginDom));
    		}
	    }
		return Collections.unmodifiableList(plugins);
	}

	public void setVersion(String version) {
		dom.setAttribute("version", version);
	}

	public List<FeatureRef> getIncludedFeatures() {
	    if (features == null) {
    		features = new ArrayList<FeatureRef>();
    		for (Xpp3Dom featureDom : dom.getChildren("includes")) {
    			features.add(new FeatureRef(featureDom));
    		}
	    }
		return Collections.unmodifiableList(features);
	}

	public List<RequiresRef> getRequires() {
		ArrayList<RequiresRef> requires = new ArrayList<RequiresRef>();
		for (Xpp3Dom requiresDom : dom.getChildren("requires")) {
			requires.add(new RequiresRef(requiresDom));
		}
		return Collections.unmodifiableList(requires);
	}

	public static class RequiresRef {

		private final Xpp3Dom dom;

		public RequiresRef(Xpp3Dom dom) {
			this.dom = dom;
		}

		public List<ImportRef> getImports() {
			ArrayList<ImportRef> imports = new ArrayList<ImportRef>();
			for (Xpp3Dom importsDom : dom.getChildren("import")) {
				imports.add(new ImportRef(importsDom));
			}
			return Collections.unmodifiableList(imports);
		}
		
	}
	
	public static class ImportRef {

		private final Xpp3Dom dom;

		public ImportRef(Xpp3Dom dom) {
			this.dom = dom;
		}

		public String getPlugin() {
			return dom.getAttribute("plugin");
		}

		public String getFeature() {
			return dom.getAttribute("feature");
		}

	}

	public String getVersion() {
		return dom.getAttribute("version");
	}

	public String getId() {
		return dom.getAttribute("id");
	}

    public static Feature read(File file) throws IOException, XmlPullParserException {
        FileInputStream is = new FileInputStream(file);
        return read(is); // closes the stream
    }

    @SuppressWarnings("deprecation")
    public static Feature read(InputStream input) throws IOException, XmlPullParserException {
        XmlStreamReader reader = ReaderFactory.newXmlReader(input);
        try {
            return new Feature(Xpp3DomBuilder.build(reader));
        } finally {
            reader.close();
        }
    }

    public static void write(Feature feature, File file) throws IOException {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        try {
            Xpp3DomWriter.write(writer, feature.dom);
        } finally {
            writer.close();
        }
    }

    public static Feature readJar(File file) throws IOException, XmlPullParserException {
		JarFile jar = new JarFile(file);
		try {
			ZipEntry ze = jar.getEntry(FEATURE_XML);
			if ( ze != null )
			{
    			InputStream is = jar.getInputStream(ze);
    			return read(is);
			}
			throw new IOException( file.getAbsolutePath() + " does not have " + FEATURE_XML + " entry." );
		} finally {
			jar.close();
		}
	}

    /**
     * Convenience method to load feature.xml file from either feature jar file
     * or directory.
     * 
     * @throws RuntimeException if feature descriptor can not be read or parsed.
     */
    public static Feature loadFeature( File location )
    {
        try
        {
            Feature feature;
            if ( location.isDirectory() )
            {
                feature = Feature.read( new File( location, Feature.FEATURE_XML ) );
            }
            else
            {
                // eclipse does NOT support packed features
                feature = Feature.readJar( location );
            }
            return feature;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Could not read feature descriptor at " + location.getAbsolutePath(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RuntimeException( "Could not read feature descriptor at " + location.getAbsolutePath(), e );
        }
    }

}
