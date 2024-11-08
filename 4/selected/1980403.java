package com.ar4j.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import com.ar4j.test.domain.Primary;
import com.ar4j.test.domain.Secondary;
import com.ar4j.test.domain.TestEnum;
import com.ar4j.type.DateWithoutMillis;

/**
 * Tests for common active record methods
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/applicationContext-junit.xml" })
@TransactionConfiguration(transactionManager = "txManager", defaultRollback = true)
@Transactional
public class ActiveRecordTest {

    @Autowired
    private Primary primaryReadOnly;

    @Autowired
    private Secondary secondaryReadOnly;

    @Test
    public void testGettersAndSetters() {
        Primary primary = primaryReadOnly.newInstance();
        String code = "testCode";
        DateWithoutMillis date = new DateWithoutMillis();
        String description = "testDescription";
        Double doubleField = 123.45;
        TestEnum enumField = TestEnum.SECOND;
        Long id = 100L;
        Long longField = 234L;
        String stringField = "testString";
        String nonReadWrite = "IShouldNotBeHere";
        primary.setCode(code);
        primary.setDateField(date);
        primary.setDescriptionValue(description);
        primary.setDoubleField(doubleField);
        primary.setEnumField(enumField);
        primary.setId(id);
        primary.setLongField(longField);
        primary.setStringField(stringField);
        primary.setNonReadWriteProperty(nonReadWrite);
        assertEquals("Code should match", code, primary.getCode());
        assertEquals("Date field should match", date, primary.getDateField());
        assertEquals("Description should match", description, primary.getDescriptionValue());
        assertEquals("Double field should match", doubleField, primary.getDoubleField());
        assertEquals("Enum field should match", enumField, primary.getEnumField());
        assertEquals("Id should match", id, primary.getId());
        assertEquals("Long field should match", longField, primary.getLongField());
        assertEquals("String field should match", stringField, primary.getStringField());
        assertEquals("Non read/write field should match", nonReadWrite, primary.getNonReadWriteProperty());
        primary.setAnotherIgnoredString("testy");
        assertNotNull("Should have been able to call the getter", primary.getIgnoredString());
    }

    @Test
    public void testArbitraryMethodPassthrough() {
        Primary primary = primaryReadOnly.newInstance();
        String code = "testCode";
        primary.setCode(code);
        assertEquals(code, primary.arbitraryMethodThatReturnsCode());
    }

    @Test
    public void testPropertyNamesAndPropertyGetSetAndPropertyMap() {
        Primary primary = primaryReadOnly.newInstance();
        Set<String> names = primaryReadOnly.getPropertyNames();
        List<String> reference = Arrays.asList(new String[] { "id", "code", "descriptionValue", "longField", "doubleField", "stringField", "enumField", "dateField" });
        assertTrue("Property names should match reference list", (names.containsAll(reference) && reference.containsAll(names)));
        assertTrue("Primary must have a property named 'id'", primaryReadOnly.hasProperty("id"));
        assertFalse("Primary must NOT have a property named 'blah'", primaryReadOnly.hasProperty("blah"));
        Long referenceLong = new Long(100);
        assertNull("ID of primary should be null via getter", primary.getId());
        assertNull("ID of primary should be null via getProperty", primary.getProperty("id"));
        primary.setProperty("id", referenceLong);
        assertEquals("ID of primary via getter should be equal to reference", referenceLong, primary.getId());
        assertEquals("ID of primary via getProperty should be equal to reference", referenceLong, primary.getProperty("id"));
        assertEquals("ID of primary via getProperty (coerced to int) should be equal to 100", new Integer(100), primary.getProperty("id", Integer.class));
        Map<String, Object> referenceMap = new HashMap<String, Object>();
        for (String name : reference) {
            referenceMap.put(name, null);
        }
        referenceMap.put("id", referenceLong);
        Map<String, Object> propertyMap = primary.getPropertyMap();
        assertEquals("Property map should be equal to reference map", referenceMap, propertyMap);
    }

    @Test
    public void testCreateMechanisms() {
        Primary primary = primaryReadOnly.newInstance();
        populateMockPrimary(primary);
        Primary fromPrototype = primaryReadOnly.newInstance(primary);
        assertTrue("Newly created instance should not be the same as the primary one", (primary != fromPrototype));
        assertNull("ID from prototype clone whould be null", fromPrototype.getId());
        validatePrimaryFields(primary, fromPrototype, false);
        Primary fromMap = primaryReadOnly.newInstance(primary.getPropertyMap());
        assertTrue("Newly created instance should not be the same as the primary one", (primary != fromMap));
        validatePrimaryFields(primary, fromMap);
    }

    @Test
    public void testCreateMechanismsWithTransactions() {
        Secondary secondary = secondaryReadOnly.newInstance();
        populateMockSecondary(secondary);
        Secondary fromPrototype = secondaryReadOnly.newInstance(secondary);
        assertTrue("Newly created instance should not be the same as the primary one", (secondary != fromPrototype));
        assertNull("ID from prototype clone whould be null", fromPrototype.getId());
        validateSecondaryFields(secondary, fromPrototype, false);
        Secondary fromMap = secondaryReadOnly.newInstance(secondary.getPropertyMap());
        assertTrue("Newly created instance should not be the same as the primary one", (secondary != fromMap));
        validateSecondaryFields(secondary, fromMap);
    }

    @Test
    public void testObjectBasics() {
        Primary primary = primaryReadOnly.newInstance();
        populateMockPrimary(primary);
        Primary copy = primaryReadOnly.newInstance(primary.getPropertyMap());
        Primary copyWithoutId = primaryReadOnly.newInstance(primary);
        assertFalse("Comparison to non active record should be false", primary.equals(1L));
        assertTrue("Copy should not be the instance as primary", (primary != copy));
        assertTrue("Copy without ID should not be the instance as primary", (primary != copyWithoutId));
        assertEquals("Copy should equal primary", primary, copy);
        assertFalse("Copy without ID should not equal primary", primary.equals(copyWithoutId));
        assertTrue("Copy without ID should have the same content as primary", primary.isSameContent(copyWithoutId));
        int primaryHashCode = primary.hashCode();
        int copyHashCode = copy.hashCode();
        assertEquals("Primary and copy hashcodes should match", primaryHashCode, copyHashCode);
        assertEquals("Primary hashcode should be: -2114690818", -2114690818, primaryHashCode);
        String primaryToString = primary.toString();
        String copyToString = copy.toString();
        assertEquals("Primary and copy string representations should match", primaryToString, copyToString);
        assertEquals("Primary toString should be same as reference", "com.ar4j.test.domain.Primary{code=testCode,dateField=Tue Dec 01 17:23:14 EST 2009,descriptionValue=testDescription,doubleField=123.45,enumField=SECOND,id=100,longField=234,stringField=testString}", primaryToString);
    }

    @Test
    public void testIdentifier() {
        Primary primary = primaryReadOnly.newInstance();
        primary.setId(new Long(100L));
        assertEquals("Primary's identifier field is 'id'", "id", primaryReadOnly.getIdentifierPropertyName());
        assertEquals("Primary's identifier field of type Long", Long.class, primaryReadOnly.getIdentifierPropertyType());
        assertEquals("Primary's identifier field value is same as its ID", primary.getId(), primary.getIdentifier());
    }

    @Test
    public void testCloneRecord() {
        Primary primary = primaryReadOnly.newInstance();
        populateMockPrimary(primary);
        Primary clone = primary.cloneRecord();
        assertTrue("Newly created instance should not be the same as the primary one", (primary != clone));
        assertNull("ID from prototype clone whould be null", clone.getId());
        validatePrimaryFields(primary, clone, false);
    }

    @Test
    public void testIsNewRecord() {
        Primary primary = primaryReadOnly.newInstance();
        assertTrue("Should be new", primary.isNewRecord());
        primary.setId(0L);
        assertTrue("Should still be new (zero id)", primary.isNewRecord());
        primary.setId(1L);
        assertFalse("Should NOT be new (proper id)", primary.isNewRecord());
    }

    @Test
    public void testGetTableName() {
        Primary primary = primaryReadOnly.newInstance();
        Secondary secondary = secondaryReadOnly.newInstance();
        assertEquals("Primary's table name should be defined by annotation", "`PRIMARY`", primary.getTableName());
        assertEquals("Secondary's table name should be inferred from its name", "SECONDARY", secondary.getTableName());
    }

    @Test
    public void testGetContext() {
        Primary primary = primaryReadOnly.newInstance();
        assertNotNull("Should have a not null context", primary.getContext());
    }

    private void populateMockPrimary(Primary primary) {
        primary.setCode("testCode");
        primary.setDescriptionValue("testDescription");
        primary.setDoubleField(123.45);
        primary.setEnumField(TestEnum.SECOND);
        primary.setId(100L);
        primary.setLongField(234L);
        primary.setStringField("testString");
        try {
            primary.setDateField(new DateWithoutMillis(DateUtils.parseDate("2009-12-01 17:23:14", new String[] { "yyyy-MM-dd HH:mm:ss" })));
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected fatal exception", e);
        }
    }

    private void populateMockSecondary(Secondary secondary) {
        secondary.setCode("testCode");
        secondary.setDescription("testDescription");
    }

    private void validatePrimaryFields(Primary primary, Primary loaded) {
        validatePrimaryFields(primary, loaded, true);
    }

    private void validatePrimaryFields(Primary primary, Primary loaded, boolean checkId) {
        assertEquals("Code should match", primary.getCode(), loaded.getCode());
        assertEquals("Date field should match", primary.getDateField(), loaded.getDateField());
        assertEquals("Description should match", primary.getDescriptionValue(), loaded.getDescriptionValue());
        assertEquals("Double field should match", primary.getDoubleField(), loaded.getDoubleField());
        assertEquals("Enum field should match", primary.getEnumField(), loaded.getEnumField());
        assertEquals("Long field should match", primary.getLongField(), loaded.getLongField());
        assertEquals("String field should match", primary.getStringField(), loaded.getStringField());
        if (checkId) {
            assertEquals("Id should match", primary.getId(), loaded.getId());
        }
    }

    private void validateSecondaryFields(Secondary secondary, Secondary loaded) {
        validateSecondaryFields(secondary, loaded, true);
    }

    private void validateSecondaryFields(Secondary secondary, Secondary loaded, boolean checkId) {
        assertEquals("Code should match", secondary.getCode(), loaded.getCode());
        assertEquals("Description should match", secondary.getDescription(), loaded.getDescription());
        if (checkId) {
            assertEquals("Id should match", secondary.getId(), secondary.getId());
        }
    }
}
