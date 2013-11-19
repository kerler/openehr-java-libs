package org.openehr.binding;

import java.io.StringWriter;

import org.openehr.rm.ehr.EHRStatus;
import org.openehr.schemas.v1.EHRSTATUS;
import org.openehr.schemas.v1.EhrDocument;
import org.openehr.schemas.v1.EhrStatusDocument;

public class EHRDocumentTest extends XMLBindingTestBase {
	public void testEHRStatusDocument() throws Exception {
        EhrStatusDocument parsed = EhrStatusDocument.Factory.parse(
				fromClasspath("ehr_status.xml"));
        EHRSTATUS parsedStatus = parsed.getEhrStatus();
        Object result = binding.bindToRM(parsedStatus);
        assertTrue(result instanceof EHRStatus);
        EHRStatus converted = (EHRStatus) result;
        result = binding.bindToXML(converted, true);
        assertTrue(result instanceof EHRSTATUS);
        EHRSTATUS serializedStatus = (EHRSTATUS) result;
        //serializedStatus.save(System.out);

        StringWriter writer = new StringWriter();
        serializedStatus.save(writer);
        String serialized = writer.toString();

        // If you mess up the xmlbeans runtime classpath in some way, you can get into a nasty issue where the schema
        // type loader will load different incompatible classes of the same type. For example, 
        // this xml-binding library has xmlbeans objects generated from the same xml schema and into the same package
        // as does the oet-parser library. If you use both, and oet-parser ends up on the classpath _first_, 
        // then you may be in for some serious issues.
        //
        // One way that problem manifests is by missing xsi:type annotations. This happens because in
        // org.apache.xmlbeans.impl.store.Cur#setType, there is a comparison of SchemaType using == (there is no good
        // equals() implementation for SchemaType, and even SchemaType#isAssignableFrom() uses == to compare base 
        // types), and the loaded schema types can be different.
        //
        // The way to fully avoid the problem is to not use xml-binding and oet-parser on the same classpath. You can
        // also force classpath ordering (good luck...), which is why this unit test succeeds even if oet-parser is 
        // on the classpath too: maven and other build tools will order the classpath so that the xml-binding classes
        // come before the oet-parser classes.
        //
        // A more proper fix probably involves fixing the openehr RI not to generate multiple xmlbeans bindings for 
        // the same schema.
        assertTrue(serialized.matches(
                "^.*?<(?:\\w+?:)?uid[^>]*?xsi:type\\s*=\\s*\"(?:\\w+?:)?HIER_OBJECT_ID\".*$"));
	}

    public void testEHRDocument() throws Exception {
   		EhrDocument.Factory.parse(
   				fromClasspath("ehr.xml"));				
   	}
}
