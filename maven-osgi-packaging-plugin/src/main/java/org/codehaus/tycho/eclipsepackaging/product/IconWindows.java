package org.codehaus.tycho.eclipsepackaging.product;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("win")
public class IconWindows {

	private boolean useIco;
	
	private IconWindowsIco ico;

	private IconWindowsBmps bmp;

	public IconWindowsIco getIco() {
		return ico;
	}

	public void setIco(IconWindowsIco ico) {
		this.ico = ico;
	}

	public IconWindowsBmps getBmp() {
		return bmp;
	}

	public void setBmp(IconWindowsBmps bmp) {
		this.bmp = bmp;
	}

	public boolean getUseIco() {
		return useIco;
	}

	public void setUseIco(boolean useIco) {
		this.useIco = useIco;
	}

}
