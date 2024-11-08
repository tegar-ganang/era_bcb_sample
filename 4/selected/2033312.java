package org.formaria.editor.project.pages.components;

import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.awt.AWTEvent;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import org.formaria.builder.AriaBuilder;
import org.formaria.debug.DebugLogger;
import org.formaria.editor.project.pages.PageResource;
import org.formaria.editor.project.pages.components.properties.LangProperty;
import org.formaria.editor.project.pages.components.proxy.ComponentProxy;
import org.formaria.aria.TextRenderer;
import org.formaria.editor.project.EditorProject;
import org.formaria.editor.project.pages.ComponentSizer;
import org.formaria.editor.project.pages.components.properties.BooleanProperty;
import org.formaria.editor.project.pages.components.properties.BuiltinProperty;
import org.formaria.editor.project.pages.components.properties.ComponentNameProperty;
import org.formaria.editor.project.pages.components.properties.DataProperty;
import org.formaria.editor.project.pages.components.properties.EventProperty;
import org.formaria.editor.project.pages.components.properties.GridBagLayoutConstraintProperty;
import org.formaria.editor.project.pages.components.properties.GuideLayoutConstraintProperty;
import org.formaria.editor.project.pages.components.properties.ImageProperty;
import org.formaria.editor.project.pages.components.properties.LayoutProperty;
import org.formaria.editor.project.pages.components.properties.LayoutConstraintProperty;
import org.formaria.editor.project.pages.components.properties.ListProperty;
import org.formaria.editor.project.pages.components.properties.PlainProperty;
import org.formaria.editor.project.pages.components.properties.StyleProperty;
import org.formaria.editor.project.pages.components.properties.SpringLayoutConstraintProperty;
import org.formaria.editor.project.pages.components.properties.ValidationProperty;
import org.formaria.aria.ProjectManager;

/**
 * A helper class to describe the properties of an individual component
 * <p>Copyright (c) Formaria Ltd., 2002-2006</p>
 * <p> $Revision: 1.33 $</p>
 */
public abstract class PropertyHelper {

    protected static final String alignmentOptions[] = { "Left", "Right", "Center", "Leading", "Trailing" };

    protected static final int alignmentOptionIds[] = { JTextField.LEFT, JTextField.RIGHT, JTextField.CENTER, JTextField.LEADING, JTextField.TRAILING };

    protected static final String lblAlignmentOptions[] = { "Left", "Right", "Center" };

    protected static final int lblAlignmentOptionIds[] = { TextRenderer.LEFT, TextRenderer.RIGHT, TextRenderer.CENTER };

    protected static final String scrollOptions[] = { "As Needed", "Never", "Always" };

    protected static final int horzScrollOptionIds[] = { JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS };

    protected static final int vertScrollOptionIds[] = { JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS };

    protected static Hashtable<String, PlainProperty> propertyTypes;

    protected static ArrayList<String> builtinProperties;

    protected boolean usesContentFile;

    protected boolean allowsChildren;

    protected boolean restrictsSize;

    protected boolean isSwing;

    protected int numBuiltinProperties;

    protected String className;

    protected String componentType;

    protected String[] extensions = { "xml" };

    protected String fileTypeDesc = "XML Files";

    protected String defExt = "xml";

    protected ArrayList<String> properties;

    protected Hashtable<String, String> defaultValues;

