package org.codehaus.tycho.model;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public interface IFeatureRef {

	String getId();

	String getVersion();

	void setVersion(String version);

    Xpp3Dom getDom();

}
