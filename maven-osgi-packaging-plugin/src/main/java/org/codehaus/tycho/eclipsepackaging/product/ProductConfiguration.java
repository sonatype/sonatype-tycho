package org.codehaus.tycho.eclipsepackaging.product;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("product")
public class ProductConfiguration {

	@XStreamAsAttribute
	private String application;

	private ConfigIni configIni;

	private List<Feature> features;

	@XStreamAsAttribute
	private String id;

	private LauncherArguments launcherArgs;
	
	private Launcher launcher;

	@XStreamAsAttribute
	private String name;

	private List<Plugin> plugins;

	@XStreamAsAttribute
	private Boolean useFeatures;

	@XStreamAsAttribute
	private String version;

	public String getApplication() {
		return application;
	}

	public ConfigIni getConfigIni() {
		return configIni;
	}

	public List<Feature> getFeatures() {
		return features;
	}

	public String getId() {
		return id;
	}

	public LauncherArguments getLauncherArgs() {
		return launcherArgs;
	}

	public String getName() {
		return name;
	}

	public List<Plugin> getPlugins() {
		return plugins;
	}

	public Boolean getUseFeatures() {
		return useFeatures;
	}

	public String getVersion() {
		return version;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public void setConfigIni(ConfigIni configIni) {
		this.configIni = configIni;
	}

	public void setFeatures(List<Feature> features) {
		this.features = features;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setLauncherArgs(LauncherArguments launcherArgs) {
		this.launcherArgs = launcherArgs;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPlugins(List<Plugin> plugins) {
		this.plugins = plugins;
	}

	public void setUseFeatures(Boolean useFeatures) {
		this.useFeatures = useFeatures;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Launcher getLauncher() {
		return launcher;
	}

	public void setLauncher(Launcher launcher) {
		this.launcher = launcher;
	}

}
