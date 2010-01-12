package org.codehaus.tycho;

import org.codehaus.tycho.model.Target;

public class TargetPlatformConfiguration
{
    private String resolver;

    private TargetEnvironment environment;

    private Target target;

    private String pomDependencies;

    private boolean ignoreTychoRepositories;

    public TargetEnvironment getEnvironment()
    {
        return environment;
    }

    public String getTargetPlatformResolver()
    {
        return resolver;
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

    public void setTarget( Target target )
    {
        this.target = target;
    }

    public void setPomDependencies( String pomDependencies )
    {
        this.pomDependencies = pomDependencies;
    }

    public String getPomDependencies()
    {
        return pomDependencies;
    }

    public void setIgnoreTychoRepositories( boolean ignoreTychoRepositories )
    {
        this.ignoreTychoRepositories = ignoreTychoRepositories;
    }

    public boolean isIgnoreTychoRepositories()
    {
        return ignoreTychoRepositories;
    }
}
