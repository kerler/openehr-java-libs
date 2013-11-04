/*
 * component:   "openEHR Reference Implementation"
 * description: "Class MultipleLanguageTest"
 * keywords:    "archetype"
 *
 * author:      "Leo Simons <leo@medvision360.com>"
 * support:     "MEDvision360 <support@medvision360.com>"
 * copyright:   "Copyright (c) 2013 MEDvision360"
 * license:     "See notice at bottom of class"
 *
 * file:        "$URL:$"
 * revision:    "$LastChangedRevision: $"
 * last_change: "$LastChangedDate: $"
 */
package org.openehr.am.serialize;

import java.util.ArrayList;
import java.util.List;

import org.openehr.am.archetype.ontology.ArchetypeOntology;
import org.openehr.am.archetype.ontology.ArchetypeTerm;
import org.openehr.am.archetype.ontology.OntologyDefinitions;

/**
 * Serialization test that has embedded double quotes and backslashes
 * 
 * @author Leo Simons
 */
public class StringEscapesTest extends SerializerTestBase {
	
	public StringEscapesTest(String test) {
		super(test);
	}
	
	public void testPrintOntology() throws Exception {
        List<OntologyDefinitions> termDefinitionsList =
            new ArrayList<OntologyDefinitions>();
        List<ArchetypeTerm> items = new ArrayList<ArchetypeTerm>();
		ArchetypeTerm item = new ArchetypeTerm("at0001",
                "text with \"quoted content\" for at0001", "desc with a literal backslash \\ at0001");
        items.add(item);
        
        OntologyDefinitions definitions = new OntologyDefinitions("en", items);
        termDefinitionsList.add(definitions);

        List<OntologyDefinitions> constraintDefinitionsList =
            new ArrayList<OntologyDefinitions>();        

        items = new ArrayList<ArchetypeTerm>();
        item = new ArchetypeTerm("ac0001",
                "text with \"quoted content\" ac0001", "desc with a literal backslash \\ ac0001");
        items.add(item);
        definitions = new OntologyDefinitions("en", items);
        constraintDefinitionsList.add(definitions);
        
        // List available languages
        List<String> languages = new ArrayList<String>();
        languages.add("en");
        
        ArchetypeOntology ontology = new ArchetypeOntology("en", languages,
                null, termDefinitionsList, constraintDefinitionsList, 
                null, null);
        
        clean();
        outputter.printOntology(ontology, out);
        verifyByFile("string-escapes.adl");
    }
}
/*
 *  ***** BEGIN LICENSE BLOCK *****
 *  Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  1.1 (the 'License'); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an 'AS IS' basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is EmbeddedQutesTest.java
 *
 *  The Initial Developer of the Original Code is MEDvision360.
 *  Portions created by the Initial Developer are Copyright (C) 2013
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 *  ***** END LICENSE BLOCK *****
 */
