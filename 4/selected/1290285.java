package fr.emn.easymol.gui;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import fr.emn.easymol.geometry.Atom2D;

/** The class to register the action "Add/Remove Bond".<br>
*   It uses some methods in the <code>LibGUI</code> to perform its operations.
**/
public class ActionAddOrRemoveBond extends AbstractAction {

    private ToolBarWindow toolB;

    private int bondType;

    private boolean remove;

    /** Constructs an <code>AbstractAction</code> and sets additional fields. 
   *   @param toolB the <code>ToolBarWindow</code> in which this action will be created.
   *   @param name the label that will be put in menus.
   *   @param toolTip the tooltip which will appear on the buttons or in the menus.
   *   @param accelerator the keyboard shortcut.
   *   @param icon the beautiful icon for the button and the menu item.
   *   @param erase if the action is in "Remove Bond" mode.
   **/
    public ActionAddOrRemoveBond(ToolBarWindow tb, String name, String toolTip, KeyStroke accelerator, Icon icon, int type, boolean erase) {
        super(name, icon);
        super.putValue(Action.SHORT_DESCRIPTION, toolTip);
        super.putValue(Action.ACCELERATOR_KEY, accelerator);
        toolB = tb;
        bondType = type;
        remove = erase;
    }

    /** The action connected to item "Add/Remove Bond" in menus or toolbars.
   *   @param e the <code>ActionEvent</code> fired by the listener that will be registered by the action.
   **/
    public void actionPerformed(ActionEvent e) {
        LewisViewWindow lws = toolB.getFocusedWindow().getLewisView();
        Atom2D lastSelected = lws.getLastSelected();
        Atom2D beforeLastSelected = lws.getBeforeLastSelected();
        boolean success = false;
        if (remove) success = lws.removeLink(lastSelected, beforeLastSelected); else success = lws.addLink(lastSelected, beforeLastSelected, bondType);
        if (!remove && !success) {
            toolB.getConsoleWindow().write("Atom valency already satisfied. You must remove links from this atom to add another one", false);
            return;
        }
        toolB.getFocusedWindow().setModified(true);
    }
}