    public PropertyHelper() {
        numBuiltinProperties = -1;
        properties = new ArrayList<String>();
        defaultValues = new Hashtable<String, String>();
        properties.add("Type");
        properties.add("Name");
        properties.add("X");
        properties.add("Y");
        properties.add("W");
        properties.add("H");
        properties.add("Style");
        properties.add("Constraint");
        isSwing = true;
        usesContentFile = allowsChildren = restrictsSize = false;
        if (propertyTypes == null) {
            builtinProperties = new ArrayList<String>();
            builtinProperties.add("Type");
            builtinProperties.add("Name");
            builtinProperties.add("X");
            builtinProperties.add("Y");
            builtinProperties.add("W");
            builtinProperties.add("H");
            builtinProperties.add("Style");
            builtinProperties.add("Constraint");
            propertyTypes = new Hashtable<String, PlainProperty>();
            propertyTypes.put("Type", new BuiltinProperty(0));
            propertyTypes.put("Name", new ComponentNameProperty(1));
            propertyTypes.put("X", new BuiltinProperty(2));
            propertyTypes.put("Y", new BuiltinProperty(3));
            propertyTypes.put("W", new BuiltinProperty(4));
            propertyTypes.put("H", new BuiltinProperty(5));
            propertyTypes.put("Style", new StyleProperty());
            propertyTypes.put("Content", new PlainProperty("getContent", "setContent"));
            propertyTypes.put("Text", new LangProperty("Content", "getText", "setText"));
            propertyTypes.put("Align", new ListProperty("Align", lblAlignmentOptions, lblAlignmentOptionIds));
            propertyTypes.put("Alignment", new ListProperty("Alignment", alignmentOptions, alignmentOptionIds));
            propertyTypes.put("LabelAlignment", new ListProperty("Alignment", lblAlignmentOptions, lblAlignmentOptionIds));
            propertyTypes.put("HorizontalAlignment", new ListProperty("HorizontalAlignment", alignmentOptions, alignmentOptionIds));
            propertyTypes.put("VerticalAlignment", new ListProperty("VerticalAlignment", alignmentOptions, alignmentOptionIds));
            propertyTypes.put("TextPosition", new ListProperty("TextPosition", alignmentOptions, alignmentOptionIds));
            propertyTypes.put("ActionHandler", new EventProperty("ActionHandler", AWTEvent.ACTION_EVENT_MASK));
            propertyTypes.put("ItemHandler", new EventProperty("ItemHandler", AWTEvent.ITEM_EVENT_MASK));
            propertyTypes.put("FocusHandler", new EventProperty("FocusHandler", AWTEvent.FOCUS_EVENT_MASK));
            propertyTypes.put("KeyHandler", new EventProperty("KeyHandler", AWTEvent.KEY_EVENT_MASK));
            propertyTypes.put("SelectionHandler", new EventProperty("SelectionHandler", AWTEvent.ITEM_EVENT_MASK));
            propertyTypes.put("MouseHandler", new EventProperty("MouseHandler", AWTEvent.MOUSE_EVENT_MASK));
            propertyTypes.put("MouseMotionHandler", new EventProperty("MouseMotionHandler", AWTEvent.MOUSE_MOTION_EVENT_MASK));
            propertyTypes.put("ListSelectionHandler", new EventProperty("ListSelectionHandler", ListSelectionEvent.class));
            propertyTypes.put("TextHandler", new EventProperty("TextHandler", AWTEvent.TEXT_EVENT_MASK));
            propertyTypes.put("Data", new DataProperty("Data", null));
            propertyTypes.put("DestinationData", new DataProperty("DestinationData", null));
            propertyTypes.put("TextData", new DataProperty("TextData", "text"));
            propertyTypes.put("SelectionData", new DataProperty("SelectionData", "state"));
            propertyTypes.put("Validation", new ValidationProperty("Validation"));
            propertyTypes.put("ImageName", new ImageProperty("ImageName"));
            propertyTypes.put("Opaque", new BooleanProperty("Opaque"));
            propertyTypes.put("Enabled", new BooleanProperty("Enabled"));
            propertyTypes.put("Visible", new BooleanProperty("Visible"));
            propertyTypes.put("Selected", new BooleanProperty("Selected"));
            propertyTypes.put("UsesLAF", new BooleanProperty("UsesLAF", "getUsesLaf", "setUsesLaf"));
            propertyTypes.put("Resource", new PlainProperty("Resource"));
            propertyTypes.put("HorizontalScrollBar", new ListProperty("HorizontalScrollBarPolicy", scrollOptions, horzScrollOptionIds));
            propertyTypes.put("VerticalScrollBar", new ListProperty("VerticalScrollBarPolicy", scrollOptions, vertScrollOptionIds));
            propertyTypes.put("Rows", new PlainProperty("Rows"));
            propertyTypes.put("Columns", new PlainProperty("Columns"));
            propertyTypes.put("Cols", new PlainProperty("Cols"));
            propertyTypes.put("Wrap", new BooleanProperty("Wrap", "getLineWrap", "setLineWrap"));
            propertyTypes.put("WordWrap", new BooleanProperty("WordWrap", "getWrapStyleWord", "setWrapStyleWord"));
            propertyTypes.put("Layout", new LayoutProperty());
            propertyTypes.put("Constraint", new LayoutConstraintProperty());
            propertyTypes.put("Hgap", new PlainProperty("Hgap", PlainProperty.INTEGER_PROPERTY));
            propertyTypes.put("Vgap", new PlainProperty("Vgap", PlainProperty.INTEGER_PROPERTY));
            propertyTypes.put("GridBagConstraint", new GridBagLayoutConstraintProperty());
            propertyTypes.put("SpringConstraint", new SpringLayoutConstraintProperty());
            propertyTypes.put("GuideConstraint", new GuideLayoutConstraintProperty());
            propertyTypes.put("ScaleAll", new BooleanProperty("ScaleAll"));
            propertyTypes.put("ScaleFonts", new BooleanProperty("ScaleFonts"));
            propertyTypes.put("LoopTime", new PlainProperty("LoopTime", PlainProperty.INTEGER_PROPERTY));
        }
    }

