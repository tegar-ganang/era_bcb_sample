package ch.hsr.ifs.jmapdesk.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

public class GUI_Components {

    /**
	 * create MenuItem with CTRL-MASK
	 * 
	 * @param label Name of the Item
	 * @param event Shortcut
	 * @return MenuItem
	 */
    JMenuItem create_menuItem_ctrlmask(String label, int event) {
        JMenuItem menuItem = new JMenuItem(label);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(event, InputEvent.CTRL_MASK));
        return menuItem;
    }

    /**
	 * create MenuItem with a char KeyStroke
	 * 
	 * @param label Name of the Item
	 * @param event Shortcut
	 * @return MenuItem
	 */
    JMenuItem create_menuItem_char(String label, char event) {
        JMenuItem menuItem = new JMenuItem(label);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(event));
        return menuItem;
    }

    /**
	 * create MenuItem with a String KeyStroke
	 * 
	 * @param label Name of the Item
	 * @param event Shortcut
	 * @return MenuItem
	 */
    JMenuItem create_menuItem_string(String label, String event) {
        JMenuItem menuItem = new JMenuItem(label);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(event));
        return menuItem;
    }

    /**
	 * create ToggleButton
	 * 
	 * @param image ButtonIcon
	 * @param label ActionCommand and Tooltip
	 * @return Button
	 */
    JToggleButton create_togglebutton(String image, String label) {
        JToggleButton button = new JToggleButton();
        button.setIcon(new ImageIcon(getClass().getResource("/ch/hsr/ifs/jmapdesk/ui/images/" + image)));
        button.setToolTipText(label);
        button.setActionCommand(label);
        return button;
    }

    /**
	 * create Button
	 * 
	 * @param image ButtonIcon
	 * @param label ActionCommand and Tooltip
	 * @return Button
	 */
    JButton create_button(String image, String label) {
        JButton button = new JButton();
        button.setIcon(new ImageIcon(getClass().getResource("/ch/hsr/ifs/jmapdesk/ui/images/" + image)));
        button.setToolTipText(label);
        button.setActionCommand(label);
        return button;
    }

    /**
	 * Customized FileChooser with overwrite Feature
	 * 
	 * @return FileChooser
	 */
    JFileChooser file_chooser() {
        JFileChooser fileChooser = new JFileChooser() {

            private static final long serialVersionUID = 1L;

            @Override
            public void approveSelection() {
                File f = getSelectedFile();
                if (f.exists() && getDialogType() == SAVE_DIALOG) {
                    int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(), "The selected file already exists. " + "Do you want to overwrite it?", "The file already exists", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    switch(result) {
                        case JOptionPane.YES_OPTION:
                            super.approveSelection();
                            return;
                        case JOptionPane.NO_OPTION:
                            return;
                        case JOptionPane.CANCEL_OPTION:
                            cancelSelection();
                            return;
                    }
                }
                super.approveSelection();
            }
        };
        return fileChooser;
    }

    /**
	 * contextmenu (just copy)
	 * 
	 * @param ta textarea
	 * @return PopupMenu
	 */
    JPopupMenu popupMenu(final JTextArea ta) {
        final String copy = "Copy";
        final String copyall = "Copy All";
        JPopupMenu pm = new JPopupMenu();
        ActionListener itemListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == copy) {
                    ta.copy();
                } else if (e.getActionCommand() == copyall) {
                    ta.selectAll();
                    ta.copy();
                }
            }
        };
        String[] items = { copy, copyall };
        for (String item : items) {
            JMenuItem menuItem = new JMenuItem(item);
            menuItem.addActionListener(itemListener);
            pm.add(menuItem);
        }
        return pm;
    }
}
