package a;

import b.B;

public class A
{
    public B b = new B();

    public void confuseJDT()
    {
        b.confuseJDT( "boo" );
    }
}
