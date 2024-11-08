package com.ca.directory.jxplorer;

import java.awt.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;
import javax.swing.*;
import javax.naming.*;
import javax.naming.directory.*;
import com.ca.directory.jxplorer.broker.DataQuery;
import com.ca.directory.jxplorer.tree.*;
import com.ca.directory.jxplorer.broker.*;
import com.ca.commons.naming.*;
import com.ca.commons.cbutil.*;

public class LdifExport extends CBDialog {

    JTextArea rootDN, newRootDN;

    DataBrokerQueryInterface dataSource;

    SmartTree searchTree;

    SmartTree schemaTree;

    FileWriter saveFile;

    LdifUtility ldifutil = new LdifUtility();

    boolean usingSearch;

    CBpbar pbar;

    static String lastDirectory = null;

    private static Logger log = Logger.getLogger(LdifExport.class.getName());

    /**
     *    Constructor for the LdifExport window.  Takes a DN, and
     *    a jndi broker.  If it is exporting from a search result
     *    set, it takes the search tree as a parameter, and a boolean
     *    flag specifying that the tree should be used to list the DNs
     *    to be exported; otherwise it does a full dump from the provided
     *    DN.<p>
     *
     *    The constructor sets up the GUI, defining buttons and fields
     *    and registering button listeners.
     *
     *    @param D the base DN to work from
     *    @param broker the jndi broker to use to read entry attribtues from,
     *                  and to physically write the ldif file
     *    @param searchTree (possibly) a tree containing a list of search
     *                      results, to be used if the search flag is set
     *    @param usingSearch a boolean flag that forces the reading of the
     *                       list of DNs to save from the tree, rather than
     *                       directly from the directory...
     */
    public LdifExport(DN D, DataBrokerQueryInterface broker, SmartTree searchTree, boolean usingSearch, Frame owner) {
        this(D, broker, searchTree, usingSearch, owner, HelpIDs.LDIF_EXPORT_TREE);
    }

    /**
     *    Constructor for the LdifExport window.  Takes a DN, and
     *    a jndi broker.  If it is exporting from a search result
     *    set, it takes the search tree as a parameter, and a boolean
     *    flag specifying that the tree should be used to list the DNs
     *    to be exported; otherwise it does a full dump from the provided
     *    DN.<p>
     *
     *    The constructor sets up the GUI, defining buttons and fields
     *    and registering button listeners.
     *
     *    @param D the base DN to work from
     *    @param broker the jndi broker to use to read entry attribtues from,
     *                  and to physically write the ldif file
     *    @param searchTree (possibly) a tree containing a list of search
     *                      results, to be used if the search flag is set
     *    @param usingSearch a boolean flag that forces the reading of the
     *                       list of DNs to save from the tree, rather than
     *                       directly from the directory...
     *    @param helpID the ID of the help page to attach to the Help button.
     */
    public LdifExport(DN D, DataBrokerQueryInterface broker, SmartTree searchTree, boolean usingSearch, Frame owner, String helpID) {
        super(owner, CBIntText.get("LDIF Export"), helpID);
        OK.setToolTipText(CBIntText.get("Perform the LDIF export"));
        Cancel.setToolTipText(CBIntText.get("Cancel without performing an LDIF export"));
        Help.setToolTipText(CBIntText.get("Display help about LDIF exporting"));
        if (D == null) D = new DN();
        this.dataSource = broker;
        this.searchTree = searchTree;
        this.usingSearch = usingSearch;
        display.add(new JLabel(CBIntText.get("Root DN")), 0, 0);
        display.makeHeavy();
        rootDN = new JTextArea(D.toString());
        rootDN.setLineWrap(true);
        display.add(new JScrollPane(rootDN, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), 1, 0);
        display.makeLight();
        display.add(new JLabel(CBIntText.get("New root DN")), 0, 1);
        display.makeHeavy();
        newRootDN = new JTextArea(D.toString());
        newRootDN.setLineWrap(true);
        display.add(new JScrollPane(newRootDN, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), 1, 1);
        display.makeLight();
    }

