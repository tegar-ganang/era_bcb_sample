package fi.hip.gb.client.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import fi.hip.gb.utils.FileUtils;

/**
 * Utilities for user interface.
 * 
 * @author Juho Karppinen
 * @version $Id: Utils.java 1073 2006-06-07 12:18:06Z jkarppin $
 */
public class Utils {

    /**
	 * Opens file-dialog so that user can choise files
	 * @param title title for the dialog
	 * @param filterString filetype to be shown, for example .txt
	 * @param owner owner of the dialog
	 * @return selected file, or null if no file returned
	 * @throws FileNotFoundException if selected file was not found
	 */
    public static File loadFile(String title, final String filterString, Frame owner) throws FileNotFoundException {
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(filterString);
            }
        };
        FileDialog dir = new FileDialog(owner, title, FileDialog.LOAD);
        dir.setFilenameFilter(filter);
        dir.setFile(filterString);
        dir.setVisible(true);
        String curFile = dir.getFile();
        if (curFile == null) return null;
        String filename = dir.getDirectory() + curFile;
        File file = new File(filename);
        if (file.exists() == false || file.canRead() == false) {
            throw new FileNotFoundException("File " + filename + " is not found or readable");
        }
        return file;
    }

    /**
	 * Shows dialog for selectiong file
	 * @param title title for the dialog
	 * @param initialName default name
	 * @param owner owner for the dialog
	 * @return filename with full path or null if canceled
	 */
    public static String selectFile(String title, String initialName, Frame owner) {
        FileDialog dir = new FileDialog(owner, title, FileDialog.SAVE);
        dir.setFile(initialName);
        dir.setVisible(true);
        if (dir.getFile() == null) return null;
        String filepath = dir.getDirectory() + "/" + dir.getFile();
        if (FileUtils.exists(filepath)) {
            String question = "<html><p>Target file " + dir.getFile() + " already exists on directory " + dir.getDirectory() + ".</p>" + "<p>Do you want to overwrite the file?</p></html>";
            String[] options = new String[] { "Yes, overwrite", "No" };
            if (showConfirmDialog("Overwrite file?", question, options, 1, owner) == 0) return filepath; else return null;
        }
        return filepath;
    }

    /**
	 * Shows dialog for selectiong directory
	 * @param title title of the dialog
	 * @param owner owner for the dialog
	 * @return directory path or null if canceled
	 * @throws FileNotFoundException if given directory was not found
	 */
    public static String selectDirectory(String title, Frame owner) throws FileNotFoundException {
        FileDialog dir = new FileDialog(owner, title, FileDialog.SAVE);
        dir.setFile("*");
        dir.setVisible(true);
        String directory = dir.getDirectory();
        if (directory == null) return null;
        File file = new File(directory);
        if (file.exists() == false || file.canRead() == false) {
            throw new FileNotFoundException("Directory " + directory + " not found");
        }
        return file.getPath();
    }

    /**
	 * Shows option dialog to confirm users action
	 * @param title title of the dialog
	 * @param question question message
	 * @param options options available
	 * @param defaultOption default option
	 * @param owner owner component, normally <code>Frame</code> or <code>Dialog</code>
	 * @return option user selected, or -1 if user canceled the dialog
	 */
    public static int showConfirmDialog(String title, String question, String[] options, int defaultOption, Component owner) {
        int n = JOptionPane.showOptionDialog(owner, question, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[defaultOption]);
        if (n == JOptionPane.CLOSED_OPTION) return -1; else return n;
    }

    /**
	 * Adds default popup editing menu into text components.
	 * @param comp text component where menu is inserted
	 */
    public static void addPopupMenu(final JTextComponent comp) {
        ActionListener listener = new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                String command = evt.getActionCommand();
                if (command.equals(DefaultEditorKit.cutAction)) {
                    comp.cut();
                } else if (command.equals(DefaultEditorKit.copyAction)) {
                    comp.copy();
                } else if (command.equals(DefaultEditorKit.pasteAction)) {
                    comp.paste();
                }
            }
        };
        final JPopupMenu popup = new JPopupMenu();
        Action action = new DefaultEditorKit.CutAction();
        action.putValue(Action.NAME, "Cut");
        action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        JMenuItem menuItem = popup.add(action);
        menuItem.addActionListener(listener);
        action = new DefaultEditorKit.CopyAction();
        action.putValue(Action.NAME, "Copy");
        action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        menuItem = popup.add(action);
        menuItem.addActionListener(listener);
        action = new DefaultEditorKit.PasteAction();
        action.putValue(Action.NAME, "Paste");
        action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        menuItem = popup.add(action);
        menuItem.addActionListener(listener);
        comp.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(comp, e.getX(), e.getY());
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(comp, e.getX(), e.getY());
                }
            }
        });
    }

    /**
	 * Pack window preventing it from moving anywhere during this operation.
	 * If size is increased so much that window is partly outside screen,
	 * move it back inside.
	 * @param frame frame to be packed
	 */
    public static void pack(JFrame frame) {
        Point p = frame.getLocation();
        frame.pack();
        frame.setLocation(p);
        stayInsideScreen(frame);
    }

    /**
	 * Calculate center point on the screen to given dialog
	 * @param component component to be centered
	 */
    public static void center(Component component) {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = component.getWidth();
        int dialogHeight = component.getHeight();
        int leftPoint = screenSize.width / 2 - dialogWidth / 2;
        int topPoint = screenSize.height / 2 - dialogHeight / 2;
        component.setLocation(leftPoint, topPoint);
        stayInsideScreen(component);
    }

    /**
	 * Don't let window to get outside of the screen boundaries
	 * @param component component to be moved if needed
	 */
    public static void stayInsideScreen(Component component) {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point p = component.getLocation();
        int dialogWidth = component.getWidth();
        int dialogHeight = component.getHeight();
        if (p.x + dialogWidth > screenSize.width) p.x = screenSize.width - dialogWidth;
        if (p.y + dialogHeight > screenSize.height) p.y = screenSize.height - dialogHeight;
        component.setLocation(p);
    }

    /**
	 * Gets spacer with more gap before the object
	 * @return GridBagLayouts Insets object
	 */
    public static Insets getSpaceBefore() {
        return new Insets(5, 2, 0, 2);
    }

    /**
	 * Gets spacer with more gap after the object
	 * @return GridBagLayouts Insets object
	 */
    public static Insets getSpaceAfter() {
        return new Insets(0, 2, 4, 2);
    }

    /**
	 * Gets spacer with equal gap to the object
	 * @return GridBagLayouts Insets object
	 */
    public static Insets getEqualSpace() {
        return new Insets(1, 2, 1, 2);
    }

    /**
	 * Gets empty insets without any gap between objects
	 * @return GridBagLayouts Insets object
	 */
    public static Insets getEmpty() {
        return new Insets(0, 0, 0, 0);
    }
}
