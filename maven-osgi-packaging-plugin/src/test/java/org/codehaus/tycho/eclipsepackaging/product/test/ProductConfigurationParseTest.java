package org.codehaus.tycho.eclipsepackaging.product.test;

import junit.framework.Assert;

import org.codehaus.tycho.eclipsepackaging.product.ConfigIni;
import org.codehaus.tycho.eclipsepackaging.product.Launcher;
import org.codehaus.tycho.eclipsepackaging.product.LauncherArguments;
import org.codehaus.tycho.eclipsepackaging.product.ProductConfiguration;
import org.codehaus.tycho.eclipsepackaging.product.ProductConfigurationConverter;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

public class ProductConfigurationParseTest {

	@Test
	public void testProductConfigurationParse() throws Exception {
		XStream xs = new XStream();
		xs.registerConverter(new ProductConfigurationConverter());
		xs.processAnnotations(ProductConfiguration.class);

		ProductConfiguration config = (ProductConfiguration) xs
				.fromXML(getClass().getResourceAsStream(
						"/product/MyFirstRCP.product"));

		Assert.assertEquals("My First RCP", config.getName());
		Assert.assertEquals("MyFirstRCP.product1", config.getId());
		Assert.assertEquals("MyFirstRCP.application", config.getApplication());
		Assert.assertEquals(false, config.getUseFeatures().booleanValue());

		ConfigIni configIni = config.getConfigIni();
		Assert.assertNotNull(configIni);
		Assert.assertEquals("linux.ini", configIni.getLinux());
		Assert.assertEquals("macosx.ini", configIni.getMacosx());
		Assert.assertEquals("solaris.ini", configIni.getSolaris());
		Assert.assertEquals("win32.ini", configIni.getWin32());

		LauncherArguments launcherArgs = config.getLauncherArgs();
		Assert.assertNotNull(launcherArgs);
		Assert.assertEquals("-all args", launcherArgs.getProgramArgs());
		Assert.assertEquals("-linux args", launcherArgs.getProgramArgsLin());
		Assert.assertEquals("-mac args", launcherArgs.getProgramArgsMac());
		Assert.assertEquals("-solaris args", launcherArgs.getProgramArgsSol());
		Assert.assertEquals("-win32 args", launcherArgs.getProgramArgsWin());
		Assert.assertEquals("-all vm", launcherArgs.getVmArgs());
		Assert.assertEquals("-linux vm", launcherArgs.getVmArgsLin());
		Assert.assertEquals("-mac vm", launcherArgs.getVmArgsMac());
		Assert.assertEquals("-solaris vm", launcherArgs.getVmArgsSol());
		Assert.assertEquals("-win32 vm", launcherArgs.getVmArgsWin());

		Launcher launcher = config.getLauncher();
		Assert.assertNotNull(launcher);
		Assert.assertEquals("launchername", launcher.getName());
		Assert.assertEquals("XPM", launcher.getLinux().getIcon());
		Assert.assertEquals("icns", launcher.getMacosx().getIcon());
		Assert.assertEquals("large", launcher.getSolaris().getSolarisLarge());
		Assert.assertEquals("medium", launcher.getSolaris().getSolarisMedium());
		Assert.assertEquals("small", launcher.getSolaris().getSolarisSmall());
		Assert.assertEquals("tiny", launcher.getSolaris().getSolarisTiny());
		Assert.assertEquals(false, launcher.getWin().getUseIco());
		Assert.assertEquals("iconon", launcher.getWin().getIco().getPath());
		Assert.assertEquals("16-32", launcher.getWin().getBmp().getWinSmallHigh());
		Assert.assertEquals("16-8", launcher.getWin().getBmp().getWinSmallLow());
		Assert.assertEquals("32-32", launcher.getWin().getBmp().getWinMediumHigh());
		Assert.assertEquals("32-8", launcher.getWin().getBmp().getWinMediumLow());
		Assert.assertEquals("48-32", launcher.getWin().getBmp().getWinLargeHigh());
		Assert.assertEquals("48-8", launcher.getWin().getBmp().getWinLargeLow());

	}

}
