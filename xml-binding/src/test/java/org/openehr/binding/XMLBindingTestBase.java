package org.openehr.binding;

import java.io.InputStream;

import junit.framework.TestCase;

public class XMLBindingTestBase extends TestCase {
	
	public void setUp() throws Exception {
		binding = new XMLBinding();
	}
	
	protected InputStream fromClasspath(String filename) throws Exception {
		return this.getClass().getClassLoader().getResourceAsStream(filename);
	}

	protected XMLBinding binding;
}
