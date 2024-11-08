package de.offis.semanticmm4u.global;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import component_interfaces.semanticmm4u.realization.IMetadataEntry;
import component_interfaces.semanticmm4u.realization.IPropertyList;
import de.offis.semanticmm4u.failures.MM4UConfigurationException;

/**
 * 
 * @testcase test.de.offis.semanticmm4u.global.TestPropertyList
 */
public class PropertyList implements IPropertyList {

    protected Hashtable hashtable;

    /**
	 * Creates an empty property list.
	 */
    public PropertyList() {
        this.hashtable = new Hashtable();
    }

    /**
	 * Insert an element into this property list. An RuntimeException is raised
	 * if the key or value is null. If the overwrite is false and the key
	 * exists, an RuntimeExeption is raised, too.
	 * 
	 * @param key
	 * @param value
	 * @param overwrite
	 *            true if a existing object should be overwritten
	 */
    protected void insert(String key, Object value, boolean overwrite) {
        if (key == null) throw new RuntimeException("Error in the internal method insert() of class " + Utilities.getClassName(this) + ": adding a property with a null key  is not allowed.");
        if (value == null) throw new RuntimeException("Error in the internal method insert() of class " + Utilities.getClassName(this) + ": adding a property with a null value is not allowed.");
        if (!overwrite && this.hashtable.containsKey(key)) throw new RuntimeException("Error in the internal method insert() of class " + Utilities.getClassName(this) + ": a property with key '" + key + "' is already defined in this property list.");
        IMetadataEntry metadtaEntry = new MetadataEntry(key);
        metadtaEntry.setValue(value);
        Vector entries = (Vector) this.hashtable.get(key);
        if (entries == null) entries = new Vector();
        entries.add(metadtaEntry);
        this.hashtable.put(key, entries);
    }

    /**
	 * Set a new value to the key. A value is only set if the key already
	 * exists.
	 * 
	 * @param key
	 * @param value
	 * @return true if the key exists and the value was set.
	 */
    protected boolean setValue(String key, Object value) {
        if (key == null) throw new RuntimeException("Error in the internal method setValue() of class " + Utilities.getClassName(this) + ": adding a property with a null key is not allowed.");
        if (value == null) throw new RuntimeException("Error in the internal method setValue() of class " + Utilities.getClassName(this) + ": adding a property with a null value is not allowed.");
        boolean containsKey = this.hashtable.containsKey(key);
        if (containsKey) this.hashtable.put(key, value);
        return containsKey;
    }

    public void add(String key, String stringValue) {
        if (Utilities.stringIsNullOrEmpty(key)) throw new RuntimeException("Error in method add() of class " + Utilities.getClassName(this) + ": adding a property with an empty or null key  is not allowed.");
        this.insert(key, stringValue, false);
    }

    public void add(String key, boolean booleanValue) {
        this.insert(key, Boolean.toString(booleanValue), false);
    }

    public void add(String key, int intValue) {
        this.insert(key, Integer.toString(intValue), false);
    }

    public void add(String key, long longValue) {
        this.insert(key, Long.toString(longValue), false);
    }

    public void add(String key, float floatValue) {
        this.insert(key, Float.toString(floatValue), false);
    }

    public void add(String key, double doubleValue) {
        this.insert(key, Double.toString(doubleValue), false);
    }

    public void put(String key, String stringValue) {
        this.insert(key, stringValue, true);
    }

    public void put(String key, boolean booleanValue) {
        this.insert(key, Boolean.toString(booleanValue), true);
    }

    public void put(String key, int intValue) {
        this.insert(key, Integer.toString(intValue), true);
    }

    public void put(String key, long longValue) {
        this.insert(key, Long.toString(longValue), true);
    }

    public void put(String key, float floatValue) {
        this.insert(key, Float.toString(floatValue), true);
    }

    public void put(String key, double doubleValue) {
        this.insert(key, Double.toString(doubleValue), true);
    }

    public void put(String key, Object myObjectValue) {
        this.insert(key, myObjectValue, true);
    }

    public boolean addIfNotNull(String key, String value) {
        if (key != null && value != null) {
            this.add(key, value);
            return true;
        }
        return false;
    }

    public boolean addIfNotUndefined(String key, int integerValue) {
        if (integerValue != Constants.UNDEFINED_INTEGER) {
            this.add(key, integerValue);
            return true;
        }
        return false;
    }

