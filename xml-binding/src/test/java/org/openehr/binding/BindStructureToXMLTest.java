package org.openehr.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlOptions;
import org.openehr.rm.common.archetyped.Archetyped;
import org.openehr.rm.common.changecontrol.OriginalVersion;
import org.openehr.rm.common.generic.PartyProxy;
import org.openehr.rm.common.generic.PartySelf;
import org.openehr.rm.composition.content.entry.Observation;
import org.openehr.rm.datastructure.history.Event;
import org.openehr.rm.datastructure.history.History;
import org.openehr.rm.datastructure.history.PointEvent;
import org.openehr.rm.datastructure.itemstructure.ItemStructure;
import org.openehr.rm.datastructure.itemstructure.ItemTree;
import org.openehr.rm.datastructure.itemstructure.representation.Element;
import org.openehr.rm.datastructure.itemstructure.representation.Item;
import org.openehr.rm.datatypes.quantity.DvQuantity;
import org.openehr.rm.datatypes.quantity.datetime.DvDateTime;
import org.openehr.rm.datatypes.text.CodePhrase;
import org.openehr.rm.datatypes.text.DvCodedText;
import org.openehr.rm.datatypes.text.DvText;
import org.openehr.rm.support.identification.ArchetypeID;
import org.openehr.rm.support.terminology.TerminologyService;
import org.openehr.schemas.v1.DVQUANTITY;
import org.openehr.schemas.v1.DVTEXT;
import org.openehr.schemas.v1.ELEMENT;
import org.openehr.schemas.v1.ITEMTREE;
import org.openehr.schemas.v1.OBSERVATION;
import org.openehr.schemas.v1.ORIGINALVERSION;
import org.openehr.schemas.v1.VERSION;
import org.openehr.schemas.v1.VersionDocument;
import org.openehr.terminology.SimpleTerminologyService;

@SuppressWarnings("SpellCheckingInspection")
public class BindStructureToXMLTest extends XMLBindingTestBase {
	
	public void testBindElementToXML() throws Exception {
		DvText text = new DvText("new text");
		Element element = new Element("at0001", "text element", text);
		
		Object obj = binding.bindToXML(element);
		
		assertTrue("XML class wrong", obj instanceof ELEMENT);
		
		ELEMENT xmlElement = (ELEMENT) obj;
		assertTrue("value type wrong", xmlElement.getValue() instanceof DVTEXT);

		DVTEXT xmlText = (DVTEXT) xmlElement.getValue();
		assertEquals("text value wrong", "new text", xmlText.getValue());
	}
	
	public void testBindEmptyElementToXML() throws Exception {
		DvCodedText nullFlavor = new DvCodedText("no information", 
				new CodePhrase(TerminologyService.OPENEHR, "271")); 
		
		TerminologyService ts = SimpleTerminologyService.getInstance();
				
		Element element = new Element(null, "at0001", new DvText("name"), 
				null, null, null, null, null, nullFlavor, ts);
		
		Object obj = binding.bindToXML(element);
		
		assertTrue("XML class wrong", obj instanceof ELEMENT);
		
		ELEMENT xmlElement = (ELEMENT) obj;
		assertNull("value type wrong", xmlElement.getValue());
		assertNotNull("nullFlavor null", xmlElement.getNullFlavour());
	}
	
	
	public void testBindItemTreeToXML() throws Exception {
		DvText text = new DvText("new text");
		Element element1 = new Element("at0001", "text element", text);
		
		DvQuantity dvq = new DvQuantity(10.0);
		Element element2 = new Element("at0002", "dvq element", dvq);
		
		List<Item> items = new ArrayList<Item>();
		items.add(element1);
		items.add(element2);
		
		ItemTree tree = new ItemTree("at0003", "item tree", items);
		
		Object obj = binding.bindToXML(tree);
		
		assertTrue("XML class wrong", obj instanceof ITEMTREE);

		ITEMTREE xmlTree = (ITEMTREE) obj;		
		
		assertEquals("items.size wrong", 2, xmlTree.getItemsArray().length);
		
		ELEMENT xmlItem1 = (ELEMENT) xmlTree.getItemsArray()[0];
		
		assertTrue("item1 type wrong", xmlItem1.getValue() instanceof DVTEXT);
		assertEquals("new text", ((DVTEXT) xmlItem1.getValue()).getValue());
		
		ELEMENT xmlItem2 = (ELEMENT) xmlTree.getItemsArray()[1];
		assertTrue("item2 type wrong", xmlItem2.getValue() instanceof DVQUANTITY);
		assertEquals(10.0, ((DVQUANTITY) xmlItem2.getValue()).getMagnitude());	
	}
	
