package com.volantis.shared.metadata.impl.type;

import com.volantis.shared.metadata.impl.JiBXTestCaseAbstract;
import com.volantis.shared.metadata.impl.type.constraint.ImmutableEnumeratedConstraintImpl;
import com.volantis.shared.metadata.impl.type.constraint.ImmutableMaximumValueConstraintImpl;
import com.volantis.shared.metadata.impl.type.constraint.ImmutableMemberTypeConstraintImpl;
import com.volantis.shared.metadata.impl.type.constraint.ImmutableMinimumValueConstraintImpl;
import com.volantis.shared.metadata.type.BooleanType;
import com.volantis.shared.metadata.type.FieldDefinition;
import com.volantis.shared.metadata.type.MetaDataType;
import com.volantis.shared.metadata.type.StringType;
import com.volantis.shared.metadata.type.constraint.NumberSubTypeConstraint;
import com.volantis.shared.metadata.type.constraint.immutable.ImmutableEnumeratedConstraint;
import com.volantis.shared.metadata.type.constraint.immutable.ImmutableMaximumValueConstraint;
import com.volantis.shared.metadata.type.constraint.immutable.ImmutableMinimumValueConstraint;
import com.volantis.shared.metadata.type.constraint.immutable.ImmutableNumberSubTypeConstraint;
import com.volantis.shared.metadata.type.immutable.ImmutableListType;
import com.volantis.shared.metadata.type.immutable.ImmutableNumberType;
import com.volantis.shared.metadata.type.immutable.ImmutableSetType;
import com.volantis.shared.metadata.type.immutable.ImmutableStringType;
import com.volantis.shared.metadata.type.immutable.ImmutableStructureType;
import com.volantis.shared.metadata.value.MetaDataValue;
import com.volantis.shared.metadata.value.StringValue;
import com.volantis.shared.metadata.value.immutable.ImmutableStringValue;
import com.volantis.synergetics.io.IOUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Tests to verify JiBX marshaller/unmarshaller.
 */
public class JiBXTestCase extends JiBXTestCaseAbstract {

    public void testList() throws Exception {
        final ImmutableListType listType = (ImmutableListType) unmarshall(getResourceAsString("res/list.xml"), ImmutableListTypeImpl.class);
        assertNull(listType.getUniqueMemberConstraint());
        final Object otherListType = doRoundTrip(listType);
        assertEquals(listType, otherListType);
    }

    public void testListWithUniqueMemberConstraint() throws Exception {
        final ImmutableListType listType = (ImmutableListType) unmarshall(getResourceAsString("res/list-with-unique-member-constraint.xml"), ImmutableListTypeImpl.class);
        assertNotNull(listType.getUniqueMemberConstraint());
        final Object otherListType = doRoundTrip(listType);
        assertEquals(listType, otherListType);
    }

    public void testListWithMemberTypeConstraintString() throws Exception {
        final ImmutableListType listType = (ImmutableListType) unmarshall(getResourceAsString("res/list-with-member-type-constraint-string.xml"), ImmutableListTypeImpl.class);
        final MetaDataType memberType = listType.getMemberTypeConstraint().getMemberType();
        assertTrue(memberType instanceof StringType);
        final Object otherListType = doRoundTrip(listType);
        assertEquals(listType, otherListType);
    }

    public void testListWithMemberTypeConstraintBoolean() throws Exception {
        final ImmutableListType listType = (ImmutableListType) unmarshall(getResourceAsString("res/list-with-member-type-constraint-boolean.xml"), ImmutableListTypeImpl.class);
        final MetaDataType memberType = listType.getMemberTypeConstraint().getMemberType();
        assertTrue(memberType instanceof BooleanType);
        final Object otherListType = doRoundTrip(listType);
        assertEquals(listType, otherListType);
    }

    public void testListWithMinimumLengthConstraint() throws Exception {
        final ImmutableListTypeImpl list = (ImmutableListTypeImpl) unmarshall(getResourceAsString("res/list-with-minimum-length-constraint.xml"), ImmutableStringTypeImpl.class);
        assertEquals(2, list.getMinimumLengthConstraint().getLimit());
        final Object otherList = doRoundTrip(list);
        assertEquals(list, otherList);
    }

