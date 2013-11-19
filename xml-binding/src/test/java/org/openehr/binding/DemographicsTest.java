package org.openehr.binding;

import org.openehr.rm.demographic.Organisation;
import org.openehr.rm.demographic.Person;
import org.openehr.rm.demographic.Role;
import org.openehr.schemas.v1.ORGANISATION;
import org.openehr.schemas.v1.OrganisationDocument;
import org.openehr.schemas.v1.PERSON;
import org.openehr.schemas.v1.PersonDocument;
import org.openehr.schemas.v1.ROLE;
import org.openehr.schemas.v1.RoleDocument;

public class DemographicsTest extends XMLBindingTestBase {
    public void testOrganisation() throws Exception {
        OrganisationDocument parsedDocument = OrganisationDocument.Factory.parse(
                fromClasspath("organisation-generated.xml"));
        
        ORGANISATION parsedObject = parsedDocument.getOrganisation();
        Object result = binding.bindToRM(parsedObject);
        assertTrue(result instanceof Organisation);
        Organisation converted = (Organisation) result;
        result = binding.bindToXML(converted, true);
        assertTrue(result instanceof ORGANISATION);
   	}

    public void testPerson() throws Exception {
        PersonDocument parsedDocument = PersonDocument.Factory.parse(
                fromClasspath("person-generated.xml"));
        
        PERSON parsedObject = parsedDocument.getPerson();
        Object result = binding.bindToRM(parsedObject);
        assertTrue(result instanceof Person);
        Person converted = (Person) result;
        result = binding.bindToXML(converted, true);
        assertTrue(result instanceof PERSON);
   	}

    public void testRole() throws Exception {
        RoleDocument parsedDocument = RoleDocument.Factory.parse(
                fromClasspath("role-generated.xml"));
        
        ROLE parsedObject = parsedDocument.getRole();
        Object result = binding.bindToRM(parsedObject);
        assertTrue(result instanceof Role);
        Role converted = (Role) result;
        result = binding.bindToXML(converted, true);
        assertTrue(result instanceof ROLE);
   	}
}
