package org.codehaus.tycho.eclipsepackaging.product;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("bmp")
public class IconWindowsBmps {

	@XStreamAsAttribute
	private String winSmallHigh;

	@XStreamAsAttribute
	private String winSmallLow;

	@XStreamAsAttribute
	private String winMediumHigh;

	@XStreamAsAttribute
	private String winMediumLow;

	@XStreamAsAttribute
	private String winLargeHigh;

	@XStreamAsAttribute
	private String winLargeLow;

	public String getWinSmallHigh() {
		return winSmallHigh;
	}

	public void setWinSmallHigh(String winSmallHigh) {
		this.winSmallHigh = winSmallHigh;
	}

	public String getWinSmallLow() {
		return winSmallLow;
	}

	public void setWinSmallLow(String winSmallLow) {
		this.winSmallLow = winSmallLow;
	}

	public String getWinMediumHigh() {
		return winMediumHigh;
	}

	public void setWinMediumHigh(String winMediumHigh) {
		this.winMediumHigh = winMediumHigh;
	}

	public String getWinMediumLow() {
		return winMediumLow;
	}

	public void setWinMediumLow(String winMediumLow) {
		this.winMediumLow = winMediumLow;
	}

	public String getWinLargeHigh() {
		return winLargeHigh;
	}

	public void setWinLargeHigh(String winLargeHigh) {
		this.winLargeHigh = winLargeHigh;
	}

	public String getWinLargeLow() {
		return winLargeLow;
	}

	public void setWinLargeLow(String winLargeLow) {
		this.winLargeLow = winLargeLow;
	}

}
