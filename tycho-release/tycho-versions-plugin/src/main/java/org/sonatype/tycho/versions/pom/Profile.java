package org.sonatype.tycho.versions.pom;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import de.pdark.decentxml.Element;

public class Profile
{

    private final Element dom;

    public Profile( Element dom )
    {
        this.dom = dom;
    }

    public List<String> getModules()
    {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for ( Element modules : dom.getChildren( "modules" ) )
        {
            for ( Element module : modules.getChildren( "module" ) )
            {
                result.add( module.getText() );
            }
        }
        return new ArrayList<String>( result );
    }

}
