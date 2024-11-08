package de.jassda.modules.trace.reflect;

import de.jassda.modules.trace.event.JdiMapping;
import de.jassda.modules.trace.parser.Token;
import de.jassda.modules.trace.parser.Node;
import de.jassda.util.log.Log;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * Class Container
 *
 *
 * @author Mark Broerkens
 * @version %I%, %G%
 */
public class Container {

    Container parent;

    String name = null;

    Hashtable eventSets = new Hashtable();

    Position from, to;

    static int objectCounter = 0;

    Vector children = new Vector();

    public Container() {
        this.parent = null;
    }

    /**
     * Constructor Container
     *
     *
     * @param parent
     *
     */
    public Container(Container parent) {
        this.parent = parent;
    }

    /**
     * Constructor Container
     *
     *
     * @param parent
     * @param node
     *
     */
    public Container(Container parent, Node node) {
        this.parent = parent;
        objectCounter++;
    }

    /**
     * Method addChannel
     *
     *
     * @param eventSet
     *
     * @return
     *
     */
    public Object addChannel(EventSetDefinitionContainer eventSet) {
        return eventSets.put(eventSet.getFullName(), eventSet);
    }

    /**
     * Method toString
     *
     *
     * @return
     *
     */
    public String toString() {
        return name;
    }

    /**
     * Method setName
     *
     *
     * @param name
     *
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Method getNames
     *
     *
     * @return
     *
     */
    public List getNames() {
        Vector names = new Vector();
        if (parent != null) {
            names.addAll(parent.getNames());
        }
        if (name != null) {
            names.add(name);
        }
        return names;
    }

    /**
     * Method getName
     *
     *
     * @return
     *
     */
    public String getveryOldName() {
        StringBuffer buffer = new StringBuffer();
        if (parent != null) {
            buffer.append(parent.getveryOldName());
            if (name != null) {
                buffer.append(".");
                buffer.append(name);
            }
        } else {
            buffer.append(name);
        }
        return buffer.toString();
    }

    /**
     * Method getParent
     *
     *
     * @return
     *
     */
    public Container getParent() {
        return parent;
    }

    /**
     * Method getChannelDefinitions
     *
     *
     * @return
     *
     */
    public Hashtable getChannelDefinitions() {
        Hashtable channelDefinitions;
        if (parent != null) {
            channelDefinitions = parent.getChannelDefinitions();
        } else {
            channelDefinitions = new Hashtable();
        }
        return channelDefinitions;
    }

    /**
     * Method getSpecificationName
     *
     *
     * @return
     *
     */
    public String getSpecificationName() {
        if (parent != null) {
            return parent.getSpecificationName();
        } else {
            return null;
        }
    }

    /**
     * Method getTraceName
     *
     *
     * @return
     *
     */
    public String getTraceName() {
        if (parent != null) {
            return parent.getTraceName();
        } else {
            return null;
        }
    }

    /**
     * Method getProcessName
     *
     *
     * @return
     *
     */
    public String getProcessName() {
        if (parent != null) {
            return parent.getProcessName();
        } else {
            return null;
        }
    }

    /**
     * Method getEventSetName
     *
     *
     * @return
     *
     */
    public String getEventSetName() {
        if (parent != null) {
            return parent.getEventSetName();
        } else {
            return null;
        }
    }

    /**
     * Method getName
     *
     *
     * @return
     *
     */
    public String getName() {
        return name;
    }

    /**
     * Method getFullName
     *
     *
     * @return
     *
     */
    public String getFullName() {
        Vector names = new Vector();
        String specification = getSpecificationName();
        if (specification != null) {
            names.add(specification);
        }
        String trace = getTraceName();
        if (trace != null) {
            names.add(trace);
        }
        String process = getProcessName();
        if (process != null) {
            names.add(process);
        }
        String eventSet = getEventSetName();
        if (eventSet != null) {
            names.add(eventSet);
        }
        StringBuffer buffer = new StringBuffer();
        if (names.size() > 0) {
            buffer.append(names.get(0));
            for (int i = 1; i < names.size(); i++) {
                buffer.append(".");
                buffer.append(names.get(i));
            }
        }
        return buffer.toString();
    }

    /**
     * Method update
     *
     *
     */
    public void update() {
    }

    public void setParent(Container parent) {
        this.parent = parent;
    }

    public void addChild(Container container) {
        this.children.add(container);
    }

    public Vector getChildren() {
        return this.children;
    }

    public void clearChildren() {
        this.children.clear();
    }
}
