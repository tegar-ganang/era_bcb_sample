package gov.sns.tools.apputils.PVSelection;

import javax.swing.tree.*;
import java.awt.event.*;
import java.awt.*;
import gov.sns.ca.*;

public class PVTreeNode extends DefaultMutableTreeNode {

    private String name = null;

    private String old_name = null;

    private boolean itIsPVName = false;

    private boolean pvNamesAllowed = false;

    private boolean itIsSelected = false;

    private boolean itIsSwitchedOn = true;

    private boolean showCheckBox = true;

    private int childNumberLimit = Integer.MAX_VALUE;

    private Channel channel = null;

    private ActionListener switchOnOffListener = null;

    public static int SWITCHED_ON = 1;

    public static int SWITCHED_OFF = 0;

    public static String SWITCHED_ON_COMMAND = "on";

    public static String SWITCHED_OFF_COMMAND = "off";

    private ActionListener createRemoveListener = null;

    public static int CREATE_PV = 2;

    public static int REMOVE_PV = 3;

    public static String CREATE_PV_COMMAND = "create";

    public static String REMOVE_PV_COMMAND = "remove";

    private ActionListener renameListener = null;

    public static int RENAME_PV = 4;

    public static String RENAME_PV_COMMAND = "rename";

    private Color color = null;

    public PVTreeNode() {
        super();
    }

    public PVTreeNode(String name) {
        super();
        this.name = name;
    }

    public void setName(String name) {
        old_name = this.name;
        this.name = name;
        if (name != null && renameListener != null) {
            ActionEvent actionEvent = new ActionEvent(this, RENAME_PV, RENAME_PV_COMMAND);
            renameListener.actionPerformed(actionEvent);
        }
    }

    public String getOldName() {
        return old_name;
    }

    public String getName() {
        return name;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public boolean isPVName() {
        return itIsPVName;
    }

    public void setAsPVName(boolean itIsPVName) {
        this.itIsPVName = itIsPVName;
    }

    public boolean isPVNamesAllowed() {
        return pvNamesAllowed;
    }

    public void setPVNamesAllowed(boolean pvNamesAllowed) {
        this.pvNamesAllowed = pvNamesAllowed;
    }

    public void setPVNumberLimit(int limit) {
        childNumberLimit = limit;
    }

    public int getPVNumberLimit() {
        return childNumberLimit;
    }

    public boolean isSwitchedOn() {
        return itIsSwitchedOn;
    }

    public void setSwitchedOnOffListener(ActionListener switchOnOffListener) {
        this.switchOnOffListener = switchOnOffListener;
    }

    public ActionListener getSwitchedOnOffListener() {
        return switchOnOffListener;
    }

    public void setSwitchedOn(boolean itIsSwitchedOn) {
        this.itIsSwitchedOn = itIsSwitchedOn;
        if (switchOnOffListener != null) {
            ActionEvent actionEvent = null;
            if (itIsSwitchedOn) {
                actionEvent = new ActionEvent(this, SWITCHED_ON, SWITCHED_ON_COMMAND);
            } else {
                actionEvent = new ActionEvent(this, SWITCHED_OFF, SWITCHED_OFF_COMMAND);
            }
            switchOnOffListener.actionPerformed(actionEvent);
        }
    }

    public void setCheckBoxVisible(boolean showCheckBox) {
        this.showCheckBox = showCheckBox;
    }

    public boolean isCheckBoxVisible() {
        return showCheckBox;
    }

    public void setCreateRemoveListener(ActionListener createRemoveListener) {
        this.createRemoveListener = createRemoveListener;
    }

    public ActionListener getCreateRemoveListener() {
        return createRemoveListener;
    }

    public void creatingOccurred() {
        if (itIsPVName && createRemoveListener != null) {
            ActionEvent actionEvent = new ActionEvent(this, CREATE_PV, CREATE_PV_COMMAND);
            createRemoveListener.actionPerformed(actionEvent);
        }
    }

    public void removingOccurred() {
        if (itIsPVName && createRemoveListener != null) {
            ActionEvent actionEvent = new ActionEvent(this, REMOVE_PV, REMOVE_PV_COMMAND);
            createRemoveListener.actionPerformed(actionEvent);
        }
    }

    public void setRenameListener(ActionListener renameListener) {
        this.renameListener = renameListener;
    }

    public ActionListener getRenameListener() {
        return renameListener;
    }

    public boolean isSelected() {
        return itIsSelected;
    }

    public void setSelected(boolean itIsSelected) {
        this.itIsSelected = itIsSelected;
    }

    public static int getNumberOfSelectedNodes(PVTreeNode root) {
        PVTreeNode next = root;
        int nSelectedCount = 0;
        while (next != null) {
            if (next.isSelected()) nSelectedCount++;
            next = (PVTreeNode) next.getNextNode();
        }
        return nSelectedCount;
    }

    public static Integer getIndexOfSelectedNode(PVTreeNode root) {
        Integer index = null;
        synchronized (root) {
            PVTreeNode next = root;
            int indSelected = 0;
            while (next != null && !next.isSelected()) {
                indSelected++;
                next = (PVTreeNode) next.getNextNode();
            }
            if (getNumberOfSelectedNodes(root) == 1) {
                index = new Integer(indSelected);
            }
        }
        return index;
    }

    public static PVTreeNode getSelectedPVTreeNode(PVTreeNode root) {
        PVTreeNode next = root;
        synchronized (root) {
            int indSelected = 0;
            while (next != null && !next.isSelected()) {
                indSelected++;
                next = (PVTreeNode) next.getNextNode();
            }
            if (getNumberOfSelectedNodes(root) == 1) {
                return next;
            }
        }
        return null;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