    public void addAll(IPropertyList myPropertyList) {
        Enumeration keys = myPropertyList.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            this.insert(key, myPropertyList.getObjectValue(key), false);
        }
    }

    public void putAll(IPropertyList myPropertyList) {
        Enumeration keys = myPropertyList.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            this.insert(key, myPropertyList.getObjectValue(key), true);
        }
    }

    public void addAllButDoNotOverride(IPropertyList myPropertyList) {
        Enumeration keys = myPropertyList.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (!this.contains(key)) this.insert(key, myPropertyList.getObjectValue(key), false);
        }
    }

    public boolean contains(String key) {
        return this.hashtable.containsKey(key);
    }

    public int size() {
        return this.hashtable.size();
    }

    /**
	 * @param key
	 * @return
	 */
    protected Object getValueFromHashtable(String key) {
        Vector value = (Vector) this.hashtable.get(key);
        if (value == null) return null;
        IMetadataEntry[] entries = (IMetadataEntry[]) value.toArray(new IMetadataEntry[0]);
        Comparator timeComparator = new Comparator() {

            public int compare(Object arg0, Object arg1) {
                long time0 = ((IMetadataEntry) arg0).getCreationTime();
                long time1 = ((IMetadataEntry) arg1).getCreationTime();
                if (time0 < time1) return -1; else if (time0 > time1) return 1; else return 0;
            }
        };
        Arrays.sort(entries, timeComparator);
        if (entries.length > 0) return entries[entries.length - 1].getObjectValue();
        return null;
    }

    public Object getObjectValue(String key) {
        return this.getValueFromHashtable(key);
    }

    public String getValue(String key) {
        return this.getStringValue(key);
    }

    public String getStringValue(String key) {
        Object value = this.getValueFromHashtable(key);
        if (value == null) return null;
        return value.toString();
    }

    public boolean getBooleanValue(String key) {
        Object value = this.getValueFromHashtable(key);
        if (value == null) return false;
        return Utilities.string2Boolean((String) value);
    }

    public int getIntegerValue(String key) {
        Object value = this.getValueFromHashtable(key);
        if (value == null) return Constants.UNDEFINED_INTEGER;
        return Utilities.string2Integer((String) value);
    }

    public long getLongValue(String key) {
        Object value = this.getValueFromHashtable(key);
        if (value == null) return Constants.UNDEFINED_LONG;
        return Utilities.string2Long((String) value);
    }

    public float getFloatValue(String key) {
        Object value = this.getValueFromHashtable(key);
        if (value == null) return Constants.UNDEFINED_FLOAT;
        return Utilities.string2Float((String) value);
    }

    public double getDoubleValue(String key) {
        Object value = this.getValueFromHashtable(key);
        if (value == null) return Constants.UNDEFINED_DOUBLE;
        return Utilities.string2Double((String) value);
    }

    public String getValueOrError(String key) throws MM4UConfigurationException {
        Object value = this.getValueFromHashtable(key);
        if (value == null) throw new MM4UConfigurationException(this, "getValueOrError", "There is no value to the key '" + key + "'.");
        return value.toString();
    }

    public int getIntegerValueOrError(String key) throws MM4UConfigurationException {
        if (!this.hashtable.containsKey(key)) throw new MM4UConfigurationException(this, "getIntegerValueOrError", "There is no value to the key '" + key + "'.");
        return this.getIntegerValue(key);
    }

    public StringVector getStringValuesWhereKeyStartsWith(String keyPrefix) {
        StringVector valueSet = new StringVector();
        Enumeration keys = this.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (key.startsWith(keyPrefix)) valueSet.add(this.getStringValue(key));
        }
        return valueSet;
    }

    public boolean putIfValueExists(String key, String stringValue) {
        return this.setValue(key, stringValue);
    }

    public boolean remove(String key) {
        if (this.hashtable.remove(key) == null) return false;
        return true;
    }

    public Enumeration keys() {
        return this.hashtable.keys();
    }

    public void load(String filename) throws IOException {
        InputStream inFile;
        if (filename.startsWith("http") || filename.startsWith("ftp")) inFile = (new URL(filename)).openStream(); else inFile = new FileInputStream(filename);
        Properties tempProperties = new Properties();
        tempProperties.load(inFile);
        Enumeration keys = tempProperties.propertyNames();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            if (!this.contains(name)) this.insert(name, tempProperties.getProperty(name), false);
        }
    }

    public void store(String filename) throws IOException {
        FileOutputStream outFile = new FileOutputStream(filename, false);
        Properties tempProperties = new Properties();
        Enumeration keys = this.hashtable.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            tempProperties.setProperty(name, this.getStringValue(name));
        }
        tempProperties.store(outFile, filename);
    }

    @Override
    public String toString() {
        Enumeration myPropertyEnum = this.keys();
        String tempString = "Class:" + this.getClass() + "Properties: ";
        while (myPropertyEnum.hasMoreElements()) {
            String tempKey = (String) myPropertyEnum.nextElement();
            String tempValue = this.getStringValue(tempKey);
            tempString = tempString + tempKey + "=" + tempValue + "; ";
        }
        return tempString + "EOF";
    }

    public IPropertyList recursiveClone() {
        PropertyList newPList = new PropertyList();
        return this.recursiveClone(newPList);
    }

    protected IPropertyList recursiveClone(PropertyList object) {
        Hashtable newObject = new Hashtable();
        for (Iterator iter = this.hashtable.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            Vector value = (Vector) entry.getValue();
            Vector newValue = new Vector(value.capacity());
            for (Iterator iterator = value.iterator(); iterator.hasNext(); ) {
                IMetadataEntry element = (IMetadataEntry) iterator.next();
                newValue.add(element.recursiveClone());
            }
            newObject.put(entry.getKey(), newValue);
        }
        object.hashtable = newObject;
        return object;
    }

    public static Property splitStringIntoKeyAndValue(String myString) {
        return splitStringIntoKeyAndValue(myString, "=");
    }

    public static Property splitStringIntoKeyAndValue(String myString, String seperator) {
        if (Utilities.stringIsNullOrEmpty(myString) || Utilities.stringIsNullOrEmpty(seperator)) {
            return null;
        }
        myString = myString.trim();
        seperator = seperator.trim();
        Property tempProperty = null;
        if (myString.indexOf(seperator) != -1) {
            String key = myString.substring(0, myString.indexOf(seperator));
            String value = myString.substring((myString.indexOf(seperator) + seperator.length()), myString.length());
            if (Utilities.stringIsNotNullAndNotEmpty(key)) {
                tempProperty = new Property(key, value);
            }
        }
        return tempProperty;
    }

    public boolean keyExists(String key) {
        if (this.getObjectValue(key) == null) {
            return false;
        } else {
            return true;
        }
    }
}
