package jmri.jmrix.nce.consist;

import javax.swing.*;
import java.io.*;
import jmri.util.StringUtil;
import jmri.jmrix.nce.NceBinaryCommand;
import jmri.jmrix.nce.NceMessage;
import jmri.jmrix.nce.NceReply;
import jmri.jmrix.nce.NceTrafficController;

/**
 * Backups NCE Consists to a text file format defined by NCE.
 * 
 * NCE "Backup consists" dumps the consists into a text file. 
 * The consists data are stored in the NCE CS starting at xF500
 * and ending at xFAFF.
 * 
 * NCE file format:
 * 
 * :F500 (16 bytes per line, grouped as 8 words with space delimiters)
 * :F510 
 *   .
 *   .
 * :FAF0
 * :0000
 * 
 *  
 * Consist data byte:
 * 
 * bit	     15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
 *
 * 
 * This backup routine uses the same consist data format as NCE.
 * 
 * @author Dan Boudreau Copyright (C) 2007
 * @version $Revision: 1.11 $
 */
public class NceConsistBackup extends Thread implements jmri.jmrix.nce.NceListener {

    private static final int CS_CONSIST_MEM = 0xF500;

    private static final int NUM_CONSIST = 96;

    private static final int CONSIST_LNTH = 16;

    private static final int REPLY_16 = 16;

    private int replyLen = 0;

    private int waiting = 0;

    private boolean fileValid = false;

    private byte[] nceConsistData = new byte[CONSIST_LNTH];

    JLabel textConsist = new JLabel();

    JLabel consistNumber = new JLabel();

    private NceTrafficController tc = null;

    public NceConsistBackup(NceTrafficController t) {
        tc = t;
    }

    public void run() {
        JFileChooser fc = new JFileChooser(jmri.jmrit.XmlFile.userFileLocationDefault());
        fc.addChoosableFileFilter(new textFilter());
        File fs = new File("NCE consist backup.txt");
        fc.setSelectedFile(fs);
        int retVal = fc.showSaveDialog(null);
        if (retVal != JFileChooser.APPROVE_OPTION) return;
        if (fc.getSelectedFile() == null) return;
        File f = fc.getSelectedFile();
        if (fc.getFileFilter() != fc.getAcceptAllFileFilter()) {
            String fileName = f.getAbsolutePath();
            String fileNameLC = fileName.toLowerCase();
            if (!fileNameLC.endsWith(".txt")) {
                fileName = fileName + ".txt";
                f = new File(fileName);
            }
        }
        if (f.exists()) {
            if (JOptionPane.showConfirmDialog(null, "File " + f.getName() + " already exists, overwrite it?", "Overwrite file?", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
        }
        PrintWriter fileOut;
        try {
            fileOut = new PrintWriter(new BufferedWriter(new FileWriter(f)), true);
        } catch (IOException e) {
            return;
        }
        if (JOptionPane.showConfirmDialog(null, "Backup can take over a minute, continue?", "NCE Consist Backup", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            fileOut.close();
            return;
        }
        JPanel ps = new JPanel();
        jmri.util.JmriJFrame fstatus = new jmri.util.JmriJFrame("Consist Backup");
        fstatus.setLocationRelativeTo(null);
        fstatus.setSize(200, 100);
        fstatus.getContentPane().add(ps);
        ps.add(textConsist);
        ps.add(consistNumber);
        textConsist.setText("Consist line number:");
        textConsist.setVisible(true);
        consistNumber.setVisible(true);
        waiting = 0;
        fileValid = true;
        for (int consistNum = 0; consistNum < NUM_CONSIST; consistNum++) {
            consistNumber.setText(Integer.toString(consistNum));
            fstatus.setVisible(true);
            getNceConsist(consistNum);
            if (!fileValid) consistNum = NUM_CONSIST;
            if (fileValid) {
                StringBuffer buf = new StringBuffer();
                buf.append(":" + Integer.toHexString(CS_CONSIST_MEM + (consistNum * CONSIST_LNTH)));
                for (int i = 0; i < CONSIST_LNTH; i++) {
                    buf.append(" " + StringUtil.twoHexFromInt(nceConsistData[i++]));
                    buf.append(StringUtil.twoHexFromInt(nceConsistData[i]));
                }
                if (log.isDebugEnabled()) log.debug("consist " + buf.toString());
                fileOut.println(buf.toString());
            }
        }
        if (fileValid) {
            String line = ":0000";
            fileOut.println(line);
        }
        fileOut.flush();
        fileOut.close();
        fstatus.dispose();
        if (fileValid) {
            JOptionPane.showMessageDialog(null, "Successful Backup!", "NCE Consist", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Backup failed", "NCE Consist", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void getNceConsist(int cN) {
        NceMessage m = readConsistMemory(cN);
        tc.sendNceMessage(m, this);
        readWait();
    }

    private boolean readWait() {
        int waitcount = 30;
        while (waiting > 0) {
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (waitcount-- < 0) {
                log.error("read timeout");
                fileValid = false;
                return false;
            }
        }
        return true;
    }

    private NceMessage readConsistMemory(int consistNum) {
        int nceConsistAddr = (consistNum * CONSIST_LNTH) + CS_CONSIST_MEM;
        replyLen = REPLY_16;
        waiting++;
        byte[] bl = NceBinaryCommand.accMemoryRead(nceConsistAddr);
        NceMessage m = NceMessage.createBinaryMessage(tc, bl, REPLY_16);
        return m;
    }

    public void message(NceMessage m) {
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NN_NAKED_NOTIFY")
    public void reply(NceReply r) {
        if (waiting <= 0) {
            log.error("unexpected response");
            return;
        }
        if (r.getNumDataElements() != replyLen) {
            log.error("reply length incorrect");
            return;
        }
        for (int i = 0; i < REPLY_16; i++) {
            nceConsistData[i] = (byte) r.getElement(i);
        }
        waiting--;
        synchronized (this) {
            notify();
        }
    }

    private static class textFilter extends javax.swing.filechooser.FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory()) return true;
            String name = f.getName();
            if (name.matches(".*\\.txt")) return true; else return false;
        }

        public String getDescription() {
            return "Text Documents (*.txt)";
        }
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(NceConsistBackup.class.getName());
}
