package org.formaria.aria.data;

import org.formaria.debug.DebugLogger;
import org.formaria.aria.build.BuildProperties;

/**
 * The basic implementation of the DataModel is handled by this class. Static data
 * is loaded from an XML file pointed to by the startup.properties file by
 * default using an DataSource. The model is central to Aria and allows the UI
 * components to be separated from the data in an abstract fashion. This model
 * element is designed to support static data, text, list and tabular structures.
 * <p>Copyright (c) Formaria Ltd., 1998-2003<br>
 * License:      see license.txt
 * @version $Revision: 2.12 $
 */
public class BaseModel extends DataModel {

    private boolean hasAutoId = false;

    /**
   * array of Object values
   */
    protected DataModel[] values;

    private volatile int hashcode;

    protected Object[] attributeValues;

    protected String[] attributeNames;

    private int numValidChildren = 0;

    /**
   * If set, paths not found will automatically be allocated
   */
    protected boolean addByDefault = true;

    /**
   * append DataModel elements which are not found by default.
   */
    protected static boolean appendByDefault = true;

    public static final int VALUE_ATTRIBUTE = 0;

    public static final int ID_ATTRIBUTE = 1;

    public static final int NUM_FIXED_ATTRIBUTE = 2;

    /**
   * Constructs an instance of the model node.
   * @param parent The DataModel to which this instance will be appended
   */
    public BaseModel(DataModel parent) {
        parentModel = parent;
        values = null;
        numValidChildren = 0;
        setNumAttributes(NUM_FIXED_ATTRIBUTE);
        attributeNames[VALUE_ATTRIBUTE] = "value";
        attributeNames[ID_ATTRIBUTE] = "id";
        tagName = "data";
    }

    /**
   * Constructor which sets the id and value attributes and appends this DataModel
   * to the parent model.
   * @param parent the DataModel instance which will be the parent of this instance
   * @param id the id of this instance
   * @param value the initial value of this instance
   */
    public BaseModel(DataModel parent, String id, Object value) {
        this(parent);
        setId(id);
        set(value);
        parentModel.append(this);
    }

    /**
   * Insert a node at a specified index in the list of children
   * @param newNode the new model node
   * @param idx the index at which to insert
   */
    public void insertChildAt(DataModel newNode, int idx) {
        int numChildren = (values == null ? 0 : values.length);
        setNumChildren(Math.max(numValidChildren + 1, numChildren));
        numValidChildren++;
        for (int i = values.length - 1; i > idx; i--) values[i] = values[i - 1];
        values[idx] = newNode;
    }

    /**
   * Move a child node up or down in the list of children
   * @param nodeA the child node to move
   * @param nodeB the child node to move
   */
    public void swapNodes(DataModel nodeA, DataModel nodeB) {
        int numChildren = (values == null ? 0 : values.length);
        int idxA = -1;
        int idxB = -1;
        for (int i = 0; i < numChildren; i++) {
            if (values[i] == nodeA) {
                idxA = i;
                if (idxB >= 0) break;
            } else if (values[i] == nodeB) {
                idxB = i;
                if (idxA >= 0) break;
            }
        }
        if ((idxA >= 0) && (idxB >= 0)) {
            values[idxA] = nodeB;
            values[idxB] = nodeA;
        }
    }

