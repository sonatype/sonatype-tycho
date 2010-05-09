package copied.org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.osgi.service.datalocation.Location;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 *  Used to parse a .product file.
 */
@SuppressWarnings( "restriction" )
public class ProductFile extends DefaultHandler implements IProductDescriptor {
	private static final String ATTRIBUTE_PATH = "path"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ICON = "icon"; //$NON-NLS-1$
	protected static final String ATTRIBUTE_FRAGMENT = "fragment"; //$NON-NLS-1$
	private static final String ATTRIBUTE_APPLICATION = "application"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$
	private static final String ATTRIBUTE_LOCATION = "location"; //$NON-NLS-1$
	private static final String ATTRIBUTE_AUTO_START = "autoStart"; //$NON-NLS-1$
	private static final String ATTRIBUTE_START_LEVEL = "startLevel"; //$NON-NLS-1$
	protected static final String ATTRIBUTE_VERSION = "version"; //$NON-NLS-1$
	protected static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String ATTRIBUTE_UID = "uid"; //$NON-NLS-1$

	private static final String PROPERTY_ECLIPSE_APPLICATION = "eclipse.application"; //$NON-NLS-1$
	private static final String PROPERTY_ECLIPSE_PRODUCT = "eclipse.product"; //$NON-NLS-1$

	private final static SAXParserFactory parserFactory = SAXParserFactory.newInstance();

	private static final String PROGRAM_ARGS = "programArgs"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_LINUX = "programArgsLin"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_MAC = "programArgsMac"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_SOLARIS = "programArgsSol"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_WIN = "programArgsWin"; //$NON-NLS-1$
	private static final String VM_ARGS = "vmArgs"; //$NON-NLS-1$
	private static final String VM_ARGS_LINUX = "vmArgsLin"; //$NON-NLS-1$
	private static final String VM_ARGS_MAC = "vmArgsMac"; //$NON-NLS-1$
	private static final String VM_ARGS_SOLARIS = "vmArgsSol"; //$NON-NLS-1$
	private static final String VM_ARGS_WIN = "vmArgsWin"; //$NON-NLS-1$

	private static final String SOLARIS_LARGE = "solarisLarge"; //$NON-NLS-1$
	private static final String SOLARIS_MEDIUM = "solarisMedium"; //$NON-NLS-1$
	private static final String SOLARIS_SMALL = "solarisSmall"; //$NON-NLS-1$
	private static final String SOLARIS_TINY = "solarisTiny"; //$NON-NLS-1$
	private static final String WIN32_16_LOW = "winSmallLow"; //$NON-NLS-1$
	private static final String WIN32_16_HIGH = "winSmallHigh"; //$NON-NLS-1$
	private static final String WIN32_24_LOW = "win24Low"; //$NON-NLS-1$
	private static final String WIN32_32_LOW = "winMediumLow"; //$NON-NLS-1$
	private static final String WIN32_32_HIGH = "winMediumHigh"; //$NON-NLS-1$
	private static final String WIN32_48_LOW = "winLargeLow"; //$NON-NLS-1$
	private static final String WIN32_48_HIGH = "winLargeHigh"; //$NON-NLS-1$

	private static final String OS_WIN32 = "win32";//$NON-NLS-1$
	private static final String OS_LINUX = "linux";//$NON-NLS-1$
	private static final String OS_SOLARIS = "solaris";//$NON-NLS-1$
	private static final String OS_MACOSX = "macosx";//$NON-NLS-1$

	//element names
	private static final String EL_FEATURES = "features"; //$NON-NLS-1$
	private static final String EL_FEATURE = "feature"; //$NON-NLS-1$
	private static final String EL_PLUGINS = "plugins"; //$NON-NLS-1$
	private static final String EL_PLUGIN = "plugin"; //$NON-NLS-1$
	private static final String EL_PRODUCT = "product"; //$NON-NLS-1$
	private static final String EL_PROPERTY = "property"; //$NON-NLS-1$
	private static final String EL_CONFIG_INI = "configIni"; //$NON-NLS-1$
	private static final String EL_LAUNCHER = "launcher"; //$NON-NLS-1$
	private static final String EL_LAUNCHER_ARGS = "launcherArgs"; //$NON-NLS-1$
	private static final String EL_SPLASH = "splash"; //$NON-NLS-1$
	private static final String EL_CONFIGURATIONS = "configurations"; //$NON-NLS-1$
	private static final String EL_LICENSE = "license"; //$NON-NLS-1$
	private static final String EL_URL = "url"; //$NON-NLS-1$
	private static final String EL_TEXT = "text"; //$NON-NLS-1$

