package org.biomage.tools.xmlutils;

import org.biomage.tools.xmlutils.MultiMap;
import org.biomage.tools.xmlutils.MultiHashMap;
import org.biomage.BioAssayData.DataInternal;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import org.biomage.Common.*;
import org.biomage.tools.helpers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * <b>Description:</b>
 *      Content handler for MAGE.
 *
 */
public class MAGEContentHandler extends DefaultHandler {

    protected MAGEJava mageJava = createMAGEJava();

    protected Map identifierMap = createIdentifierMap();

    protected Map classConstructorMap = createClassConstructorMap();

    protected Map methodsMap = createMethodsMap();

    protected Stack objectStack = createObjectStack();

    protected Stack associationStack = createAssociationStack();

    protected LinkedList unresolvedRefList = createUnresolvedRefList();

    protected PCDataImpl pcDataStack = null;

    protected boolean lastDocument = true;

    protected HashMap elementSubstitutions = null;

    protected AggrBiDirectionalAssocMap aggrBiDiMap = new AggrBiDirectionalAssocMap();

    public MAGEContentHandler() {
        elementSubstitutions = new HashMap(2);
        elementSubstitutions.put("ExperimentDesign_assn", "ExperimentDesigns_assnlist");
        elementSubstitutions.put("MerckIndex_assn", "CompoundIndices_assnlist");
    }

    /**
   * Get the finished MAGE top-level object
   */
    public MAGEJava getMAGEJava() {
        return mageJava;
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
    }

    public void setLastDocument(boolean b) {
        lastDocument = b;
    }

    public boolean getLastDocument() {
        return lastDocument;
    }

    public void endDocument() throws SAXException {
        if (!lastDocument) {
            return;
        } else {
            try {
                StringOutputHelpers.writeOutput("Fixing unresolved references: Count = " + unresolvedRefList.size(), 3);
                ListIterator iter = unresolvedRefList.listIterator(0);
                while (iter.hasNext()) {
                    UnresolvedRef uRef = (UnresolvedRef) iter.next();
                    Object refdObj = identifierMap.get(uRef.childIdentifier);
                    if (refdObj != null) {
                        makeAssociation(uRef.associationType, uRef.parent, refdObj);
                    } else {
                        StringOutputHelpers.writeOutput("A reference ( refid = " + uRef.childIdentifier + " ) to a " + uRef.childType + " not found!", 3);
                        AttributesImpl idAttr = new AttributesImpl();
                        idAttr.addAttribute("", "identifier", "identifier", "", uRef.childIdentifier);
                        createMAGEObject(uRef.childType, (Attributes) idAttr);
                        Extendable newObj = (Extendable) objectStack.pop();
                        NameValueType nvt = new NameValueType();
                        nvt.setName("Placeholder");
                        newObj.addToPropertySets(nvt);
                        StringOutputHelpers.writeOutput("Popping " + newObj.getClass().getName() + " from object stack", 3);
                        makeAssociation(uRef.associationType, uRef.parent, newObj);
                    }
                }
                StringOutputHelpers.writeOutput("Dangling object count: " + objectStack.size(), 3);
                Vector pckgList = new Vector();
                while (!objectStack.empty()) {
                    pckgList.add(objectStack.pop());
                }
                int i = 0;
                while (i < pckgList.size()) {
                    Object pckg1 = pckgList.get(i);
                    int j = i + 1;
                    while (j < pckgList.size()) {
                        Object pckg2 = pckgList.get(j);
                        if (!pckg1.getClass().getName().endsWith("String") && pckg1.getClass().getName().equals(pckg2.getClass().getName())) {
                            mergePckgs(pckg1, pckg2);
                            pckgList.remove(j);
                        } else {
                            j++;
                        }
                    }
                    i++;
                }
                for (int j = pckgList.size() - 1; j >= 0; j--) {
                    objectStack.push(pckgList.get(j));
                }
                while (!objectStack.empty()) {
                    Object pkgObj = objectStack.pop();
                    String associationName = pkgObj.getClass().getName();
                    int index = associationName.lastIndexOf(".");
                    if (index > 0) {
                        associationName = associationName.substring(index + 1);
                    }
                    StringOutputHelpers.writeOutput("now association name = " + associationName, 3);
                    makeAssociation(new Association(associationName, "assn"), mageJava, pkgObj);
                }
            } catch (Exception e) {
                System.err.println("Caught exception in endDocument: " + e);
                e.printStackTrace();
            }
        }
    }

