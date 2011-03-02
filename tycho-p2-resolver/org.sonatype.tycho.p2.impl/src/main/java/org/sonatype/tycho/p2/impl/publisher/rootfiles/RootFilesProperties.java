package org.sonatype.tycho.p2.impl.publisher.rootfiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RootFilesProperties
{

    public class Permission
    {
        private final String path;

        private final String chmodPermissionPattern;

        public Permission( String path, String chmodPermissionPattern )
        {
            this.path = path.trim();
            this.chmodPermissionPattern = chmodPermissionPattern;
        }

        public String[] toP2Format()
        {
            return new String[] { chmodPermissionPattern, path };
        }

    }

    private List<Permission> permissions = new ArrayList<Permission>();

    public Collection<Permission> getPermissions()
    {
        return permissions;
    }

    public void addPermission( String chmodPermissionPattern, String[] pathsInInstallation )
    {
        for ( String path : pathsInInstallation )
        {
            permissions.add( new Permission( path, chmodPermissionPattern ) );
        }
    }
}