    public void testListWithMaximumLengthConstraint() throws Exception {
        final ImmutableListTypeImpl list = (ImmutableListTypeImpl) unmarshall(getResourceAsString("res/list-with-maximum-length-constraint.xml"), ImmutableStringTypeImpl.class);
        assertEquals(2, list.getMaximumLengthConstraint().getLimit());
        final Object otherList = doRoundTrip(list);
        assertEquals(list, otherList);
    }

    public void testSetWithMemberTypeConstraintString() throws Exception {
        final ImmutableSetType setType = (ImmutableSetType) unmarshall(getResourceAsString("res/set-with-member-type-constraint-string.xml"), ImmutableSetTypeImpl.class);
        final MetaDataType memberType = setType.getMemberTypeConstraint().getMemberType();
        assertTrue(memberType instanceof StringType);
        final Object otherSetType = doRoundTrip(setType);
        assertEquals(setType, otherSetType);
    }

    public void testSeWithMemberTypeConstraintBoolean() throws Exception {
        final ImmutableSetType setType = (ImmutableSetType) unmarshall(getResourceAsString("res/set-with-member-type-constraint-boolean.xml"), ImmutableSetTypeImpl.class);
        final MetaDataType memberType = setType.getMemberTypeConstraint().getMemberType();
        assertTrue(memberType instanceof BooleanType);
        final Object otherSetType = doRoundTrip(setType);
        assertEquals(setType, otherSetType);
    }

