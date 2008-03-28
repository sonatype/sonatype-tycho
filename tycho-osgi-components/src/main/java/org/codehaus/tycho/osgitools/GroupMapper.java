package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupMapper {

	private HashMap/* <Pattern, String> */groupMap;

	public GroupMapper(File configuration) throws IOException {
		groupMap = new HashMap/* <Pattern, String> */();

		if (configuration != null && configuration.exists()) {
			Properties p = new Properties();
			p.load(new FileInputStream(configuration));

			for (Iterator it = p.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				String groupPattern = (String) entry.getKey();
				String groupId = (String) entry.getValue();
				Pattern pattern = Pattern.compile(groupPattern);
				groupMap.put(pattern, groupId);
			}
		}
	}

	private Pattern group2Pattern = Pattern.compile("^(\\w*\\.\\w*)(\\..*)?$");
	private Pattern group3Pattern = Pattern.compile("^(\\w*\\.\\w*\\.\\w*)(\\..*)?$");
	
	public static String group3[] = {
		"net.sf",
		"org.apache",
		"org.codehaus",
		"org.tigris"
	};

	public String getGroupId(String symbolicName) {
		for (Iterator it = groupMap.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			Pattern key = (Pattern) entry.getKey();
			if (key.matcher(symbolicName).matches()) {
				return (String) entry.getValue();
			}
		}
		
		Matcher m = null;
		for (int i = 0; i < group3.length; i++) {
			if (symbolicName.startsWith(group3[i])) {
				m = group3Pattern.matcher(symbolicName);
			}
		}
		
		if (m == null) {
			m = group2Pattern.matcher(symbolicName);
		}
		
		if (m.matches()) {
			return m.group(1);
		}

		if (!symbolicName.contains(".")) {
			return symbolicName;
		}

		throw new IllegalArgumentException("artifactId " + symbolicName
				+ " does not match any pattern");
	}

}
