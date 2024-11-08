package com.volantis.shared.metadata.impl.value;

import com.volantis.shared.metadata.impl.JiBXTestCaseAbstract;
import com.volantis.shared.metadata.value.MetaDataValue;
import com.volantis.shared.metadata.value.immutable.ImmutableListValue;
import com.volantis.shared.metadata.value.immutable.ImmutableNumberValue;
import com.volantis.shared.metadata.value.immutable.ImmutableSetValue;
import com.volantis.shared.metadata.value.immutable.ImmutableStringValue;
import com.volantis.shared.metadata.value.immutable.ImmutableStructureValue;
import com.volantis.shared.metadata.value.mutable.MutableBooleanValue;
import com.volantis.shared.metadata.value.mutable.MutableNumberValue;
import com.volantis.shared.metadata.value.mutable.MutableStringValue;
import com.volantis.synergetics.io.IOUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests to verify JiBX marshaller/unmarshaller.
 */
public class JiBXTestCase extends JiBXTestCaseAbstract {

    public void testStructureWithStrings() throws Exception {
        final ImmutableStructureValue structure = (ImmutableStructureValue) unmarshall(getResourceAsString("res/structure-with-strings.xml"), ImmutableStructureValueImpl.class);
        final Map expectedFields = new HashMap();
        MutableStringValue value = new MutableStringValueImpl();
        value.setValue("First Value");
        expectedFields.put("first", value.createImmutable());
        value.setValue("Second Value");
        expectedFields.put("second", value.createImmutable());
        assertEquals(expectedFields, structure.getFieldValuesAsMap());
        final Object otherStructure = doRoundTrip(structure);
        assertEquals(structure, otherStructure);
    }

    public void testStructureWithBooleansAndStrings() throws Exception {
        final ImmutableStructureValue structure = (ImmutableStructureValue) unmarshall(getResourceAsString("res/structure-with-booleans-and-strings.xml"), ImmutableStructureValueImpl.class);
        final Map expectedFields = new HashMap();
        MutableStringValue stringValue = new MutableStringValueImpl();
        stringValue.setValue("First Value");
        expectedFields.put("first", stringValue.createImmutable());
        stringValue.setValue("Fourth Value");
        expectedFields.put("fourth", stringValue.createImmutable());
        MutableBooleanValue booleanValue = new MutableBooleanValueImpl();
        booleanValue.setValue(Boolean.TRUE);
        expectedFields.put("second", booleanValue.createImmutable());
        booleanValue.setValue(Boolean.FALSE);
        expectedFields.put("third", booleanValue.createImmutable());
        assertEquals(expectedFields, structure.getFieldValuesAsMap());
        final Object otherStructure = doRoundTrip(structure);
        assertEquals(structure, otherStructure);
    }

    public void testNestedStructure() throws Exception {
        final ImmutableStructureValue structure = (ImmutableStructureValue) unmarshall(getResourceAsString("res/nested-structure.xml"), ImmutableStructureValueImpl.class);
        final Map expectedFields = new HashMap();
        final MutableStructureValueImpl structureValue = new MutableStructureValueImpl();
        final MutableStringValue value = new MutableStringValueImpl();
        value.setValue("1/A Value");
        structureValue.addField(new StructureFieldValue("sub1", (MetaDataValue) value.createImmutable()));
        value.setValue("1/B Value");
        structureValue.addField(new StructureFieldValue("sub2", (MetaDataValue) value.createImmutable()));
        expectedFields.put("first", structureValue);
        value.setValue("Second Value");
        expectedFields.put("second", value.createImmutable());
        assertEquals(expectedFields, structure.getFieldValuesAsMap());
        final Object otherStructure = doRoundTrip(structure);
        assertEquals(structure, otherStructure);
    }

    public void testListWithStrings() throws Exception {
        final ImmutableListValue list = (ImmutableListValue) unmarshall(getResourceAsString("res/list-with-strings.xml"), ImmutableListValueImpl.class);
        final List expectedList = new LinkedList();
        MutableStringValue value = new MutableStringValueImpl();
        value.setValue("First Value");
        expectedList.add(value.createImmutable());
        value.setValue("Second Value");
        expectedList.add(value.createImmutable());
        assertEquals(expectedList, list.getContentsAsList());
        final Object otherList = doRoundTrip(list);
        assertEquals(list, otherList);
    }

    public void testListWithBooleansAndStrings() throws Exception {
        final ImmutableListValue list = (ImmutableListValue) unmarshall(getResourceAsString("res/list-with-booleans-and-strings.xml"), ImmutableListValueImpl.class);
        final List expectedList = new LinkedList();
        MutableStringValue stringValue = new MutableStringValueImpl();
        stringValue.setValue("First Value");
        expectedList.add(stringValue.createImmutable());
        MutableBooleanValue booleanValue = new MutableBooleanValueImpl();
        booleanValue.setValue(Boolean.TRUE);
        expectedList.add(booleanValue.createImmutable());
        booleanValue.setValue(Boolean.FALSE);
        expectedList.add(booleanValue.createImmutable());
        stringValue.setValue("Fourth Value");
        expectedList.add(stringValue.createImmutable());
        assertEquals(expectedList, list.getContentsAsList());
        final Object otherList = doRoundTrip(list);
        assertEquals(list, otherList);
    }

