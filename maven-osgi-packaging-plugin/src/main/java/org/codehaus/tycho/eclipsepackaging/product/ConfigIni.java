package org.codehaus.tycho.eclipsepackaging.product;

public class ConfigIni {

	private String linux;
	
	private String macosx;

	private String solaris;

	private String win32;

	public String getLinux() {
		return linux;
	}

	public String getMacosx() {
		return macosx;
	}

	public String getSolaris() {
		return solaris;
	}

	public String getWin32() {
		return win32;
	}

	public void setLinux(String linux) {
		this.linux = linux;
	}

	public void setMacosx(String macosx) {
		this.macosx = macosx;
	}

	public void setSolaris(String solaris) {
		this.solaris = solaris;
	}

	public void setWin32(String win32) {
		this.win32 = win32;
	}

}