    /**
   * Check to see if the specified child node exists. Doing a get() creates the
   * named node by design, so it will always result in a value
   * @param name the name of the child we are looking for
   * @return boolean indicating the existance of the child.
   */
    public boolean getChildExists(String name) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if ((values[i] != null) && values[i].getId().equals(name)) return true;
            }
        }
        return false;
    }

    /**
   * null ctor. Calls the BaseModel( DataModel parent ) ctor will null parameter
   */
    public BaseModel() {
        this(null);
    }

    /**
   * Get the value of the element located at the path in the element parameter.
   * The element name parameter can include an attribute name by appending
   * '@attributeName' to the path, where attributeName is the name of the
   * attribute.
   * @param element The path to the DataModel required
   * @return The value of the DataModel or the attribute
   */
    public Object get(String element) {
        if ((element == null) || (element.length() == 0)) return null;
        if (element.charAt(0) == '/') element = element.substring(1);
        String attribName = getAttribFromPath(element);
        String attribValue = null;
        int numValues = values == null ? 0 : values.length;
        int i = 0;
        String subPath;
        int pos = element.indexOf('/');
        if (attribName != null) {
            int equalsPos = attribName.indexOf('=');
            if (equalsPos > 0) {
                if (attribName.charAt(equalsPos + 1) == '[') {
                    int endPos = attribName.indexOf(']');
                    attribValue = attribName.substring(equalsPos + 2, endPos).trim();
                    pos = element.indexOf('/', element.indexOf(']'));
                } else attribValue = attribName.substring(equalsPos + 1).trim();
                attribName = attribName.substring(0, equalsPos).trim();
            } else return getAttribValue(getAttribute(attribName));
            subPath = getBaseFromPath(element);
        } else subPath = pos > -1 ? element.substring(0, pos) : element;
        int spHash = subPath.hashCode();
        for (; i < numValues; i++) {
            DataModel node = values[i];
            if (node != null) {
                if (node.hashCode() == spHash) {
                    if (attribName == null) {
                        if (pos < 0) {
                            if (BuildProperties.DEBUG) {
                                String nodeValue = "";
                                if (node.getClass().getName().indexOf("BaseModel") > 0) {
                                    Object objValue = node.get();
                                    if (objValue != null) nodeValue = objValue.toString();
                                } else nodeValue = node.toString();
                                DebugLogger.trace("==> get( " + (String) attributeValues[ID_ATTRIBUTE] + ", " + node.getId() + " ) : " + nodeValue);
                            }
                            return node;
                        } else return node.get(element.substring(pos + 1));
                    } else {
                        if (pos < 0) {
                            if (BuildProperties.DEBUG) DebugLogger.trace("==> get( " + (String) attributeValues[ID_ATTRIBUTE] + ", " + node.getId() + " ) : " + node.getAttribValueAsString(0));
                            if (attribValue == null) return node.getAttribValue(node.getAttribute(attribName)); else {
                                String testAttribValue = node.getAttribValueAsString(node.getAttribute(attribName));
                                if ((testAttribValue != null) && testAttribValue.equals(attribValue)) return node;
                            }
                        } else {
                            if (attribValue == null) return node.get(element.substring(pos + 1)); else {
                                int attribPos = node.getAttribute(attribName);
                                if (attribPos > -1) {
                                    String attribStr = node.getAttribValueAsString(attribPos);
                                    if ((attribStr != null) && attribStr.equals(attribValue)) return node.get(element.substring(pos + 1));
                                }
                            }
                        }
                    }
                }
            } else break;
        }
        if (appendByDefault) return append(element);
        return null;
    }

    /**
   * Set the value of the element in the child DataModel located at the elementName.
   * The child node matching the element is first retrieved and then the named
   * attribute is updated. The attributeName can be specified by appending
   * '@attributeName' to the path, where attributeName is the name of the
   * attribute (e.g. '/fonts/arial/@bold' where 'bold' is the attributeName.
   * If the attributeName is not specified the retreived node's value is updated.
   * <br>
   * To update this node's value use <code>set( newObject )</code>
   * @param elementName The path to the DataModel in the format 'base/foo
   * @param newObject The new value of the attribute
   */
    public void set(String elementName, Object newObject) {
        String attribName = getAttribFromPath(elementName);
        String path = getBaseFromPath(elementName);
        DataModel node = path == null ? this : (DataModel) get(path);
        if (attribName == null) node.setAttribValue(node.getAttribute("value"), newObject); else node.setAttribValue(node.getAttribute(attribName), newObject);
    }

    /**
   * Get the index of the attribiteNames array whose value is the same
   * as the attribName
   * @param attribName The name of the attribute we are trying to locate
   * @return The index of the attributeNames array containg the name
   */
    public int getAttribute(String attribName) {
        int attribHashcode = attribName.hashCode();
        int numAttributes = attributeValues.length;
        int j = 0;
        for (; j < numAttributes; j++) {
            if (attributeNames[j] != null) {
                if (attributeNames[j].hashCode() == attribHashcode) {
                    return j;
                }
            } else break;
        }
        if (addByDefault && appendByDefault) {
            if (j == numAttributes) {
                setNumAttributes(numAttributes + 1);
            }
            attributeNames[j] = attribName;
            return j;
        }
        return -1;
    }

    /**
   * Get the DataModel at element i
   * @param i The index of the values array
   * @return The DataModel at location i
   */
    public DataModel get(int i) {
        return values[i];
    }

    /**
   * gets the value attribute
   * @return the value of the model
   */
    public Object get() {
        return attributeValues[VALUE_ATTRIBUTE];
    }

    /**
   * Sets the model value of this node.
   * @param s the new value
   */
    public void set(Object s) {
        boolean changed = !(attributeValues[VALUE_ATTRIBUTE] == s);
        if (BuildProperties.DEBUG) DebugLogger.trace((String) attributeValues[ID_ATTRIBUTE] + "=" + (s == null ? "null" : s.toString()));
        attributeValues[VALUE_ATTRIBUTE] = s;
        if (changed) fireModelUpdated();
    }

    /**
   * Used for elements which need a name assigned temporarily because one doesn't
   * exist in the DataSource.
   * @param b true if there was no name in the DataSource
   */
    public void hasAutoId(boolean b) {
        hasAutoId = b;
    }

    /**
   * Determine if the element needs a name assigned temporarily because one doesn't
   * exist in the DataSource.
   * @return true if there was no name for the element in the DataSource
   */
    public boolean hasAutoId() {
        return hasAutoId;
    }

    /**
   * Gets the name attribute
   * @return the name attribute
   */
    public String getId() {
        return (String) attributeValues[ID_ATTRIBUTE];
    }

    /**
   * Sets the name attribute
   * @param newName the new name
   */
    public void setId(String newName) {
        attributeValues[ID_ATTRIBUTE] = newName;
        hashcode = newName.hashCode();
    }

    /**
   * Set the name of the attribute located at the specified index
   * @param i The index of the attributeNames array whose value we want
   * @return The string value of the attributeNames array at position i
   */
    public String getAttribName(int i) {
        return attributeNames[i];
    }

    /**
   * Retrive the value of the attribute at the specified index
   * @param i The index of the attributeValues array whose value we want
   * @return The (Object) value of the attributeValues array at position i
   */
    public Object getAttribValue(int i) {
        return attributeValues[i];
    }

    /**
   * Convert the attribute at the specified index to a String and return it
   * @param i The index of the attributeValues array whose value we want
   * @return The string value of the attributeValues array at position i
   */
    public String getAttribValueAsString(int i) {
        try {
            return (String) attributeValues[i];
        } catch (ClassCastException e) {
            return attributeValues[i].toString();
        }
    }

    /**
   * Convert the attribute at the specified index to a double and return it
   * @param i The index of the attributeValues array whose value we want
   * @return The double value of the attributeValues array at position i
   * @deprecated use getAttribValueAsDouble( i, decimalSeparator, groupingSeparator ) 
   * instead, if the locale is different from the locale used to write the values 
   * to the model, then the parsed value may be incorrect.
   */
    public double getAttribValueAsDouble(int i) {
        if (attributeValues[i] instanceof Double) return ((Double) attributeValues[i]).doubleValue();
        return Double.parseDouble(attributeValues[i].toString());
    }

    /**
   * Convert the attribute at the specified index to a double and return it
   * @param i The index of the attributeValues array whose value we want
   * @param decimalSeparator the decimal separator
   * @param groupingSeparator the grouping (thousands) separator
   * @return The double value of the attributeValues array at position i
   */
    public double getAttribValueAsDouble(int i, char decimalSeparator, char groupingSeparator) {
        if (attributeValues[i] instanceof Double) return ((Double) attributeValues[i]).doubleValue();
        return Double.parseDouble(attributeValues[i].toString());
    }

    /**
   * Convert the attribute at the specified index to an int and return it
   * @param index The index of the attributeValues array whose value we want
   * @return The int value of the attributeValues array at position i
   */
    public int getAttribValueAsInt(int index) {
        if (attributeValues[index] instanceof Double) return Integer.parseInt(attributeValues[index].toString());
        return Integer.parseInt(attributeValues[index].toString());
    }

    /**
   * Sets the attribute value
   * @param index The index of the attributeValues array whose value we want
   * @param value the value object
   */
    public void setAttribValue(int index, Object value) {
        if (index == ID_ATTRIBUTE) setId((String) value);
        attributeValues[index] = value;
    }

    /**
   * Sets the attribute value
   * @param index The index of the attributeValues array whose value we want
   * @param attribName the name of the attribute
   * @param value the value object
   */
    public void setAttribValue(int index, String attribName, Object value) {
        if (index >= NUM_FIXED_ATTRIBUTE) attributeNames[index] = attribName;
        attributeValues[index] = value;
    }

    /**
   * Gets the value attribute as a Double value
   * @param elementName The name of the element to be retrieved from this instance
   * @return the value as a double
   */
    public double getValueAsDouble(String elementName) {
        DataModel node = (DataModel) get(elementName);
        if (node.get() instanceof Double) return ((Double) node.get()).doubleValue();
        return Double.parseDouble(node.getAttribValueAsString(VALUE_ATTRIBUTE));
    }

    /**
   * Gets the value attribute of the specified node as an int.
   * @param elementName The name of the element to be retrieved from this instance
   * @return the value as an int
   */
    public int getValueAsInt(String elementName) {
        DataModel node = (DataModel) get(elementName);
        if (node.get() instanceof Double) return Integer.parseInt(node.getAttribValueAsString(VALUE_ATTRIBUTE));
        return Integer.parseInt(node.getAttribValueAsString(VALUE_ATTRIBUTE));
    }

    /**
   * Gets the value attribute of the specified node as a string.
   * @param elementName The name of the element to be retrieved from this instance
   * @return the value as a string
   */
    public String getValueAsString(String elementName) {
        DataModel node = (DataModel) get(elementName);
        return node.getAttribValueAsString(VALUE_ATTRIBUTE);
    }

    /**
   * Gets the value attribute of the specified node as an DataModel.
   * @param elementName The name of the element to be retrieved from this instance
   * @return the value as an DataModel
   */
    public DataModel getModel(String elementName) {
        return (DataModel) get(elementName);
    }

    /**
   * The hashcode of this instance. Based on the ID String
   * @return The hashcode of this instance
   */
    public int hashCode() {
        return hashcode;
    }

    /**
   * Gets the number of immediate children of this node
   * @return  the number of child nodes
   */
    public int getNumChildren() {
        if (values == null) return 0;
        return numValidChildren;
    }

    /**
   * Gets the number of attributes of this node
   * @return the number of attributes
   */
    public int getNumAttributes() {
        return attributeValues.length;
    }

    /**
   * Set the number of children of this node
   * @param num the new number of children
   */
    public void setNumChildren(int num) {
        DataModel[] temp = values;
        values = new DataModel[num];
        if (temp != null) {
            int numChildren = Math.min(num, temp.length);
            for (int i = 0; i < numChildren; i++) values[i] = temp[i];
        }
    }

    /**
   * Append a new node with the specified name. This method does not replace any
   * existing nodes.
   * @param elementName The immediate path to the DataModel required
   * @return The value of the DataModel or the attribute
   */
    public Object append(String elementName) {
        String path = getBaseFromPath(elementName);
        int numValues = values == null ? 0 : values.length;
        int i = 0;
        for (; i < numValues; i++) {
            if (values[i] == null) break;
        }
        if (i == numValues) setNumChildren(numValidChildren + 1);
        int pos = path.lastIndexOf('/');
        values[i] = new BaseModel(this);
        numValidChildren++;
        if (pos < 0) values[i].setId(path); else {
            pos = path.indexOf('/');
            values[i].setId(path.substring(0, pos));
            return values[i].append(path.substring(pos + 1));
        }
        return values[i];
    }

    /**
   * Appends a node to the model. If a node of the same name is found it is
   * replaced. If there is insufficient space to store the new node then the
   * storage is automatically expanded.
   * <br>
   * The child nodes should be named uniquely
   * or not at all (i.e. they should be annonymous). If the new childNode has a
   * name then it is compared to the existing nodes and will replace a node of
   * the same name if one exists.
   * @param childNode the child node
   */
    public void append(DataModel childNode) {
        if (values == null) values = new DataModel[1];
        String childName = childNode.getId();
        int numChildren = numValidChildren;
        if (childName == null) {
            for (int i = 0; i < values.length; i++) {
                if ((values[i] == null)) {
                    values[i] = childNode;
                    numValidChildren++;
                    return;
                }
            }
        } else {
            for (int i = 0; i < numChildren; i++) {
                if ((values[i].getId() != null) && (values[i].getId().compareTo(childName) == 0)) {
                    values[i] = childNode;
                    return;
                }
            }
        }
        setNumChildren(numChildren + 5);
        values[numValidChildren] = childNode;
        if (childNode instanceof BaseModel) childNode.setParent(this);
        numValidChildren = numChildren + 1;
    }

    /**
   * Remove a child node from this DataModel instance. Squeeze the following
   * children so that they are contiguous.
   * @param child the child to be removed.
   */
    public void remove(DataModel child) {
        boolean found = false;
        int numChildren = values.length;
        for (int i = 0; i < numChildren; i++) {
            if (!found) {
                if (values[i] == child) {
                    found = true;
                    values[i] = null;
                    numValidChildren--;
                }
            } else values[i - 1] = values[i];
        }
        if (found && (numChildren == (numValidChildren + 1))) values[numChildren - 1] = null;
    }

    /**
   * Remove the nodes attributes and attribute names
   */
    public void removeAttributes() {
        attributeValues = new Object[NUM_FIXED_ATTRIBUTE];
        attributeNames = new String[NUM_FIXED_ATTRIBUTE];
    }

    /**
   * Remove the children of this node
   */
    public void removeChildren() {
        values = null;
        numValidChildren = 0;
    }

    /**
   * Remove a child node
   * @param name the ID or name of the node
   */
    public void removeChild(String name) {
        boolean found = false;
        for (int i = 0; i < values.length; i++) {
            if (!found) {
                if (values[i].getId().compareTo(name) == 0) {
                    values[i] = null;
                    found = true;
                    numValidChildren--;
                }
            }
            if (found && i < (values.length - 1)) values[i] = values[i + 1];
        }
        if (found) values[values.length - 1] = null;
    }

    /**
   * Remove a child node
   * @param name the ID or name of the node
   * @param value the value of the matching node to remove
   */
    public void removeChild(String name, String value) {
        boolean found = false;
        for (int i = 0; i < values.length; i++) {
            if (!found) {
                if (values[i].getId().equals(name)) {
                    if (values[i].get().equals(value)) {
                        values[i] = null;
                        found = true;
                        numValidChildren--;
                    }
                }
            }
            if (i < (values.length - 1)) values[i] = values[i + 1];
        }
        if (found) values[values.length - 1] = null;
    }

    /**
   * Setup the attributeNames and attributeValues arrays. If not already
   * initialised set the size of each to 2 otherwise store them temporarily
   * and reassign to the increased size arrays.
   * @param num The new size of the array
   */
    public void setNumAttributes(int num) {
        num = Math.max(num, 2);
        Object[] temp = new Object[num];
        String[] tempNames = new String[num];
        if (attributeNames != null) {
            if (num != attributeNames.length) {
                int numAttributes = Math.min(num, attributeNames.length);
                System.arraycopy(attributeNames, 0, tempNames, 0, numAttributes);
                System.arraycopy(attributeValues, 0, temp, 0, numAttributes);
            } else return;
        }
        attributeValues = temp;
        attributeNames = tempNames;
    }

    /**
   * Get the attribute from a path e.g. <br>
   * returns 'attrib' from 'a/b/c/@attrib'
   * returns null from 'a/b/c/'
   * @param path the path to split
   * @return the attribute name
   */
    public static String getAttribFromPath(String path) {
        int pos = path.indexOf('@');
        if (pos >= 0) {
            int endPos = path.indexOf('/');
            int startEscapePos = path.indexOf('[');
            if ((startEscapePos > pos) && (startEscapePos < endPos)) endPos = path.indexOf(']') + 1;
            if (endPos < 0) return path.substring(pos + 1);
            if (endPos < pos) return null;
            return path.substring(pos + 1, endPos);
        }
        return null;
    }

    /**
   * Get the base path from a path e.g. <br>
   * returns 'a/b/c/' from 'a/b/c/@attrib'
   * returns 'a/b/c/' from 'a/b/c/'
   * returns null from 'a/b/c/@attrib'
   * @param path the path to split
   * @return the path stripped of attributes
   */
    public static String getBaseFromPath(String path) {
        int pos = path.indexOf('@');
        if (pos < 0) return path; else if (pos > 0) return path.substring(0, pos);
        return null;
    }

    /**
   * Set the flags that determines if attributes are added when queried
   * @param state true to add an attribute if it is missing, false to return -1
   * when querying a node attribute with "getAttribute( name )".
   */
    public void setAddByDefault(boolean state) {
        addByDefault = state;
    }

    /**
   * Get the flags that determines if attributes are added when queried
   * @return true if an attribute is added if it is missing when queried, or false to return -1
   * when querying a node attribute with "getAttribute( name )".
   */
    public boolean getAddByDefault() {
        return addByDefault;
    }

    /**
   * Set the flags that determines if attributes are added when queried
   * @param state true to add an attribute if it is missing, false to return -1
   * when querying a node attribute with "getAttribute( name )".
   */
    public static void setAppendByDefault(boolean state) {
        appendByDefault = state;
    }

    /**
   * Get the flags that determines if attributes are added when queried
   * @return true if an attribute is added if it is missing when queried, or false to return -1
   * when querying a node attribute with "getAttribute( name )".
   */
    public static boolean getAppendByDefault() {
        return appendByDefault;
    }
}
