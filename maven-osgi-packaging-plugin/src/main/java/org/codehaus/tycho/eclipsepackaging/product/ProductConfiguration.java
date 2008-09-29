package org.codehaus.tycho.eclipsepackaging.product;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("product")
public class ProductConfiguration {

	@XStreamAsAttribute
	private String application;

	private ConfigIni configIni;

	@XStreamAsAttribute
	private String id;

	private LauncherArguments launcherArgs;

	@XStreamAsAttribute
	private String name;

	private List<Plugin> plugins;

	@XStreamAsAttribute
	private Boolean useFeatures;

	@XStreamAsAttribute
	private String version;

	@XStreamOmitField
	private String windowImages;
	@XStreamOmitField
	private String launcher;
	@XStreamOmitField
	private String vm;
	
	public String getApplication() {
		return application;
	}

	public ConfigIni getConfigIni() {
		return configIni;
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

}