	public void testBindObservationToXML() throws Exception {
		DvText text = new DvText("new text");
		Element element1 = new Element("at0001", "text element", text);
		
		DvQuantity dvq = new DvQuantity(10.0);
		Element element2 = new Element("at0002", "dvq element", dvq);
		
		List<Item> items = new ArrayList<Item>();
		items.add(element1);
		items.add(element2);
		
		ItemTree tree = new ItemTree("at0003", "item tree", items);
		
		String archetypeNodeId="openEHR-EHR-OBSERVATION.laboratory-lipids.v1";
		DvText name = new DvText("Lipids");
		CodePhrase language = new CodePhrase("ISO_639-1", "en");
		CodePhrase encoding = new CodePhrase("IANA_character-sets", "UTF-8");
		PartyProxy subject = new PartySelf();
		PartyProxy provider = null;
		DvDateTime time = new DvDateTime("2008-05-22T20:04:26");
		PointEvent<ItemStructure> event = new PointEvent<ItemStructure>("at0002", 
				new DvText("Any event"), time, tree);
		Archetyped archetypeDetails = new Archetyped(
                new ArchetypeID(archetypeNodeId), "1.0.2");        
		List<Event<ItemStructure>> events = new ArrayList<Event<ItemStructure>>();
        events.add(event);
        
		History<ItemStructure> data = new History<ItemStructure>("at0005", "data",
        		new DvDateTime("2008-05-17T10:00:00"), events);
		
		TerminologyService terminologyService = 
			SimpleTerminologyService.getInstance();
		
		Observation obs = new Observation(archetypeNodeId, name, 
				archetypeDetails, language, encoding, subject, provider,
                data, terminologyService);		
		        		
		Object obj = binding.bindToXML(obs);
		
		assertTrue("unexpected XML class: " + obj.getClass(), 
				obj instanceof OBSERVATION);		
	}
	
	public void testBindVersionedCompositionToXML() throws Exception {
		VERSION document = VersionDocument.Factory.parse(
				fromClasspath("original_version_002.xml")).getVersion();
		
		assertTrue("expected original_version, but got: "
				+ (document == null ? null : document.getClass()),
				document instanceof ORIGINALVERSION);

		// do the data binding
		Object rmObj = binding.bindToRM(document);
		
		OriginalVersion orgVer = (OriginalVersion) rmObj;
	
		// Will throw exceptions if faulty
		binding.bindToXML(orgVer);

		// *** Code that exemplifies pretty printing with namespace tweaks
		XmlOptions xmlOptions = new XmlOptions();
		
		xmlOptions.setSaveOuter();
		xmlOptions.setSaveSyntheticDocumentElement(new QName(XMLBinding.SCHEMA_OPENEHR_ORG_V1, "version"));				
		
		HashMap<String, String> uriToPrefixMap = new HashMap<String, String>();
		//uriToPrefixMap.put(XMLBinding.SCHEMA_OPENEHR_ORG_V1, "v1");
		xmlOptions.setSaveSuggestedPrefixes(uriToPrefixMap);
		
		xmlOptions.setSaveNamespacesFirst();
		xmlOptions.setSaveAggressiveNamespaces();
		xmlOptions.setSavePrettyPrint(); 
		xmlOptions.setCharacterEncoding("UTF-8");

		// Uncomment two lines below if less prefixing is desired, but repeated
		// namespace declarations are ok (in nodes containing xsi:type)
		uriToPrefixMap.put(XMLBinding.SCHEMA_OPENEHR_ORG_V1, "");
		xmlOptions.setUseDefaultNamespace();
	}
}
