package noop;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal noop
 */
public class Noop
    extends AbstractMojo
{
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
    }

}