    /**
   * Get the page builder
   * @return the builder
   */
    public AriaBuilder getBuilder() {
        return (AriaBuilder) ProjectManager.getCurrentProject().getObject("Builder");
    }

    /**
   * Add a property type if it is not already present
   * @param name the name of the new property
   * @param type the property type
   * @return true if successfully added, otherwise false (if it already exists)
   */
    public boolean addPropertyType(String name, PlainProperty type) {
        if (propertyTypes.get(name) == null) {
            propertyTypes.put(name, type);
            return true;
        }
        return false;
    }

    public void setComponent(Object comp) {
        try {
            className = comp.getClass().getName();
            cleanupClassName();
            if (className.indexOf(".swing.") > 0) isSwing = true;
            Class clazz = (comp instanceof ComponentProxy ? ((ComponentProxy) comp).getProxiedComponent().getClass() : comp.getClass());
            BeanInfo bi = Introspector.getBeanInfo(clazz);
            PropertyDescriptor props[] = bi.getPropertyDescriptors();
            for (int i = 0; i < props.length; i++) {
                Method readMethod = props[i].getReadMethod();
                Method writeMethod = props[i].getWriteMethod();
                if ((readMethod != null) && (writeMethod != null)) {
                    String propertyName = props[i].getName();
                    String qualifiedName = className + "." + propertyName;
                    if (!properties.contains(qualifiedName)) {
                        addPropertyType(qualifiedName, new PlainProperty(propertyName, readMethod.getName(), writeMethod.getName()));
                        properties.add(qualifiedName);
                    }
                }
            }
            EventSetDescriptor[] reflectedEvents = bi.getEventSetDescriptors();
            for (int i = 0; i < reflectedEvents.length; i++) {
                String eventName = "_" + reflectedEvents[i].getName();
                String qualifiedName = className + "." + eventName;
                Method adder = reflectedEvents[i].getAddListenerMethod();
                if (!properties.contains(qualifiedName)) {
                    addPropertyType(qualifiedName, new EventProperty(eventName, adder));
                    properties.add(qualifiedName);
                }
            }
        } catch (IntrospectionException ex) {
            DebugLogger.logError("Couldn't introspect property: " + ex.getMessage());
        }
    }

    /**
   * Gets the className of the soure component. If the name has not been explicitly
   * set the class name of this helper is used and the name is truncated to
   * remove the 'Helper' part.
   * @return the class name of the source component
   */
    public String getClassName() {
        if (className == null) {
            className = getClass().toString();
            cleanupClassName();
        }
        return className;
    }

    /**
   * Does this property helper work with the specified class
   * @param targetClass
   * @return true if this helper works with the target class
   */
    public boolean accepts(String targetClass) {
        String propertyClass = getClassName();
        int pos = propertyClass.indexOf("class ");
        if (pos == 0) propertyClass = propertyClass.substring(6);
        if (propertyClass.equals(targetClass)) return true;
        return false;
    }

    public void cleanupClassName() {
        if (className.startsWith("class ")) className = className.substring(6);
        if (className.endsWith(".class")) className = className.substring(0, className.length() - 6);
    }

    public String getComponentType() {
        return componentType;
    }

    /**
   * Get the number of properties that the component exposes
   * @return the number of propeties
   */
    public int getNumProperties(Object comp) {
        return properties.size();
    }