    public void testMemberTypeConstraintString() throws Exception {
        final ImmutableMemberTypeConstraintImpl constraint = (ImmutableMemberTypeConstraintImpl) unmarshall(getResourceAsString("res/member-type-constraint-string.xml"), ImmutableMemberTypeConstraintImpl.class);
        final MetaDataType memberType = constraint.getMemberType();
        assertTrue(memberType instanceof StringType);
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testEnumeratedConstraint() throws Exception {
        final ImmutableEnumeratedConstraintImpl constraint = (ImmutableEnumeratedConstraintImpl) unmarshall(getResourceAsString("res/enumerated-constraint-string.xml"), ImmutableEnumeratedConstraintImpl.class);
        final List enumeratedValues = constraint.getEnumeratedValues();
        assertEquals(3, enumeratedValues.size());
        assertEquals("one", ((StringValue) enumeratedValues.get(0)).getAsString());
        assertEquals("two", ((StringValue) enumeratedValues.get(1)).getAsString());
        assertEquals("three", ((StringValue) enumeratedValues.get(2)).getAsString());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testString() throws Exception {
        final ImmutableStringValue string = (ImmutableStringValue) unmarshall(getResourceAsString("res/string.xml"), ImmutableStringTypeImpl.class);
        assertEquals("hello world", string.getAsString());
        final Object otherString = doRoundTrip(string);
        assertEquals(string, otherString);
    }

    public void testStringType() throws Exception {
        final ImmutableStringType string = (ImmutableStringType) unmarshall(getResourceAsString("res/string-without-enumerated-constraint.xml"), ImmutableStringTypeImpl.class);
        assertNull(string.getEnumeratedConstraint());
        final Object otherString = doRoundTrip(string);
        assertEquals(string, otherString);
    }

    public void testStringWithEnumeratedConstraint() throws Exception {
        final ImmutableStringTypeImpl string = (ImmutableStringTypeImpl) unmarshall(getResourceAsString("res/string-with-enumerated-constraint.xml"), ImmutableStringTypeImpl.class);
        final List enumeratedValues = string.getEnumeratedConstraint().getEnumeratedValues();
        assertEquals(2, enumeratedValues.size());
        final MetaDataValue first = (MetaDataValue) enumeratedValues.get(0);
        assertTrue(first instanceof StringValue);
        assertEquals("foo", first.getAsString());
        final MetaDataValue second = (MetaDataValue) enumeratedValues.get(1);
        assertEquals("bar", second.getAsString());
        assertTrue(second instanceof StringValue);
        final Object otherString = doRoundTrip(string);
        assertEquals(string, otherString);
    }

    public void testStringWithMinimumLengthConstraint() throws Exception {
        final ImmutableStringTypeImpl string = (ImmutableStringTypeImpl) unmarshall(getResourceAsString("res/string-with-minimum-length-constraint.xml"), ImmutableStringTypeImpl.class);
        assertEquals(2, string.getMinimumLengthConstraint().getLimit());
        final Object otherString = doRoundTrip(string);
        assertEquals(string, otherString);
    }

    public void testStringWithMaximumLengthConstraint() throws Exception {
        final ImmutableStringTypeImpl string = (ImmutableStringTypeImpl) unmarshall(getResourceAsString("res/string-with-maximum-length-constraint.xml"), ImmutableStringTypeImpl.class);
        assertEquals(6, string.getMaximumLengthConstraint().getLimit());
        final Object otherString = doRoundTrip(string);
        assertEquals(string, otherString);
    }

    public void testNumberWithSubTypeConstraintByte() throws Exception {
        final ImmutableNumberTypeImpl number = (ImmutableNumberTypeImpl) unmarshall(getResourceAsString("res/number-with-sub-type-constraint-byte.xml"), ImmutableNumberTypeImpl.class);
        final NumberSubTypeConstraint subTypeConstraint = number.getNumberSubTypeConstraint();
        assertEquals(Byte.class, subTypeConstraint.getNumberSubType());
        assertNull(number.getMinimumValueConstraint());
        final Object otherNumber = doRoundTrip(number);
        assertEquals(number, otherNumber);
    }

    public void testNumberWithMinimumValueConstraintByte() throws Exception {
        final ImmutableNumberTypeImpl number = (ImmutableNumberTypeImpl) unmarshall(getResourceAsString("res/number-with-minimum-value-constraint-byte.xml"), ImmutableNumberTypeImpl.class);
        final ImmutableMinimumValueConstraint minimumValueConstraint = number.getMinimumValueConstraint();
        assertEquals(new Byte((byte) 42), minimumValueConstraint.getLimitAsNumber());
        assertNull(number.getNumberSubTypeConstraint());
        assertNull(number.getMaximumValueConstraint());
        final Object otherNumber = doRoundTrip(number);
        assertEquals(number, otherNumber);
    }

    public void testNumberWithMaximumValueConstraintShort() throws Exception {
        final ImmutableNumberTypeImpl number = (ImmutableNumberTypeImpl) unmarshall(getResourceAsString("res/number-with-maximum-value-constraint-short.xml"), ImmutableNumberTypeImpl.class);
        final ImmutableMaximumValueConstraint maximumValueConstraint = number.getMaximumValueConstraint();
        assertEquals(new Short((short) 42), maximumValueConstraint.getLimitAsNumber());
        assertNull(number.getNumberSubTypeConstraint());
        assertNull(number.getMinimumValueConstraint());
        final Object otherNumber = doRoundTrip(number);
        assertEquals(number, otherNumber);
    }

    public void testNumberWithBothMinimumAndMaximumValueConstraintInt() throws Exception {
        final ImmutableNumberTypeImpl number = (ImmutableNumberTypeImpl) unmarshall(getResourceAsString("res/number-with-both-minimum-and-maximum-value-constraint-int.xml"), ImmutableNumberTypeImpl.class);
        final ImmutableMinimumValueConstraint minimumValueConstraint = number.getMinimumValueConstraint();
        assertEquals(new Integer(-42), minimumValueConstraint.getLimitAsNumber());
        final ImmutableMaximumValueConstraint maximumValueConstraint = number.getMaximumValueConstraint();
        assertEquals(new Integer(42), maximumValueConstraint.getLimitAsNumber());
        assertNull(number.getNumberSubTypeConstraint());
        final Object otherNumber = doRoundTrip(number);
        assertEquals(number, otherNumber);
    }

    public void testNumberWithAllValueConstraintsDecimal() throws Exception {
        final ImmutableNumberTypeImpl number = (ImmutableNumberTypeImpl) unmarshall(getResourceAsString("res/number-with-all-value-constraints-decimal.xml"), ImmutableNumberTypeImpl.class);
        final ImmutableMinimumValueConstraint minimumValueConstraint = number.getMinimumValueConstraint();
        assertEquals(new BigDecimal("-4.2"), minimumValueConstraint.getLimitAsNumber());
        final ImmutableMaximumValueConstraint maximumValueConstraint = number.getMaximumValueConstraint();
        assertEquals(new BigDecimal("4.2"), maximumValueConstraint.getLimitAsNumber());
        final NumberSubTypeConstraint subTypeConstraint = number.getNumberSubTypeConstraint();
        assertEquals(BigDecimal.class, subTypeConstraint.getNumberSubType());
        final Object otherNumber = doRoundTrip(number);
        assertEquals(number, otherNumber);
    }

    public void testMinimumValueConstraintByte() throws Exception {
        final ImmutableMinimumValueConstraint constraint = (ImmutableMinimumValueConstraint) unmarshall(getResourceAsString("res/minimum-value-constraint-byte.xml"), ImmutableMinimumValueConstraintImpl.class);
        assertEquals(new Byte((byte) 42), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMinimumValueConstraintShort() throws Exception {
        final ImmutableMinimumValueConstraint constraint = (ImmutableMinimumValueConstraint) unmarshall(getResourceAsString("res/minimum-value-constraint-short.xml"), ImmutableMinimumValueConstraintImpl.class);
        assertEquals(new Short((short) 42), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMinimumValueConstraintInt() throws Exception {
        final ImmutableMinimumValueConstraint constraint = (ImmutableMinimumValueConstraint) unmarshall(getResourceAsString("res/minimum-value-constraint-int.xml"), ImmutableMinimumValueConstraintImpl.class);
        assertEquals(new Integer(42), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMinimumValueConstraintLong() throws Exception {
        final ImmutableMinimumValueConstraint constraint = (ImmutableMinimumValueConstraint) unmarshall(getResourceAsString("res/minimum-value-constraint-long.xml"), ImmutableMinimumValueConstraintImpl.class);
        assertEquals(new Long(42), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMinimumValueConstraintInteger() throws Exception {
        final ImmutableMinimumValueConstraint constraint = (ImmutableMinimumValueConstraint) unmarshall(getResourceAsString("res/minimum-value-constraint-integer.xml"), ImmutableMinimumValueConstraintImpl.class);
        assertEquals(new BigInteger("42"), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMinimumValueConstraintDecimal() throws Exception {
        final ImmutableMinimumValueConstraint constraint = (ImmutableMinimumValueConstraint) unmarshall(getResourceAsString("res/minimum-value-constraint-decimal.xml"), ImmutableMinimumValueConstraintImpl.class);
        assertEquals(new BigDecimal("4.2"), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMinimumValueConstraintFloat() throws Exception {
        final ImmutableMinimumValueConstraint constraint = (ImmutableMinimumValueConstraint) unmarshall(getResourceAsString("res/minimum-value-constraint-float.xml"), ImmutableMinimumValueConstraintImpl.class);
        assertEquals(new Float(4.2f), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMinimumValueConstraintDouble() throws Exception {
        final ImmutableMinimumValueConstraint constraint = (ImmutableMinimumValueConstraint) unmarshall(getResourceAsString("res/minimum-value-constraint-double.xml"), ImmutableMinimumValueConstraintImpl.class);
        assertEquals(new Double(4.2d), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMaximumValueConstraintByte() throws Exception {
        final ImmutableMaximumValueConstraint constraint = (ImmutableMaximumValueConstraint) unmarshall(getResourceAsString("res/maximum-value-constraint-byte.xml"), ImmutableMaximumValueConstraintImpl.class);
        assertEquals(new Byte((byte) 42), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMaximumValueConstraintShort() throws Exception {
        final ImmutableMaximumValueConstraint constraint = (ImmutableMaximumValueConstraint) unmarshall(getResourceAsString("res/maximum-value-constraint-short.xml"), ImmutableMaximumValueConstraintImpl.class);
        assertEquals(new Short((short) 42), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMaximumValueConstraintInt() throws Exception {
        final ImmutableMaximumValueConstraint constraint = (ImmutableMaximumValueConstraint) unmarshall(getResourceAsString("res/maximum-value-constraint-int.xml"), ImmutableMaximumValueConstraintImpl.class);
        assertEquals(new Integer(42), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMaximumValueConstraintLong() throws Exception {
        final ImmutableMaximumValueConstraint constraint = (ImmutableMaximumValueConstraint) unmarshall(getResourceAsString("res/maximum-value-constraint-long.xml"), ImmutableMaximumValueConstraintImpl.class);
        assertEquals(new Long(42), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMaximumValueConstraintInteger() throws Exception {
        final ImmutableMaximumValueConstraint constraint = (ImmutableMaximumValueConstraint) unmarshall(getResourceAsString("res/maximum-value-constraint-integer.xml"), ImmutableMaximumValueConstraintImpl.class);
        assertEquals(new BigInteger("42"), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMaximumValueConstraintDecimal() throws Exception {
        final ImmutableMaximumValueConstraint constraint = (ImmutableMaximumValueConstraint) unmarshall(getResourceAsString("res/maximum-value-constraint-decimal.xml"), ImmutableMaximumValueConstraintImpl.class);
        assertEquals(new BigDecimal("4.2"), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMaximumValueConstraintFloat() throws Exception {
        final ImmutableMaximumValueConstraint constraint = (ImmutableMaximumValueConstraint) unmarshall(getResourceAsString("res/maximum-value-constraint-float.xml"), ImmutableMaximumValueConstraintImpl.class);
        assertEquals(new Float(4.2f), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testMaximumValueConstraintDouble() throws Exception {
        final ImmutableMaximumValueConstraint constraint = (ImmutableMaximumValueConstraint) unmarshall(getResourceAsString("res/maximum-value-constraint-double.xml"), ImmutableMaximumValueConstraintImpl.class);
        assertEquals(new Double(4.2d), constraint.getLimitAsNumber());
        final Object otherConstraint = doRoundTrip(constraint);
        assertEquals(constraint, otherConstraint);
    }

    public void testAddressType() throws Exception {
        final ImmutableStructureType rootType = (ImmutableStructureType) unmarshall(getResourceAsString("res/address-type.xml"), ImmutableStructureTypeImpl.class);
        final Set rootFields = rootType.getFields();
        assertEquals(1, rootFields.size());
        final FieldDefinition addressDef = (FieldDefinition) rootFields.iterator().next();
        final ImmutableStructureType addressType = (ImmutableStructureType) addressDef.getType();
        final Set addressFields = addressType.getFields();
        assertEquals(2, addressFields.size());
        final FieldDefinition postalDef = getFieldDefinition("postal", addressFields);
        final FieldDefinition emailDef = getFieldDefinition("email", addressFields);
        final ImmutableStringType emailType = (ImmutableStringType) emailDef.getType();
        assertNull(emailType.getEnumeratedConstraint());
        final ImmutableStructureType postalType = (ImmutableStructureType) postalDef.getType();
        final Set postalFields = postalType.getFields();
        assertEquals(4, postalFields.size());
        final FieldDefinition cityDef = getFieldDefinition("city", postalFields);
        final FieldDefinition streetNameDef = getFieldDefinition("street-name", postalFields);
        final FieldDefinition streetTypeDef = getFieldDefinition("street-type", postalFields);
        final FieldDefinition houseNumberDef = getFieldDefinition("house-number", postalFields);
        final ImmutableStringType cityType = (ImmutableStringType) cityDef.getType();
        assertNull(cityType.getEnumeratedConstraint());
        final ImmutableStringType streetNameType = (ImmutableStringType) streetNameDef.getType();
        assertNull(streetNameType.getEnumeratedConstraint());
        final ImmutableStringType streetTypeType = (ImmutableStringType) streetTypeDef.getType();
        final ImmutableEnumeratedConstraint enumeratedConstraint = streetTypeType.getEnumeratedConstraint();
        final Iterator constraintIter = enumeratedConstraint.getEnumeratedValues().iterator();
        final ImmutableStringValue road = (ImmutableStringValue) constraintIter.next();
        assertEquals("Road", road.getValueAsString());
        final ImmutableStringValue street = (ImmutableStringValue) constraintIter.next();
        assertEquals("Street", street.getValueAsString());
        final ImmutableStringValue avenue = (ImmutableStringValue) constraintIter.next();
        assertEquals("Avenue", avenue.getValueAsString());
        final ImmutableNumberType houseNumberType = (ImmutableNumberType) houseNumberDef.getType();
        final ImmutableNumberSubTypeConstraint subTypeConstraint = houseNumberType.getNumberSubTypeConstraint();
        assertEquals(Integer.class, subTypeConstraint.getNumberSubType());
        final ImmutableMinimumValueConstraint minimumValueConstraint = houseNumberType.getMinimumValueConstraint();
        assertEquals(new Integer(0), minimumValueConstraint.getLimitAsNumber());
        final Object otherAddress = doRoundTrip(rootType);
        assertEquals(rootType, otherAddress);
    }

    private FieldDefinition getFieldDefinition(final String name, final Set fieldDefs) {
        FieldDefinition result = null;
        for (Iterator iter = fieldDefs.iterator(); iter.hasNext() && result == null; ) {
            final FieldDefinition fieldDef = (FieldDefinition) iter.next();
            if (name.equals(fieldDef.getName())) {
                result = fieldDef;
            }
        }
        return result;
    }

    private String getResourceAsString(final String name) throws IOException {
        final InputStream is = JiBXTestCase.class.getResourceAsStream(name);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copyAndClose(is, baos);
        return baos.toString();
    }
}
