package com.sonatype.tycho.enforcer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.osgitools.EquinoxResolver;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.BundleException;
import org.sonatype.tycho.ArtifactKey;

public class TychoDependencyVersion
    implements EnforcerRule
{
    private String requireBundles;

    private String importPackages;

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        try
        {
            EquinoxResolver equinoxResolver = (EquinoxResolver) helper.getComponent( EquinoxResolver.class );

            MavenProject project = (MavenProject) helper.evaluate( "${project}" );

            ArrayList<String> errors = new ArrayList<String>();

            String packaging = project.getPackaging();
            TychoProject projectType = (TychoProject) helper.getComponent( TychoProject.class.getName(), packaging );

            if ( projectType == null )
            {
                // unknown/unsupported packaging
                return;
            }

            TargetPlatform targetPlatform = projectType.getTargetPlatform( project );

            if ( ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals( packaging ) || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals( packaging ) )
            {
                State state;
                try
                {
                    state = equinoxResolver.newResolvedState( project, targetPlatform );
                }
                catch ( BundleException e )
                {
                    throw new EnforcerRuleException( "Could not resolve OSGi state", e );
                }
                BundleDescription bundle = state.getBundleByLocation( project.getBasedir().getAbsolutePath() );

                for ( BundleSpecification dep : bundle.getRequiredBundles() )
                {
                    validateVersionContraint( errors, dep, toVersionConstraints( requireBundles ) );
                }

                for ( ImportPackageSpecification dep : bundle.getImportPackages() )
                {
                    validateVersionContraint( errors, dep, toVersionConstraints( importPackages ) );
                }
            }
            else if ( ArtifactKey.TYPE_ECLIPSE_FEATURE.equals( packaging ) )
            {

            }

            if ( !errors.isEmpty() )
            {
                StringBuilder sb = new StringBuilder();
                for ( String error : errors )
                {
                    sb.append( "  " ).append( error ).append( '\n' );
                }
                throw new EnforcerRuleException( sb.toString() );
            }
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to lookup an expression " + e.getLocalizedMessage(), e );
        }
        catch ( ComponentLookupException e )
        {
            throw new EnforcerRuleException( "Unable to lookup a component" + e.getLocalizedMessage(), e );
        }
    }

    private List<VersionConstraintRule> toVersionConstraints( String configuration )
    {
        BufferedReader r = new BufferedReader( new StringReader( configuration ) );
        String str;
        try
        {
            while ( ( str = r.readLine() ) != null )
            {
                int idx = str.indexOf( '=' );
                if ( idx > 0 )
                {
                    String name = str.substring( 0, idx );
                    String version = str.substring( idx );
                }
                else
                {
                    // TODO warn or throw something
                }
            }
        }
        catch ( IOException e )
        {
            // can't happen
        }
        return null;
    }

    private boolean isIncluded( VersionRange expected, VersionRange actual )
    {
        return VersionRangeHelper.isIncluded( expected, actual );
    }

    private void validateVersionContraint( List<String> errors, VersionConstraint dependency,
                                           List<VersionConstraintRule> rules )
    {
        VersionRange version = dependency.getVersionRange();

        if ( version == null || version.equals( VersionRange.emptyRange ) )
        {
            errors.add( "No version range specified for " + dependency.toString() );
            return;
        }

        if ( rules != null && !rules.isEmpty() )
        {
            String name = dependency.getName();

            for ( VersionConstraintRule rule : rules )
            {
                VersionRange ruleRange = new VersionRange( rule.getVersionRange() );
                if ( SelectorUtils.match( rule.getNameTemplate(), name ) && !isIncluded( ruleRange, version ) )
                {
                    errors.add( "Dependency " + dependency.toString() + " violates rule " + rule.toString() );
                }
            }
        }
    }

    public boolean isCacheable()
    {
        return false;
    }

    public boolean isResultValid( EnforcerRule cachedRule )
    {
        return false;
    }

    public String getCacheId()
    {
        return getClass().getName();
    }
}