    /**
   * Get the number of built-in properties
   */
    public int getNumBuiltinProperties() {
        if (numBuiltinProperties < 0) {
            numBuiltinProperties = 0;
            int numProperties = properties.size();
            for (int i = 0; i < numProperties; i++) {
                PlainProperty pp = propertyTypes.get(properties.get(i));
                if (pp instanceof BuiltinProperty) numBuiltinProperties++;
            }
        }
        return numBuiltinProperties;
    }

    /**
   * Get the name of the property
   * @param i the property index
   * @return the name
   */
    public String getPropertyName(int i) {
        return properties.get(i);
    }

    /**
   * Get the display name of the property
   * @param key the property index
   * @return the display name
   */
    public String getPropertyDisplayName(String key) {
        PlainProperty pp = propertyTypes.get(key);
        if (pp != null) {
            String name = pp.getPropertyName();
            if (name != null) {
                int pos = name.lastIndexOf('.');
                if (pos >= 0) return name.substring(pos + 1);
                return name;
            }
        }
        return key;
    }

    /**
   * Get the XML tag name for the property
   * @param i the property index
   * @return the tag
   */
    public String getPropertyTag(int i) {
        String name = properties.get(i);
        PlainProperty pp = getProperty(name);
        if (pp != null) {
            String displayName = pp.getPropertyName();
            if (displayName != null) return displayName;
        }
        return name;
    }

    /**
   * Get a property of the specified name
   * @param name the property name
   * @return the property or null if the proeprty is not available
   */
    public PlainProperty getProperty(String name) {
        if (properties.contains(name)) return propertyTypes.get(name); else return null;
    }

    /**
   * Get a property of the specified name
   * @param name the property name
   * @param checkTags true to check if a property tag matches the name and that property is contained in the list of properties.
   * @return the property or null if the proeprty is not available
   */
    public PlainProperty getProperty(String name, boolean checkTags) {
        if (properties.contains(name)) return propertyTypes.get(name);
        if (checkTags) {
            Enumeration propTypesEnum = propertyTypes.keys();
            while (propTypesEnum.hasMoreElements()) {
                String key = (String) propTypesEnum.nextElement();
                PlainProperty propType = (PlainProperty) propertyTypes.get(key);
                String displayName = propType.getPropertyName();
                if (displayName == null) continue; else if (displayName.equals(name)) {
                    if (properties.contains(key)) return propType;
                }
            }
        }
        return null;
    }

    /**
   * Get a table cell editor
   * @param table the table being edited
   * @param comp the current component
   * @param currentProject the current project
   * @param propertyName the property name
   * @param row the current row
   * @param col the current column
   * @param the current value
   */
    public TableCellEditor getCellEditor(JTable table, Object comp, EditorProject currentProject, String propertyName, int row, int col, Object value) {
        return propertyTypes.get(propertyName).getCellEditor(table, comp, currentProject, this, propertyName, row, col, value);
    }

    /**
   * Is the property enabled
   * @param propertyName the property name
   * @param comp the component instance
   * @return the name
   */
    public boolean getEnabled(Object comp, String propertyName) {
        return propertyTypes.get(propertyName).getEnabled(comp);
    }

    /**
   * Get the type of the property
   * @param propertyName the property name
   * @return the type as defined by PropertiesEditor
   */
    public int getPropertyType(String propertyName) {
        return propertyTypes.get(propertyName).getPropertyType();
    }

    /**
   * Get the type name of the property displaying the name (synonym)
   * @param propertyName the property name
   * @return the type as defined by PropertiesEditor
   */
    public String getPropertyTypeName(String propertyName) {
        int numProperties = properties.size();
        for (int i = 0; i < numProperties; i++) {
            String typeName = properties.get(i);
            PlainProperty pp = propertyTypes.get(typeName);
            if (pp.getPropertyName() == propertyName) {
                return typeName;
            }
        }
        return propertyName;
    }

    /**
   * Get the names of the proeprties provided for this component of the 
   * specified type
   * @param propertyType the property type e.g. PlainProperty.DATA_PROPERTY
   */
    public ArrayList getProperties(int propertyType) {
        ArrayList propNames = new ArrayList();
        int numProperties = properties.size();
        for (int i = 0; i < numProperties; i++) {
            PlainProperty pp = propertyTypes.get(properties.get(i));
            if (pp.getPropertyType() == propertyType) {
                propNames.add(pp.getPropertyName());
            }
        }
        return propNames;
    }

