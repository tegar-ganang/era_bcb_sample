package com.ibm.oti.pim;

import java.util.Vector;
import javax.microedition.pim.Contact;
import javax.microedition.pim.Event;
import javax.microedition.pim.FieldFullException;
import javax.microedition.pim.PIM;
import javax.microedition.pim.PIMException;
import javax.microedition.pim.PIMItem;
import javax.microedition.pim.PIMList;
import javax.microedition.pim.ToDo;
import javax.microedition.pim.UnsupportedFieldException;

class PIMItemImpl implements PIMItem {

    /**
	 *	PIMList that owns this PIMItem.
	 */
    protected PIMListImpl owner;

    /**
	 *	The handle of this PIMItem.
	 */
    protected int rechandle;

    protected Int2 i2;

    /**
	 * Int array that holds field IDs, attributes and indexes of 
	 * STRING / STRING_ARRAY fields.
	 * <p>
	 * <table border=1>
	 * <TR>
	 * <th> stringids </th>
	 * </tr>
	 * <tr><td>Number of entries</td></tr>
	 * <tr><td>field ID 1</td></tr>
	 * <tr><td>index</td></tr>
	 * <tr><td>attributes</td></tr>
	 * <tr><td>Field ID 2</td></tr>
	 * <tr><td>...</td></tr>
	 * </table>
	 */
    protected int[] stringids;

    /**
	 * String array that holds the values of STRING / STRING_ARRAY fields.
	 * The order follows the order in <code>stringids</code> 
	 * (<code>value n</code> corresponds to the entry described 
	 * by <code>field ID n</code> in the <code>stringids
	 * </code> array).
	 * <p>
	 * <table border=1>
	 * <TR>
	 * <th> stringvalues </th>
	 * </tr>
	 * <tr><td>Number of entries</td></tr>
	 * <tr><td>value 1</td></tr>
	 * <tr><td>value 2</td></tr>
	 * <tr><td>...</td></tr>
	 * </table>
	 */
    protected String[] stringvalues;

    /**
	 * Long array that holds the field IDs, attributes, indexes and values 
	 * of INT, BOOLEAN and DATE fields.
	 * <p>
	 * <table border=1>
	 * <TR>
	 * <th> longvalues </th>
	 * </tr>
	 * <tr><td>Number of entries</td></tr>
	 * <tr><td>field ID 1</td></tr>
	 * <tr><td>index</td></tr>
	 * <tr><td>attributes</td></tr>
	 * <tr><td>value</td></tr>
	 * <tr><td>Field ID 2</td></tr>
	 * <tr><td>...</td></tr>
	 * </table>
	 */
    protected long[] longvalues;

    /**
	 * Int array that holds field IDs, attributes and indexes of BINARY fields.
	 * <p>
	 * <table border=1>
	 * <TR>
	 * <th> byteids </th>
	 * </tr>
	 * <tr><td>Number of entries</td></tr>
	 * <tr><td>field ID 1</td></tr>
	 * <tr><td>index</td></tr>
	 * <tr><td>attributes</td></tr>
	 * <tr><td>Field ID 2</td></tr>
	 * <tr><td>...</td></tr>
	 * </table>
	 */
    protected int[] byteids;

    /**
	 * String array that holds the values of BINARY fields.
	 * The order follows the order of <code>byteids</code> 
	 * (<code>value n</code> corresponds to the entry described 
	 * by <code>field ID n</code> in the <code>byteids
	 * </code> array).
	 * <p>
	 * <table border=1>
	 * <TR>
	 * <th> bytevalues </th>
	 * </tr>
	 * <tr><td>Number of entries</td></tr>
	 * <tr><td>value 1</td></tr>
	 * <tr><td>value 2</td></tr>
	 * <tr><td>...</td></tr>
	 * </table>
	 */
    protected byte[] bytevalues;

    /**
	 * String array that holds the categories that the item 
	 * belongs to.
	 * <p>
	 * <table border=1>
	 * <TR>
	 * <th> categories </th>
	 * </tr>
	 * <tr><td>Number of entries</td></tr>
	 * <tr><td>category 1</td></tr>
	 * <tr><td>category 2</td></tr>
	 * <tr><td>...</td></tr>
	 * </table>
	 */
    protected String[] categories;

    protected boolean isDeleted = false;

    protected boolean modified = false;

    protected boolean valuesFromOS = true;

    protected boolean isLoadedFromOS = false;

    protected boolean isStructLoading = false;

    /**
	 * Stores extended properties read from serial format.
	 */
    protected Vector VExtendedFields = null;

    protected class Int2 {

        public int int1 = -1, int2 = -1;

        public Int2() {
        }
    }

    /**
	 * Constructor for PIMItemImpl.
	 */
    PIMItemImpl(PIMListImpl owner, int rechandle) {
        this.owner = owner;
        this.rechandle = rechandle;
        if (rechandle == -1) {
            valuesFromOS = false;
            initArrays();
            modified = true;
        }
        i2 = new Int2();
    }

    /**
	 * Answers if the given record is deleted.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle.
	 * @return boolean true if the record has been deleted.
	 * @throws RuntimeException if the list is not accessible.
	 */
    private native boolean isRecordDeletedN(int listType, int handle, int rechandle);

    /**
	 * Answers the maximun number of categories supported by the underlying implementation.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle.
	 * @return int the number of categories supported by this list.
	 * 		0 indicates no category supported and -1 indicates there is no limit.
	 * @throws RuntimeException if the record has been deleted or the list
	 * 		is not accessible.
	 */
    private native int maxCategoriesN(int listType, int handle, int rechandle);

    /**
	 * Answers the list of categories for the given ietm.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle.
	 * @return String[] A string array containing the categories defined for the PIM list.
	 * 		A zero length array is returned if no categories have been assigned to this item or categories are not supported, there is no list.
	 * @throws RuntimeException if the record has been deleted or the list
	 * 		is not accessible.
	 */
    private native String[] getCategoriesN(int listType, int handle, int rechandle);

    /**
	 * Counts the number of values set for the given field..
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle.
	 * @param field One of the supported fields.
	 * @return int
	 * @throws RuntimeException if the record has been deleted 
	 * 		or the list is not accesible.
	 */
    private native int countValuesN(int listType, int handle, int rechandle, int field);

    /**
	 * Answers an array of set fields.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle. The record handle.
	 * @return int[] The fields set or a zero length array if 
	 * 		there is no field set.
	 * @throws RuntimeException if the record has been deleted or
	 * 		the list is not accessible.
	 */
    private native int[] getFieldsN(int listType, int handle, int rechandle);

