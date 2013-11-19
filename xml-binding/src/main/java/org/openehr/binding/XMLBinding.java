/*
 * component:   "openEHR Java Reference Implementation"
 * description: "Class XMLBinding"
 * keywords:    "XML binding"
 *
 * author:      "Rong Chen <rong.acode@gmail.com>"
 * copyright:   "Copyright (c) 2008-2010 Cambio Healthcare Systems, Sweden"
 * copyright:   "Copyright (c) 2013 MEDvision360"
 * license:     "See notice at bottom of class"
 *
 * file:        "$URL$"
 * revision:    "$LastChangedRevision$"
 * last_change: "$LastChangedDate$"
 */
package org.openehr.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.XmlOptions;
import org.openehr.build.RMObjectBuilder;
import org.openehr.build.SystemValue;
import org.openehr.rm.datatypes.quantity.ProportionKind;
import org.openehr.rm.datatypes.text.CodePhrase;
import org.openehr.rm.support.measurement.MeasurementService;
import org.openehr.rm.support.measurement.SimpleMeasurementService;
import org.openehr.rm.support.terminology.TerminologyService;
import org.openehr.terminology.SimpleTerminologyService;

/**
 * Bind data from XMLBeans class to openEHR RM classes
 *
 * @author Rong.Chen
 * @author minor modifications by Erik Sundvall, Linköping University
 */
public class XMLBinding {
	public XMLBinding(Map<SystemValue, Object> values) {
		if (values == null) {
			throw new NullPointerException("values cannot be null");
		}
		init(values);
	}

	public XMLBinding() {
		TerminologyService terminologyService = SimpleTerminologyService
				.getInstance();
		MeasurementService measurementService = SimpleMeasurementService
				.getInstance();
		CodePhrase charset = new CodePhrase("IANA_character-sets",
				"UTF-8");

		Map<SystemValue, Object> values = new HashMap<SystemValue, Object>();
		values.put(SystemValue.TERMINOLOGY_SERVICE, terminologyService);
		values.put(SystemValue.MEASUREMENT_SERVICE, measurementService);
		values.put(SystemValue.CHARSET, charset);
		init(values);
	}

	public Object bindToXML(Object obj) throws XMLBindingException {
		return bindToXML(obj, false);
	}