    /**
   * Get the value of the property displayed by the editor
   * @param pageResource the page resource to which this component belongs
   * @param comp the component instance
   * @param propertyName the component property name
   * @return the value
   */
    public String getDisplayPropertyValue(PageResource pageResource, Object comp, String propertyName) {
        PlainProperty pp = propertyTypes.get(propertyName);
        if (pp.getEnabled(comp)) {
            Object value = pp.getDisplayPropertyValue(this, pageResource, comp, propertyName);
            if (value == null) return ""; else if (value instanceof String) return (String) value; else return value.toString();
        }
        return null;
    }

    /**
   * Get the value of the property exposed by the component
   * @param pageResource the page resource to which this component belongs
   * @param comp the component instance
   * @param propertyName the component property name
   * @param propertyName the property name
   * @return the value
   */
    public String getPropertyValue(PageResource pageResource, Object comp, String propertyName) {
        PlainProperty pp = propertyTypes.get(propertyName);
        if (pp.getEnabled(comp)) {
            Object value = pp.getPropertyValue(this, pageResource, comp, propertyName);
            if (value == null) return ""; else if (value instanceof String) return (String) value; else return value.toString();
        }
        return null;
    }

    /**
   * Get the value of the property written to the XML file
   * @param pageResource the page resource to which this component belongs
   * @param comp the component instance
   * @param propertyName the component property name
   * @return the value
   */
    public String getOutputPropertyValue(PageResource pageResource, Object comp, String propertyName) {
        String expression = getExpression(comp, propertyName);
        if (expression != null) return expression;
        PlainProperty pp = propertyTypes.get(propertyName);
        if (pp.getEnabled(comp)) {
            Object value = pp.getOutputPropertyValue(this, pageResource, comp, propertyName);
            if (value == null) return ""; else if (value instanceof String) return (String) value; else return value.toString();
        }
        return null;
    }

    /**
   * Get the value of the property exposed by the component
   * @param pageResource the page resource to which this component belongs
   * @param comp the component instance
   * @param propertyName the component property name
   * @return the value
   */
    public String getDefaultValue(PageResource pageResource, Object comp, String propertyName) {
        return defaultValues.get(propertyName);
    }

    /**
   * Set the value of the property exposed by the component
   * @param pageResource the page resource to which this component belongs
   * @param comp the component instance
   * @param propertyName the component property name
   * @return true if all is OK
   */
    public boolean setPropertyValue(PageResource pageResource, Object comp, String propertyName, Object value) {
        if (comp instanceof ComponentSizer) comp = ((ComponentSizer) comp).getTarget();
        return propertyTypes.get(propertyName).setPropertyValue(this, pageResource, comp, propertyName, value);
    }

    /**
   * Get the event mask attribute
   * @param propertyName the component property name
   * @return the value
   */
    public long getEventMask(Object comp, String propertyName) {
        return ((EventProperty) propertyTypes.get(propertyName)).getEventMask();
    }

    /**
   * Get the event class attribute
   * @param propertyName the component property name
   * @return the value
   */
    public Class getEventClass(Object comp, String propertyName) {
        return ((EventProperty) propertyTypes.get(propertyName)).getEventClass();
    }

    /**
   * Get the items to display for a list property
   * @param propertyName the property name
   * @return an array of list items
   */
    public String[] getListItems(Object comp, String propertyName) {
        return ((ListProperty) propertyTypes.get(propertyName)).getListItems(this, comp);
    }

    /**
   * Check if this component allows children to be added.
   * @return true to allow addition of children. By default false is returned
   * as most components do not intend to allow addition of children
   */
    public boolean allowsChildren() {
        return allowsChildren;
    }

    /**
   * Set the component allows children flag
   * @param state true to allow addition of children. By default false is returned
   */
    public void setAllowsChildren(boolean state) {
        allowsChildren = state;
    }

    /**
   * Flag the component as having a restricted size if true is returned
   * @return false
   */
    public boolean restrictsSize() {
        return restrictsSize;
    }

    /**
   * Set the flag for restricted size
   * @param state true for restricted size
   */
    public void setRestrictsSize(boolean state) {
        restrictsSize = state;
    }

    /**
   * Sets the flag to indicate if this is a swing component helper?
   * @param iss true if it is a swing helper
   */
    public void setSwing(boolean iss) {
        isSwing = iss;
    }

