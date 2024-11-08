package org.jxpl.bindings;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.jxpl.Processor;
import org.jxpl.exception.*;
import java.util.Set;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Collection;
import java.util.Vector;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.w3c.dom.*;
import javax.xml.parsers.*;

/**
*   Extends Jxpl to include set based data structures
*
*   In this implementation, the hashtable will use the atom as 
*   the key and the value of the key is the number of occurances in 
*   the set.
*   <br><br>
*   Example:      key:  "a"   value: [13 occurances, option 1, option 2, option 3]
*   <br><br>
*   Multiset also implements support for ordering (but is costly so use only as needed).
*
*   @author Eric Harris and Jeff Brown
*   @version 0.1
*/
public class JxplMultiset extends JxplElement implements Cloneable, java.io.Serializable, Set {

    protected Map atoms;

    protected final int svgHeight = 30;

    protected final int svgWidth = 100;

    protected boolean ordered = false;

    /**
    *   Creates either an ordered or un-ordered multiset.
    *   @param ordering Toggles Ordered or not-ordered
    *   
    */
    public JxplMultiset(boolean ordering) {
        if (ordering) {
            ordered = true;
            atoms = new LinkedHashMap();
        } else {
            atoms = new Hashtable();
        }
    }

    /**
    *   Default constructor - creates un-ordered multiset
    */
    public JxplMultiset() {
        this(false);
    }

    /**
    *   Private Constructor for non-ordered multiset
    */
    private JxplMultiset(LinkedHashMap t) {
        atoms = t;
    }

    /**
    *   Private Constructor for non-ordered multiset
    */
    private JxplMultiset(Hashtable t) {
        atoms = t;
    }

    /**
     * Changed - we are not type checking at DOM level, so we need
     * a simpler structure.
     * JxplElement should have attribute 'number' that gives count
     */
    public JxplMultiset(Element in) throws JxplMalformedException {
        super(in);
        try {
            if (in.getAttribute("ordered") != null && in.getAttribute("ordered").equalsIgnoreCase("true")) {
                atoms = new LinkedHashMap();
            } else atoms = new Hashtable();
            Vector children = Util.getChildElements(in);
            for (int i = 0; i < children.size(); i++) {
                Element element = (Element) children.get(i);
                JxplElement jelem = Util.unmarshal(element);
                String num = jelem.getAttribute("number");
                int n = 1;
                if (num != null) n = Integer.parseInt(num);
                add(jelem, n);
            }
        } catch (Exception ex) {
            throw new JxplMalformedException("XML Malformed " + ex.getMessage());
        }
    }

    /**
    *   Adds one more occurance of atom to the set
    */
    public boolean add(Object atom) {
        add((JxplElement) atom, 1);
        return true;
    }

    public boolean add(List list) throws JxplMalformedException {
        int i = 0;
        while (i < list.size()) {
            JxplElement atom = null;
            atom = (JxplElement) list.get(i);
            i++;
            if (i < list.size()) {
                if (list.get(i) instanceof JxplInteger) add(atom, ((JxplInteger) list.get(i)).getValue().intValue()); else if (list.get(i) instanceof JxplList) add(atom, (JxplList) list.get(i));
            }
            i++;
        }
        return true;
    }

    /**
    *   Add member with a list of properties
    */
    public boolean add(JxplElement member, JxplList properties) {
        Vector props = properties.getElements();
        int[] propa = new int[props.size()];
        for (int j = 0; j < propa.length; j++) propa[j] = ((JxplInteger) props.get(j)).getValue().intValue();
        setArray(member, propa);
        return true;
    }

    /**
    *   Add Special Properties to an element
    */
    public void setProperties(JxplElement atom, int[] properties) {
        int[] a = (int[]) atoms.get(atom);
        for (int i = 0; i < properties.length && i < a.length - 1; i++) a[i + 1] = properties[i];
    }

    /**
     * Set the entire multiplicity array
     */
    public void setArray(JxplElement atom, int[] mults) {
        atoms.put(atom, mults);
    }

    /**
    *   Retreive special properties 
    */
    public int[] getProperties(JxplElement atom) {
        int[] a, properties;
        properties = null;
        a = (int[]) atoms.get(atom);
        if (a != null) {
            properties = new int[a.length - 1];
            for (int i = 0; i < properties.length; i++) properties[i] = a[i + 1];
        }
        return properties;
    }

    /**
     * Retrieve the entire int[] associated with atom
     */
    public int[] getArray(JxplElement atom) {
        return (int[]) atoms.get(atom);
    }

    /**
    *   Returns count of how many atoms of this type exist
    */
    public int getCount(JxplElement atom) {
        Object o = atoms.get(atom);
        return ((int[]) o)[0];
    }

    /**
    *   Adds an element to the list
    */
    public boolean add(JxplElement atom, int multiplicity) {
        if (multiplicity > 0) {
            int[] temp = (int[]) atoms.get(atom);
            if (temp != null) {
                temp[0] += multiplicity;
            } else {
                int[] a = new int[1];
                a[0] = multiplicity;
                atoms.put(atom, a);
            }
        }
        return true;
    }