    public void testListWithNumbers() throws Exception {
        final ImmutableListValue list = (ImmutableListValue) unmarshall(getResourceAsString("res/list-with-numbers.xml"), ImmutableListValueImpl.class);
        final List expectedList = new LinkedList();
        MutableNumberValue value = new MutableNumberValueImpl();
        value.setValue(new Byte((byte) 42));
        expectedList.add(value.createImmutable());
        value.setValue(new Short((short) 1));
        expectedList.add(value.createImmutable());
        value.setValue(new Integer(2));
        expectedList.add(value.createImmutable());
        value.setValue(new Long(3));
        expectedList.add(value.createImmutable());
        value.setValue(new BigInteger("4"));
        expectedList.add(value.createImmutable());
        value.setValue(new BigDecimal("4.2"));
        expectedList.add(value.createImmutable());
        value.setValue(new Float(0.1f));
        expectedList.add(value.createImmutable());
        value.setValue(new Double(0.2d));
        expectedList.add(value.createImmutable());
        assertEquals(expectedList, list.getContentsAsList());
        final Object otherList = doRoundTrip(list);
        assertEquals(list, otherList);
    }

    public void testNumberByte() throws Exception {
        final ImmutableNumberValue number = (ImmutableNumberValue) unmarshall(getResourceAsString("res/number.xml"), ImmutableNumberValueImpl.class);
        assertEquals(new Byte("42"), number.getValueAsNumber());
        final Object otherNumber = doRoundTrip(number);
        assertEquals(number, otherNumber);
    }

    public void testString() throws Exception {
        final ImmutableStringValue string = (ImmutableStringValue) unmarshall(getResourceAsString("res/string.xml"), ImmutableStringValueImpl.class);
        assertEquals("hello world", string.getAsString());
        final Object otherString = doRoundTrip(string);
        assertEquals(string, otherString);
    }

    public void testSetWithStrings() throws Exception {
        final ImmutableSetValue set = (ImmutableSetValue) unmarshall(getResourceAsString("res/set-with-strings.xml"), ImmutableSetValueImpl.class);
        final Set expectedSet = new HashSet();
        MutableStringValue value = new MutableStringValueImpl();
        value.setValue("First Value");
        expectedSet.add(value.createImmutable());
        value.setValue("Second Value");
        expectedSet.add(value.createImmutable());
        assertEquals(expectedSet, set.getContentsAsSet());
        final Object otherList = doRoundTrip(set);
        assertEquals(set, otherList);
    }

    public void testSetWithBooleansAndStrings() throws Exception {
        final ImmutableSetValue set = (ImmutableSetValue) unmarshall(getResourceAsString("res/set-with-booleans-and-strings.xml"), ImmutableSetValueImpl.class);
        final Set expectedSet = new HashSet();
        MutableStringValue stringValue = new MutableStringValueImpl();
        stringValue.setValue("First Value");
        expectedSet.add(stringValue.createImmutable());
        MutableBooleanValue booleanValue = new MutableBooleanValueImpl();
        booleanValue.setValue(Boolean.TRUE);
        expectedSet.add(booleanValue.createImmutable());
        booleanValue.setValue(Boolean.FALSE);
        expectedSet.add(booleanValue.createImmutable());
        stringValue.setValue("Fourth Value");
        expectedSet.add(stringValue.createImmutable());
        assertEquals(expectedSet, set.getContentsAsSet());
        final Object otherList = doRoundTrip(set);
        assertEquals(set, otherList);
    }

    public void testAddressValue() throws Exception {
        final ImmutableStructureValue structure = (ImmutableStructureValue) unmarshall(getResourceAsString("res/address-value.xml"), ImmutableStructureValueImpl.class);
        final Map fields = structure.getFieldValuesAsMap();
        assertEquals(1, fields.size());
        final ImmutableStructureValue addressValue = (ImmutableStructureValue) fields.get("address");
        final Map addressFields = addressValue.getFieldValuesAsMap();
        assertEquals(2, addressFields.size());
        final ImmutableStringValue emailValue = (ImmutableStringValue) addressFields.get("email");
        assertEquals("foo-bar@example.com", emailValue.getValueAsString());
        final ImmutableStructureValue postalValue = (ImmutableStructureValue) addressFields.get("postal");
        final Map postalFields = postalValue.getFieldValuesAsMap();
        assertEquals(4, postalFields.size());
        final ImmutableNumberValue houseNumberValue = (ImmutableNumberValue) postalFields.get("house-number");
        assertEquals(new Integer(42), houseNumberValue.getValueAsNumber());
        final ImmutableStringValue streetNameValue = (ImmutableStringValue) postalFields.get("street-name");
        assertEquals("Foo", streetNameValue.getValueAsString());
        final ImmutableStringValue streetTypeValue = (ImmutableStringValue) postalFields.get("street-type");
        assertEquals("Road", streetTypeValue.getValueAsString());
        final ImmutableStringValue cityValue = (ImmutableStringValue) postalFields.get("city");
        assertEquals("Bar", cityValue.getValueAsString());
        final Object otherStructure = doRoundTrip(structure);
        assertEquals(structure, otherStructure);
    }

    private String getResourceAsString(final String name) throws IOException {
        final InputStream is = JiBXTestCase.class.getResourceAsStream(name);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copyAndClose(is, baos);
        return baos.toString();
    }
}
