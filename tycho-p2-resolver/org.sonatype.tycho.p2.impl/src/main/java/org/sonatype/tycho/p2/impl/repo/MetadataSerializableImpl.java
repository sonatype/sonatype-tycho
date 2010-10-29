package org.sonatype.tycho.p2.impl.repo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryIO;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.sonatype.tycho.p2.MetadataSerializable;
import org.sonatype.tycho.p2.impl.Activator;

@SuppressWarnings( "restriction" )
public class MetadataSerializableImpl
    implements MetadataSerializable
{
    private static final String SUFFIX_QUALIFIER = ".qualifier"; //$NON-NLS-1$

    private final IProvisioningAgent agent;

    public MetadataSerializableImpl()
        throws ProvisionException
    {
        super();
        this.agent = Activator.newProvisioningAgent();
    }

    public void serialize( OutputStream stream, Set<Object> installableUnits, String qualifier )
        throws IOException
    {
        final List<IInstallableUnit> units = toInstallableUnits( installableUnits );

        replaceBuildQualifier( units, qualifier );

        // TODO check if we can really "reuse" LocalMetadataRepository or should we implement our own Repository
        AbstractMetadataRepository targetRepo =
            new AbstractMetadataRepository( agent, "TychoTargetPlatform", LocalMetadataRepository.class.getName(), //$NON-NLS-1$
                                            "0.0.1", null, null, null, null ) //$NON-NLS-1$
            {

                @Override
                public void initialize( RepositoryState state )
                {

                }

                public Collection<IRepositoryReference> getReferences()
                {
                    return Collections.emptyList();
                }

                public IQueryResult<IInstallableUnit> query( IQuery<IInstallableUnit> query, IProgressMonitor monitor )
                {
                    return query.perform( units.iterator() );
                }

            };

        new MetadataRepositoryIO( agent ).write( targetRepo, stream );
    }

    private List<IInstallableUnit> toInstallableUnits( Set<Object> installableUnits )
    {
        ArrayList<IInstallableUnit> units = new ArrayList<IInstallableUnit>();

        for ( Object o : installableUnits )
        {
            units.add( (IInstallableUnit) o );
        }

        return units;
    }

    public void replaceBuildQualifier( List<IInstallableUnit> units, String qualifier )
    {
        for ( IInstallableUnit iu : units )
        {
            if ( isQualifierVersion( iu.getVersion() ) )
            {
                InstallableUnit modifiableIu = (InstallableUnit) iu;
                final Version qualifiedIuVersion = createQualifiedVersion( iu.getVersion(), qualifier );
                modifiableIu.setVersion( qualifiedIuVersion );
                qualifyCapabilityVersions( modifiableIu, qualifier );
                qualifyRequiredVersions( modifiableIu, qualifier );
                qualifyArtifactKeys( modifiableIu, qualifier );
                if ( iu.getUpdateDescriptor() != null )
                {
                    final VersionRange range = new VersionRange( Version.emptyVersion, true, qualifiedIuVersion, false );
                    modifiableIu.setUpdateDescriptor( MetadataFactory.createUpdateDescriptor( iu.getId(), range,
                                                                                              IUpdateDescriptor.NORMAL,
                                                                                              null ) );
                }
            }
        }
    }

    private void qualifyArtifactKeys( InstallableUnit iu, String qualifier )
    {
        List<IArtifactKey> qualifiedArtifacts = new ArrayList<IArtifactKey>();
        for ( IArtifactKey artifactKey : iu.getArtifacts() )
        {
            if ( isQualifierVersion( artifactKey.getVersion() ) )
            {
                IArtifactKey qualfiedKey =
                    new ArtifactKey( artifactKey.getClassifier(), artifactKey.getId(),
                                     createQualifiedVersion( artifactKey.getVersion(), qualifier ) );
                qualifiedArtifacts.add( qualfiedKey );
            }
            else
            {
                qualifiedArtifacts.add( artifactKey );
            }
        }
        iu.setArtifacts( qualifiedArtifacts.toArray( new IArtifactKey[qualifiedArtifacts.size()] ) );
    }

    private void qualifyRequiredVersions( InstallableUnit iu, String qualifier )
    {
        List<IRequirement> qualifiedRequirements = new ArrayList<IRequirement>();
        for ( IRequirement requirement : iu.getRequirements() )
        {
            IRequirement replacedRequirement = requirement;
            if ( requirement instanceof RequiredCapability )
            {
                RequiredCapability requiredCapability = (RequiredCapability) requirement;
                final VersionRange unqualifiedVersionRange = requiredCapability.getRange();
                if ( isQualifierVersion( unqualifiedVersionRange.getMinimum() ) )
                {
                    VersionRange range =
                        new VersionRange( createQualifiedVersion( unqualifiedVersionRange.getMinimum(), qualifier ),
                                          unqualifiedVersionRange.getIncludeMinimum(),
                                          createQualifiedVersion( unqualifiedVersionRange.getMaximum(), qualifier ),
                                          unqualifiedVersionRange.getIncludeMaximum() );
                    replacedRequirement =
                        new RequiredCapability( requiredCapability.getNamespace(), requiredCapability.getName(), range,
                                                requiredCapability.getFilter(), requiredCapability.getMin(),
                                                requiredCapability.getMax(), requiredCapability.isGreedy(),
                                                requiredCapability.getDescription() );
                }
            }
            qualifiedRequirements.add( replacedRequirement );
        }
        iu.setRequiredCapabilities( qualifiedRequirements.toArray( new IRequirement[qualifiedRequirements.size()] ) );
    }

    private void qualifyCapabilityVersions( InstallableUnit iu, String qualifier )
    {
        List<IProvidedCapability> qualifiedCapabilities = new ArrayList<IProvidedCapability>();
        for ( IProvidedCapability capability : iu.getProvidedCapabilities() )
        {
            if ( isQualifierVersion( capability.getVersion() ) )
            {
                IProvidedCapability qualifiedCapability =
                    new ProvidedCapability( capability.getNamespace(), capability.getName(),
                                            createQualifiedVersion( capability.getVersion(), qualifier ) );
                qualifiedCapabilities.add( qualifiedCapability );
            }
            else
            {
                qualifiedCapabilities.add( capability );
            }
        }
        iu.setCapabilities( qualifiedCapabilities.toArray( new IProvidedCapability[qualifiedCapabilities.size()] ) );
    }

    private Version createQualifiedVersion( final Version version, String qualifier )
    {
        if ( isQualifierVersion( version ) )
        {
            final String oldVersion = version.toString();
            Version newVersion =
                Version.create( oldVersion.substring( 0, oldVersion.length() - SUFFIX_QUALIFIER.length() + 1 )
                    + qualifier );
            return newVersion;
        }
        return version;
    }

    private boolean isQualifierVersion( final Version version )
    {
        return version.toString().endsWith( SUFFIX_QUALIFIER );
    }

}
