package org.sonatype.tycho.versions.pom;

import de.pdark.decentxml.Element;

public class Parent
{
    private final Element dom;

    public Parent( Element element )
    {
        this.dom = element;
    }

    public String getGroupId()
    {
        return dom.getChild( "groupId" ).getText();
    }

    public String getArtifactId()
    {
        return dom.getChild( "artifactId" ).getText();
    }

    public String getVersion()
    {
        return dom.getChild( "version" ).getText();
    }

    public void setVersion( String version )
    {
        dom.getChild( "version" ).setText( version );
    }
}
