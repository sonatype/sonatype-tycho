/*
 * 	Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.tycho.osgicompiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.osgitools.OsgiStateController;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class ClasspathComputer3_0 {
	public static class ClasspathElement {
		private String path;
		private String accessRules;
		
		/**
		 * Create a ClasspathElement object
		 * @param path
		 * @param accessRules
		 * @throws NullPointerException if path is null
		 */
		public ClasspathElement(String path, String accessRules){
			this.path = path;
			this.accessRules = accessRules;
		}
		public String toString() {
			return path;
		}
		public String getPath() {
			return path;
		}
		public String getAccessRules(){
			return accessRules;
		}
		public void addRules(String newRule){
			if (accessRules.equals("") || accessRules.equals(newRule)) //$NON-NLS-1$
				return;
			if (!newRule.equals("")) { //$NON-NLS-1$
				String join = accessRules.substring(0, accessRules.length() - EXCLUDE_ALL_RULE.length() - 1);
				newRule = join + newRule.substring(1);
			}
			accessRules = newRule;
			return;
		}
		/**
		 * ClasspathElement objects are equal if they have the same path.
		 * Access rules are not considered.
		 */
		public boolean equals(Object obj) {
			if (obj instanceof ClasspathElement) {
				ClasspathElement element = (ClasspathElement) obj;
				return (path != null && path.equals(element.getPath()));
			}
			return false;
		}
		public int hashCode() {
			return path.hashCode();
		}
		
		public static String normalize(String path) {
			//always use '/' as a path separator to help with comparing paths in equals
			return path.replaceAll("\\\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private static final String EXCLUDE_ALL_RULE = "-**/*"; //$NON-NLS-1$
	private static final String error_pluginCycle = "A cycle was detected when generating the classpath {0}.";//$NON-NLS-1$ 
	
	private Map visiblePackages = null;
	private Map/*<String,ClasspathElement>*/ pathElements = null;
	private OsgiState state;
	private BundleDescription theBundle;
	private BundleStorageManager epm;
	
	public ClasspathComputer3_0(OsgiState osgiState, BundleStorageManager epm) {
		this.state = osgiState;
		this.epm = epm;
	}

	/**
	 * Compute the classpath for the given jar.
	 * The path returned conforms to Parent / Prerequisite / Self  
	 * 
	 * @param model the plugin containing the jar compiled
	 * @param jar the jar for which the classpath is being compiled
	 * @return a list of ClasspathElement
	 * @
	 */
	public List<ClasspathElement> getClasspath(BundleDescription model)  {
		this.theBundle = model;
		List classpath = new ArrayList(20);
		List pluginChain = new ArrayList(10); //The list of plugins added to detect cycle
		
		Set addedPlugins = new HashSet(10); //The set of all the plugins already added to the classpath (this allows for optimization)
		pathElements = new HashMap();
		visiblePackages = getVisiblePackages(model);

		//SELF
		addSelf(model,  classpath, pluginChain, addedPlugins);

		//PREREQUISITE
		BundleDescription[] dependencies = state.getDependencies(model);
		for (int i = 0; i < dependencies.length; i++) {
			addPlugin(dependencies[i], classpath);
		}

		return classpath;

	}

	private Map getVisiblePackages(BundleDescription model) {
		Map packages = new HashMap(20);
		StateHelper helper = state.getStateHelper();
		addVisiblePackagesFromState(helper, model, packages);
		if (model.getHost() != null)
			addVisiblePackagesFromState(helper, (BundleDescription)model.getHost().getSupplier(), packages);
		return packages;
	}
	
	private void addVisiblePackagesFromState(StateHelper helper, BundleDescription model, Map packages) {
		ExportPackageDescription[] exports = helper.getVisiblePackages(model);
		for (int i = 0; i < exports.length; i++) {
			BundleDescription exporter = exports[i].getExporter();
			if (exporter == null)
				continue;
			
			boolean discouraged = helper.getAccessCode(model, exports[i]) == StateHelper.ACCESS_DISCOURAGED;
			String pattern = exports[i].getName().replaceAll("\\.", "/") + "/*"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String rule = (discouraged ? '~' : '+') + pattern;
			
			String rules = (String) packages.get(exporter.getSymbolicName());
			if (rules != null) {
				if (rules.indexOf(rule) == -1)
					rules = (rules != null) ? rules + File.pathSeparator + rule : rule;
			} else {
				rules = rule;
			}
				
			packages.put(exporter.getSymbolicName(), rules);
		}
	}
	/**
	 * Add the specified plugin (including its jars) and its fragments 
	 * @param plugin
	 * @param classpath
	 * @param location
	 * @
	 */
	private void addPlugin(BundleDescription plugin, List classpath)  {
//		boolean allFragments = true;
//		String patchInfo = (String)state.getPatchData().get(new Long(plugin.getBundleId()));
//		if (patchInfo != null && plugin != theBundle) {
//			addFragmentsLibraries(plugin, classpath,false, false);
//			allFragments = false;
//		}
		addRuntimeLibraries(plugin, classpath);
	}

	/**
	 * Add the runtime libraries for the specified plugin. 
	 * @param model
	 * @param classpath
	 * @param baseLocation
	 * @
	 */
	private void addRuntimeLibraries(BundleDescription model, List classpath)  {
		String[] libraries = getBundleClasspath(model);
		MavenProject project = state.getMavenProject(model);
		String base = null;
		File artifact = null;
		if (project != null && project.getArtifact().getFile() != null) {
			artifact = project.getArtifact().getFile();
			if (artifact.isFile() && artifact.canRead()) {
				base = artifact.getAbsolutePath();
			}
		}
		if (base == null) {
			base = model.getLocation();
		}

		for (int i = 0; i < libraries.length; i++) {
//			addDevEntries(model, baseLocation, classpath, Utils.getArrayFromString(modelProps.getProperty(PROPERTY_OUTPUT_PREFIX + libraries[i])));
			addPathAndCheck(model, base, artifact, libraries[i], classpath);
		}
	}

	/**
	 * Add all fragments of the given plugin
	 * @param plugin
	 * @param classpath
	 * @param baseLocation
	 * @
	 */
	private void addFragmentsLibraries(BundleDescription plugin, List classpath, boolean afterPlugin, boolean all)  {
		// if plugin is not a plugin, it's a fragment and there is no fragment for a fragment. So we return.
		BundleDescription[] fragments = plugin.getFragments();
		if (fragments == null)
			return;

		for (int i = 0; i < fragments.length; i++) {
			if (fragments[i] == theBundle)
				continue;
//			if (matchFilter(fragments[i]) == false)
//				continue;
			//check resolved status instead of filter
			if(!fragments[i].isResolved())
				continue;
			if (! afterPlugin && isPatchFragment(fragments[i])) {
				addPluginLibrariesToFragmentLocations(plugin, fragments[i], classpath);
				addRuntimeLibraries(fragments[i], classpath);
				continue;
			}
			if ( (afterPlugin && !isPatchFragment(fragments[i])) || all) {
				addRuntimeLibraries(fragments[i], classpath);
				addPluginLibrariesToFragmentLocations(plugin, fragments[i], classpath);
				continue;
			}
		}
	}

	private boolean isPatchFragment(BundleDescription fragment)  {
		return state.getPatchData().get(new Long(fragment.getBundleId())) != null;
	}

	/**
	 * There are cases where the plug-in only declares a library but the real JAR is under
	 * a fragment location. This method gets all the plugin libraries and place them in the
	 * possible fragment location.
	 * 
	 * @param plugin
	 * @param fragment
	 * @param classpath
	 * @param baseLocation
	 * @
	 */
	private void addPluginLibrariesToFragmentLocations(BundleDescription plugin, BundleDescription fragment, List classpath)  {
		//TODO This methods causes the addition of a lot of useless entries. See bug #35544
		//If we reintroduce the test below, we reintroduce the problem 35544	
		//	if (fragment.getRuntime() != null)
		//		return;
		String[] libraries = getBundleClasspath(plugin);

		String root = fragment.getLocation();
		//IPath base = Utils.makeRelative(new Path(root), new Path(baseLocation));
		//Properties modelProps = getBuildPropertiesFor(fragment);
		for (int i = 0; i < libraries.length; i++) {
			addPathAndCheck(fragment, root, null, libraries[i], classpath);
		}
	}

	// Add a path into the classpath for a given model
	// pluginId the plugin we are adding to the classpath
	// basePath : the relative path between the plugin from which we are adding the classpath and the plugin that is requiring this entry 
	// classpath : The classpath in which we want to add this path 
	private void addPathAndCheck(BundleDescription model, String basePath, File artifact, String libraryName,  List classpath) {
		String pluginId = model != null ? model.getSymbolicName() : null;
		String rules = ""; //$NON-NLS-1$
		//only add access rules to libraries that are not part of the current bundle
		//and are not this bundle's host if we are a fragment
		if (model != null && model != theBundle && (theBundle.getHost() == null || theBundle.getHost().getSupplier() != model) ) {
			String packageKey = pluginId;
			if (model.isResolved() && model.getHost() != null) {
				packageKey = ((BundleDescription) model.getHost().getSupplier()).getSymbolicName();
			}
			if (visiblePackages.containsKey(packageKey)) {
				rules = "[" + (String) visiblePackages.get(packageKey) + File.pathSeparator + EXCLUDE_ALL_RULE + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				rules = "[" + EXCLUDE_ALL_RULE + "]"; //$NON-NLS-1$//$NON-NLS-2$
			}
		}

		String path = null;
		try {
			File libraryFile = epm.getLibraryFile(model, basePath, artifact, libraryName);
			if (libraryFile != null && libraryFile.exists()) {
				path = libraryFile.getAbsolutePath();
			}
		} catch(IOException e ) {
			
		}
		
		if(path != null)
			addClasspathElementWithRule(classpath, path, rules);
	}

	private void addClasspathElementWithRule(List classpath, String path, String rules) {
		String normalizedPath = ClasspathElement.normalize(path);
		ClasspathElement existing = (ClasspathElement) pathElements.get(normalizedPath);
		if (existing != null){
			existing.addRules( rules);
		} else {
			ClasspathElement element = new ClasspathElement(normalizedPath, rules);
			classpath.add(element);
			pathElements.put(normalizedPath, element);
		}
	}

	/**
	 * 
	 * @param model
	 * @param classpath
	 * @param pluginChain
	 * @param addedPlugins set of {@link BundleDescription}
	 */
	private void addSelf(BundleDescription model, List classpath, List pluginChain, Set addedPlugins)  {
		// If model is a fragment, we need to add in the classpath the plugin to which it is related
		HostSpecification host = model.getHost();
		if (host != null) {
			BundleDescription[] hosts = host.getHosts();
			for (int i = 0; i < hosts.length; i++)
				addPluginAndPrerequisites(hosts[i], classpath, pluginChain, addedPlugins);
		}

		// Add the libraries
		File artifact = new File(state.getMavenProject(model).getBuild().getOutputDirectory()).getAbsoluteFile();
		artifact.mkdirs();
		String[] libraries = getBundleClasspath(model);
		for (int i = 0; i < libraries.length; i++) {
			String libraryName = libraries[i];
			addPathAndCheck(model, model.getLocation(), artifact, libraryName, classpath);
		}

	}

	/**
	 * Add the prerequisite of a given plugin (target)
	 * 
	 * @param classpath list of ClasspathElement
	 * @param pluginChain list of BundleDescription
	 * @param  addedPlugins set of BundleDescription
	 */
	private void addPrerequisites(BundleDescription target, List classpath, List pluginChain, Set addedPlugins)  {
		if (pluginChain.contains(target)) {
			String cycleString = ""; //$NON-NLS-1$
			for (Iterator iter = pluginChain.iterator(); iter.hasNext();)
				cycleString += iter.next().toString() + ", "; //$NON-NLS-1$
			cycleString += target.toString();
			throw new RuntimeException(NLS.bind(error_pluginCycle, cycleString));
		}
		if (addedPlugins.contains(target)) //the plugin we are considering has already been added	
			return;

		// add libraries from pre-requisite plug-ins.  Don't worry about the export flag
		// as all required plugins may be required for compilation.
		BundleDescription[] requires = OsgiStateController.getDependentBundles(target);
		pluginChain.add(target);
		for (int i = 0; i < requires.length; i++) {
			addPluginAndPrerequisites(requires[i], classpath,  pluginChain, addedPlugins);
		}
		pluginChain.remove(target);
		addedPlugins.add(target);
	}

	/**
	 * The pluginChain parameter is used to keep track of possible cycles. If prerequisite is already
	 * present in the chain it is not included in the classpath.
	 * 
	 * @param target : the plugin for which we are going to introduce
	 * @param classpath 
	 * @param baseLocation
	 * @param pluginChain
	 * @param addedPlugins
	 * @
	 */
	private void addPluginAndPrerequisites(BundleDescription target, List classpath,  List pluginChain, Set addedPlugins)  {
//		if (matchFilter(target) == false)
//			return;
		if(!target.isResolved())
			return;

		addPlugin(target, classpath);
		addPrerequisites(target, classpath,  pluginChain, addedPlugins);
	}

	private String[] getBundleClasspath(BundleDescription bundle)  {
		String[] result = new String[] {"."};
		String classpath = state.getManifestAttribute(bundle, Constants.BUNDLE_CLASSPATH);
		if (classpath != null) {
			ManifestElement[] classpathEntries;
			try {
				classpathEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, classpath);
				result = new String[classpathEntries.length];
				for (int i = 0; i < classpathEntries.length; i++) {
					result[i] = classpathEntries[i].getValue();
				}
			} catch (BundleException e) {
				// ignore
			}
		}
		return result;
	}
}
