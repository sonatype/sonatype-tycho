package org.sonatype.tycho.p2.maven.repository.tests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.TouchpointType;
import org.eclipse.equinox.internal.p2.metadata.UpdateDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.MatchQuery;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.tycho.p2.maven.repository.LocalMetadataRepository;
import org.sonatype.tycho.p2.maven.repository.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.maven.repository.xstream.PropertiesConverter;
import org.sonatype.tycho.p2.maven.repository.xstream.VersionConverter;
import org.sonatype.tycho.p2.maven.repository.xstream.VersionRangeConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.io.xml.XppDriver;

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
        manager = (IMetadataRepositoryManager) ServiceHelper.getService(
            Activator.getContext(),
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

    protected LocalMetadataRepository createRepository( File location, String groupId, String artifactId, String version )
        throws ProvisionException
    {
        location.mkdirs();
        File metadataFile = new File( location, LocalMetadataRepository.METADATA_FILE );
        metadataFile.delete();
        metadataFile.getParentFile().mkdirs();

        Map properties = new HashMap();
        properties.put( RepositoryLayoutHelper.PROP_GROUP_ID, groupId );
        properties.put( RepositoryLayoutHelper.PROP_ARTIFACT_ID, artifactId );
        properties.put( RepositoryLayoutHelper.PROP_VERSION, version );

        return (LocalMetadataRepository) manager.createRepository(
            location.toURI(),
            location.getAbsolutePath(),
            LocalMetadataRepository.class.getName(),
            properties );
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void addInstallableUnit()
        throws CoreException
    {
        File location = new File( "target/repo" );
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

    public void xstream()
        throws Exception
    {
        IMetadataRepository repository = manager.loadRepository(
            new File( "/var/tmp/p2/ganymede-sr2/" ).toURI(),
            monitor );
        Collector collector = repository.query( InstallableUnitQuery.ANY, new Collector(), monitor );

        IInstallableUnit[] ius = (IInstallableUnit[]) collector.toArray( IInstallableUnit.class );

        // //////////////////////////////////////

        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        CompositeClassLoader cl = new CompositeClassLoader();
        cl.add( InstallableUnit.class.getClassLoader() );

        XStream xs = new XStream( null, new XppDriver(), cl );
        xs.setMode(XStream.NO_REFERENCES);

        xs.alias( "unit", InstallableUnit.class );
        xs.useAttributeFor( InstallableUnit.class, "id" );
        xs.useAttributeFor( InstallableUnit.class, "singleton" );
        xs.useAttributeFor( InstallableUnit.class, "version" );

        xs.aliasField( "provides", InstallableUnit.class, "providedCapabilities" );
        xs.alias( "provided", ProvidedCapability.class );
        xs.useAttributeFor( ProvidedCapability.class, "name" );
        xs.useAttributeFor( ProvidedCapability.class, "namespace" );
        xs.useAttributeFor( ProvidedCapability.class, "version" );

        xs.alias( "required", RequiredCapability.class );
        xs.useAttributeFor( RequiredCapability.class, "name" );
        xs.useAttributeFor( RequiredCapability.class, "namespace" );
        xs.useAttributeFor( RequiredCapability.class, "range" );
        xs.useAttributeFor( RequiredCapability.class, "multiple" );
        xs.useAttributeFor( RequiredCapability.class, "optional" );
        xs.useAttributeFor( RequiredCapability.class, "greedy" );

        xs.alias( "update", UpdateDescriptor.class );
        xs.useAttributeFor( UpdateDescriptor.class, "id" );
        xs.useAttributeFor( UpdateDescriptor.class, "range" );
        xs.useAttributeFor( UpdateDescriptor.class, "severity" );

        xs.alias( "artifact", ArtifactKey.class );
        xs.useAttributeFor( ArtifactKey.class, "id" );
        xs.useAttributeFor( ArtifactKey.class, "classifier" );
        xs.useAttributeFor( ArtifactKey.class, "version" );

        xs.alias( "touchpoint", TouchpointType.class );
        xs.useAttributeFor( TouchpointType.class, "id" );
        xs.useAttributeFor( TouchpointType.class, "version" );

        xs.registerConverter( new VersionConverter() );
        xs.registerConverter( new VersionRangeConverter() );
        xs.registerLocalConverter( InstallableUnit.class, "properties", new PropertiesConverter() );

        OutputStream os = new BufferedOutputStream( new FileOutputStream( "target/units.xml" ) );
        xs.toXML( ius, os );
        os.close();

        InputStream is = new BufferedInputStream( new FileInputStream( "target/units.xml" ) );
        ius = (IInstallableUnit[]) xs.fromXML( is );
        is.close();
    }
}