	/**
	 * Binds data from reference model instance to XML binding classes
	 */
	@SuppressWarnings("ConstantConditions")
	public Object bindToXML(Object obj, boolean asDocument) throws XMLBindingException {
		if (obj == null) {
			return null;
		}
		String className = obj.getClass().getSimpleName();
		String xmlClassName = className;
		if ("EHRStatus".equalsIgnoreCase(className)) {
			// deal with case-sensitively challenged class name
			xmlClassName = "EhrStatus";
		}
		Method[] methods = obj.getClass().getMethods();

		try {
			Class<?> xmlClass = null;
			Object xmlObj = null;

			if (asDocument) {
				// when serializing back to XML strings, XMLBeans needs a Document wrapper to be able to write a 
				// proper root element. If it doesn't have one, it will output an <xml-fragment/>
				Class<?> factoryClass;
				try {
					factoryClass = Class.forName(XML_BINDING_PACKAGE +
							xmlClassName + "Document$Factory");
				} catch (ClassNotFoundException e) {
					factoryClass = Class.forName(XML_BINDING_PACKAGE +
							xmlClassName.toUpperCase() + "Document$Factory");
				} catch (NoClassDefFoundError e) {
					factoryClass = Class.forName(XML_BINDING_PACKAGE +
							xmlClassName.toUpperCase() + "Document$Factory");
				}

				Method factoryMethod = factoryClass.getMethod(NEW_INSTANCE, XmlOptions.class);
				Object documentObj = factoryMethod.invoke(null, xmlOptions);
				Class<?> documentClass = documentObj.getClass();
				Method[] documentMethods = documentClass.getMethods();
				boolean found = false;
				for (int i = 0; i < documentMethods.length; i++) {
					Method documentMethod = documentMethods[i];
					if (documentMethod.getName().startsWith("addNew")) {
						xmlObj = documentMethod.invoke(documentObj);
						xmlClass = xmlObj.getClass();
						found = true;
						break;
					}
				}
				if (!found) {
					throw new XMLBindingException("Could not find XXXDocument.addNewXXX() method to invoke");
				}
			} else {
				xmlClass = Class.forName(XML_BINDING_PACKAGE +
						xmlClassName.toUpperCase());

				Class<?> factoryClass = xmlClass.getClasses()[0];
				Method factoryMethod = factoryClass.getMethod(NEW_INSTANCE, XmlOptions.class);
				xmlObj = factoryMethod.invoke(null, xmlOptions);
			}

			Map<String, Class<?>> attributes = builder.retrieveAttribute(className);
			Set<String> attributeNames = attributes.keySet();
			Object attributeValue;
			Method setterMethod;

			for (Method method : methods) {
				String name = method.getName();

				// cause dead-loop
				if ("getParent".equals(name)) {
					continue; //
				}

				if (isGetter(name, attributeNames)) {
					if (method.getParameterTypes().length > 0) {
						continue;
					}

					attributeValue = method.invoke(obj, null);
					if (attributeValue == null) {
						continue;
					}

					boolean isList = false;

					if (attributeValue.getClass().isArray()) {
						Object[] array = (Object[]) attributeValue;
						if (array.length == 0) {
							continue;
						}
						Object[] done = new Object[array.length];
						for (int i = 0; i < array.length; i++) {
							done[i] = bindToXML(array[i]);
						}
						attributeValue = done;
					} else if (ProportionKind.class.equals(
							attributeValue.getClass())) {
						ProportionKind kind = (ProportionKind) attributeValue;
						attributeValue = BigInteger.valueOf(kind.getValue());
					} else if (builder.isOpenEHRRMClass(attributeValue)) {
						attributeValue = bindToXML(attributeValue);
					} else if (List.class.isAssignableFrom(
							attributeValue.getClass())) {
						isList = true;
						List list = (List) attributeValue;

						String attributeName = getAttributeNameFromGetter(name);
						setterMethod = findSetter(attributeName, xmlClass, isList);

						Method addNew = findAddNew(attributeName, xmlClass);

						for (int i = 0, j = list.size() - 1; i <= j; i++) {
							Object value = list.get(i);
							Object[] array = new Object[2];
							addNew.invoke(xmlObj, null);
							array[0] = new Integer(i);
							array[1] = bindToXML(value);
							setterMethod.invoke(xmlObj, array);
						}
					}

					if (!isList) {
						String attributeName = getAttributeNameFromGetter(name);

						if ("nullFlavor".equals(attributeName)) {
							attributeName = "nullFlavour";
						}

						// skip function according to specs
						if ("isMerged".equals(attributeName)) {
							continue;
						}

						setterMethod = findSetter(attributeName, xmlClass, isList);
						if (setterMethod == null) {
							continue;
						}

						// special handling deals with 'real' typed
						// attributes in specs but typed 'float' in xsd
						String setter = setterMethod.getName();
						if ("setAccuracy".equals(setter)
								|| "setDenominator".equals(setter)
								|| "setNumerator".equals(setter)) {

							Double d = (Double) attributeValue;
							attributeValue = d.floatValue();
						}

						setterMethod.invoke(xmlObj, attributeValue);
					}
				}
			}

			return xmlObj;
		} catch (Exception e) {
			throw new XMLBindingException("exception caught when bind obj to "
					+ className + ", " + e.getMessage(), e);
		}
	}

