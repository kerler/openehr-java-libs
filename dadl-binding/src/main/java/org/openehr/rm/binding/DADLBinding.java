/*
 * component:   "openEHR Java Reference Implementation"
 * description: "Class DADLBinding"
 * keywords:    "binding"
 *
 * author:      "Rong Chen <rong.acode@gmail.com>"
 * copyright:   "Copyright (c) 2008 Cambio Healthcare Systems, Sweden"
 * copyright:   "Copyright (c) 2013 MEDvision360"
 * license:     "See notice at bottom of class"
 *
 * file:        "$URL$"
 * revision:    "$LastChangedRevision$"
 * last_change: "$LastChangedDate$"
 */
package org.openehr.rm.binding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.openehr.am.parser.AttributeValue;
import org.openehr.am.parser.ComplexObjectBlock;
import org.openehr.am.parser.ContentObject;
import org.openehr.am.parser.KeyedObject;
import org.openehr.am.parser.MultipleAttributeObjectBlock;
import org.openehr.am.parser.ObjectBlock;
import org.openehr.am.parser.PrimitiveObjectBlock;
import org.openehr.am.parser.SimpleValue;
import org.openehr.am.parser.SingleAttributeObjectBlock;
import org.openehr.build.RMObjectBuilder;
import org.openehr.build.RMObjectBuildingException;
import org.openehr.build.SystemValue;
import org.openehr.rm.Attribute;
import org.openehr.rm.RMObject;
import org.openehr.rm.datatypes.quantity.ProportionKind;
import org.openehr.rm.datatypes.text.CodePhrase;
import org.openehr.rm.support.measurement.MeasurementService;
import org.openehr.rm.support.measurement.SimpleMeasurementService;
import org.openehr.rm.support.terminology.TerminologyService;
import org.openehr.terminology.SimpleTerminologyService;

/**
 * Utility class that binds data in DADL format to openEHR RM
 * 
 * @author rong.chen
 */
public class DADLBinding {

	public DADLBinding(Map<SystemValue, Object> values) {
		init(values);
	}

	public DADLBinding() {
		TerminologyService terminologyService = SimpleTerminologyService.getInstance();
		MeasurementService measurementService = SimpleMeasurementService.getInstance();

		CodePhrase lang = new CodePhrase("ISO_639-1", "en");
		CodePhrase charset = new CodePhrase("IANA_character-sets", "UTF-8");

		Map<SystemValue, Object> values = new HashMap<SystemValue, Object>();
		values.put(SystemValue.LANGUAGE, lang);
		values.put(SystemValue.CHARSET, charset);
		values.put(SystemValue.ENCODING, charset);
		values.put(SystemValue.TERMINOLOGY_SERVICE, terminologyService);
		values.put(SystemValue.MEASUREMENT_SERVICE, measurementService);
		init(values);
	}

	private void init(Map<SystemValue, Object> values) {
		builder = new RMObjectBuilder(values);
	}

	public Object bind(ContentObject co) throws DADLBindingException,
			RMObjectBuildingException {
		if (co.getAttributeValues() != null) {
			return bindAttributes(null, co.getAttributeValues());
		} else {
			ComplexObjectBlock complexObj = co.getComplexObjectBlock();
			return bindComplexBlock(complexObj);
		}
	}

	RMObject bindAttributes(String type, List<AttributeValue> attributes)
			throws DADLBindingException, RMObjectBuildingException {
		Map<String, Object> values = new HashMap<String, Object>();
		for (AttributeValue attr : attributes) {
			String id = attr.getId();
			Object value = bindObjectBlock(attr.getValue());
			values.put(id, value);
		}		
		return invokeRMObjectBuilder(type, values);
	}
	
	RMObject invokeRMObjectBuilder(String type, Map<String, Object> valueMap)
			throws DADLBindingException, RMObjectBuildingException {
		if(type == null) {
			type = builder.findMatchingRMClass(valueMap);
		}
		RMObject rmObj = builder.construct(type, valueMap);
		return rmObj;
	}

	Object bindObjectBlock(ObjectBlock block) throws DADLBindingException,
			RMObjectBuildingException {
		if (block instanceof PrimitiveObjectBlock) {
			return bindPrimitiveBlock((PrimitiveObjectBlock) block);
		} else {
			return bindComplexBlock((ComplexObjectBlock) block);
		}
	}

	Object bindPrimitiveBlock(PrimitiveObjectBlock block)
			throws DADLBindingException {
		if (block.getSimpleValue() != null) {
			return block.getSimpleValue().getValue();
		} else if (block.getSimpleListValue() != null) {
			List<SimpleValue> values = block.getSimpleListValue();
			List<Object> list = new ArrayList<Object>(values.size());
			for (SimpleValue sv : values) {
				list.add(sv.getValue());
			}
			return list;
		} else if (block.getSimpleIntervalValue() != null) {
			//Interval<Comparable> values = block.getSimpleIntervalValue();
			// TODO
			return null;
		} else if (block.getTermCode() != null) {
			return block.getTermCode();
		} else if (block.getTermCodeListValue() != null) {
			return block.getTermCodeListValue();
		} else {
			throw new DADLBindingException("empty block");
		}
	}