    /**
   * Sets the flag to indicate if this is component uses a content file
   * @param ucf true if it is a swing helper
   */
    public void setUsesContentFile(boolean ucf) {
        usesContentFile = ucf;
    }

    /**
   * Is this a swing component helper?
   * @return true if it is a swing helper
   */
    public boolean isSwing() {
        return isSwing;
    }

    /**
   * Is this a list property
   */
    public boolean isListProperty(String propertyName) {
        return (propertyTypes.get(propertyName) instanceof ListProperty);
    }

    /**
   * Is this a lang property
   */
    public boolean isLangProperty(String propertyName) {
        return (propertyTypes.get(propertyName) instanceof LangProperty);
    }

    /**
   * Is this helper's component oen that supports layouts?
   * @return false  by default
   */
    public boolean isLayoutOwner() {
        return false;
    }

    /**
   * Is the property read-only?
   * @param propertyName the name of the selected property
   * @return true if the property is read-only
   */
    public boolean isReadOnly(String propertyName) {
        if (propertyName.compareTo("Type") == 0) return true; else return false;
    }

    /**
   * Calls the relevant component helper to set the attributes of a new component
   * when a paste operation has been invoked from the main menu.
   * @param srcComp the component which was selected when the copy or cut command
   * was invoked
   * @param targetComp the new component which will be added to the current page
   */
    public void setCopiedProperties(PageResource page, Component srcComp, Component targetComp) {
        for (int i = 0; i < getNumProperties(srcComp); i++) {
            String propertyName = properties.get(i);
            if (propertyName.equals("X") || propertyName.equals("Y")) continue;
            if ((getPropertyType(propertyName) != PlainProperty.DATA_PROPERTY) && getIsCloneable(propertyName)) setPropertyValue(page, targetComp, propertyName, getPropertyValue(page, srcComp, propertyName));
        }
    }

    /**
   * Determine whether the passed property should be copied in a paste operation
   * @param propName The name of the property
   * @return boolean indicating whether the property should be copied
   */
    private boolean getIsCloneable(String propName) {
        if (propName.compareTo("Name") == 0) return false; else {
            PlainProperty property = getProperty(propName);
            if (property != null) return property.getIsCloneable();
        }
        return true;
    }

    /**
   * Get the file extension set to be used for file/image names in the popup chooser
   * @return an array of strings
   */
    public String[] getFileExtensions() {
        return extensions;
    }

    /**
   * Get a description of the file extension set to be used for file/image names in the popup chooser
   * @return an array of strings
   */
    public String getFileDescription() {
        return fileTypeDesc;
    }

    public String getDefaultExtension() {
        return defExt;
    }

    /**
   * Set the array/list of extensions used by the file/image name chooser
   * @param desc the description of the file type.
   * @param defaultExt the default file extension
   * @param newExtensions
   */
    public void setFileExtensions(String desc, String defaultExt, String[] newExtensions) {
        fileTypeDesc = desc;
        defExt = defaultExt;
        extensions = newExtensions;
    }

    /**
   * Get the expression value of this property if one exists
   * @param propertyName the name of the property
   * @return the expression or null
   */
    public String getExpression(Object comp, String propertyName) {
        if (comp instanceof JComponent) {
            JComponent component;
            if (comp instanceof ComponentProxy) component = (JComponent) ((ComponentProxy) comp).getProxiedComponent(); else component = (JComponent) comp;
            return (String) component.getClientProperty(propertyName);
        }
        return null;
    }

    /**
   * Set the expression value for this property
   * @param comp the component instance
   * @param propertyName the name of the property
   * @param expression the new expression
   * @return true if the value is an expression and it is stored, otherwise false is returned
   */
    public boolean setExpression(Object comp, String propertyName, Object expression) {
        if (expression == null) return false;
        String strValue = expression.toString().trim();
        if (strValue.indexOf(";") > 0) return false;
        if (comp instanceof JComponent) {
            JComponent component;
            if (comp instanceof ComponentProxy) component = (JComponent) ((ComponentProxy) comp).getProxiedComponent(); else component = (JComponent) comp;
            if (strValue.startsWith("${") && strValue.endsWith("}")) {
                component.putClientProperty(propertyName, expression);
                return true;
            } else component.putClientProperty(propertyName, null);
        }
        return false;
    }
}
