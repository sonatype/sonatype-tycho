package org.sonatype.tycho.p2.maven.repository.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.MatchQuery;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.facade.internal.LocalTychoRepositoryIndex;
import org.sonatype.tycho.p2.maven.repository.LocalMetadataRepository;

@SuppressWarnings( { "restriction" } )
public class LocalMetadataRepositoryTest
{
    private IProgressMonitor monitor = new NullProgressMonitor();

    private IMetadataRepositoryManager manager;

    static class AnyIUQuery
        extends MatchQuery
    {
        @Override
        public boolean isMatch( Object candidate )
        {
            return candidate instanceof IInstallableUnit;
        }
    }

    @Before
    public void initializeMetadataRepositoryManager()
    {
        manager =
            (IMetadataRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IMetadataRepositoryManager.class.getName() );
        if ( manager == null )
        {
            throw new IllegalStateException( "No metadata repository manager found" ); //$NON-NLS-1$
        }
    }

    @Test
    public void emptyRepository()
        throws CoreException
    {
        File location = new File( "target/empty" );
        createRepository( location, "group", "artifact", "version" );

        manager.removeRepository( location.toURI() );

        IMetadataRepository repository = manager.loadRepository( location.toURI(), monitor );
        Assert.assertNotNull( repository );
    }

    @SuppressWarnings( "unchecked" )
    protected LocalMetadataRepository createRepository( File location, String groupId, String artifactId, String version )
        throws ProvisionException
    {
        location.mkdirs();
        File metadataFile = new File( location, LocalTychoRepositoryIndex.INDEX_RELPATH );
        metadataFile.delete();
        metadataFile.getParentFile().mkdirs();

        Map properties = new HashMap();
        properties.put( RepositoryLayoutHelper.PROP_GROUP_ID, groupId );
        properties.put( RepositoryLayoutHelper.PROP_ARTIFACT_ID, artifactId );
        properties.put( RepositoryLayoutHelper.PROP_VERSION, version );

        return (LocalMetadataRepository) manager.createRepository( location.toURI(), location.getAbsolutePath(),
                                                                   LocalMetadataRepository.class.getName(), properties );
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void addInstallableUnit()
        throws CoreException
    {
        File location = new File( "target/metadataRepo" );
        LocalMetadataRepository repository = createRepository( location, "group", "artifact", "version" );

        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        iud.setId( "test" );
        iud.setVersion( new Version( "1.0.0" ) );

        iud.setProperty( RepositoryLayoutHelper.PROP_GROUP_ID, "group" );
        iud.setProperty( RepositoryLayoutHelper.PROP_ARTIFACT_ID, "artifact" );
        iud.setProperty( RepositoryLayoutHelper.PROP_VERSION, "version" );

        IInstallableUnit iu = MetadataFactory.createInstallableUnit( iud );
        repository.addInstallableUnits( new IInstallableUnit[] { iu } );
        
        manager.removeRepository( location.toURI() );
        repository = (LocalMetadataRepository) manager.loadRepository( location.toURI(), monitor );

        Collector iusCollector = repository.query( new AnyIUQuery(), new Collector(), monitor );
        ArrayList<IInstallableUnit> ius = new ArrayList<IInstallableUnit>( iusCollector.toCollection() );
        Assert.assertEquals( 1, ius.size() );
        Assert.assertEquals( iu.getId(), ius.get( 0 ).getId() );
    }

}