    /**
	 * Answers the string value of the given field.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle.
	 * @param field One of the supported string fields.
	 * @param index The value index.
	 * @return String Returns the string value or an empty String.
	 * @throws RuntimeException if the record has been deleted or
	 * 		the list is not accessible.
	 */
    private native String getStringN(int listType, int handle, int rechandle, int field, int index);

    /**
	 * Answers the string array value of the given field.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle.
	 * @param field One of the supported string array fields.
	 * @param index The value index.
	 * @return String[] The value or an empty array.
	 * @throws RuntimeException if the record has been deleted or
	 * 		the list is not accessible.
	 */
    private native String[] getStringArrayN(int listType, int handle, int rechandle, int field, int index);

    /**
	 * Answers the int value of the given field.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle.
	 * @param field One of the supported int fields.
	 * @param index The value index.
	 * @return int
	 * @throws RuntimeException if the record has been deleted or
	 * 		the list is not accessible.
	 */
    private native int getIntN(int listType, int handle, int rechandle, int field, int index);

    /**
	 * Answers the boolean value of the given field.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle.
	 * @param field One of the supported boolean fields.
	 * @param index The value index.
	 * @return boolean
	 * @throws RuntimeException if the record has been deleted or
	 * 		the list is not accessible.
	 */
    private native boolean getBooleanN(int listType, int handle, int rechandle, int field, int index);

    /**
	 * Answers the date value of the given field.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle.
	 * @param field One of the supported date fields.
	 * @param index The value index.
	 * @return long.
	 * @throws RuntimeException if the record has been deleted or
	 * 		the list is not accessible.
	 */
    private native long getDateN(int listType, int handle, int rechandle, int field, int index);

    /**
	 * Answers the binary value of the given field.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle.
	 * @param field One of the supported binary fields.
	 * @param index The value index.
	 * @return byte[] The value or an empty array;
	 * @throws RuntimeException if the record has been deleted or
	 * 		the list is not accessible.
	 */
    private native byte[] getBinaryN(int listType, int handle, int rechandle, int field, int index);

    /**
	 * Answers a int representing the field attributes.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle.
	 * @param field One of the supported date fields.
	 * @param index The value index.
	 * @return int The field attributes or -1.
	 * @throws RuntimeException if record has been deleted or
	 * 		the list is not accessible.
	 */
    private native int getAttributesN(int listType, int handle, int rechandle, int field, int index);

    /**
	 * This method persists the data in the item to its PIM list. 
	 * The method must provide default values for required fields when necessary.
	 * (The repeat fields are not stored with their field ID but with an internal 
	 * index, see {@link javax.microedition.pim.RepeatRule#fields}).
	 * Invalid repeat rules should be silently discarded.
	 * @param listType {@link PIM#CONTACT_LIST}, {@link PIM#EVENT_LIST} or {@link PIM#TODO_LIST}.
	 * @param handle The list handle.
	 * @param rechandle The record handle. -1 indicates a new item 
	 * 		otherwise indicates the record to update
	 * @param stringids {@link #stringids}.
	 * @param stringvalues {@link #stringvalues}.
	 * @param byteids {@link #byteids}. 
	 * @param bytevalues {@link #bytevalues}.
	 * @param longvalues {@link #longvalues}.
	 * @param categories {@link #categories}.
	 * @param numcats The number of categories to which this item has been assigned.
	 * @return int
	 * @throws PIMException if the commit encounters an error and cannot complete.
	 */
    private native int commitN(int listType, int handle, int rechandle, int[] stringids, byte[][] stringvalues, int[] byteids, byte[] bytevalues, long[] longvalues, String[] categories, int numcats) throws PIMException;

