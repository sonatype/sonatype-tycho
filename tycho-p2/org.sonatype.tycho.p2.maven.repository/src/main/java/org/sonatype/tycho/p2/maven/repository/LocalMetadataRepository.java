package org.sonatype.tycho.p2.maven.repository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.TouchpointType;
import org.eclipse.equinox.internal.p2.metadata.UpdateDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.sonatype.tycho.p2.maven.repository.xstream.PropertiesConverter;
import org.sonatype.tycho.p2.maven.repository.xstream.VersionConverter;
import org.sonatype.tycho.p2.maven.repository.xstream.VersionRangeConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.io.xml.XppDriver;

@SuppressWarnings( "restriction" )
public class LocalMetadataRepository
    extends AbstractMetadataRepository
{

    private static final String REPOSITORY_TYPE = LocalMetadataRepository.class.getName();

    private static final String REPOSITORY_VERSION = "1.0.0";

    // must match extension point filter in plugin.xml
    public static final String METADATA_FILE = ".p2/metadata.properties";

    private Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();

    private Map<GAV, Set<IInstallableUnit>> unitsMap = new HashMap<GAV, Set<IInstallableUnit>>();

    /**
     * Create new repository
     */
    public LocalMetadataRepository( URI location, String name, Map properties )
    {
        super( location.toString(), REPOSITORY_TYPE, REPOSITORY_VERSION, location, null, null, properties );
        if ( !location.getScheme().equals( "file" ) )
        {
            throw new IllegalArgumentException( "Invalid local repository location: " + location ); //$NON-NLS-1$
        }

        // when creating a repository, we must ensure it exists on disk so a subsequent load will succeed
        save();
    }

    @Override
    public void addInstallableUnits( IInstallableUnit[] newUnits )
    {
        for ( IInstallableUnit unit : newUnits )
        {
            GAV gav = RepositoryLayoutHelper.getGAV( unit.getProperties() );

            this.units.add( unit );

            Set<IInstallableUnit> gavUnits = unitsMap.get( gav );
            if ( gavUnits == null )
            {
                gavUnits = new HashSet<IInstallableUnit>();
                unitsMap.put( gav, gavUnits );
            }
            gavUnits.add( unit );
        }

        save();
    }

    private void save()
    {
        File basedir = new File( getLocation() );

        Properties properties = new Properties();

        XStream xs = getXStream();

        for ( Map.Entry<GAV, Set<IInstallableUnit>> gavEntry : unitsMap.entrySet() )
        {
            GAV gav = gavEntry.getKey();
            String relpath = RepositoryLayoutHelper.getRelativePath( gav, "p2content", "xml" );

            File file = new File( basedir, relpath );
            file.getParentFile().mkdirs();

            try
            {
                OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) );
                try
                {
                    xs.toXML( gavEntry.getValue(), os );
                }
                finally
                {
                    os.close();
                }

                properties.put( gav.toExternalForm(), relpath );
            }
            catch ( IOException e )
            {
                // XXX not good
            }
            catch ( XStreamException e )
            {
                // XXX not good
            }
        }

        try
        {
            OutputStream os = new BufferedOutputStream( new FileOutputStream( new File( basedir, METADATA_FILE ) ) );
            try
            {
                properties.store( os, null );
            }
            finally
            {
                os.close();
            }
        }
        catch ( IOException e )
        {
            // XXX not good
        }
    }

    private void load()
    {
        File basedir = new File( getLocation() );

        Properties properties = new Properties();

        try
        {
            InputStream is = new BufferedInputStream( new FileInputStream( new File( basedir, METADATA_FILE ) ) );
            try
            {
                properties.load( is );
            }
            finally
            {
                is.close();
            }
        }
        catch ( IOException e )
        {
            // should not really happen
            return;
        }

        XStream xs = getXStream();

        for ( Map.Entry gavEntry : properties.entrySet() )
        {
            GAV gav = GAV.parse( (String) gavEntry.getKey() );

            String relpath = RepositoryLayoutHelper.getRelativePath( gav, "p2content", "xml" );
            try
            {
                InputStream is = new BufferedInputStream( new FileInputStream( new File( basedir, relpath ) ) );
                try
                {
                    Set<IInstallableUnit> gavUnits = (Set<IInstallableUnit>) xs.fromXML( is );

                    unitsMap.put( gav, gavUnits );
                    units.addAll( gavUnits );
                }
                finally
                {
                    is.close();
                }
            }
            catch ( IOException e )
            {
                // too bad
            }
            catch ( XStreamException e )
            {
                // too bad
            }

        }
    }

    /**
     * Local existing repository
     */
    public LocalMetadataRepository( URI location )
    {
        super( location.toString(), REPOSITORY_TYPE, REPOSITORY_VERSION, location, null, null, null );

        load();
    }

    @Override
    public void initialize( RepositoryState state )
    {
    }

    public Collector query( Query query, Collector collector, IProgressMonitor monitor )
    {
        return query.perform( units.iterator(), collector );
    }

    @Override
    public boolean isModifiable()
    {
        return true;
    }

    public static XStream getXStream()
    {
        CompositeClassLoader cl = new CompositeClassLoader();
        cl.add( InstallableUnit.class.getClassLoader() );

        XStream xs = new XStream( null, new XppDriver(), cl );
        xs.setMode( XStream.NO_REFERENCES );

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

        return xs;
    }
}
