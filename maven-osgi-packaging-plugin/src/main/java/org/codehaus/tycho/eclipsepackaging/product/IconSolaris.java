package org.codehaus.tycho.eclipsepackaging.product;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("solaris")
public class IconSolaris {

	@XStreamAsAttribute
	private String solarisLarge;

	@XStreamAsAttribute
	private String solarisMedium;

	@XStreamAsAttribute
	private String solarisSmall;

	public String getSolarisLarge() {
		return solarisLarge;
	}

	public void setSolarisLarge(String solarisLarge) {
		this.solarisLarge = solarisLarge;
	}

	public String getSolarisMedium() {
		return solarisMedium;
	}

	public void setSolarisMedium(String solarisMedium) {
		this.solarisMedium = solarisMedium;
	}

	public String getSolarisSmall() {
		return solarisSmall;
	}

	public void setSolarisSmall(String solarisSmall) {
		this.solarisSmall = solarisSmall;
	}

	public String getSolarisTiny() {
		return solarisTiny;
	}

	public void setSolarisTiny(String solarisTiny) {
		this.solarisTiny = solarisTiny;
	}

	@XStreamAsAttribute
	private String solarisTiny;

}