    /**
    *   Add all elements of the collection to the set
    *   Doesn't work in general case of collection...must requires JxplMultiset
    *   FIXME set iterator
    */
    public boolean addAll(Collection c) {
        Iterator itr = c.iterator();
        while (itr.hasNext()) {
            JxplElement atom = (JxplElement) itr.next();
            add(atom, ((JxplMultiset) c).getCount(atom));
        }
        return true;
    }

    /**
    *   Contains Atom
    */
    public boolean contains(Object atom) {
        return atoms.containsKey(atom);
    }

    /**
    *   Contains all occurances of the following atoms
    */
    public boolean containsAll(Collection c) {
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            JxplElement el = (JxplElement) iter.next();
            if (!contains(el)) return false;
            if (c instanceof JxplMultiset) if (getCount(el) < ((JxplMultiset) c).getCount(el)) return false;
        }
        return true;
    }

    /**
    *  Is the Set Empty
    */
    public boolean isEmpty() {
        return atoms.isEmpty();
    }

    /**
    *   Returns the hashCode for this Set
    */
    public int hashCode() {
        return atoms.hashCode();
    }

    /**
    *   Return the set in the form of an xml element
    */
    public Element getElement(Document doc) {
        return getElement(doc, false);
    }

    /**
    *   GetElement
    */
    public Element getElement(Document doc, boolean rootElement) {
        Element out = doc.createElementNS(org.jxpl.Processor.namespaceURI, "jxpl:multiset");
        if (rootElement) out.setAttribute("xmlns:jxpl", org.jxpl.Processor.namespaceURI);
        Iterator itr = iterator();
        if (attributes != null) {
            java.util.Enumeration attr = attributes.keys();
            while (attr.hasMoreElements()) {
                String key = (String) attr.nextElement();
                if (!key.equalsIgnoreCase("xmlns:jxpl")) out.setAttribute(key, (String) attributes.get(key));
            }
        }
        while (itr.hasNext()) {
            JxplElement atom = (JxplElement) itr.next();
            int c = getCount(atom);
            String acount = Integer.toString(c);
            atom.setAttribute("number", acount);
            out.appendChild(atom.getElement(doc));
        }
        return out;
    }

    /**
    *   Remove a single occurance of the atom
    */
    public boolean remove(Object atom) {
        return remove((JxplElement) atom, 1);
    }

    /**
    *   Remove All Occurances of this Atom
    */
    public boolean removeAll(Object atom) {
        atoms.remove(atom);
        return true;
    }

    public boolean remove(List list) throws JxplMalformedException {
        int i = 0;
        boolean successOrFail = false;
        while (i < list.size()) {
            JxplElement atom = null;
            atom = (JxplElement) list.get(i);
            i++;
            if (i < list.size()) {
                if (list.get(i) instanceof JxplInteger) successOrFail = remove(atom, ((JxplInteger) list.get(i)).getValue().intValue());
            }
            i++;
        }
        return successOrFail;
    }

    /**
    *   Remove atom from list...decrement the count  
    */
    public boolean remove(JxplElement atom, int multiplicity) {
        int[] a = (int[]) atoms.get(atom);
        if (a != null) {
            if (a[0] - multiplicity > 0) a[0] -= multiplicity; else {
                atoms.remove(atom);
            }
            return true;
        } else return false;
    }

    /**
    *  Remove elements of the collection from the Set
    */
    public boolean removeAll(Collection c) {
        return removeAll((JxplMultiset) c);
    }

    public boolean removeAll(JxplMultiset c) {
        Iterator itr = c.iterator();
        while (itr.hasNext()) {
            JxplElement next = (JxplElement) itr.next();
            if (!remove(next, c.getCount(next))) return false;
        }
        return true;
    }

    /**
    *   Clear all data in the set
    */
    public void clear() {
        if (isOrdered()) atoms = new LinkedHashMap(); else atoms = new Hashtable();
    }

    /**
    *  This operation is optional and therefore is not implemented
    */
    public boolean retainAll(Collection c) {
        return false;
    }

    /**
    *   FIXME this should probably go through an iteration for every atom
    *   return  Iterator for searching through the keys
    */
    public Iterator iterator() {
        return atoms.keySet().iterator();
    }

    /**
    *  FIXME should this return repetitions of elements based on multiplicity?  Currently it just returns an array of every unique element
    */
    public Object[] toArray() {
        return atoms.keySet().toArray();
    }

    /**
    *   
    */
    public Object[] toArray(Object[] array) {
        return atoms.keySet().toArray(array);
    }

    /**
    *   Returns the total number of members in the set (will soon be applying the multiplicities)
    *   @return Number of members
    */
    public int size() {
        return atoms.keySet().size();
    }

    /**
    *   Total size of this multiset with the multiplicites computed
    */
    public int totalSize() {
        int size = 0;
        Iterator itr = iterator();
        while (itr.hasNext()) {
            JxplElement atom = (JxplElement) itr.next();
            size += getCount(atom);
        }
        return size;
    }

    /**
    * @deprecated not recommended as is it costly in terms of time and resources
    */
    public Object clone() {
        if (isOrdered()) return new JxplMultiset((LinkedHashMap) new LinkedHashMap(atoms).clone()); else return new JxplMultiset((Hashtable) new Hashtable(atoms).clone());
    }

    /**
    *   Determines if the multiset is ordered or not
    */
    public boolean isOrdered() {
        return ordered;
    }
}
