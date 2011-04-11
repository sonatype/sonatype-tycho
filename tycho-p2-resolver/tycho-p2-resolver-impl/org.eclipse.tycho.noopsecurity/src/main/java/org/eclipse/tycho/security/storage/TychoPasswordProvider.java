package org.eclipse.tycho.security.storage;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;

public class TychoPasswordProvider
    extends PasswordProvider
{

    @Override
    public PBEKeySpec getPassword( IPreferencesContainer container, int passwordType )
    {
        return new PBEKeySpec( "secret".toCharArray() );
    }

}
