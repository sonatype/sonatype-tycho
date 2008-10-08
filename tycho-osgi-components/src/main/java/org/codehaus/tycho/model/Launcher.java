package org.codehaus.tycho.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class Launcher {

	public static final String ICON_LINUX = "icon";
	public static final String ICON_MAC = ICON_LINUX;
	public static final String ICON_WINDOWS_ICO_PATH = "path";
	public static final String ICON_WINDOWS_LARGE_LOW = "winLargeLow";
	public static final String ICON_WINDOWS_LARGE_HIGH = "winLargeHigh";
	public static final String ICON_WINDOWS_MEDIUM_LOW = "winMediumLow";
	public static final String ICON_WINDOWS_MEDIUM_HIGH = "winMediumHigh";
	public static final String ICON_WINDOWS_SMALL_LOW = "winSmallLow";
	public static final String ICON_WINDOWS_SMALL_HIGH = "winSmallHigh";
	public static final String ICON_SOLARIS_TINY = "solarisTiny";
	public static final String ICON_SOLARIS_SMALL = "solarisSmall";
	public static final String ICON_SOLARIS_MEDIUM = "solarisMedium";
	public static final String ICON_SOLARIS_LARGE = "solarisLarge";

	private Xpp3Dom dom;

	public Launcher(Xpp3Dom dom) {
		this.dom = dom;
	}

	public String getName() {
		return dom.getAttribute("name");
	}

	public Map<String, String> getLinuxIcon() {
		Xpp3Dom linuxDom = dom.getChild("linux");
		if (linuxDom == null) {
			return Collections.emptyMap();
		}
		Map<String, String> linux = new HashMap<String, String>();
		putIfNotNull(linux, ICON_LINUX, linuxDom.getAttribute(ICON_LINUX));
		return Collections.unmodifiableMap(linux);
	}

	public Map<String, String> getMacosxIcon() {
		Xpp3Dom macosxDom = dom.getChild("macosx");
		if (macosxDom == null) {
			return Collections.emptyMap();
		}
		Map<String, String> mac = new HashMap<String, String>();
		putIfNotNull(mac, ICON_LINUX, macosxDom.getAttribute(ICON_LINUX));
		return Collections.unmodifiableMap(mac);
	}

	public Map<String, String> getSolarisIcon() {
		Xpp3Dom solarisDom = dom.getChild("solaris");
		if (solarisDom == null) {
			return Collections.emptyMap();
		}
		Map<String, String> solaris = new HashMap<String, String>();
		putIfNotNull(solaris, ICON_SOLARIS_LARGE, solarisDom
				.getAttribute(ICON_SOLARIS_LARGE));
		putIfNotNull(solaris, ICON_SOLARIS_MEDIUM, solarisDom
				.getAttribute(ICON_SOLARIS_MEDIUM));
		putIfNotNull(solaris, ICON_SOLARIS_SMALL, solarisDom
				.getAttribute(ICON_SOLARIS_SMALL));
		putIfNotNull(solaris, ICON_SOLARIS_TINY, solarisDom
				.getAttribute(ICON_SOLARIS_TINY));
		return Collections.unmodifiableMap(solaris);
	}

	public boolean getWindowsUseIco() {
		Xpp3Dom winDom = dom.getChild("win");
		if (winDom == null) {
			return false;
		}
		boolean useIco = Boolean.parseBoolean(winDom.getAttribute("useIco"));
		return useIco;
	}

	public Map<String, String> getWindowsIcon() {
		Xpp3Dom winDom = dom.getChild("win");
		if (winDom == null) {
			return Collections.emptyMap();
		}
		Map<String, String> windows = new HashMap<String, String>();
		if (getWindowsUseIco()) {
			Xpp3Dom ico = winDom.getChild("ico");
			if (ico != null) {
				putIfNotNull(windows, ICON_WINDOWS_ICO_PATH, ico
						.getAttribute(ICON_WINDOWS_ICO_PATH));
			}
		} else {
			Xpp3Dom bmp = winDom.getChild("bmp");
			if (bmp != null) {
				putIfNotNull(windows, ICON_WINDOWS_SMALL_HIGH, bmp
						.getAttribute(ICON_WINDOWS_SMALL_HIGH));
				putIfNotNull(windows, ICON_WINDOWS_SMALL_LOW, bmp
						.getAttribute(ICON_WINDOWS_SMALL_LOW));
				putIfNotNull(windows, ICON_WINDOWS_MEDIUM_HIGH, bmp
						.getAttribute(ICON_WINDOWS_MEDIUM_HIGH));
				putIfNotNull(windows, ICON_WINDOWS_MEDIUM_LOW, bmp
						.getAttribute(ICON_WINDOWS_MEDIUM_LOW));
				putIfNotNull(windows, ICON_WINDOWS_LARGE_HIGH, bmp
						.getAttribute(ICON_WINDOWS_LARGE_HIGH));
				putIfNotNull(windows, ICON_WINDOWS_LARGE_LOW, bmp
						.getAttribute(ICON_WINDOWS_LARGE_LOW));
			}
		}
		return Collections.unmodifiableMap(windows);
	}

	private void putIfNotNull(Map<String, String> map, String key, String value) {
		if (value != null) {
			map.put(key, value);
		}
	}

}