	Object bindComplexBlock(ComplexObjectBlock block)
			throws DADLBindingException, RMObjectBuildingException {
		if (block instanceof SingleAttributeObjectBlock) {
			SingleAttributeObjectBlock singleBlock = 
					(SingleAttributeObjectBlock) block;
			// a special case to deal with empty attribute list
			if("LIST".equalsIgnoreCase(singleBlock.getTypeIdentifier())
					&& singleBlock.getAttributeValues().isEmpty()) {
				return new ArrayList();
			} 
			return bindAttributes(singleBlock.getTypeIdentifier(), singleBlock
					.getAttributeValues());
		} else {
			MultipleAttributeObjectBlock multiBlock = 
					(MultipleAttributeObjectBlock) block;
			List<KeyedObject> list = multiBlock.getKeyObjects();
			// TODO assume list?
			List<Object> valueList = new ArrayList<Object>();
			for(KeyedObject ko : list) {
				Object value = bindObjectBlock(ko.getObject());
				valueList.add(value);
			}
			return valueList;
		}
	}
	
	public List<String> toDADL(Object obj) throws InvocationTargetException, IllegalAccessException {
		List<String> lines = new ArrayList<String>();
		return toDADL(obj, 1, lines);
	}
	
	@SuppressWarnings("ConstantConditions")
	public List<String> toDADL(Object obj, int indent, List<String> lines)
			throws InvocationTargetException, IllegalAccessException {		
		Class klass = obj.getClass();		
		String className = klass.getSimpleName();
		String rmName = builder.toRmEntityName(className).toUpperCase();
		String typeHeader = "(" + rmName + ") <";

		int size = lines.size();
		if(size == 0) {
			lines.add(typeHeader); 
		} else {
			String l = lines.get(size - 1);
			l += typeHeader;
			lines.set(size -1, l);
		}	
		
		SortedMap<String, Attribute> attributes = builder.getAttributes(obj.getClass());
		String name;
		Object value;
		StringBuffer buf;
		for(Iterator<String> names = attributes.keySet().iterator(); names.hasNext();) {	
			name = names.next();
			
			Attribute attribute = attributes.get(name);
			if(attribute.system()) {
				continue;
			}
			
			if("parent".equals(attribute.name())) {
				continue; // causing dead-loops
			}
			
			Method getter = builder.getter(name, obj.getClass());
			if(getter != null) { 
				value = getter.invoke(obj, null);
				buf = new StringBuffer();
				if(value != null ) {
					for(int i = 0; i < indent; i++) {
						buf.append("\t");
					}
					buf.append(builder.toAttributeName(name));
					buf.append(" = ");
					
					if(builder.isOpenEHRRMClass(value) && !(value instanceof ProportionKind)) {
						lines.add(buf.toString());
						toDADL(value, indent + 1, lines);						
					} else if(value instanceof List) {
						buf.append("<");
						lines.add(buf.toString());
						
						List list = (List) value;
						for(int i = 0, j = list.size(); i < j; i++) {
							buf = new StringBuffer();
							for(int k = 0; k < indent + 1; k++) {
								buf.append("\t");
							}
							lines.add(buf.toString() + "[" + (i+1) + "] = ");
							toDADL(list.get(i), indent + 2, lines);
						}
						
						buf = new StringBuffer();
						for(int i = 0; i < indent; i++) {
							buf.append("\t");
						}
						buf.append(">");
						lines.add(buf.toString());
					} else {
						buf.append("<");
						if(value instanceof String || value instanceof Boolean) {							
							buf.append("\"");
							buf.append(value);
							buf.append("\"");						
						} else {
							buf.append(value.toString());
						}
						buf.append(">");
						lines.add(buf.toString());
					}
				}
			}
		}
		buf = new StringBuffer();
		for(int i = 0; i < indent - 1; i++) {
			buf.append("\t");
		}
		buf.append(">");
		lines.add(buf.toString());		
		return lines;
	}
	
	private RMObjectBuilder builder;
}
/*
 * ***** BEGIN LICENSE BLOCK ***** Version: MPL 1.1/GPL 2.0/LGPL 2.1
 * 
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the 'License'); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * The Original Code is DADLBinding.java
 * 
 * The Initial Developer of the Original Code is Rong Chen. Portions created by
 * the Initial Developer are Copyright (C) 2003-2008 the Initial Developer. All
 * Rights Reserved.
 * 
 * Contributor(s): Leo Simons
 * 
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * ***** END LICENSE BLOCK *****
 */
