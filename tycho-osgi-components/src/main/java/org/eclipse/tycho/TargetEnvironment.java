package org.eclipse.tycho;

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

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = 17 * hash + ( os != null ? os.hashCode() : 0 );
        hash = 17 * hash + ( ws != null ? ws.hashCode() : 0 );
        hash = 17 * hash + ( arch != null ? arch.hashCode() : 0 );
        hash = 17 * hash + ( nl != null ? nl.hashCode() : 0 );
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof TargetEnvironment ) )
        {
            return false;
        }

        TargetEnvironment other = (TargetEnvironment) obj;

        return eq( os, other.os ) && eq( ws, other.ws ) && eq( arch, other.arch ) && eq( nl, other.nl );
    }

    private static boolean eq( String a, String b )
    {
        return a != null ? a.equals( b ) : b == null;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( os ).append( '/' ).append( ws ).append( '/' ).append( arch );
        return sb.toString();
    }
}
