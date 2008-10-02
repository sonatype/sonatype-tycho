package org.codehaus.tycho.eclipsepackaging.product;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("launcher")
public class Launcher {

	@XStreamAsAttribute
	private String name;
	
	private IconLinux linux;
	
	private IconMac macosx;
	
	private IconSolaris solaris;
	
	private IconWindows win;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public IconLinux getLinux() {
		return linux;
	}

	public void setLinux(IconLinux linux) {
		this.linux = linux;
	}

	public IconMac getMacosx() {
		return macosx;
	}

	public void setMacosx(IconMac macos) {
		this.macosx = macos;
	}

	public IconSolaris getSolaris() {
		return solaris;
	}

	public void setSolaris(IconSolaris solaris) {
		this.solaris = solaris;
	}

	public IconWindows getWin() {
		return win;
	}

	public void setWin(IconWindows win) {
		this.win = win;
	}
	
}
