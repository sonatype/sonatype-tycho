package org.codehaus.tycho;

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

    public boolean match( String os, String ws, String arch )
    {
        return ( os == null || os.equals( this.os ) ) && //
            ( ws == null || ws.equals( this.ws ) ) && //
            ( arch == null || arch.equals( this.arch ) );
    }
}