    protected void mergePckgs(Object pckg1, Object pckg2) {
        try {
            Field[] pckgFields = pckg1.getClass().getFields();
            for (int i = 0; i < pckgFields.length; i++) {
                Method getMethod = pckg1.getClass().getMethod("get" + StringOutputHelpers.initialCap(pckgFields[i].getName()), null);
                Object container1 = getMethod.invoke(pckg1, null);
                Object container2 = getMethod.invoke(pckg2, null);
                Method sizeMethod = container1.getClass().getMethod("size", null);
                int sizeContainer1 = ((Integer) sizeMethod.invoke(container1, null)).intValue();
                int sizeContainer2 = ((Integer) sizeMethod.invoke(container2, null)).intValue();
                if (sizeContainer1 == 0) {
                    if (sizeContainer2 > 0) {
                        Method setMethod = pckg1.getClass().getMethod("set" + StringOutputHelpers.initialCap(pckgFields[i].getName()), new Class[] { container2.getClass() });
                        setMethod.invoke(pckg1, new Object[] { container2 });
                    } else {
                    }
                } else {
                    if (sizeContainer2 > 0) {
                        Method getElementMethod = pckg1.getClass().getMethod("getFrom" + StringOutputHelpers.initialCap(pckgFields[i].getName()), new Class[] { int.class });
                        int index = pckgFields[i].getName().lastIndexOf('_');
                        String elementType = new String(pckg1.getClass().getPackage().getName() + "." + StringOutputHelpers.initialCap(pckgFields[i].getName().substring(0, index)));
                        Method setElementMethod = pckg1.getClass().getMethod("addTo" + StringOutputHelpers.initialCap(pckgFields[i].getName()), new Class[] { Class.forName(elementType) });
                        Object element = null;
                        int j = 0;
                        while (j < sizeContainer2) {
                            element = getElementMethod.invoke(pckg2, new Object[] { new Integer(j) });
                            setElementMethod.invoke(pckg1, new Object[] { element });
                            j++;
                        }
                    } else {
                    }
                }
            }
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes atts) throws SAXException {
        try {
            String tagPrefix = localName;
            String tagSuffix = null;
            int index = 0;
            Object parent = null;
            if (localName.equals("")) {
                localName = qualifiedName;
            }
            if (elementSubstitutions.containsKey(localName)) {
                String replacement = (String) elementSubstitutions.get(localName);
                localName = replacement;
                qualifiedName.replaceFirst(localName, replacement);
            }
            StringOutputHelpers.writeOutput("startElement: " + localName, 1);
            if (!objectStack.empty()) {
                parent = objectStack.peek();
            }
            if ((index = localName.indexOf("_")) > -1) {
                tagPrefix = localName.substring(0, index);
                tagSuffix = localName.substring(index + 1);
            }
            if ((tagSuffix == null) || tagSuffix.equals("package")) {
                createMAGEObject(localName, atts);
                if ((parent != null) && !objectStack.isEmpty() && !associationStack.isEmpty()) {
                    makeAssociation((Association) associationStack.peek(), parent, objectStack.peek());
                }
            } else if (tagSuffix.equals("assn") || tagSuffix.equals("assnref") || tagSuffix.equals("assnlist") || tagSuffix.equals("assnreflist")) {
                StringOutputHelpers.writeOutput("Found ref.." + tagPrefix + ".pushing on stack " + tagSuffix, 3);
                associationStack.push(new Association(tagPrefix, tagSuffix));
            } else {
                int identIndex = atts.getIndex("identifier");
                if (identIndex == -1) {
                    System.err.println("Hey. startElement is starting an element with no identifier attribute.  Returning now.");
                    return;
                }
                Object newChild = identifierMap.get(atts.getValue(identIndex));
                if (newChild == null) {
                    StringOutputHelpers.writeOutput("Cannot resolve...storing unresolved ref for later.", 3);
                    unresolvedRefList.add(new UnresolvedRef((Association) associationStack.peek(), parent, atts.getValue(identIndex), tagPrefix));
                } else {
                    StringOutputHelpers.writeOutput("Located ref'd object by identifier map!", 3);
                    makeAssociation((Association) associationStack.peek(), parent, newChild);
                }
            }
            parent = null;
            if (localName.equals("DataInternal")) {
                pcDataStack = new PCDataImpl();
                DataInternal pcDataContainingElement = (DataInternal) objectStack.peek();
                if (pcDataContainingElement != null) {
                    pcDataContainingElement.setPcData(pcDataStack);
                } else {
                    throw new SAXException("No owner object of PCDATA found ...");
                }
            }
        } catch (Exception e) {
            System.err.println("Caught exception in startElement: " + e);
            e.printStackTrace();
        }
    }

    /**
   * Method to make an association between object via reflection.
   */
    public void makeAssociation(Association association_info, Object parent, Object child) throws SAXException {
        Method set_method = null;
        String method_name;
        StringOutputHelpers.writeOutput("makeAssociation:", 1);
        StringOutputHelpers.writeOutput("  Parent type = " + parent.getClass().getName(), 2);
        StringOutputHelpers.writeOutput("  Child type = " + child.getClass().getName(), 2);
        StringOutputHelpers.writeOutput("  Association name = " + association_info.name, 2);
        StringOutputHelpers.writeOutput("  Association type = " + association_info.type, 2);
        if (association_info.type.equals("assn") || association_info.type.equals("assnref")) {
            method_name = "set" + StringOutputHelpers.initialCap(association_info.name);
        } else {
            method_name = "addTo" + StringOutputHelpers.initialCap(association_info.name);
        }
        if (methodsMap.containsKey(method_name)) {
            Iterator methods_iterator = ((Set) methodsMap.get(method_name)).iterator();
            while (methods_iterator.hasNext()) {
                set_method = (Method) methods_iterator.next();
                if (set_method.getDeclaringClass().isInstance(parent)) {
                    break;
                } else {
                    set_method = null;
                }
            }
        }
        if (set_method == null) {
            StringOutputHelpers.writeOutput("    Trying to find method: " + method_name + "[_list]", 3);
            Method[] parent_methods = null;
            try {
                parent_methods = parent.getClass().getMethods();
            } catch (SecurityException e) {
                throw new SAXException(e);
            }
            for (int i = 0; i < parent_methods.length; i++) {
                StringOutputHelpers.writeOutput("      Checking method: " + parent_methods[i].getName(), 4);
                if ((parent_methods[i].getName().equals(method_name) || parent_methods[i].getName().equals(method_name + "_list")) && (parent_methods[i].getParameterTypes().length == 1)) {
                    set_method = parent_methods[i];
                    methodsMap.put(method_name, set_method);
                    break;
                }
            }
        }
        if (set_method == null) {
            StringOutputHelpers.writeOutput("    Method Not Found!", 2);
        } else {
            StringOutputHelpers.writeOutput("    Found method " + set_method.getName() + "!  Calling it...", 3);
            try {
                set_method.invoke(parent, new Object[] { child });
            } catch (IllegalAccessException e) {
                throw new SAXException(e);
            } catch (InvocationTargetException e) {
                throw new SAXException(e);
            } catch (IllegalArgumentException e) {
                throw new SAXException(e);
            }
        }
    }

    public void endElement(String namespaceURI, String localName, String qualifiedName) throws SAXException {
        try {
            if (localName.equals("")) {
                localName = qualifiedName;
            }
            if (elementSubstitutions.containsKey(localName)) {
                String replacement = (String) elementSubstitutions.get(localName);
                localName = replacement;
                qualifiedName.replaceFirst(localName, replacement);
            }
            StringOutputHelpers.writeOutput("endElement: " + localName, 1);
            if (localName.endsWith("_assn") || localName.endsWith("_assnlist") || localName.endsWith("_assnref") || localName.endsWith("_assnreflist")) {
                Association association_info = (Association) associationStack.pop();
                StringOutputHelpers.writeOutput("  Popped " + association_info.name + " from association stack", 3);
            } else if (localName.endsWith("_ref") || localName.endsWith("_package") || localName.equals("MAGE-ML")) {
            } else if (localName.indexOf("_") < 0) {
                Object objInfo = objectStack.pop();
                StringOutputHelpers.writeOutput("  Popped " + objInfo.getClass().getName() + " from object stack", 3);
            } else {
            }
            if (localName.equals("DataInternal")) {
                if (pcDataStack != null) {
                    pcDataStack = null;
                } else {
                    throw new SAXException("No object on pcDataStack when closing PCDATA containing element");
                }
            }
        } catch (Exception e) {
            System.err.println("Caught exception in endElement: " + e);
            e.printStackTrace();
        }
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void characters(char[] text, int start, int length) throws SAXException {
        Object parent;
        if (!objectStack.empty()) {
            parent = objectStack.peek();
        } else {
            parent = null;
        }
        if (parent != null && parent.getClass().getName().equals("org.biomage.BioAssayData.DataInternal")) {
            if (pcDataStack != null) {
                pcDataStack.appendChars(text, start, length);
            } else {
                throw new SAXException("No object on pcDataStack when receiving chars in a PCDATA containing element");
            }
        }
    }

    public void ignorableWhitespace(char[] text, int start, int length) throws SAXException {
        Object parent;
        if (!objectStack.empty()) {
            parent = objectStack.peek();
        } else {
            parent = null;
        }
        if (parent != null && parent.getClass().getName().equals("org.biomage.BioAssayData.DataInternal")) {
            if (pcDataStack != null) {
                pcDataStack.appendChars(text, start, length);
            } else {
                throw new SAXException("No object on pcDataStack when receiving whitespace in a PCDATA containing element");
            }
        }
    }

    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    protected String createMAGEObject(String localName, Attributes atts) throws SAXException {
        Class mageClass = mageJava.getClassWithModelName(localName);
        if (mageClass == null) {
            StringOutputHelpers.writeOutput("Don't know how to create an object " + " for " + localName + " yet! Skipping.", 3);
            objectStack.push(new String("localName"));
            throw new SAXException("Unrecognized element local name " + localName + ".");
        }
        int nIdentifierIndex = atts.getIndex("", "identifier");
        String sIdentifier = null;
        if (nIdentifierIndex != -1) {
            sIdentifier = atts.getValue(nIdentifierIndex);
        }
        Constructor constructor = (Constructor) classConstructorMap.get(mageClass);
        if (constructor == null) {
            StringOutputHelpers.writeOutput("Creating a class constructor " + localName, 3);
            try {
                constructor = mageClass.getConstructor(new Class[] { Attributes.class });
            } catch (NoSuchMethodException e) {
                throw new SAXException(e);
            } catch (SecurityException e) {
                throw new SAXException(e);
            }
            classConstructorMap.put(mageClass, constructor);
        }
        StringOutputHelpers.writeOutput("Calling the class constructor for " + localName, 3);
        Object object = null;
        try {
            object = constructor.newInstance(new Object[] { atts });
            if (localName.equalsIgnoreCase("MAGE-ML")) {
                mageJava = (MAGEJava) object;
            }
        } catch (IllegalAccessException e) {
            throw new SAXException(e);
        } catch (InstantiationException e) {
            throw new SAXException(e);
        } catch (InvocationTargetException e) {
            throw new SAXException(e);
        }
        String childClassName = object.getClass().getName();
        if (aggrBiDiMap.containChild(childClassName)) {
            Object parentObject = objectStack.peek();
            Class parentClass = parentObject.getClass();
            String parentClassName = parentClass.getName();
            try {
                if (aggrBiDiMap.getParentName(childClassName).equals(parentClassName)) {
                    String methodName = aggrBiDiMap.getSetMethodName(childClassName);
                    Method setMethod = object.getClass().getMethod(methodName, new Class[] { parentClass });
                    setMethod.invoke(object, new Object[] { parentObject });
                }
            } catch (NoSuchMethodException e) {
                throw new SAXException(e);
            } catch (IllegalAccessException e) {
                throw new SAXException(e);
            } catch (IllegalArgumentException e) {
                throw new SAXException(e);
            } catch (InvocationTargetException e) {
                throw new SAXException(e);
            }
        }
        if (sIdentifier != null) {
            if (identifierMap.containsKey(sIdentifier)) {
                StringOutputHelpers.writeOutput("WARNING: The identifier \"" + sIdentifier + "\" is already associated with an object.", 2);
            } else {
                identifierMap.put(sIdentifier, object);
            }
        }
        if (!localName.equalsIgnoreCase("MAGE-ML")) {
            objectStack.push(object);
        }
        return sIdentifier;
    }

    /**
   * Factory method to instantiate the MAGEJava object.
   */
    protected MAGEJava createMAGEJava() {
        return new MAGEJava();
    }

    /**
   * Factory method to instantiate the String->Object (identifiers->identified objects) Map.
   */
    protected Map createIdentifierMap() {
        return new HashMap();
    }

    /**
   * Factory method to instantiate the String->Method MultiMap.
   */
    protected MultiMap createMethodsMap() {
        return new MultiHashMap();
    }

    /**
   * Factory method to instantiate the Class->Constructor Map.
   */
    protected Map createClassConstructorMap() {
        return new HashMap();
    }

    /**
   * Factory method to instantiate the Associations Stack.
   */
    protected Stack createAssociationStack() {
        return new Stack();
    }

    /**
   * Factory method to instantiate the created Objects Stack.
   */
    protected Stack createObjectStack() {
        return new Stack();
    }

    protected LinkedList createUnresolvedRefList() {
        return new LinkedList();
    }

    /**
   * Basic structore to hold object association info.
   */
    class Association {

        protected String name;

        protected String type;

        public Association(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    /**
   * Basic structure to hold unresolved references.
   */
    protected class UnresolvedRef {

        protected Association associationType;

        protected Object parent;

        protected String childIdentifier;

        protected String childType;

        public UnresolvedRef(Association association_type, Object parent, String child_identifier, String child_type) {
            this.associationType = association_type;
            this.parent = parent;
            this.childIdentifier = child_identifier;
            this.childType = child_type;
        }
    }
}
