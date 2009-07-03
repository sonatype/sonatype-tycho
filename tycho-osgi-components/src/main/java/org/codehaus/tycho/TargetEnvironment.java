package org.codehaus.tycho;

import java.util.Properties;

public class TargetEnvironment
{
    private String os;

    private String ws;

    private String arch;

    private String nl;

    public TargetEnvironment()
    {
        // do I really need no-arg constructor for mojo parameter injection?
    }

    public TargetEnvironment( Properties properties )
    {
        this.os = PlatformPropertiesUtils.getOS( properties );
        this.ws = PlatformPropertiesUtils.getWS( properties );
        this.arch = PlatformPropertiesUtils.getArch( properties );
    }

    public TargetEnvironment( String os, String ws, String arch, String nl )
    {
        this.os = os;
        this.ws = ws;
        this.arch = arch;
        this.nl = nl;
    }

    public String getOs()
    {
        return os;
    }

    public String getWs()
    {
        return ws;
    }

    public String getArch()
    {
        return arch;
    }

    public String getNl()
    {
        return nl;
    }
}