    /**
    *    A quick spot of mucking around to add '.ldif' to naked files.
    *
    */
    protected File adjustFileName(File readFile) {
        if (readFile == null) return null;
        if (readFile.exists()) return readFile;
        String name = readFile.getName();
        if (name.indexOf('.') != -1) return readFile;
        name = name + ".ldif";
        return new File(readFile.getParentFile(), name);
    }

    /**
     *    This method is called by the base class when the OK button is pressed.
     *    Handles actually writing the ldif file (relying heavily on LdifUtility for
     *    the grunt work).  Does the actual file writing in a separate thread.
     */
    public void doOK() {
        if (!checkRootDN()) return;
        setVisible(false);
        JFileChooser chooser = new JFileChooser(JXConfig.getProperty("ldif.homeDir"));
        chooser.addChoosableFileFilter(new CBFileFilter(new String[] { "ldif", "ldi" }, "Ldif Files (*.ldif, *.ldi)"));
        int option = chooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File readFile = chooser.getSelectedFile();
            if (readFile == null) {
                CBUtility.error(CBIntText.get("Please select a file"));
            } else {
                readFile = adjustFileName(readFile);
                int response = -1;
                if (readFile.exists()) {
                    response = JOptionPane.showConfirmDialog(this, CBIntText.get("File ''{0}'' already exsists.  Do you want to replace it?", new String[] { readFile.toString() }), CBIntText.get("Overwrite Confirmation"), JOptionPane.YES_NO_OPTION);
                    if (response != JOptionPane.YES_OPTION) {
                        setVisible(true);
                        return;
                    }
                }
                JXConfig.setProperty("ldif.homeDir", readFile.getParent());
                doFileWrite(readFile);
            }
        }
    }

    /**
     * Does three checks.<br><br>
     * 1) if there is a root DN and a new root DN.  If not a confirmation message appears
     * asking if the user wants to export the full subtree.<br>
     * 2) if there is no root DN.  In this case a dialog appears asking for it.
     * 3) if there is no new root DN.  In this case a dialog appears asking for it.
     * @return true only if all the checks succeed (or user approves the export full subtree).
     */
    public boolean checkRootDN() {
        String oldRoot = (rootDN.getText()).trim();
        String newRoot = (newRootDN.getText()).trim();
        if ((oldRoot == null || oldRoot.length() <= 0) && (newRoot == null || newRoot.length() <= 0)) {
            int response = JOptionPane.showConfirmDialog(this, CBIntText.get("Without a 'Root DN' and a 'New Root DN', the full tree will be exported.  Do you want to continue?"), CBIntText.get("Export Full Tree"), JOptionPane.YES_NO_OPTION);
            if (response != JOptionPane.YES_OPTION) return false;
            return true;
        } else if (oldRoot == null || oldRoot.length() <= 0) {
            JOptionPane.showMessageDialog(this, CBIntText.get("Please enter a 'Root DN'."), CBIntText.get("Root DN"), JOptionPane.INFORMATION_MESSAGE);
            return false;
        } else if (newRoot == null || newRoot.length() <= 0) {
            JOptionPane.showMessageDialog(this, CBIntText.get("Please enter a 'New Root DN'."), CBIntText.get("New Root DN"), JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     *    Launch a DataQuery that will write the ldif file.
     *
     */
    protected void doFileWrite(File saveFile) {
        if (saveFile == null) CBUtility.error(CBIntText.get("Unable to write to empty file"), null);
        final File myFile = saveFile;
        dataSource.extendedRequest(new DataQuery(DataQuery.EXTENDED) {

            public void doExtendedRequest(DataBroker b) {
                try {
                    FileWriter myFileWriter = new FileWriter(myFile);
                    pbar = new CBpbar(LdifExport.this, CBIntText.get("Saving LDIF file"), CBIntText.get("Saving Data"));
                    myFileWriter.write("version: 1\n");
                    DN oldRoot = new DN(rootDN.getText());
                    DN newRoot = new DN(newRootDN.getText());
                    if (usingSearch) {
                        Vector bloop = searchTree.getAllNodes(new DN(rootDN.getText()));
                        saveLdifList(bloop, myFileWriter, oldRoot.toString(), newRoot.toString(), b);
                    } else {
                        saveLdifTree(oldRoot, myFileWriter, oldRoot.toString(), newRoot.toString(), b);
                    }
                    myFileWriter.close();
                } catch (Exception e) {
                    setException(e);
                }
                if (pbar.isCanceled()) myFile.delete();
                closeDown();
                return;
            }
        });
    }

    /**
     *    Write a subtree to an ldif file by recursing through the
     *    tree, calling saveLdifEntry as it goes...
     *
     *    @param treeApex the root node of the sub tree to be written out.
     *    @param saveFile the file being written to...
     *    @param origPrefix the original DN prefix, that may be modified
     *                           on write to be replacementPrefix.  This may be
     *                           null if no action is to be taken.
     *    @param newPrefix another DN to replace the originalPrefix.
     *    @return number of entries written
     */
    public boolean saveLdifTree(DN treeApex, FileWriter saveFile, String origPrefix, String newPrefix, DataBroker broker) {
        if (treeApex == null) return false;
        if (pbar == null) return false;
        if (newPrefix == null) origPrefix = null;
        if ((origPrefix != null) && (origPrefix.equals(newPrefix))) {
            origPrefix = null;
            newPrefix = null;
        }
        if (pbar.isCanceled()) return false;
        Attributes atts = null;
        try {
            if (treeApex.isEmpty() == false) {
                atts = broker.unthreadedReadEntry(treeApex, null);
            }
            if (atts != null) {
                DN escapeMe = new DN(treeApex);
                ldifutil.writeLdifEntry(escapeMe.toString(), saveFile, origPrefix, newPrefix, atts);
            }
            DXNamingEnumeration children = broker.unthreadedList(treeApex);
            pbar.push(children.size());
            while (children != null && children.hasMore()) {
                String subDNString = ((NameClassPair) children.next()).getName();
                DN child = new DN(treeApex);
                DN subDN = new DN(subDNString);
                child.addChildRDN(subDN.getLowestRDN());
                if (saveLdifTree(child, saveFile, origPrefix, newPrefix, broker) == false) return false;
            }
        } catch (NamingException e) {
            CBUtility.error(this, CBIntText.get("Unable to read dn: {0} from directory", new String[] { treeApex.toString() }), e);
        } catch (Exception e) {
            CBUtility.error(this, CBIntText.get("General error reading dn: {0} from directory", new String[] { treeApex.toString() }), e);
            e.printStackTrace();
        }
        pbar.pop();
        pbar.inc();
        return true;
    }

    /**
     *    Writes a list of entries to an ldif file.
     *
     *    @param dns the list of the dns of objects to write out...
     *    @param saveFile the file being written to...
     *    @param originalPrefix the original DN prefix, that may be modified
     *                           on write to be replacementPrefix.  This may be
     *                           null if no action is to be taken.
     *    @param replacementPrefix another DN to replace the originalPrefix.
     */
    public void saveLdifList(Vector dns, FileWriter saveFile, String originalPrefix, String replacementPrefix, DataBroker broker) {
        if (replacementPrefix == null) originalPrefix = null;
        if ((originalPrefix != null) && (originalPrefix.equals(replacementPrefix))) {
            originalPrefix = null;
            replacementPrefix = null;
        }
        int size = dns.size();
        pbar.push(size);
        for (int i = 0; i < size; i++) {
            DN dn = (DN) dns.elementAt(i);
            try {
                Attributes atts = broker.unthreadedReadEntry(dn, null);
                ldifutil.writeLdifEntry(dn.toString(), saveFile, originalPrefix, replacementPrefix, atts);
            } catch (NamingException e) {
                log.log(Level.WARNING, "Unable to read dn: '" + dn.toString() + "' from directory ", e);
            } catch (Exception e) {
                log.log(Level.WARNING, "General error reading: dn: '" + dn.toString() + "' from directory ", e);
            }
            if (pbar.isCanceled()) return;
            pbar.inc();
        }
        pbar.close();
    }

    private void closeDown() {
        try {
            if (saveFile != null) saveFile.close();
            log.warning("Closed LDIF file");
        } catch (IOException e) {
            ;
        }
        if (pbar != null) pbar.close();
        setVisible(false);
        dispose();
    }
}
