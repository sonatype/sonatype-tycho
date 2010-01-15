package org.codehaus.tycho.osgicompiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;

/**
 * A source mapping for simply copying files to the target directory.
 * 
 * @author jan.sievers@sap.com
 */
public class CopyMapping implements SourceMapping {

	List<SourceTargetPair> sourceTargetMappings = new ArrayList<SourceTargetPair>();

	public Set getTargetFiles(File targetDir, String source)
			throws InclusionScanException {
		File targetFile = new File(targetDir, source);
		sourceTargetMappings.add(new SourceTargetPair(source, targetFile));
		return Collections.singleton(targetFile);
	}

	public List<SourceTargetPair> getSourceTargetPairs() {
		return sourceTargetMappings;
	}

	public static class SourceTargetPair {

		public String source;
		public File target;

		public SourceTargetPair(String source, File target) {
			this.source = source;
			this.target = target;
		}

	}
}