    /**
	 * @see javax.microedition.pim.PIMItem#getPIMList()
	 */
    public PIMList getPIMList() {
        synchronized (i2) {
            return owner;
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#commit()
	 */
    public void commit() throws PIMException {
        synchronized (i2) {
            if (owner == null) throw new PIMException("The item does not belong to a list.");
            owner.checkListClosed();
            owner.checkListMode(PIM.READ_ONLY);
            if (isModified()) {
                updateRevisionDate();
                int length = 0;
                boolean next = indexInStringArray(-1, -1);
                if (!next) length = i2.int2;
                byte[][] stringbytes = new byte[length][];
                for (int i = 0; i < length; i++) {
                    if (stringvalues[i] != null) stringbytes[i] = stringvalues[i].getBytes(); else stringbytes[i] = null;
                }
                if (getListType() == PIM.EVENT_LIST) ((EventImpl) this).loadRepeatRule();
                rechandle = commitN(owner.listType, owner.handle, rechandle, stringids, stringbytes, byteids, bytevalues, longvalues, categories, Integer.parseInt(categories[0]));
                modified = false;
            }
            valuesFromOS = true;
            clearArrays();
        }
    }

    /**
	 * Updates the revision date if the field is supported.
	 */
    protected void updateRevisionDate() {
        int fieldID = 0;
        switch(owner.listType) {
            case PIM.CONTACT_LIST:
                fieldID = Contact.REVISION;
                break;
            case PIM.TODO_LIST:
                fieldID = ToDo.REVISION;
                break;
            case PIM.EVENT_LIST:
                fieldID = Event.REVISION;
                break;
        }
        if (owner.isSupportedField(fieldID)) {
            int count = countValues(fieldID);
            if (count == owner.maxValues(fieldID)) setLongValue(fieldID, count - 1, ATTR_NONE, System.currentTimeMillis(), Contact.DATE, false); else addLongValue(fieldID, ATTR_NONE, System.currentTimeMillis(), Contact.DATE);
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#isModified()
	 */
    public boolean isModified() {
        synchronized (i2) {
            checkItemDeleted();
            return modified;
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#getFields()
	 */
    public int[] getFields() {
        synchronized (i2) {
            checkItemDeleted();
            if (!valuesFromOS) return getFieldsFromStruct(); else {
                checkListClosed();
                return (int[]) getFieldsN(owner.listType, owner.handle, rechandle);
            }
        }
    }

    /**
	 * Answers the fields set from the pim structure.
	 * @return int[]
	 */
    protected int[] getFieldsFromStruct() {
        int size = stringids[0] + (int) longvalues[0] + byteids[0];
        int[] fields = new int[size];
        int count = 0, i = 1, ind = 0;
        int fieldCount = 0;
        if (!valuesFromOS) {
            while (count < stringids[0]) {
                if (!PIMUtil.contains(fields, stringids[i])) {
                    fields[ind++] = stringids[i];
                    fieldCount++;
                }
                i += 3;
                count++;
            }
            i = 1;
            count = 0;
            while (count < longvalues[0]) {
                if (!PIMUtil.contains(fields, (int) longvalues[i])) {
                    fields[ind++] = (int) longvalues[i];
                    fieldCount++;
                }
                i += 4;
                count++;
            }
            i = 1;
            count = 0;
            while (count < byteids[0]) {
                if (!PIMUtil.contains(fields, byteids[i])) {
                    fields[ind++] = byteids[i];
                    fieldCount++;
                }
                i += 4;
                count++;
            }
        }
        int[] result = new int[fieldCount];
        System.arraycopy(fields, 0, result, 0, fieldCount);
        return result;
    }

    /**
	 * @see javax.microedition.pim.PIMItem#getBinary(int, int)
	 */
    public byte[] getBinary(int field, int index) {
        synchronized (i2) {
            checkForExceptions(field, Contact.BINARY, true, index, true);
            if (!valuesFromOS) return getBinaryFromStruct(field, index); else {
                checkListClosed();
                return getBinaryN(owner.listType, owner.handle, rechandle, field, index);
            }
        }
    }

    /**
	 * Answers the binary value of the given field from the pim structure.
	 * @param field
	 * @param index
	 * @return byte[]
	 */
    protected byte[] getBinaryFromStruct(int field, int index) {
        byte[] value = null;
        if (indexInByteArray(field, index)) {
            value = new byte[byteids[i2.int1 + 3]];
            System.arraycopy(bytevalues, i2.int2, value, 0, value.length);
        } else {
            throw new RuntimeException("An error occured.");
        }
        return value;
    }

    /**
	 * @see javax.microedition.pim.PIMItem#addBinary(int, int, byte[], int, int)
	 */
    public void addBinary(int field, int attributes, byte[] value, int offset, int length) {
        synchronized (i2) {
            if (value == null) throw new NullPointerException();
            int arrayLength = value.length;
            if (offset >= arrayLength || offset < 0) throw new IllegalArgumentException("Invalid offset");
            if (length < 0 || offset + length > arrayLength) throw new IllegalArgumentException("Invalid length");
            int numValues = checkForExceptions(field, Contact.BINARY, false, 0, false);
            loadPIMStruct();
            int listType = getListType();
            if (listType == PIM.CONTACT_LIST && field == Contact.PHOTO) {
                int count = countValues(Contact.PHOTO_URL);
                for (int i = 0; i < count; i++) removeValue(Contact.PHOTO_URL, i);
            }
            if (listType == PIM.CONTACT_LIST && field == Contact.PUBLIC_KEY) {
                int count = countValues(Contact.PUBLIC_KEY_STRING);
                for (int i = 0; i < count; i++) removeValue(Contact.PUBLIC_KEY_STRING, i);
            }
            int arrayIndex = 0, valueIndex = 0;
            if (!indexInByteArray(-1, -1)) {
                arrayIndex = i2.int1;
                valueIndex = i2.int2;
            }
            int size = byteids.length;
            if (arrayIndex + 4 >= size) byteids = adjustIntArray(byteids, size + 15);
            if (valueIndex - 1 + length >= bytevalues.length) bytevalues = adjustByteArray(bytevalues, bytevalues.length + length);
            byteids[arrayIndex] = field;
            byteids[arrayIndex + 1] = numValues;
            byteids[arrayIndex + 2] = cleanAttributes(field, attributes);
            byteids[arrayIndex + 3] = length;
            System.arraycopy(value, offset, bytevalues, valueIndex, length);
            byteids[0]++;
            modified = true;
        }
    }

    /**
	 * Removes all attributes not supported.
	 * @param field
	 * @param attributes
	 * @return int
	 */
    protected int cleanAttributes(int field, int attributes) {
        if (owner == null) return attributes;
        int result = PIMItem.ATTR_NONE;
        if (attributes < 0) return result;
        int[] supported = owner.getSupportedAttributes(field);
        for (int i = 0; i < supported.length; i++) {
            if ((supported[i] & attributes) == supported[i]) result |= supported[i];
        }
        return result;
    }

    /**
	 * @see javax.microedition.pim.PIMItem#setBinary(int, int, int, byte[], int, int)
	 */
    public void setBinary(int field, int index, int attributes, byte[] value, int offset, int length) {
        synchronized (i2) {
            if (value == null) throw new NullPointerException();
            int arrayLength = value.length;
            if (offset >= arrayLength || offset < 0) throw new IllegalArgumentException("Invalid offset");
            if (length < 0 || offset + length > arrayLength) throw new IllegalArgumentException("Invalid length");
            checkForExceptions(field, Contact.BINARY, true, index, false);
            loadPIMStruct();
            int arrayIndex = 0, valueIndex = 0;
            if (indexInByteArray(field, index)) {
                arrayIndex = i2.int1;
                valueIndex = i2.int2;
            }
            int size = byteids[arrayIndex + 3];
            byteids[arrayIndex + 2] = cleanAttributes(field, attributes);
            byteids[arrayIndex + 3] = length;
            if (length != size) {
                if (length < size) {
                    int diff = size - length;
                    for (int i = valueIndex + length; i < bytevalues.length - valueIndex - diff; i++) {
                        bytevalues[i] = bytevalues[i + diff];
                    }
                }
                if (length > size) {
                    int diff = length - size;
                    int sizes = 0, count = 0;
                    for (int i = 1; count < byteids[0]; i++) {
                        i += 3;
                        sizes += byteids[i];
                        count++;
                    }
                    if (bytevalues.length - sizes < 0) bytevalues = adjustByteArray(bytevalues, bytevalues.length + diff);
                    int bytvlen = bytevalues.length;
                    for (int i = 0; i < bytvlen - valueIndex - length; i++) {
                        bytevalues[sizes - 1 - i] = bytevalues[sizes - 1 - diff - i];
                    }
                    System.arraycopy(value, offset, bytevalues, valueIndex, length);
                    modified = true;
                }
            } else {
                System.arraycopy(value, offset, bytevalues, valueIndex, length);
                modified = true;
            }
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#getDate(int, int)
	 */
    public long getDate(int field, int index) {
        synchronized (i2) {
            checkForExceptions(field, Contact.DATE, true, index, true);
            if (!valuesFromOS) return getLongValue(field, index); else {
                checkListClosed();
                return getDateN(owner.listType, owner.handle, rechandle, field, index);
            }
        }
    }

    /**
	 * Answers a long value from the pim structure.
	 * @param field
	 * @param index
	 * @return long
	 */
    protected long getLongValue(int field, int index) {
        long result = -1;
        int i = indexInLongArray(field, index);
        if (i != -1) result = longvalues[i + 3]; else {
            throw new RuntimeException("An error occured.");
        }
        return result;
    }

    /**
	 * @see javax.microedition.pim.PIMItem#addDate(int, int, long)
	 */
    public void addDate(int field, int attributes, long value) {
        synchronized (i2) {
            addLongValue(field, attributes, value, Contact.DATE);
        }
    }

    /**
	 * Adds a long value to the pim structure.
	 * @param field
	 * @param attributes
	 * @param value
	 * @param type
	 */
    protected void addLongValue(int field, int attributes, long value, int type) {
        int numvalues = checkForExceptions(field, type, false, 0, false);
        int listType = getListType();
        if (!isStructLoading && rechandle != -1 && ((listType == PIM.CONTACT_LIST && field == Contact.REVISION) || (listType == PIM.TODO_LIST && field == ToDo.REVISION) || (listType == PIM.EVENT_LIST && field == Event.REVISION))) throw new IllegalArgumentException("Cannot add a revision date to this item.");
        checkIntValue(field, (int) value);
        loadPIMStruct();
        int index = (int) longvalues[0] * 4 + 1;
        int length = longvalues.length;
        if (index + 3 >= length) longvalues = adjustLongArray(longvalues, length + 8);
        longvalues[index] = field;
        longvalues[index + 1] = numvalues;
        longvalues[index + 2] = cleanAttributes(field, attributes);
        longvalues[index + 3] = value;
        longvalues[0]++;
        modified = true;
    }

    /**
	 * @see javax.microedition.pim.PIMItem#setDate(int, int, int, long)
	 */
    public void setDate(int field, int index, int attributes, long value) {
        synchronized (i2) {
            setLongValue(field, index, attributes, value, Contact.DATE, true);
        }
    }

    /**
	 * Sets a long value to the pim structure.
	 * @param field
	 * @param index
	 * @param attributes
	 * @param value
	 * @param type
	 * @param checkExceptions
	 */
    protected void setLongValue(int field, int index, int attributes, long value, int type, boolean checkExceptions) {
        if (checkExceptions) {
            checkForExceptions(field, type, true, index, false);
            int listType = getListType();
            if (!isStructLoading && rechandle != -1 && ((listType == PIM.CONTACT_LIST && field == Contact.REVISION) || (listType == PIM.TODO_LIST && field == ToDo.REVISION) || (listType == PIM.EVENT_LIST && field == Event.REVISION))) throw new IllegalArgumentException("Cannot set the item's revision date.");
            checkIntValue(field, (int) value);
        }
        loadPIMStruct();
        int ind = indexInLongArray(field, index);
        longvalues[ind + 2] = cleanAttributes(field, attributes);
        longvalues[ind + 3] = value;
        modified = true;
    }

    /**
	 * @see javax.microedition.pim.PIMItem#getInt(int, int)
	 */
    public int getInt(int field, int index) {
        synchronized (i2) {
            checkForExceptions(field, Contact.INT, true, index, true);
            if (!valuesFromOS) return (int) getLongValue(field, index); else {
                checkListClosed();
                return getIntN(owner.listType, owner.handle, rechandle, field, index);
            }
        }
    }

    /**
	 * Checks the value is a valid int value.
	 * @param field
	 * @param value
	 */
    protected void checkIntValue(int field, int value) {
        if (owner != null) {
            if ((owner.listType == PIM.CONTACT_LIST && field == Contact.CLASS) || (owner.listType == PIM.EVENT_LIST && field == Event.CLASS) || (owner.listType == PIM.TODO_LIST && field == ToDo.CLASS)) {
                if (value < Contact.CLASS_CONFIDENTIAL || value > Contact.CLASS_PUBLIC) throw new IllegalArgumentException("The value can only be CLASS_PUBLIC, CLASS_PRIVATE, CLASS_CONFIDENTIAL");
            }
            if (owner.listType == PIM.TODO_LIST && field == ToDo.PRIORITY) {
                if (value < 0 || value > 9) throw new IllegalArgumentException("The value for PRIORITY can only be an int between 0 and 9");
            }
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#addInt(int, int, int)
	 */
    public void addInt(int field, int attributes, int value) {
        synchronized (i2) {
            addLongValue(field, attributes, value, Contact.INT);
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#setInt(int, int, int, int)
	 */
    public void setInt(int field, int index, int attributes, int value) {
        synchronized (i2) {
            setLongValue(field, index, attributes, value, Contact.INT, true);
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#getString(int, int)
	 */
    public String getString(int field, int index) {
        synchronized (i2) {
            checkForExceptions(field, Contact.STRING, true, index, true);
            if (!valuesFromOS) return getStringFromStruct(field, index); else {
                checkListClosed();
                return getStringN(owner.listType, owner.handle, this.rechandle, field, index);
            }
        }
    }

    /**
	 * Gets a string from the pim structure.
	 * @param field
	 * @param index
	 * @return String
	 */
    protected String getStringFromStruct(int field, int index) {
        String result = null;
        if (indexInStringArray(field, index)) result = stringvalues[i2.int2]; else {
            throw new RuntimeException("An error occured.");
        }
        return result;
    }

    /**
	 * @see javax.microedition.pim.PIMItem#addString(int, int, String)
	 */
    public void addString(int field, int attributes, String value) {
        synchronized (i2) {
            if (value == null) throw new NullPointerException();
            int numValues = checkForExceptions(field, Contact.STRING, false, 0, false);
            int listType = getListType();
            if (!isStructLoading && rechandle != -1 && ((listType == PIM.CONTACT_LIST && field == Contact.UID) || (listType == PIM.TODO_LIST && field == ToDo.UID) || (listType == PIM.EVENT_LIST && field == Event.UID))) throw new IllegalArgumentException("Cannot add a UID to this item. The field is readonly.");
            loadPIMStruct();
            if (listType == PIM.CONTACT_LIST && field == Contact.PHOTO_URL) {
                int count = countValues(Contact.PHOTO);
                for (int i = 0; i < count; i++) removeValue(Contact.PHOTO, i);
            }
            if (listType == PIM.CONTACT_LIST && field == Contact.PUBLIC_KEY_STRING) {
                int count = countValues(Contact.PUBLIC_KEY);
                for (int i = 0; i < count; i++) removeValue(Contact.PUBLIC_KEY, i);
            }
            int arrayIndex = 0, valueIndex = 0;
            if (!indexInStringArray(-1, -1)) {
                arrayIndex = i2.int1;
                valueIndex = i2.int2;
            }
            int length = stringids.length;
            if (arrayIndex + 3 >= length) stringids = adjustIntArray(stringids, length + 9);
            length = stringvalues.length;
            if (valueIndex + 1 >= length) stringvalues = adjustStringArray(stringvalues, length + 2);
            stringids[arrayIndex] = field;
            stringids[arrayIndex + 1] = numValues;
            stringids[arrayIndex + 2] = cleanAttributes(field, attributes);
            stringvalues[valueIndex] = value;
            stringids[0]++;
            modified = true;
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#setString(int, int, int, String)
	 */
    public void setString(int field, int index, int attributes, String value) {
        synchronized (i2) {
            if (value == null) throw new NullPointerException();
            checkForExceptions(field, Contact.STRING, true, index, false);
            int listType = getListType();
            if (!isStructLoading && rechandle != -1 && ((listType == PIM.CONTACT_LIST && field == Contact.UID) || (listType == PIM.TODO_LIST && field == ToDo.UID) || (listType == PIM.EVENT_LIST && field == Event.UID))) throw new IllegalArgumentException("Cannot set a UID to this item. The field is readonly");
            loadPIMStruct();
            int arrayIndex = 0, valueIndex = 0;
            if (indexInStringArray(field, index)) {
                arrayIndex = i2.int1;
                valueIndex = i2.int2;
            }
            stringids[arrayIndex + 2] = cleanAttributes(field, attributes);
            stringvalues[valueIndex] = value;
            modified = true;
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#getBoolean(int, int)
	 */
    public boolean getBoolean(int field, int index) {
        synchronized (i2) {
            checkForExceptions(field, Contact.BOOLEAN, true, index, true);
            if (!valuesFromOS) return ((getLongValue(field, index) == 1) ? true : false); else {
                checkListClosed();
                return getBooleanN(owner.listType, owner.handle, rechandle, field, index);
            }
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#addBoolean(int, int, boolean)
	 */
    public void addBoolean(int field, int attributes, boolean value) {
        synchronized (i2) {
            addLongValue(field, attributes, (value ? 1 : 0), Contact.BOOLEAN);
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#setBoolean(int, int, int, boolean)
	 */
    public void setBoolean(int field, int index, int attributes, boolean value) {
        synchronized (i2) {
            setLongValue(field, index, attributes, (value ? 1 : 0), Contact.BOOLEAN, true);
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#getStringArray(int, int)
	 */
    public String[] getStringArray(int field, int index) {
        synchronized (i2) {
            checkForExceptions(field, PIMItem.STRING_ARRAY, true, index, true);
            if (!valuesFromOS) return getStringArrayFromStruct(field, index); else {
                checkListClosed();
                return getStringArrayN(owner.listType, owner.handle, rechandle, field, index);
            }
        }
    }

    /**
	 * Answers one of the values for the given STRING_ARRAY field.
	 * @param field
	 * @param index
	 * @return String[]
	 */
    protected String[] getStringArrayFromStruct(int field, int index) {
        String[] result = null;
        if (indexInStringArray(field, index)) {
            int size = PIMListImpl.getStringArraySizeS(getListType(), field);
            result = new String[size];
            System.arraycopy(stringvalues, i2.int2, result, 0, size);
        }
        return result;
    }

    /**
	 * Checks the value is a valid string array value:
	 * correct length and at least one non-null element.
	 * @param field
	 * @param value
	 */
    protected void checkStringArrayValue(int field, String[] value) {
        if (value == null) throw new NullPointerException();
        boolean valid = false;
        for (int i = 0; i < value.length; i++) {
            if (value[i] != null) {
                valid = true;
                break;
            }
        }
        if (!valid) throw new IllegalArgumentException("All strings in the array are null");
        if (owner != null) {
            if (value.length != owner.stringArraySize(field)) throw new IllegalArgumentException("Invalid array length");
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#addStringArray(int, int, String[])
	 */
    public void addStringArray(int field, int attributes, String[] value) {
        synchronized (i2) {
            int numValues = checkForExceptions(field, Contact.STRING_ARRAY, false, 0, false);
            checkStringArrayValue(field, value);
            loadPIMStruct();
            int arrayIndex = 0, valueIndex = 0;
            if (!indexInStringArray(-1, -1)) {
                arrayIndex = i2.int1;
                valueIndex = i2.int2;
            }
            int length = stringids.length;
            if (arrayIndex + 3 >= length) stringids = adjustIntArray(stringids, length + 9);
            if (valueIndex + value.length >= stringvalues.length) stringvalues = adjustStringArray(stringvalues, stringvalues.length + 15);
            stringids[arrayIndex] = field;
            stringids[arrayIndex + 1] = numValues;
            stringids[arrayIndex + 2] = cleanAttributes(field, attributes);
            System.arraycopy(value, 0, stringvalues, valueIndex, value.length);
            stringids[0]++;
            modified = true;
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#setStringArray(int, int, int, String[])
	 */
    public void setStringArray(int field, int index, int attributes, String[] value) {
        synchronized (i2) {
            checkForExceptions(field, Contact.STRING_ARRAY, true, index, false);
            checkStringArrayValue(field, value);
            loadPIMStruct();
            int arrayIndex = 0, valueIndex = 0;
            if (indexInStringArray(field, index)) {
                arrayIndex = i2.int1;
                valueIndex = i2.int2;
            }
            stringids[arrayIndex + 2] = cleanAttributes(field, attributes);
            System.arraycopy(value, 0, stringvalues, valueIndex, value.length);
            modified = true;
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#countValues(int)
	 */
    public int countValues(int field) {
        synchronized (i2) {
            checkItemDeleted();
            checkValidField(field);
            if (!valuesFromOS) return countValuesInArrays(field); else {
                checkListClosed();
                return countValuesN(owner.listType, owner.handle, rechandle, field);
            }
        }
    }

    /**
	 * Answers the number of values set for the given field.
	 * @param field
	 * @return int
	 */
    protected int countValuesInArrays(int field) {
        int numValues = 0;
        int i = 1, count = 0;
        switch(PIMListImpl.getFieldDataTypeS(getListType(), field)) {
            case PIMItem.STRING:
            case PIMItem.STRING_ARRAY:
                while (count < stringids[0]) {
                    if (field == stringids[i]) numValues++;
                    i += 3;
                    count++;
                }
                break;
            case PIMItem.BINARY:
                while (count < byteids[0]) {
                    if (field == byteids[i]) numValues++;
                    i += 4;
                    count++;
                }
                break;
            case PIMItem.DATE:
            case PIMItem.INT:
            case PIMItem.BOOLEAN:
                while (count < longvalues[0]) {
                    if (field == longvalues[i]) numValues++;
                    i += 4;
                    count++;
                }
                break;
        }
        return numValues;
    }

    /**
	 * Answers the field data type of the given field.
	 * @param field
	 * @return int
	 */
    protected int getFieldDataType(int field) {
        if (owner == null) return PIMListImpl.getFieldDataTypeS(getListType(), field); else return owner.getFieldDataType(field);
    }

    /**
	 * @see javax.microedition.pim.PIMItem#removeValue(int, int)
	 */
    public void removeValue(int field, int index) {
        synchronized (i2) {
            int i;
            checkForExceptions(field, -1, true, index, false);
            int listType = getListType();
            if (!isStructLoading && rechandle != -1 && ((listType == PIM.CONTACT_LIST && field == Contact.UID) || (listType == PIM.TODO_LIST && field == ToDo.UID) || (listType == PIM.EVENT_LIST && field == Event.UID))) throw new IllegalArgumentException("Cannot remove the UID to this item.");
            if (!isStructLoading && rechandle != -1 && ((listType == PIM.CONTACT_LIST && field == Contact.REVISION) || (listType == PIM.TODO_LIST && field == ToDo.REVISION) || (listType == PIM.EVENT_LIST && field == Event.REVISION))) throw new IllegalArgumentException("Cannot remove the revision date to this item.");
            loadPIMStruct();
            switch(PIMListImpl.getFieldDataTypeS(listType, field)) {
                case PIMItem.STRING:
                case PIMItem.STRING_ARRAY:
                    int arrayIndex = 0, valueIndex = 0;
                    if (indexInStringArray(field, index)) {
                        arrayIndex = i2.int1;
                        valueIndex = i2.int2;
                        int numst = 1;
                        if (PIMListImpl.getFieldDataTypeS(getListType(), stringids[arrayIndex]) == PIMItem.STRING_ARRAY) numst = PIMListImpl.getStringArraySizeS(getListType(), stringids[arrayIndex]);
                        for (int j = valueIndex; j < stringvalues.length - numst; j++) {
                            stringvalues[j] = stringvalues[j + numst];
                        }
                        for (int j = arrayIndex; j < stringids.length - 3; j++) {
                            stringids[j] = stringids[j + 3];
                        }
                        stringids[0]--;
                        int numv = countValues(field);
                        for (int j = index + 1; j <= numv; j++) {
                            int inde = indexInStringArrayI(field, j);
                            stringids[inde + 1]--;
                        }
                        modified = true;
                        checkToCompactStringArray();
                    }
                    break;
                case PIMItem.BINARY:
                    arrayIndex = 0;
                    valueIndex = 0;
                    if (indexInByteArray(field, index)) {
                        arrayIndex = i2.int1;
                        valueIndex = i2.int2;
                        for (int j = valueIndex; j < bytevalues.length - byteids[arrayIndex + 3]; j++) {
                            bytevalues[j] = bytevalues[j + byteids[arrayIndex + 3]];
                        }
                        for (int j = arrayIndex; j < byteids.length - 4; j++) {
                            byteids[j] = byteids[j + 4];
                        }
                        byteids[0]--;
                        int numv = countValues(field);
                        for (int j = index + 1; j < numv; j++) {
                            int inde = indexInByteArrayI(field, j);
                            byteids[inde + 1]--;
                        }
                        modified = true;
                        checkToCompactByteArray();
                    }
                    break;
                case PIMItem.DATE:
                case PIMItem.INT:
                case PIMItem.BOOLEAN:
                    i = indexInLongArray(field, index);
                    if (i != -1) {
                        for (int j = i; j < longvalues.length - 4; j++) {
                            longvalues[j] = longvalues[j + 4];
                        }
                        longvalues[0]--;
                        int numv = countValues(field);
                        for (int j = index + 1; j < numv; j++) {
                            int inde = indexInLongArray(field, j);
                            longvalues[inde + 1]--;
                        }
                        modified = true;
                        checkToCompactLongArray();
                    }
                    break;
            }
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#getAttributes(int, int)
	 */
    public int getAttributes(int field, int index) {
        synchronized (i2) {
            checkForExceptions(field, -1, true, index, true);
            if (!valuesFromOS) return getAttributesFromStruct(field, index); else {
                checkListClosed();
                return getAttributesN(owner.listType, owner.handle, rechandle, field, index);
            }
        }
    }

    /**
	 * Answers the field attributes from the pim structure.
	 * @param field
	 * @param index
	 * @return int
	 */
    protected int getAttributesFromStruct(int field, int index) {
        int attributes = -1;
        int i;
        switch(PIMListImpl.getFieldDataTypeS(getListType(), field)) {
            case PIMItem.STRING:
            case PIMItem.STRING_ARRAY:
                i = indexInStringArrayI(field, index);
                if (i != -1) attributes = stringids[i + 2];
                break;
            case PIMItem.BINARY:
                i = indexInByteArrayI(field, index);
                if (i != -1) attributes = byteids[i + 2];
                break;
            case PIMItem.DATE:
            case PIMItem.INT:
            case PIMItem.BOOLEAN:
                i = indexInLongArray(field, index);
                if (i != -1) attributes = (int) longvalues[i + 2];
                break;
        }
        return attributes;
    }

    /**
	 * @see javax.microedition.pim.PIMItem#addToCategory(String)
	 */
    public void addToCategory(String category) throws PIMException {
        synchronized (i2) {
            checkItemDeleted();
            boolean catexist = false;
            if (category == null) throw new NullPointerException();
            int maxCategories = maxCategories();
            if (maxCategories == 0) throw new PIMException("Categories are not supported.", PIMException.FEATURE_NOT_SUPPORTED);
            loadPIMStruct();
            int catlen = categories.length;
            int numCat = Integer.parseInt(categories[0]);
            for (int i = 1; i <= numCat; i++) {
                if (category.equals(categories[i])) {
                    return;
                }
            }
            if (owner != null) {
                String[] cats = owner.getCategories();
                for (int i = 0; i < cats.length; i++) {
                    if (category.equals(cats[i])) {
                        catexist = true;
                        break;
                    }
                }
                if (!catexist) {
                    StringBuffer message = new StringBuffer(80);
                    message.append("The category ");
                    message.append(category);
                    message.append(" does not exist in the list.");
                    throw new PIMException(message.toString());
                }
            }
            if (maxCategories != -1 && numCat >= maxCategories) throw new PIMException("Maximum number of categories exceeded.", PIMException.MAX_CATEGORIES_EXCEEDED);
            if (catlen <= (numCat + 1)) categories = adjustStringArray(categories, catlen + 2);
            categories[numCat + 1] = category;
            categories[0] = Integer.toString(++numCat);
            modified = true;
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#removeFromCategory(String)
	 */
    public void removeFromCategory(String category) {
        synchronized (i2) {
            checkItemDeleted();
            boolean catexist = false;
            if (category == null) throw new NullPointerException();
            loadPIMStruct();
            int j;
            for (j = 0; j < Integer.parseInt(categories[0]); j++) {
                if (category.equals(categories[j + 1])) {
                    catexist = true;
                    break;
                }
            }
            if (catexist) {
                int numCat = Integer.parseInt(categories[0]);
                for (int k = j; k < categories.length - 1; k++) {
                    categories[k] = categories[k + 1];
                }
                categories[0] = Integer.toString(--numCat);
                modified = true;
                if (numCat <= categories.length / 2) categories = adjustStringArray(categories, numCat + 2);
            }
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#getCategories()
	 */
    public String[] getCategories() {
        synchronized (i2) {
            checkItemDeleted();
            if (maxCategories() == 0) return new String[0];
            if (!valuesFromOS) {
                int numCat = Integer.parseInt(categories[0]);
                String[] cats = new String[numCat];
                System.arraycopy(categories, 1, cats, 0, numCat);
                return cats;
            } else {
                checkListClosed();
                return getCategoriesN(owner.listType, owner.handle, rechandle);
            }
        }
    }

    /**
	 * @see javax.microedition.pim.PIMItem#maxCategories()
	 */
    public int maxCategories() {
        synchronized (i2) {
            if (owner == null) return -1;
            checkItemDeleted();
            return maxCategoriesN(owner.listType, owner.handle, rechandle);
        }
    }

    protected int indexInLongArray(int field, int index) {
        int i = 1, count = 0;
        boolean found = false;
        while (count < longvalues[0]) {
            if (field == longvalues[i] && index == longvalues[i + 1]) {
                found = true;
                break;
            }
            i += 4;
            count++;
        }
        if (found || (field == -1 && index == -1)) return i; else throw new IndexOutOfBoundsException();
    }

    protected boolean indexInByteArray(int field, int index) {
        int i = 1, count = 0, inbytevalue = 0;
        boolean found = false;
        while (count < byteids[0]) {
            if (field == byteids[i] && index == byteids[i + 1]) {
                found = true;
                break;
            }
            if (i - 1 > 0) inbytevalue += byteids[i - 1];
            i += 4;
            count++;
        }
        i2.int1 = i;
        i2.int2 = inbytevalue;
        if (found || (field == -1 && index == -1)) return found; else throw new IndexOutOfBoundsException();
    }

    protected int indexInByteArrayI(int field, int index) {
        int i = 1, count = 0;
        boolean found = false;
        while (count < byteids[0]) {
            if (field == byteids[i] && index == byteids[i + 1]) {
                found = true;
                break;
            }
            i += 4;
            count++;
        }
        if (found || (field == -1 && index == -1)) return i; else throw new IndexOutOfBoundsException();
    }

    protected boolean indexInStringArray(int field, int index) {
        int i = 1, add = 1, indexvalue = 0, count = 0;
        boolean found = false;
        int listType = getListType();
        while (count < stringids[0]) {
            if (field == stringids[i] && index == stringids[i + 1]) {
                found = true;
                break;
            }
            if (PIMListImpl.getFieldDataTypeS(listType, stringids[i]) == PIMItem.STRING_ARRAY) add = PIMListImpl.getStringArraySizeS(listType, stringids[i]);
            i += 3;
            indexvalue += add;
            add = 1;
            count++;
        }
        i2.int1 = i;
        i2.int2 = indexvalue;
        if (found || (field == -1 && index == -1)) return found; else throw new IndexOutOfBoundsException();
    }

    protected int indexInStringArrayI(int field, int index) {
        int i = 1, count = 0;
        boolean found = false;
        while (count < stringids[0]) {
            if (field == stringids[i] && index == stringids[i + 1]) {
                found = true;
                break;
            }
            i += 3;
            count++;
        }
        if (found || (field == -1 && index == -1)) return i; else throw new IndexOutOfBoundsException();
    }

    /**
	 * Retrieves an interger from the special format string
	 * returned by indexInStringArray.
	 * @param string
	 * @param index
	 * @return int
	 */
    protected int getIntInString(String string, int index) {
        StringBuffer buffer = new StringBuffer();
        int numcom = 0;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == ';') {
                if (numcom == index) break;
                numcom++;
                buffer.delete(0, buffer.length());
            } else buffer.append(string.charAt(i));
        }
        return Integer.parseInt(buffer.toString());
    }

    /**
	 * Adjusts the size of an int array.
	 * @param array
	 * @param newSize
	 * @return int[]
	 */
    protected int[] adjustIntArray(int[] array, int newSize) {
        int[] newArray = new int[newSize];
        System.arraycopy(array, 0, newArray, 0, Math.min(array.length, newSize));
        return newArray;
    }

    /**
	 * Adjusts the size of a String array.
	 * @param array
	 * @param newSize
	 * @return String[]
	 */
    protected String[] adjustStringArray(String[] array, int newSize) {
        String[] newArray = new String[newSize];
        System.arraycopy(array, 0, newArray, 0, Math.min(array.length, newSize));
        return newArray;
    }

    /**
	 * Adjusts the size of a long array.
	 * @param array
	 * @param newSize
	 * @return long[]
	 */
    protected long[] adjustLongArray(long[] array, int newSize) {
        long[] newArray = new long[newSize];
        System.arraycopy(array, 0, newArray, 0, Math.min(array.length, newSize));
        return newArray;
    }

    /**
	 * Adjusts the size of a byte array.
	 * @param array
	 * @param newSize
	 * @return byte[]
	 */
    protected byte[] adjustByteArray(byte[] array, int newSize) {
        byte[] newArray = new byte[newSize];
        System.arraycopy(array, 0, newArray, 0, Math.min(array.length, newSize));
        return newArray;
    }

    protected void checkToCompactStringArray() {
        int size = 0, count = 0, add = 1;
        if (stringids.length > 2 * (stringids[0] * 3 + 1) && stringids.length > 20) stringids = adjustIntArray(stringids, stringids.length / 2);
        for (int i = 1; count < stringids[0]; count++) {
            if (PIMListImpl.getFieldDataTypeS(getListType(), stringids[i]) == PIMItem.STRING_ARRAY) add = PIMListImpl.getStringArraySizeS(getListType(), stringids[i]);
            size += add;
            add = 1;
            i += 3;
        }
        if (stringvalues.length > size * 2 && stringvalues.length > 20) stringvalues = adjustStringArray(stringvalues, size);
    }

    protected void checkToCompactByteArray() {
        int siz = 0, count = 0;
        if (byteids.length > 2 * (byteids[0] * 4 + 1) && byteids.length > 20) byteids = adjustIntArray(byteids, byteids.length / 2);
        for (int i = 1; count < byteids[0]; count++) {
            siz += byteids[i + 3];
            i += 4;
        }
        if (bytevalues.length > siz * 2 && bytevalues.length > 20) bytevalues = adjustByteArray(bytevalues, siz);
    }

    protected void checkToCompactLongArray() {
        if (longvalues.length > 2 * (longvalues[0] * 4 + 1) && longvalues.length > 20) longvalues = adjustLongArray(longvalues, longvalues.length / 2);
    }

    /**
	 * Checks for exceptions.
	 * @param field
	 * @param readNotWrite : used when we want to load the PIM Structure
	 * 						  because we want to read from the OS but write 
	 * 						  to the structure and we need indexing!
	 */
    protected int checkForExceptions(int field, int datatype, boolean indexOutNotFieldFull, int index, boolean readNotWrite) {
        if (owner != null) {
            checkItemDeleted();
            checkValidField(field);
            if (datatype != -1) if (owner.getFieldDataType(field) != datatype) throw new IllegalArgumentException("Field data type should be " + datatype + ".");
        }
        int numvalues;
        if (isStructLoading && !readNotWrite) numvalues = countValuesInArrays(field); else numvalues = countValues(field);
        if (indexOutNotFieldFull) {
            if (index >= numvalues || index < 0) throw new IndexOutOfBoundsException("There is no value currently set at this index");
        } else if (owner != null) {
            int maxValues = owner.maxValues(field);
            if (numvalues >= maxValues && maxValues != -1) throw new FieldFullException("", field);
        }
        return numvalues;
    }

    /**
	 * Checks if the given field is valid (one of the standard or extended fields).
	 * @param field
	 */
    protected void checkValidField(int field) {
        if (owner == null) return;
        if (!owner.isSupportedField(field)) {
            int listType = getListType();
            if (listType == PIM.CONTACT_LIST && (field < Contact.ADDR || field > Contact.URL)) throw new IllegalArgumentException();
            if (listType == PIM.EVENT_LIST && (field < Event.ALARM || field > Event.UID)) throw new IllegalArgumentException();
            if (listType == PIM.TODO_LIST && (field < ToDo.CLASS || field > ToDo.UID)) throw new IllegalArgumentException();
            throw new UnsupportedFieldException("", field);
        }
    }

    /**
	 * Checks if the list is closed then throws a RuntimeException.
	 */
    protected void checkListClosed() {
        try {
            owner.checkListClosed();
        } catch (PIMException e) {
            throw new RuntimeException("List closed.");
        }
    }

    /**
	 * Checks if this item has been deleted then throws a RuntimeException.
	 */
    protected void checkItemDeleted() {
        boolean throwex = false;
        if (isDeleted) throwex = true; else if (isLoadedFromOS) {
            checkListClosed();
            throwex = isRecordDeletedN(owner.listType, owner.handle, rechandle);
        }
        if (throwex) throw new RuntimeException("The record has been deleted");
    }

    /**
	 * Loads data from the OS into the pim structure arrays.
	 */
    protected void loadPIMStruct() {
        if (valuesFromOS && !isStructLoading) {
            isStructLoading = true;
            int datatype = 0;
            int[] f = getFields();
            String[] cats = getCategories();
            initArrays();
            for (int i = 0; i < cats.length; i++) {
                try {
                    addToCategory(cats[i]);
                } catch (PIMException e) {
                }
            }
            for (int i = 0; i < f.length; i++) {
                datatype = owner.getFieldDataType(f[i]);
                for (int j = 0; j < countValues(f[i]); j++) {
                    if (datatype == Contact.STRING) addString(f[i], getAttributes(f[i], j), getString(f[i], j)); else if (datatype == Contact.STRING_ARRAY) addStringArray(f[i], getAttributes(f[i], j), getStringArray(f[i], j)); else if (datatype == Contact.INT) addInt(f[i], getAttributes(f[i], j), getInt(f[i], j)); else if (datatype == Contact.BOOLEAN) addBoolean(f[i], getAttributes(f[i], j), getBoolean(f[i], j)); else if (datatype == Contact.DATE) addDate(f[i], getAttributes(f[i], j), getDate(f[i], j)); else if (datatype == Contact.BINARY) {
                        byte[] bin = getBinary(f[i], j);
                        addBinary(f[i], getAttributes(f[i], j), bin, 0, bin.length);
                    }
                }
            }
            isStructLoading = false;
            valuesFromOS = false;
            isLoadedFromOS = true;
            modified = false;
        }
    }

    /**
	 * Initializes the pim structure arrays.
	 */
    private void initArrays() {
        stringids = new int[20];
        stringvalues = new String[5];
        longvalues = new long[8];
        byteids = new int[8];
        bytevalues = new byte[20];
        categories = new String[2];
        categories[0] = "0";
    }

    /**
	 * Clears the pim structure arrays.
	 */
    private void clearArrays() {
        stringids = null;
        stringvalues = null;
        longvalues = null;
        byteids = null;
        bytevalues = null;
        categories = null;
    }

    /**
	 * Sets this item as deleted.
	 */
    void delete() {
        isDeleted = true;
        owner = null;
    }

    /**
	 * Answers the listType.
	 */
    private int getListType() {
        if (owner != null) return owner.listType;
        if (this instanceof Contact) return PIM.CONTACT_LIST;
        if (this instanceof ToDo) return PIM.TODO_LIST;
        return PIM.EVENT_LIST;
    }

    /**
	 * Answers the extended fields
	 * coming from a stream.
	 * @return Vector
	 */
    Vector getVExtendedFields() {
        return VExtendedFields;
    }

    /**
	 * Stores a new extended field and its data.
	 * @param property
	 */
    void addVExtendedField(Property property) {
        if (VExtendedFields == null) VExtendedFields = new Vector();
        VExtendedFields.addElement(property);
    }
}
