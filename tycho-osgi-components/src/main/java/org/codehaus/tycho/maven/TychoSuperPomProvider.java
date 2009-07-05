package org.codehaus.tycho.maven;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.tycho.osgitools.utils.TychoVersion;

@Component(role = SuperPomProvider.class)
public class TychoSuperPomProvider implements SuperPomProvider {

	private static final String PLUGIN_GROUP_ID = "org.codehaus.tycho";

	private static final String[] PLUGIN_ARTIFACT_IDS = {
		"maven-osgi-compiler-plugin",
		"maven-osgi-packaging-plugin",
        "maven-osgi-source-plugin",
		"maven-osgi-test-plugin",
		"maven-tycho-plugin",
		"target-platform-configuration",
	};

	private List<Plugin> getTychoPlugins() {
		ArrayList<Plugin> plugins = new ArrayList<Plugin>();
		
		String version = TychoVersion.getTychoVersion();
		
		for (String artifactId : PLUGIN_ARTIFACT_IDS) {
			Plugin plugin = new Plugin();
			plugin.setGroupId(PLUGIN_GROUP_ID);
			plugin.setArtifactId(artifactId);
			plugin.setVersion(version);

			plugins.add(plugin);
		}

		return plugins;
	}

    /**
     * The cached super POM, lazily created.
     */
    private Model superModel;

    @Requirement
    private ModelReader modelReader;

    public Model getSuperModel( String version )
    {
        if ( superModel == null )
        {
            String resource = "/org/apache/maven/model/pom-" + version + ".xml";

            InputStream is = getClass().getResourceAsStream( resource );

            if ( is == null )
            {
                throw new IllegalStateException( "The super POM " + resource + " was not found"
                    + ", please verify the integrity of your Maven installation" );
            }

            try
            {
                superModel = modelReader.read( is, null );
                
                PluginManagement plugins = superModel.getBuild().getPluginManagement(); 

                for (Plugin plugin : getTychoPlugins()) {
                    plugins.addPlugin(plugin);
                }
                
            }
            catch ( IOException e )
            {
                throw new IllegalStateException( "The super POM " + resource + " is damaged"
                    + ", please verify the integrity of your Maven installation", e );
            }
        }

        return superModel;
    }

}
