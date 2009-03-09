package org.codehaus.tycho.maven;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.osgitools.utils.TychoVersion;

@Component(role = ProjectBuilder.class)
public class TychoProjectBuilder extends DefaultProjectBuilder {
	
	private static final String PLUGIN_GROUP_ID = "org.codehaus.tycho";

	private static final String[] PLUGIN_ARTIFACT_IDS = {
		"maven-osgi-compiler-plugin",
		"maven-osgi-lifecycle-plugin",
		"maven-osgi-packaging-plugin",
		"maven-osgi-test-plugin",
		"maven-targetplatform-plugin",
		"maven-tycho-plugin",
		"maven-osgi-source-plugin",
	};
	
    private Model superModel;

	@Override
	public Model getSuperModel() {
		if (superModel == null) {
			superModel = super.getSuperModel();

			PluginManagement plugins = superModel.getBuild().getPluginManagement(); 

			for (Plugin plugin : getTychoPlugins()) {
				plugins.addPlugin(plugin);
			}
		}

		return superModel;

	}

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
}
