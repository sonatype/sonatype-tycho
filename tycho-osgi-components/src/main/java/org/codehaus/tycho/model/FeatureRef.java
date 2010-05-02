package org.codehaus.tycho.model;

import de.pdark.decentxml.Element;

public class FeatureRef
{

    protected final Element dom;

    public FeatureRef( Element dom )
    {
        this.dom = dom;
    }

    public String getId()
    {
        return dom.getAttributeValue( "id" );
    }

    public String getVersion()
    {
        return dom.getAttributeValue( "version" );
    }

    public void setVersion( String version )
    {
        dom.setAttribute( "version", version );
    }

    @Override
    public String toString()
    {
        return getId() + "_" + getVersion();
    }

    public Element getDom()
    {
        return dom;
    }

}
