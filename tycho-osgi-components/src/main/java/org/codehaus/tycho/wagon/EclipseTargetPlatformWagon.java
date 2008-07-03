package org.codehaus.tycho.wagon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.osgitools.OsgiState;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * @plexus.component role="org.apache.maven.wagon.Wagon" role-hint="eclipse"
 */
public class EclipseTargetPlatformWagon extends AbstractWagon implements Contextualizable {

	/** @plexus.requirement */
	private OsgiState state;

	private PlexusContainer plexus;

	@Override
	protected void closeConnection() throws ConnectionException {
	}
	public void openConnection() throws ConnectionException, AuthenticationException {
	}

	public void get(String resourceName, File destination) throws TransferFailedException,	ResourceDoesNotExistException, AuthorizationException {
		// javax/xml/javax.xml/1.3.4.v2008041723/javax.xml-1.3.4.v2008041723.jar
//		System.err.println(">>> " + resourceName);
		String[] elements = resourceName.split("/");
		if (resourceName.endsWith(".jar") && elements.length >= 3) {
			String version = elements[elements.length - 2];
			String symbolicName = elements[elements.length - 3];
			BundleDescription desc = state.getBundleDescription(symbolicName, version);
			if (desc != null) {
				File file = new File(desc.getLocation());
				try {
					if (file.isFile()) {
						FileUtils.copyFile(file, destination);
					} else if (file.isDirectory()){
						archive(file, destination);
					}
				} catch (Exception e) {
					throw new TransferFailedException(e.getMessage(), e);
				}
			}
		}
	}

	private void archive(File dir, File destination) throws Exception {
		Set<String> files = new LinkedHashSet<String>();
		list(dir, null, files);
		
		byte[] buf = new byte[1024];
		destination.getParentFile().mkdirs();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destination));
        for (String source : files) {
            File file = new File(dir, source);
            if (file.isFile()) {
				FileInputStream in = new FileInputStream(file);
	            out.putNextEntry(new ZipEntry(source));
	            int len;
	            while ((len = in.read(buf)) > 0) {
	                out.write(buf, 0, len);
	            }
	            out.closeEntry();
	            in.close();
            }
        }
        out.close();
		
//		JarArchiver jarArchiver = (JarArchiver) plexus.lookup(JarArchiver.ROLE, "jar");
//		jarArchiver.setDestFile(destination);
//		jarArchiver.addDirectory(dir);
//		jarArchiver.createArchive();
	}

	private void list(File basedir, String relpath, Set<String> members) {
		File dir = relpath == null? basedir: new File(basedir, relpath);
		for (File member : dir.listFiles()) {
			String memberPath = relpath == null? member.getName() : relpath + "/" + member.getName();
			members.add(memberPath);
			if (member.isDirectory()) {
				list(basedir, memberPath, members);
			}
		}
	}

	public boolean getIfNewer(String resourceName, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException,	AuthorizationException {
		return false;
	}

	public void put(File arg0, String arg1) throws TransferFailedException,	ResourceDoesNotExistException, AuthorizationException {
		throw new AuthorizationException("Not supported");
	}

	public void contextualize(Context context) throws ContextException {
		plexus = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
	}

	@Override
	protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
	}

}