	//These constants form a small state machine to parse the .product file
	private static final int STATE_START = 0;
	private static final int STATE_PRODUCT = 1;
	private static final int STATE_LAUNCHER = 2;
	private static final int STATE_LAUNCHER_ARGS = 3;
	private static final int STATE_PLUGINS = 4;
	private static final int STATE_FEATURES = 5;
	private static final int STATE_PROGRAM_ARGS = 6;
	private static final int STATE_PROGRAM_ARGS_LINUX = 7;
	private static final int STATE_PROGRAM_ARGS_MAC = 8;
	private static final int STATE_PROGRAM_ARGS_SOLARIS = 9;
	private static final int STATE_PROGRAM_ARGS_WIN = 10;
	private static final int STATE_VM_ARGS = 11;
	private static final int STATE_VM_ARGS_LINUX = 12;
	private static final int STATE_VM_ARGS_MAC = 13;
	private static final int STATE_VM_ARGS_SOLARIS = 14;
	private static final int STATE_VM_ARGS_WIN = 15;
	private static final int STATE_CONFIG_INI = 16;
	private static final int STATE_CONFIGURATIONS = 17;
	private static final int STATE_LICENSE = 18;
	private static final int STATE_LICENSE_URL = 19;
	private static final int STATE_LICENSE_TEXT = 20;

	private int state = STATE_START;

	private SAXParser parser;
	private String launcherName = null;
	//	private boolean useIco = false;
	private Map<String, Collection<String>> icons = new HashMap<String, Collection<String>>(6);
	private String configPath = null;
	private final Map<String, String> platformSpecificConfigPaths = new HashMap<String, String>();
	private String configPlatform = null;
	private String platformConfigPath = null;
	private String id = null;
	private String uid = null;
	private boolean useFeatures = false;
	protected List<IVersionedId> plugins = null;
	protected List<IVersionedId> fragments = null;
	private List<IVersionedId> features = null;
	private String splashLocation = null;
	private String productName = null;
	private String application = null;
	private String version = null;
	private Properties launcherArgs = new Properties();
	private File location;
	private List<BundleInfo> bundleInfos;
	private Map<String, String> properties;
	private String licenseURL;
	private String licenseText = null;

