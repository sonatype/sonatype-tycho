package org.codehaus.tycho;

import java.util.List;

import org.codehaus.tycho.model.Target;

public class TargetPlatformConfiguration
{
    private String resolver;

    private TargetEnvironment environment;

    private List<String> repositories;

    private Target target;

    public TargetEnvironment getEnvironment()
    {
        return environment;
    }

    public String getTargetPlatformResolver()
    {
        return resolver;
    }
    
    public List<String> getRepositories()
    {
        return repositories;
    }

    public Target getTarget()
    {
        return target;
    }

    public void setEnvironment( TargetEnvironment environment )
    {
        this.environment = environment;
    }

    public void setResolver( String resolver )
    {
        this.resolver = resolver;
    }

    public void setRepositories( List<String> repositories )
    {
        this.repositories = repositories;
        
    }

    public void setTarget( Target target )
    {
        this.target = target;
    }
}