	public Object bindToRM(Object object) throws Exception {
		Method[] methods = object.getClass().getMethods();
		Object value;
		Map<String, Object> valueMap = new HashMap<String, Object>();

		String className = object.getClass().getSimpleName();
		if (className.endsWith("Impl")) {
			className = className.substring(0, className.length() - 4);
		}

		Map<String, Class<?>> attributes = builder.retrieveAttribute(className);
		Set<String> attributeNames = attributes.keySet();

		for (Method method : methods) {
			String name = method.getName();

			if (isGetter(name, attributeNames)) {
				if (method.getParameterTypes().length > 0) {
					continue;
				}

				String attribute = getAttributeNameFromGetter(name);

				value = method.invoke(object, null);
				if (value == null) {
					continue;
				}

				if (value.getClass().isArray()) {
					Object[] array = (Object[]) value;
					if (array.length == 0) {
						// special fix for item_structure.items
						if ("items".equals(attribute)) {
							valueMap.put(attribute, new ArrayList());
						}
						continue;
					} else {
						Object[] done = new Object[array.length];
						for (int i = 0; i < array.length; i++) {
							done[i] = bindToRM(array[i]);
						}
						value = done;
					}

				} else if (isXMLBindingClass(value)) {
					value = bindToRM(value);
				}
				valueMap.put(attribute, value);
			}
		}

		Object rmObj = builder.construct(className, valueMap);
		return rmObj;
	}

	protected Method findSetter(String attributeName, Class<?> xmlClass, boolean isList) {
		Method[] methods = xmlClass.getMethods();
		String name = "set" + attributeName.substring(0, 1).toUpperCase() +
				attributeName.substring(1);

		if (isList) {
			name += "Array";
		}

		for (Method method : methods) {
			if (method.getName().equals(name)) {
				Type[] paras = method.getParameterTypes();
				if (isList) {
					if (paras.length == 2) {
						return method;
					}
				} else if (paras.length == 1) {
					return method;
				}
			}
		}
		return null;
	}

	protected Method findAddNew(String attributeName, Class<?> xmlClass) {
		Method[] methods = xmlClass.getMethods();
		String name = "addNew" + attributeName.substring(0, 1).toUpperCase() +
				attributeName.substring(1);

		for (Method method : methods) {
			if (method.getName().equals(name)) {
				return method;
			}
		}
		return null;
	}

	protected boolean isGetter(String method, Set<String> attributes) {
		if (!method.startsWith("get")) {
			return false;
		}
		String name = getAttributeNameFromGetter(method);
		return attributes.contains(name);
	}

	protected String getAttributeNameFromGetter(String name) {
		name = name.substring(3, name.length());
		name = name.substring(0, 1).toLowerCase() + name.substring(1);
		if (name.endsWith("Array")) {
			name = name.substring(0, name.length() - 5);
		}
		return name;
	}

	protected boolean isXMLBindingClass(Object obj) {
		return obj.getClass().getName().contains(XML_BINDING_PACKAGE);
	}

	protected void init(Map<SystemValue, Object> values) {
		xmlOptions = new XmlOptions();

		HashMap<String, String> uriToPrefixMap = new HashMap<String, String>();
		uriToPrefixMap.put(SCHEMA_XSI, "xsi");
		uriToPrefixMap.put(SCHEMA_OPENEHR_ORG_V1, "v1");
		xmlOptions.setSaveSuggestedPrefixes(uriToPrefixMap);

		xmlOptions.setSaveAggressiveNamespaces();
		xmlOptions.setSavePrettyPrint();
		xmlOptions.setCharacterEncoding("UTF-8");

		builder = new RMObjectBuilder(values);
	}

	/* namespace for generated binding class */
	private static String XML_BINDING_PACKAGE = "org.openehr.schemas.v1.";

	private static final String NEW_INSTANCE = "newInstance";

	public static final String SCHEMA_XSI = "http://www.w3.org/2001/XMLSchema-instance";
	public static final String SCHEMA_OPENEHR_ORG_V1 = "http://schemas.openehr.org/v1";

	private RMObjectBuilder builder;
	private XmlOptions xmlOptions;
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
 * The Original Code is XMLBinding.java
 *
 * The Initial Developer of the Original Code is Rong Chen. Portions created by
 * the Initial Developer are Copyright (C) 2003-2010 the Initial Developer. All
 * Rights Reserved.
 *
 * Contributor(s): Erik Sundvall, Leo Simons
 *
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * ***** END LICENSE BLOCK *****
 */
