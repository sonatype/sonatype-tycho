package org.codehaus.tycho.eclipsepackaging.product.test;

import junit.framework.Assert;

import org.codehaus.tycho.eclipsepackaging.product.ConfigIni;
import org.codehaus.tycho.eclipsepackaging.product.LauncherArguments;
import org.codehaus.tycho.eclipsepackaging.product.ProductConfiguration;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

public class ProductConfigurationParseTest {

	@Test
	public void testProductConfigurationParse() {
		XStream xs = new XStream();
		xs.processAnnotations(ProductConfiguration.class);
		
		ProductConfiguration config = (ProductConfiguration) xs.fromXML(getClass().getResourceAsStream("/product/MyFirstRCP.product"));
		
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

		LauncherArguments launcher = config.getLauncherArgs();
		Assert.assertNotNull(launcher);
		Assert.assertEquals("-all args", launcher.getProgramArgs());
		Assert.assertEquals("-linux args", launcher.getProgramArgsLin());
		Assert.assertEquals("-mac args", launcher.getProgramArgsMac());
		Assert.assertEquals("-solaris args", launcher.getProgramArgsSol());
		Assert.assertEquals("-win32 args", launcher.getProgramArgsWin());
		Assert.assertEquals("-all vm", launcher.getVmArgs());
		Assert.assertEquals("-linux vm", launcher.getVmArgsLin());
		Assert.assertEquals("-mac vm", launcher.getVmArgsMac());
		Assert.assertEquals("-solaris vm", launcher.getVmArgsSol());
		Assert.assertEquals("-win32 vm", launcher.getVmArgsWin());

	}

}
