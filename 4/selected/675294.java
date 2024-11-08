package jsmex.function;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultTreeModel;
import jsmex.LogTracer;
import jsmex.cardservice.JSmexCardService;
import jsmex.gui.ExplorerGUI;
import jsmex.gui.PINDialog;
import opencard.core.terminal.ResponseAPDU;
import opencard.core.util.HexString;

/**
 *
 * @author Tobias Senger
 */
public class ExplorerFunctions {

    private ExplorerGUI exgui = null;

    private LogTracer lt = null;

    private JSmexCardService cs = null;

    /** Creates a new instance of ExplorerFunctions */
    public ExplorerFunctions(JSmexCardService cards, LogTracer ltracer) {
        cs = cards;
        lt = ltracer;
        showGUI();
    }

    /** Returns the LogTracer instance.
     *
     * @return
     */
    public LogTracer getLogTracer() {
        lt.info("called method getLogTracer().", this);
        return lt;
    }

    public int verifyCHV(int type, String pin) {
        ResponseAPDU resp = cs.verifyCHV((byte) type, pin);
        if (resp.sw() == 0x9000) return 1; else return -1;
    }

    /**Build a ExplorerGUI and show it.
     *
     */
    public void showGUI() {
        if (exgui == null) exgui = new ExplorerGUI(this);
        lt.info("called method showGUI().", this);
        exgui.setVisible(true);
    }

    /**Close the Explorer-GUI
     *
     */
    public void closeGUI() {
        lt.info("called method closeGUI().", this);
        exgui.setVisible(false);
        exgui.dispose();
        exgui = null;
    }

