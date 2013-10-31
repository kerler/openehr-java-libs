package org.openehr.binding;

import java.math.BigInteger;

import org.openehr.schemas.v1.DVPROPORTION;
import org.openehr.schemas.v1.ELEMENT;
import org.openehr.schemas.v1.ITEMTREE;
import org.openehr.schemas.v1.ItemsDocument;

@SuppressWarnings("SpellCheckingInspection")
public class BindItemTreeWithDvProportionTest extends XMLBindingTestBase {
	
	public void testBindXMLDvProportionToRM() throws Exception {
		ItemsDocument document = ItemsDocument.Factory.parse(fromClasspath(
				"item_tree_003.xml"));
		Object obj = document.getItems();
		ITEMTREE tree = (ITEMTREE) obj;
		ELEMENT element = (ELEMENT) tree.getItemsArray(0);
		Object value = element.getValue();
		
		assertTrue("expected dv_proportion, but got: " 
				+ (value == null ? null : value.getClass()),
				value instanceof DVPROPORTION);

		DVPROPORTION prop = (DVPROPORTION) value;		
		assertEquals("unexpected proportion.numerator", 0.5f, 
				prop.getNumerator());
		assertEquals("unexpected proportion.denominator", 1.0f, 
				prop.getDenominator());
		assertEquals("unexpected proportion.type", BigInteger.valueOf(0), 
				prop.getType());
	}
}