	private static String normalize(String text) {
		if (text == null || text.trim().length() == 0)
			return ""; //$NON-NLS-1$

		StringBuffer result = new StringBuffer(text.length());
		boolean haveSpace = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isWhitespace(c)) {
				if (haveSpace)
					continue;
				haveSpace = true;
				result.append(" "); //$NON-NLS-1$
			} else {
				haveSpace = false;
				result.append(c);
			}
		}
		return result.toString();
	}

	/**
	 * Constructs a product file parser.
	 */
	public ProductFile(String location) throws Exception {
		super();
		this.location = new File(location);

		parserFactory.setNamespaceAware(true);
		parser = parserFactory.newSAXParser();
		InputStream in = new BufferedInputStream(new FileInputStream(location));
		try {
			parser.parse(new InputSource(in), this);
		} finally {
			if (in != null)
				in.close();
		}
		parser = null;
	}

	/**
	 * Gets the name of the launcher specified in the .product file.
	 */
	public String getLauncherName() {
		return launcherName;
	}

	/**
	 * Gets the location of the .product file.
	 */
	public File getLocation() {
		return location;
	}

	/**
	 * Returns the properties found in .product file.  Properties
	 * are located in the <configurations> block of the file
	 */
	public Map<String, String> getConfigurationProperties() {
		Map<String, String> result = properties != null ? properties : new HashMap<String, String>();
		if (application != null && !result.containsKey(PROPERTY_ECLIPSE_APPLICATION))
			result.put(PROPERTY_ECLIPSE_APPLICATION, application);
		if (id != null && !result.containsKey(PROPERTY_ECLIPSE_PRODUCT))
			result.put(PROPERTY_ECLIPSE_PRODUCT, id);

		return result;
	}

	/**
	 * Returns a List<VersionedName> for each bundle that makes up this product.
	 * @param includeFragments Indicates whether or not fragments should
	 * be included in the list
	 */
	public List<IVersionedId> getBundles(boolean includeFragments) {
		List<IVersionedId> p = plugins != null ? plugins : CollectionUtils.<IVersionedId> emptyList();
		if (!includeFragments)
			return p;

		List<IVersionedId> f = fragments != null ? fragments : CollectionUtils.<IVersionedId> emptyList();
		int size = p.size() + f.size();
		if (size == 0)
			return CollectionUtils.emptyList();

		List<IVersionedId> both = new ArrayList<IVersionedId>(size);
		both.addAll(p);
		both.addAll(f);
		return both;
	}

	/**
	 * Returns a List<BundleInfo> for each bundle that has custom configuration data
	 * in the product file.
	 * @return A List<BundleInfo>
	 */
	public List<BundleInfo> getBundleInfos() {
		return bundleInfos != null ? bundleInfos : CollectionUtils.<BundleInfo> emptyList();
	}

	/**
	 * Returns a list<VersionedName> of fragments that constitute this product.
	 */
	public List<IVersionedId> getFragments() {
		return fragments != null ? fragments : CollectionUtils.<IVersionedId> emptyList();
	}

	/**
	 * Returns a List<VersionedName> of features that constitute this product.
	 */
	public List<IVersionedId> getFeatures() {
		return features != null ? features : CollectionUtils.<IVersionedId> emptyList();
	}

	public String[] getIcons(String os) {
		Collection<String> result = icons.get(os);
		if (result == null)
			return null;
		return result.toArray(new String[result.size()]);
	}

	public String getConfigIniPath(String os) {
		String specific = platformSpecificConfigPaths.get(os);
		return specific == null ? configPath : specific;
	}

	public String getConfigIniPath() {
		return configPath;
	}

	/**
	 * Returns the ID for this product.
	 */
	public String getId() {
		if (uid != null)
			return uid;
		return id;
	}

	public String getProductId() {
		return id;
	}

	/**
	 * Returns the location (the bundle) that defines the splash screen
	 */
	public String getSplashLocation() {
		return splashLocation;
	}

	/**
	 * Returns the product name.
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * Returns the application identifier for this product.
	 */
	public String getApplication() {
		return application;
	}

	/**
	 * Returns true if this product is built using feature, 
	 * false otherwise.
	 */
	public boolean useFeatures() {
		return useFeatures;
	}

	/**
	 * Returns the version of the product
	 */
	public String getVersion() {
		return (version == null || version.length() == 0) ? "0.0.0" : version; //$NON-NLS-1$
	}

	/**
	 * Returns the VM arguments for a specific platform.
	 * If the empty string is used for the OS, this returns
	 * the default VM arguments
	 */
	public String getVMArguments(String os) {
		os = os == null ? "" : os; //$NON-NLS-1$
		String key = null;
		if (os.equals(OS_WIN32)) {
			key = VM_ARGS_WIN;
		} else if (os.equals(OS_LINUX)) {
			key = VM_ARGS_LINUX;
		} else if (os.equals(OS_MACOSX)) {
			key = VM_ARGS_MAC;
		} else if (os.equals(OS_SOLARIS)) {
			key = VM_ARGS_SOLARIS;
		}

		String prefix = launcherArgs.getProperty(VM_ARGS);
		String platform = null, args = null;
		if (key != null)
			platform = launcherArgs.getProperty(key);
		if (prefix != null)
			args = platform != null ? prefix + " " + platform : prefix; //$NON-NLS-1$
		else
			args = platform != null ? platform : ""; //$NON-NLS-1$
		return normalize(args);
	}

	/**
	 * Returns the program arguments for a specific platform.
	 * If the empty string is used for the OS, this returns
	 * the default program arguments
	 */
	public String getProgramArguments(String os) {
		os = os == null ? "" : os; //$NON-NLS-1$
		String key = null;
		if (os.equals(OS_WIN32)) {
			key = PROGRAM_ARGS_WIN;
		} else if (os.equals(OS_LINUX)) {
			key = PROGRAM_ARGS_LINUX;
		} else if (os.equals(OS_MACOSX)) {
			key = PROGRAM_ARGS_MAC;
		} else if (os.equals(OS_SOLARIS)) {
			key = PROGRAM_ARGS_SOLARIS;
		}

		String prefix = launcherArgs.getProperty(PROGRAM_ARGS);
		String platform = null, args = null;
		if (key != null)
			platform = launcherArgs.getProperty(key);
		if (prefix != null)
			args = platform != null ? prefix + " " + platform : prefix; //$NON-NLS-1$
		else
			args = platform != null ? platform : ""; //$NON-NLS-1$
		return normalize(args);
	}

	public String getLicenseText() {
		return licenseText;
	}

	public String getLicenseURL() {
		return licenseURL;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		switch (state) {
			case STATE_START :
				if (EL_PRODUCT.equals(localName)) {
					processProduct(attributes);
					state = STATE_PRODUCT;
				}
				break;

			case STATE_PRODUCT :
				if (EL_CONFIG_INI.equals(localName)) {
					processConfigIni(attributes);
					state = STATE_CONFIG_INI;
				} else if (EL_LAUNCHER.equals(localName)) {
					processLauncher(attributes);
					state = STATE_LAUNCHER;
				} else if (EL_PLUGINS.equals(localName)) {
					state = STATE_PLUGINS;
				} else if (EL_FEATURES.equals(localName)) {
					state = STATE_FEATURES;
				} else if (EL_LAUNCHER_ARGS.equals(localName)) {
					state = STATE_LAUNCHER_ARGS;
				} else if (EL_SPLASH.equals(localName)) {
					splashLocation = attributes.getValue(ATTRIBUTE_LOCATION);
				} else if (EL_CONFIGURATIONS.equals(localName)) {
					state = STATE_CONFIGURATIONS;
				} else if (EL_LICENSE.equals(localName)) {
					state = STATE_LICENSE;
				}
				break;

			case STATE_CONFIG_INI :
				processConfigIniPlatform(localName, true);
				break;

			case STATE_LAUNCHER :
				if (OS_SOLARIS.equals(localName)) {
					processSolaris(attributes);
				} else if ("win".equals(localName)) { //$NON-NLS-1$
					processWin(attributes);
				} else if (OS_LINUX.equals(localName)) {
					processLinux(attributes);
				} else if (OS_MACOSX.equals(localName)) {
					processMac(attributes);
				}
				if ("ico".equals(localName)) { //$NON-NLS-1$
					processIco(attributes);
				} else if ("bmp".equals(localName)) { //$NON-NLS-1$
					processBmp(attributes);
				}
				break;

			case STATE_LAUNCHER_ARGS :
				if (PROGRAM_ARGS.equals(localName)) {
					state = STATE_PROGRAM_ARGS;
				} else if (PROGRAM_ARGS_LINUX.equals(localName)) {
					state = STATE_PROGRAM_ARGS_LINUX;
				} else if (PROGRAM_ARGS_MAC.equals(localName)) {
					state = STATE_PROGRAM_ARGS_MAC;
				} else if (PROGRAM_ARGS_SOLARIS.equals(localName)) {
					state = STATE_PROGRAM_ARGS_SOLARIS;
				} else if (PROGRAM_ARGS_WIN.equals(localName)) {
					state = STATE_PROGRAM_ARGS_WIN;
				} else if (VM_ARGS.equals(localName)) {
					state = STATE_VM_ARGS;
				} else if (VM_ARGS_LINUX.equals(localName)) {
					state = STATE_VM_ARGS_LINUX;
				} else if (VM_ARGS_MAC.equals(localName)) {
					state = STATE_VM_ARGS_MAC;
				} else if (VM_ARGS_SOLARIS.equals(localName)) {
					state = STATE_VM_ARGS_SOLARIS;
				} else if (VM_ARGS_WIN.equals(localName)) {
					state = STATE_VM_ARGS_WIN;
				}
				break;

			case STATE_PLUGINS :
				if (EL_PLUGIN.equals(localName)) {
					processPlugin(attributes);
				}
				break;

			case STATE_LICENSE :
				if (EL_URL.equals(localName)) {
					state = STATE_LICENSE_URL;
				} else if (EL_TEXT.equals(localName)) {
					licenseText = ""; //$NON-NLS-1$
					state = STATE_LICENSE_TEXT;
				}
				break;

			case STATE_FEATURES :
				if (EL_FEATURE.equals(localName)) {
					processFeature(attributes);
				}
				break;
			case STATE_CONFIGURATIONS :
				if (EL_PLUGIN.equals(localName)) {
					processPluginConfiguration(attributes);
				} else if (EL_PROPERTY.equals(localName)) {
					processPropertyConfiguration(attributes);
				}
				break;
		}
	}

	/**
	 * Processes the property tag in the .product file.  These tags contain
	 * a Name and Value pair.  For each tag (with a non-null name), a property 
	 * is created.
	 */
	private void processPropertyConfiguration(Attributes attributes) {
		String name = attributes.getValue(ATTRIBUTE_NAME);
		String value = attributes.getValue(ATTRIBUTE_VALUE);
		if (name == null)
			return;
		if (value == null)
			value = ""; //$NON-NLS-1$
		if (properties == null)
			properties = new HashMap<String, String>();
		properties.put(name, value);
	}

	private void processPluginConfiguration(Attributes attributes) {
		BundleInfo info = new BundleInfo();
		info.setSymbolicName(attributes.getValue(ATTRIBUTE_ID));
		info.setVersion(attributes.getValue(ATTRIBUTE_VERSION));
		String value = attributes.getValue(ATTRIBUTE_START_LEVEL);
		if (value != null) {
			int startLevel = Integer.parseInt(value);
			if (startLevel > 0)
				info.setStartLevel(startLevel);
		}
		value = attributes.getValue(ATTRIBUTE_AUTO_START);
		if (value != null)
			info.setMarkedAsStarted(Boolean.valueOf(value).booleanValue());
		if (bundleInfos == null)
			bundleInfos = new ArrayList<BundleInfo>();
		bundleInfos.add(info);
	}

	public void endElement(String uri, String localName, String qName) {
		switch (state) {
			case STATE_PLUGINS :
				if (EL_PLUGINS.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_FEATURES :
				if (EL_FEATURES.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_LAUNCHER_ARGS :
				if (EL_LAUNCHER_ARGS.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_LAUNCHER :
				if (EL_LAUNCHER.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_CONFIGURATIONS :
				if (EL_CONFIGURATIONS.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_LICENSE :
				if (EL_LICENSE.equals(localName))
					state = STATE_PRODUCT;
				break;

			case STATE_PROGRAM_ARGS :
			case STATE_PROGRAM_ARGS_LINUX :
			case STATE_PROGRAM_ARGS_MAC :
			case STATE_PROGRAM_ARGS_SOLARIS :
			case STATE_PROGRAM_ARGS_WIN :
			case STATE_VM_ARGS :
			case STATE_VM_ARGS_LINUX :
			case STATE_VM_ARGS_MAC :
			case STATE_VM_ARGS_SOLARIS :
			case STATE_VM_ARGS_WIN :
				state = STATE_LAUNCHER_ARGS;
				break;
			case STATE_LICENSE_URL :
			case STATE_LICENSE_TEXT :
				state = STATE_LICENSE;
				break;

			case STATE_CONFIG_INI :
				if (EL_CONFIG_INI.equals(localName))
					state = STATE_PRODUCT;
				else
					processConfigIniPlatform(localName, false);
				break;
		}
	}

	public void characters(char[] ch, int start, int length) {
		switch (state) {
			case STATE_PROGRAM_ARGS :
				addLaunchArgumentToMap(PROGRAM_ARGS, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_LINUX :
				addLaunchArgumentToMap(PROGRAM_ARGS_LINUX, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_MAC :
				addLaunchArgumentToMap(PROGRAM_ARGS_MAC, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_SOLARIS :
				addLaunchArgumentToMap(PROGRAM_ARGS_SOLARIS, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_WIN :
				addLaunchArgumentToMap(PROGRAM_ARGS_WIN, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS :
				addLaunchArgumentToMap(VM_ARGS, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_LINUX :
				addLaunchArgumentToMap(VM_ARGS_LINUX, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_MAC :
				addLaunchArgumentToMap(VM_ARGS_MAC, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_SOLARIS :
				addLaunchArgumentToMap(VM_ARGS_SOLARIS, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_WIN :
				addLaunchArgumentToMap(VM_ARGS_WIN, String.valueOf(ch, start, length));
				break;
			case STATE_CONFIG_INI :
				if (platformConfigPath != null)
					platformConfigPath += String.valueOf(ch, start, length);
				break;
			case STATE_LICENSE_URL :
				licenseURL = String.valueOf(ch, start, length);
				break;
			case STATE_LICENSE_TEXT :
				if (licenseText != null)
					licenseText += String.valueOf(ch, start, length);
				break;

		}
	}

	private void addLaunchArgumentToMap(String key, String value) {
		if (launcherArgs == null)
			launcherArgs = new Properties();

		String oldValue = launcherArgs.getProperty(key);
		if (oldValue != null)
			launcherArgs.setProperty(key, oldValue + value);
		else
			launcherArgs.setProperty(key, value);
	}

	protected void processPlugin(Attributes attributes) {
		String fragment = attributes.getValue(ATTRIBUTE_FRAGMENT);
		IVersionedId name = new VersionedId(attributes.getValue(ATTRIBUTE_ID), attributes.getValue(ATTRIBUTE_VERSION));
		if (fragment != null && new Boolean(fragment).booleanValue()) {
			if (fragments == null)
				fragments = new ArrayList<IVersionedId>();
			fragments.add(name);
		} else {
			if (plugins == null)
				plugins = new ArrayList<IVersionedId>();
			plugins.add(name);
		}
	}

	private void processFeature(Attributes attributes) {
		IVersionedId name = new VersionedId(attributes.getValue(ATTRIBUTE_ID), attributes.getValue(ATTRIBUTE_VERSION));
		if (features == null)
			features = new ArrayList<IVersionedId>();
		features.add(name);
	}

	private void processProduct(Attributes attributes) {
		id = attributes.getValue(ATTRIBUTE_ID);
		uid = attributes.getValue(ATTRIBUTE_UID);
		productName = attributes.getValue(ATTRIBUTE_NAME);
		application = attributes.getValue(ATTRIBUTE_APPLICATION);
		String use = attributes.getValue("useFeatures"); //$NON-NLS-1$
		if (use != null)
			useFeatures = Boolean.valueOf(use).booleanValue();
		version = attributes.getValue(ATTRIBUTE_VERSION);
	}

	private void processConfigIni(Attributes attributes) {
		String path = null;
		if ("custom".equals(attributes.getValue("use"))) { //$NON-NLS-1$//$NON-NLS-2$
			path = attributes.getValue(ATTRIBUTE_PATH);
		}
		String os = attributes.getValue("os"); //$NON-NLS-1$
		if (os != null && os.length() > 0) {
			// TODO should we allow a platform-specific default to over-ride a custom generic path?
			if (path != null)
				platformSpecificConfigPaths.put(os, path);
		} else if (path != null) {
			configPath = path;
		}
	}

	private void processConfigIniPlatform(String key, boolean begin) {
		if (begin) {
			configPlatform = key;
			platformConfigPath = ""; //$NON-NLS-1$
		} else if (configPlatform.equals(key) && platformConfigPath.length() > 0) {
			platformSpecificConfigPaths.put(key, platformConfigPath);
			platformConfigPath = null;
		}
	}

	private void processLauncher(Attributes attributes) {
		launcherName = attributes.getValue(ATTRIBUTE_NAME);
	}

	private void addIcon(String os, String value) {
		if (value == null)
			return;

		File iconFile = new File(value);
		if (!iconFile.isFile()) {
			//workspace
			Location instanceLocation = (Location) ServiceHelper.getService(Activator.getContext(), Location.class.getName(), Location.INSTANCE_FILTER);
			if (instanceLocation != null && instanceLocation.getURL() != null) {
				File workspace = URLUtil.toFile(instanceLocation.getURL());
				if (workspace != null)
					iconFile = new File(workspace, value);
			}
		}
		if (!iconFile.isFile())
			iconFile = new File(location.getParentFile(), value);

		Collection<String> list = icons.get(os);
		if (list == null) {
			list = new ArrayList<String>(6);
			icons.put(os, list);
		}
		list.add(iconFile.getAbsolutePath());
	}

	private void processSolaris(Attributes attributes) {
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_LARGE));
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_MEDIUM));
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_SMALL));
		addIcon(OS_SOLARIS, attributes.getValue(SOLARIS_TINY));
	}

	private void processWin(Attributes attributes) {
		//		useIco = Boolean.valueOf(attributes.getValue(P_USE_ICO)).booleanValue();
	}

	private void processIco(Attributes attributes) {
		addIcon(OS_WIN32, attributes.getValue(ATTRIBUTE_PATH));
	}

	private void processBmp(Attributes attributes) {
		addIcon(OS_WIN32, attributes.getValue(WIN32_16_HIGH));
		addIcon(OS_WIN32, attributes.getValue(WIN32_16_LOW));
		addIcon(OS_WIN32, attributes.getValue(WIN32_24_LOW));
		addIcon(OS_WIN32, attributes.getValue(WIN32_32_HIGH));
		addIcon(OS_WIN32, attributes.getValue(WIN32_32_LOW));
		addIcon(OS_WIN32, attributes.getValue(WIN32_48_HIGH));
		addIcon(OS_WIN32, attributes.getValue(WIN32_48_LOW));
	}

	private void processLinux(Attributes attributes) {
		addIcon(OS_LINUX, attributes.getValue(ATTRIBUTE_ICON));
	}

	private void processMac(Attributes attributes) {
		addIcon(OS_MACOSX, attributes.getValue(ATTRIBUTE_ICON));
	}

}