    /** Opens a FileChooser dialog an returns the file which was selected.
     *
     * @param sName A proposal for the file name.
     * @param dialogType A Integer which defines a DialogTyp
     * @return
     */
    private File getFilename(String sName, int dialogType) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(dialogType);
        fc.setDialogTitle("Select a file");
        fc.setSelectedFile(new File(sName));
        int state = fc.showOpenDialog(exgui);
        if (state == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            lt.info("called method getFilename(sName: " + sName + ", dialogType: " + dialogType + "). returns filename: " + file.toString(), this);
            return file;
        } else {
            lt.info("called method getFilename(sName: " + sName + ", dialogType: " + dialogType + "). returns 'null'", this);
            return null;
        }
    }

    /**Saves data with the name given in parameter efName into a local file.
     *
     * @param efName The Name of the EF.
     * @param data
     * @return Returns 'true' if the record were saved to a local file on hd.
     */
    private boolean saveToFile(String efName, byte[] data) {
        boolean success = false;
        lt.info("called method saveToFile(efName: " + efName + ", byte[] data).", this);
        try {
            File file = getFilename(efName + ".dat", JFileChooser.SAVE_DIALOG);
            if ((new File(file.toString() + 1)).exists()) {
                int choice = JOptionPane.showConfirmDialog(null, "File exists already!\nDo wan't to overwrite it?", "Warning!", JOptionPane.YES_NO_OPTION);
                if (choice == 1) return false;
            }
            if (file != null) {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
                success = true;
            } else success = false;
        } catch (Exception e) {
            System.out.println(e);
        }
        return success;
    }

    /** Opens a FileChooserDialog, reads the selected file and returns the file content in a byte array.
     *
     * @return The file content in a byte array.
     */
    private byte[] readFromFile() {
        File file = null;
        File sourcefile = null;
        boolean foundFile = false;
        byte[] filedata = null;
        lt.info("called method readFromFile().", this);
        try {
            file = getFilename("*.dat", JFileChooser.OPEN_DIALOG);
            if (file == null) return null;
            FileInputStream fis = new FileInputStream(file);
            filedata = new byte[(int) file.length()];
            fis.read(filedata);
            fis.close();
        } catch (Exception e) {
        }
        return filedata;
    }

    /**Saves the records which come in parameter records with the name given in parameter efName into a local file.
     *
     * @param efName The Name of the EF.
     * @param records
     * @return Returns 'true' if the record were saved to a local file on hd.
     */
    private boolean saveToFile(String efName, Vector records) {
        boolean success = false;
        File file = null;
        File savefile = null;
        lt.info("called method saveToFile(efName: " + efName + ", Vector records).", this);
        try {
            file = getFilename(efName + ".dat", JFileChooser.SAVE_DIALOG);
            if ((new File(file.toString() + 1)).exists()) {
                int choice = JOptionPane.showConfirmDialog(null, "File exists already!\nDo wan't to overwrite it?", "Warning!", JOptionPane.YES_NO_OPTION);
                if (choice == 1) return false;
            }
            if (file != null) {
                for (int i = 0; i < records.size(); i++) {
                    savefile = new File(file.toString() + (i + 1));
                    FileOutputStream fos = new FileOutputStream(savefile);
                    fos.write((byte[]) records.elementAt(i));
                    fos.close();
                }
                success = true;
            } else success = false;
        } catch (Exception e) {
            System.out.println(e);
        }
        return success;
    }

    public boolean updateEF(SmartCardFileNode node, byte[] data, int recno) {
        selectFile(node);
        SmartCardFile scfile = (SmartCardFile) node.getUserObject();
        lt.info("called method updateEF(SmartCardFileNode node, byte[] data, int recno). node name is: " + node.toString(), this);
        if (scfile.fp.getUpdateAC() == (byte) 0x0F) {
            JOptionPane.showMessageDialog(exgui, "Can't update EF because update-AC is \"NEVER\"");
            return false;
        } else if (!doUpdateAuthentication(scfile.fp)) {
            JOptionPane.showMessageDialog(exgui, scfile.toString() + " was NOT updated.");
            return false;
        }
        int tof = scfile.fp.getTof();
        if (tof == 1) {
            ResponseAPDU resp;
            try {
                resp = cs.updateBinary((byte) 0, (byte) 0, data);
                if (resp.sw() == 0x9000) {
                    JOptionPane.showMessageDialog(exgui, scfile.toString() + " was successful updated.");
                    return true;
                } else {
                    JOptionPane.showMessageDialog(exgui, scfile.toString() + " was NOT updated.");
                    return false;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (tof == 2 || tof == 4 || tof == 6) {
            ResponseAPDU resp;
            try {
                resp = cs.updateRecord((byte) recno, data);
                if (resp.sw() == 0x9000) {
                    JOptionPane.showMessageDialog(exgui, scfile.toString() + " Record " + recno + " was successful updated.");
                    return true;
                } else {
                    JOptionPane.showMessageDialog(exgui, scfile.toString() + " Record " + recno + " was NOT updated!");
                    return false;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    /**Overwrite an existing EF with new data.
     *
     * @param node
     */
    public boolean updateEF(SmartCardFileNode node) {
        byte[] efdata = null;
        selectFile(node);
        SmartCardFile scfile = (SmartCardFile) node.getUserObject();
        lt.info("called method updateEF(SmartCardFileNode node). node name is: " + node.toString(), this);
        if (scfile.fp.getUpdateAC() == (byte) 0x0F) {
            JOptionPane.showMessageDialog(exgui, "Can't update EF because update-AC is \"NEVER\"");
        } else if (!doUpdateAuthentication(scfile.fp)) {
            return false;
        }
        if (scfile.fp.getTof() == 1) {
            byte[] newdata = readFromFile();
            if (newdata == null) {
                JOptionPane.showMessageDialog(exgui, "Canceled");
                return false;
            }
            if (newdata.length != scfile.fp.getFileSize()) {
                JOptionPane.showMessageDialog(exgui, scfile.toString() + " can't be updated because the EF size doesn't match to the selected file size.");
                return false;
            }
            ResponseAPDU resp;
            try {
                resp = cs.updateBinary((byte) 0, (byte) 0, newdata);
                if (resp.sw() == 0x9000) {
                    JOptionPane.showMessageDialog(exgui, scfile.toString() + " was successful updated.");
                    return true;
                } else {
                    JOptionPane.showMessageDialog(exgui, scfile.toString() + " was NOT successful updated.");
                    return false;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (scfile.fp.getTof() == 2 || scfile.fp.getTof() == 4 || scfile.fp.getTof() == 6) {
            int recno = -1;
            do {
                String s = JOptionPane.showInputDialog(exgui, "Which record should be updated? (1-" + scfile.fp.getRecordsCount() + ")");
                if (s == null) return false;
                try {
                    recno = Integer.parseInt(s);
                } catch (NumberFormatException ex) {
                    recno = -1;
                }
            } while (!(recno <= scfile.fp.getRecordsCount() && recno >= 1));
            byte[] newdata = readFromFile();
            if (newdata == null) {
                JOptionPane.showMessageDialog(exgui, "Canceled");
                return false;
            }
            if (newdata.length != scfile.fp.getRecordLength()) {
                JOptionPane.showMessageDialog(exgui, scfile.toString() + " can't be updated because the record size doesn't match to the selected file size.");
                return false;
            }
            ResponseAPDU resp;
            try {
                resp = cs.updateRecord((byte) recno, newdata);
                if (resp.sw() == 0x9000) {
                    JOptionPane.showMessageDialog(exgui, scfile.toString() + " Record " + recno + " was successful updated.");
                    return true;
                } else {
                    JOptionPane.showMessageDialog(exgui, scfile.toString() + " Record " + recno + " was NOT successful updated!");
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    /**Gets the EF from the given SmartCardFileNode and saves the file content into a local file an the harddisc.
     *
     * @param node
     */
    public void exportEF(SmartCardFileNode node) {
        byte[] efdata = null;
        selectFile(node);
        lt.info("called method exportEF(SmartCardFileNode node). EF name is: " + node.toString(), this);
        SmartCardFile scfile = (SmartCardFile) node.getUserObject();
        if (scfile.fp.getReadAC() == (byte) 0x0F) {
            JOptionPane.showMessageDialog(exgui, "Can't export EF because read-AC is \"NEVER\"");
        } else doReadAuthentication(scfile.fp);
        if (scfile.fp.getTof() == 1) {
            int fs = scfile.fp.getFileSize();
            int i = 0;
            efdata = new byte[fs];
            while (fs > 0xFF) {
                ResponseAPDU resp;
                try {
                    resp = cs.readBinary((byte) (i), (byte) 0, (byte) 255);
                    System.arraycopy(resp.data(), 0, efdata, i * 0xFF, 0xFF);
                    if (fs > 0xFF) fs -= 0xFF;
                    i++;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            ResponseAPDU resp;
            try {
                resp = cs.readBinary((byte) (i), (byte) 0, (byte) fs);
                System.arraycopy(resp.data(), 0, efdata, i * 0xFF, resp.data().length);
                if (saveToFile(scfile.toString(), efdata)) JOptionPane.showMessageDialog(exgui, scfile.toString() + " was successful exported.");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (scfile.fp.getTof() == 2 || scfile.fp.getTof() == 4 || scfile.fp.getTof() == 6) {
            int numberOfRecords = scfile.fp.getRecordsCount();
            Vector records = new Vector(numberOfRecords);
            for (int i = 1; i <= numberOfRecords; i++) {
                ResponseAPDU resp;
                try {
                    resp = cs.readRecord((byte) i, scfile.fp.getRecordLength());
                    efdata = new byte[resp.data().length];
                    for (int j = 0; j < efdata.length; j++) {
                        efdata[j] = resp.data()[j];
                    }
                    records.add(efdata);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (saveToFile(scfile.toString(), records)) JOptionPane.showMessageDialog(exgui, scfile.toString() + " was successful exported.");
            ;
        }
    }

    /**This searchs a SmartCardFile with FID fidString on the card and creates a new SmartCardFileNode if the file exists.
     *
     * @param fidString This String contains a FID which will be searched on the card.
     * @return Returns a new SmartCardNode if the FID was found on the card.
     */
    public SmartCardFileNode createNewNode(String fidString) {
        lt.info("called method createNewNode(fidString: " + fidString + ").", this);
        SmartCardFile scfile = cs.getSmartCardFile(fidString);
        if (scfile != null) {
            return new SmartCardFileNode(scfile);
        } else return null;
    }

    /** Build the root SmartCardFileNode an build a DefaultTreeModel.
     *
     * @return
     */
    public DefaultTreeModel getTreeModel() {
        lt.info("called method getTreeModel().", this);
        byte[] fid = { (byte) 0x3F, (byte) 0x00 };
        FileProperties fp = new FileProperties(0, 0x38, 0, 0, fid, (byte) 0x00, (byte) 0x0F);
        SmartCardFile root = new SmartCardFile("MF", fp);
        root.setCardService(cs);
        SmartCardFileNode rootNode = new SmartCardFileNode(root);
        rootNode.explore();
        return new DefaultTreeModel(rootNode);
    }

    /**Gets the path from the jTree and selects all DF from MF to the EF which is selected by the SmartCardFileNode node.
     *
     * @param node
     */
    private void selectFile(SmartCardFileNode node) {
        int i = 0;
        int j = 0;
        lt.info("called method selectFile(SmartCardFileNode node). node name is:" + node.toString(), this);
        SmartCardFile[] parentFile = new SmartCardFile[5];
        SmartCardFileNode pn = node;
        SmartCardFile scfile = (SmartCardFile) node.getUserObject();
        do {
            pn = (SmartCardFileNode) pn.getParent();
            if (pn != null) {
                parentFile[i] = (SmartCardFile) pn.getUserObject();
                i++;
            }
        } while (pn != null);
        try {
            cs.selectMF();
            for (j = i - 1; j >= 0; j--) {
                cs.selectDF(parentFile[j].getFid());
            }
            cs.selectEF(scfile.getFid());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**This method checks if a authentication is necessary to read the File with the FID in paramter fid.
     * It will asks for the PIN and do the authentication if necessary.
     *
     * @param fp The FileProperties from the file for which the authentication should be done.
     * @return Returns 'true' if the authentication was successful.
     */
    private boolean doReadAuthentication(FileProperties fp) {
        lt.info("called method doReadAuthentication(FileProperties fp). FID is: " + HexString.hexify(fp.getFID()), this);
        boolean authenticationPassed = false;
        if (fp.getReadAC() == (byte) 0x01 || fp.getReadAC() == (byte) 0x02) {
            if (!cs.chvIsVerified((int) fp.getReadAC())) {
                PINDialog pd = new PINDialog();
                String pin = pd.getCHV(exgui, (int) fp.getReadAC());
                if (pin == null) JOptionPane.showMessageDialog(exgui, "Abort Authentication."); else {
                    int returncode = verifyCHV((int) fp.getReadAC(), pin);
                    if (returncode == -1) {
                        JOptionPane.showMessageDialog(exgui, "Wrong PIN!");
                    } else authenticationPassed = true;
                }
            } else return true;
        } else if (fp.getReadAC() == (byte) 0x00) return true;
        return authenticationPassed;
    }

    /**This method checks if a authentication is necessary to update the File with the FID in paramter fid.
     * It will asks for the PIN and do the authentication if necessary.
     *
     * @param fp The FileProperties from the file for which the authentication should be done.
     * @return Returns 'true' if the authentication was successful.
     */
    private boolean doUpdateAuthentication(FileProperties fp) {
        lt.info("called method doUpdateAuthentication(FileProperties fp). FID is: " + HexString.hexify(fp.getFID()), this);
        boolean authenticationPassed = false;
        if (fp.getUpdateAC() == (byte) 0x01 || fp.getUpdateAC() == (byte) 0x02) {
            if (!cs.chvIsVerified((int) fp.getUpdateAC())) {
                PINDialog pd = new PINDialog();
                String pin = pd.getCHV(exgui, (int) fp.getUpdateAC());
                if (pin == null) JOptionPane.showMessageDialog(exgui, "Abort Authentication."); else {
                    int returncode = verifyCHV((int) fp.getUpdateAC(), pin);
                    if (returncode == -1) {
                        JOptionPane.showMessageDialog(exgui, "Wrong PIN!");
                    } else authenticationPassed = true;
                }
            } else return true;
        } else if (fp.getUpdateAC() == (byte) 0x00) return true;
        return authenticationPassed;
    }

    /**Gets the EF from the given SmartCardFileNode and returns the file content in a byte array.
     *
     * @param node
     * @param recno Record number to read
     * @return A StringBuffer with the data of the file.
     */
    public byte[] getFileContent(SmartCardFileNode node, int recno) {
        selectFile(node);
        lt.info("called method getFileContent(SmartCardFileNode node, int recno). EF name is: " + node.toString(), this);
        SmartCardFile scfile = (SmartCardFile) node.getUserObject();
        if (scfile.fp.getReadAC() == (byte) 0x0F) {
            JOptionPane.showMessageDialog(exgui, "Can't read EF because read-AC is \"NEVER\"");
            return null;
        } else if (!doReadAuthentication(scfile.fp)) {
            JOptionPane.showMessageDialog(exgui, "Can't authenticate.");
            return null;
        }
        try {
            if (scfile.fp.getTof() == 1 || recno == -1) {
                int fs = scfile.fp.getFileSize();
                int i = 0;
                byte[] data = new byte[fs];
                while (fs > 0xFF) {
                    ResponseAPDU resp;
                    resp = cs.readBinary((byte) (i), (byte) 0, (byte) 255);
                    System.arraycopy(resp.data(), 0, data, i * 0xFF, 0xFF);
                    if (fs > 0xFF) fs -= 0xFF;
                    i++;
                }
                ResponseAPDU resp = cs.readBinary((byte) (i), (byte) 0x00, (byte) fs);
                System.arraycopy(resp.data(), 0, data, i * 0xFF, resp.data().length);
                return data;
            }
            if ((scfile.fp.getTof() == 2 || scfile.fp.getTof() == 4 || scfile.fp.getTof() == 6) && recno >= 0) {
                int numberOfRecords = scfile.fp.getRecordsCount();
                ResponseAPDU resp = cs.readRecord((byte) recno, scfile.fp.getRecordLength());
                if (resp.sw() != 0x9000) return null;
                byte[] data = new byte[resp.data().length];
                System.arraycopy(resp.data(), 0, data, 0, data.length);
                return data;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**Gets the EF from the given SmartCardFileNode and returns the file content in a StringBuffer.
     *
     * @param node
     * @return A StringBuffer with the data of the file.
     */
    public byte[] getFileContent(SmartCardFileNode node) {
        return getFileContent(node, -1);
    }

    /** Gets the FileProperties from the given SmartCardFileNode and return them.
     *
     * @param node The SmartCardFileNode to get the FileProperties from.
     * @return
     */
    public FileProperties getFileProperties(SmartCardFileNode node) {
        lt.info("called method getFileProperties(SmartCardFileNode node). EF name is: " + node.toString(), this);
        SmartCardFile smf = (SmartCardFile) node.getUserObject();
        return smf.fp;
    }
}
