package org.sonatype.tycho.p2.facade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class P2ResolutionRequest
{
    private String id;
    private String name;
    private String version;
    private Map<String, String> roots = new HashMap<String, String>();
    private List<RepositoryContentLocator> repositories = new ArrayList<RepositoryContentLocator>();
    private List<String> environments = new ArrayList<String>();

    public P2ResolutionRequest( String id, String version, String name )
    {
        this.id = id;
        this.version = version;
        this.name = name;
    }

    public List<String> getTargetEnvironments()
    {
        return environments;
    }
    public void addTargetEnvironment( String environment )
    {
        environments.add( environment );
    }

    public Map<String, String> getRootInstallableUnits()
    {
        return roots;
    }

    public void addRootInstallableUnit( String id, String version )
    {
        roots.put( id, version );
    }

    public List<RepositoryContentLocator> getRepositories()
    {
        return repositories;
    }
    
    public void addRepository( RepositoryContentLocator repository )
    {
        repositories.add( repository );
    }

    public String getId()
    {
        return id;
    }

    public String getVersion()
    {
        return version;
    }

    public String getName()
    {
        return name;
    }
}
