package jmms.processor;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import ren.util.Err;
import ren.util.PO;
import ren.util.Save;

/**
 * @author wooller
 * 
 * 18/01/2005
 * 
 * Copyright JEDI/Rene Wooller
 *  
 */
public class ActuatorContainerManager implements ActuatorContainer, Serializable {

    private ActuatorContainer[] acs = new ActuatorContainer[50];

    private int acCount = 0;

    public static ActuatorContainerManager acm;

    /**
     *  
     */
    public ActuatorContainerManager() {
        super();
        acm = this;
    }

    public void registerRoot(ActuatorContainer toReg) {
        this.acs[acCount++] = toReg;
    }

    public void removeRoot(ActuatorContainer root) {
        for (int i = 0; i < acCount; i++) {
            if (acs[i] == root) {
                acs[i] = acs[i + 1];
                root = acs[i];
            }
        }
        acCount--;
    }

    /**
     * (non-Javadoc)
     * 
     * @see jmms.processor.ActuatorContainer#getSubActuatorContainers()
     */
    private transient ActuatorContainer[] aca;

    public ActuatorContainer[] getSubActuatorContainers() {
        aca = new ActuatorContainer[acCount];
        for (int i = 0; i < aca.length; i++) {
            aca[i] = acs[i];
        }
        return aca;
    }

    public Actuator[] getActuators() {
        return null;
    }

    public int getActuatorContainerCount() {
        return acCount;
    }

    public int getIndexOfSubContainer(ActuatorContainer sub) {
        int toRet = -1;
        for (int i = 0; i < acCount; i++) {
            if (aca[i] == sub) {
                toRet = i;
                break;
            }
        }
        return toRet;
    }

    public ActuatorContainer getActuatorContainer(ActuatorContainer[] a, String s) {
        for (int i = 0; i < a.length; i++) {
            if (a[i].toString().equalsIgnoreCase(s)) return a[i];
        }
        return null;
    }

    public Actuator getActuator(Actuator[] a, String s) {
        if (a == null || s == null) return null;
        for (int i = 0; i < a.length; i++) {
            if (a[i].toString().equalsIgnoreCase(s)) return a[i];
        }
        return null;
    }

    private String name = "Root";

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return this.getName();
    }

    private Hashtable unfinPaths = new Hashtable();

    /**
     * should be called before loading
     */
    public void resetUnfinishedPaths() {
        unfinPaths.clear();
    }

    /**
     * Check all the different loading algorithms
     * 
     * 
     * @param ap
     * @param s
     * @return true if the loading was sucessful, false
     *         otherwise
     */
    public boolean loadActuatorPath(ActuatorPath ap, String s) {
        ActuatorContainer[] ar = loadContainerPath(s);
        if (ap == null) {
            if (!unfinPaths.containsValue(ap)) unfinPaths.put(s, ap); else PO.p("actuator path is null");
            return false;
        }
        if (ar == null) {
            if (!unfinPaths.containsValue(ap)) unfinPaths.put(s, ap); else PO.p("the list of actuator containers is null");
            return false;
        }
        Actuator ac = loadActuator(ar, s);
        if (ac == null) {
            if (!unfinPaths.containsValue(ap)) unfinPaths.put(s, ap); else PO.p("the list of actuators is null");
            return false;
        }
        ap.setPath(ar, ac);
        return true;
    }

    /**
     * By the time this is called, all the actuators and
     * actuator containers that could be loaded should
     * have been loaded, and so the unfinished paths
     * will be able to be loaded.
     *  
     */
    public void loadUninishedPaths() {
        Enumeration e = unfinPaths.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            if (loadActuatorPath((ActuatorPath) unfinPaths.get(key), (String) key) == false) Err.e("unable to load the unfinished path " + (String) key);
        }
    }

    /**
     * Note - if this method returns null, it means that
     * the string given cannot be found. this may be
     * because the actuator that it is linked to hasn't
     * been loaded yet.
     * 
     * For actuatorPaths this problem is handled here
     * 
     * @param s
     * @return
     * @see loadActuatorPath
     */
    public ActuatorContainer[] loadContainerPath(String s) {
        s = Save.getStringXML(s, ActuatorPath.psn);
        String[] sarr = s.split(Save.et(ActuatorContainer.sn));
        ActuatorContainer[] ret = new ActuatorContainer[sarr.length];
        ret[0] = this;
        if (!sarr[0].substring(Save.st(ActuatorContainer.sn).length()).equalsIgnoreCase(this.name)) {
            Err.e("root is not pecified.. may be an error in the data:  " + s);
        }
        for (int i = 1; i < ret.length; i++) {
            ret[i] = getActuatorContainer(ret[i - 1].getSubActuatorContainers(), sarr[i].substring(Save.st(ActuatorContainer.sn).length()));
            if (ret[i] == null) {
                return null;
            }
        }
        return ret;
    }

    /**
     * takes format: <Actuator.sn>nameOfActuator
     * </Actuator.sn>
     * 
     * @param path
     * @param a
     * @return
     */
    public Actuator loadActuator(ActuatorContainer[] path, String a) {
        a = Save.getStringXML(a, Actuator.sn);
        ActuatorContainer acon = path[path.length - 1];
        return this.getActuator(path[path.length - 1].getActuators(), a);
    }
}
