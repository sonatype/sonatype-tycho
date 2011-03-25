package org.codehaus.tycho.p2;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.transfer.NoRepositoryConnectorException;

@Component( role = RepositoryConnectorFactory.class, hint = "p2" )
public class P2RepositoryConnectorFactory
    implements RepositoryConnectorFactory
{

    public int getPriority()
    {
        return 0;
    }

    public RepositoryConnector newInstance( RepositorySystemSession session, RemoteRepository repository )
        throws NoRepositoryConnectorException
    {
        if ( P2ArtifactRepositoryLayout.ID.equals( repository.getContentType() ) )
        {
            return new P2RepositoryConnector( repository );
        }
        throw new NoRepositoryConnectorException( repository );
    }

}
