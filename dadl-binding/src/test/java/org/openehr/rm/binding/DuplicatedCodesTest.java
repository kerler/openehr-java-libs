package org.openehr.rm.binding;

import org.openehr.am.parser.DADLParser;

public class DuplicatedCodesTest extends DADLBindingTestBase {
	
	public void testDuplicatedCodesInMap() throws Exception {
		DADLParser parser = new DADLParser(fromClasspath("duplicated_codes.dadl"));
		parser.parse();
	}

}
