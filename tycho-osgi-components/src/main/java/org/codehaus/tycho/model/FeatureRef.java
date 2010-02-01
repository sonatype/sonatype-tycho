package org.codehaus.tycho.model;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class FeatureRef {

    protected final Xpp3Dom dom;

    public FeatureRef(Xpp3Dom dom) {
        this.dom = dom;
    }

    public String getId() {
        return dom.getAttribute("id");
    }

    public String getVersion() {
        return dom.getAttribute("version");
    }

    public void setVersion(String version) {
        dom.setAttribute("version", version);
    }

    @Override
    public String toString()
    {
        return getId() + "_" + getVersion();
    }

    public Xpp3Dom getDom()
    {
        return dom;
    }
    
}
