package org.eclipse.tycho.versions.pom;

import java.util.ArrayList;
import java.util.List;

import de.pdark.decentxml.Element;

public class DependencyManagement
{
    final Element dependencyManagement;

    DependencyManagement( Element dependencyManagement )
    {
        this.dependencyManagement = dependencyManagement;
    }

    public List<GAV> getDependencies()
    {

        List<GAV> result = new ArrayList<GAV>();

        Element dependencies = dependencyManagement.getChild( "dependencies" );

        if ( dependencies != null )
        {
            for ( Element dependency : dependencies.getChildren( "dependency" ) )
                result.add( new GAV( dependency ) );
        }

        return result;
    }
}
