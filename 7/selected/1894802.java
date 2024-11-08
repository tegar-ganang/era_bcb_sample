package jmms.processor.gui;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import jmms.processor.Actuator;
import jmms.processor.ActuatorContainer;
import jmms.processor.ActuatorContainerManager;

/**
 * @author wooller
 *
 *19/01/2005
 *
 * Copyright JEDI/Rene Wooller
 *
 */
public class ActuatorTreeModel implements TreeModel {

    private ActuatorContainerManager acm;

    /**
	 * 
	 */
    public ActuatorTreeModel() {
        super();
    }

    public void setActuatorContainerManager(ActuatorContainerManager acm) {
        this.acm = acm;
    }

    public Object getRoot() {
        return acm;
    }

    public int getChildCount(Object arg0) {
        if (arg0 instanceof ActuatorContainer) return ((ActuatorContainer) arg0).getActuatorContainerCount(); else return 0;
    }

    public boolean isLeaf(Object arg0) {
        if (this.getChildCount(arg0) > 0) return false; else return true;
    }

    private TreeModelListener[] tmls = new TreeModelListener[30];

    private int tcount = 0;

    public void addTreeModelListener(TreeModelListener arg0) {
        tmls[tcount++] = arg0;
    }

    public void removeTreeModelListener(TreeModelListener arg0) {
        for (int i = 0; i < tcount; i++) {
            if (tmls[i] == arg0) {
                tmls[i] = tmls[i + 1];
                arg0 = tmls[i];
            }
        }
        tcount--;
    }

    public Object getChild(Object arg0, int arg1) {
        return ((ActuatorContainer) arg0).getSubActuatorContainers()[arg1];
    }

    public int getIndexOfChild(Object p, Object c) {
        if (p == null || c == null) return -1;
        return ((ActuatorContainer) p).getIndexOfSubContainer((ActuatorContainer) c);
    }

    public void valueForPathChanged(TreePath path, Object changed) {
        if (path.getLastPathComponent().toString().equals(changed.toString())) return; else {
            ((ActuatorContainer) path.getLastPathComponent()).setName(changed.toString());
        }
    }
}
