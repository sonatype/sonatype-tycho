package com.sonatype.tycho.enforcer;

public class VersionConstraintRule
{
    private String constraint;
    private String nameTemplate;
    private String versionRange;

    public String getNameTemplate()
    {
        return nameTemplate;
    }

    public String getVersionRange()
    {
        return versionRange;
    }

    @Override
    public String toString()
    {
        return constraint + ": " + nameTemplate + ";version=" + versionRange;
    }
    
}
