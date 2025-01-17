package jmri.jmrix.nce.macro;

import javax.swing.*;
import java.io.*;
import jmri.util.StringUtil;
import jmri.jmrix.nce.NceBinaryCommand;
import jmri.jmrix.nce.NceMessage;
import jmri.jmrix.nce.NceReply;
import jmri.jmrix.nce.NceTrafficController;

/**
 * Backups NCE Macros to a text file format defined by NCE.
 * 
 * NCE "Backup macros" dumps the macros into a text file. Each line contains
 * the contents of one macro.  The first macro, 0 starts at address xC800. 
 * The last macro 255 is at address xDBEC.
 * 
 * NCE file format:
 * 
 * :C800 (macro 0: 20 hex chars representing 10 accessories) 
 * :C814 (macro 1: 20 hex chars representing 10 accessories)
 * :C828 (macro 2: 20 hex chars representing 10 accessories)
 *   .
 *   .
 * :DBEC (macro 255: 20 hex chars representing 10 accessories)
 * :0000
 * 
 *  
 * Macro data byte:
 * 
 * bit	     15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
 *                                 _     _  _  _
 *  	      1  0  A  A  A  A  A  A  1  A  A  A  C  D  D  D
 * addr bit         7  6  5  4  3  2    10  9  8     1  0  
 * turnout												   T
 * 
 * By convention, MSB address bits 10 - 8 are one's complement.  NCE macros always set the C bit to 1.
 * The LSB "D" (0) determines if the accessory is to be thrown (0) or closed (1).  The next two bits
 * "D D" are the LSBs of the accessory address. Note that NCE display addresses are 1 greater than 
 * NMRA DCC. Note that address bit 2 isn't supposed to be inverted, but it is the way NCE implemented
 * their macros.
 * 
 * Examples:
 * 
 * 81F8 = accessory 1 thrown
 * 9FFC = accessory 123 thrown
 * B5FD = accessory 211 close
 * BF8F = accessory 2044 close
 * 
 * FF10 = link macro 16 
 * 
 * This backup routine uses the same macro data format as NCE.
 * 
 * @author Dan Boudreau Copyright (C) 2007
 * @version $Revision: 1.15 $
 */
public class NceMacroBackup extends Thread implements jmri.jmrix.nce.NceListener {

    private static final int CS_MACRO_MEM = 0xC800;

    private static final int NUM_MACRO = 256;

    private static final int MACRO_LNTH = 20;

    private static final int REPLY_16 = 16;

    private static int replyLen = 0;

    private int waiting = 0;

    private boolean secondRead = false;

    private boolean fileValid = false;

    private static byte[] nceMacroData = new byte[MACRO_LNTH];

    javax.swing.JLabel textMacro = new javax.swing.JLabel();

    javax.swing.JLabel macroNumber = new javax.swing.JLabel();

    private NceTrafficController tc = null;

    public NceMacroBackup(NceTrafficController t) {
        super();
        this.tc = t;
    }

    public void run() {
        JFileChooser fc = new JFileChooser(jmri.jmrit.XmlFile.userFileLocationDefault());
        fc.addChoosableFileFilter(new textFilter());
        File fs = new File("NCE macro backup.txt");
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
        if (JOptionPane.showConfirmDialog(null, "Backup can take over a minute, continue?", "NCE Macro Backup", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            fileOut.close();
            return;
        }
        JPanel ps = new JPanel();
        jmri.util.JmriJFrame fstatus = new jmri.util.JmriJFrame("Macro Backup");
        fstatus.setLocationRelativeTo(null);
        fstatus.setSize(200, 100);
        fstatus.getContentPane().add(ps);
        ps.add(textMacro);
        ps.add(macroNumber);
        textMacro.setText("Macro number:");
        textMacro.setVisible(true);
        macroNumber.setVisible(true);
        waiting = 0;
        fileValid = true;
        for (int macroNum = 0; macroNum < NUM_MACRO; macroNum++) {
            macroNumber.setText(Integer.toString(macroNum));
            fstatus.setVisible(true);
            getNceMacro(macroNum);
            if (!fileValid) macroNum = NUM_MACRO;
            if (fileValid) {
                StringBuffer buf = new StringBuffer();
                buf.append(":" + Integer.toHexString(CS_MACRO_MEM + (macroNum * MACRO_LNTH)));
                for (int i = 0; i < MACRO_LNTH; i++) {
                    buf.append(" " + StringUtil.twoHexFromInt(nceMacroData[i++]));
                    buf.append(StringUtil.twoHexFromInt(nceMacroData[i]));
                }
                if (log.isDebugEnabled()) log.debug("macro " + buf.toString());
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
            JOptionPane.showMessageDialog(null, "Successful Backup!", "NCE Macro", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Backup failed", "NCE Macro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void getNceMacro(int mN) {
        NceMessage m = readMacroMemory(mN, false);
        tc.sendNceMessage(m, this);
        if (!readWait()) return;
        NceMessage m2 = readMacroMemory(mN, true);
        tc.sendNceMessage(m2, this);
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

    private NceMessage readMacroMemory(int macroNum, boolean second) {
        secondRead = second;
        int nceMacroAddr = (macroNum * MACRO_LNTH) + CS_MACRO_MEM;
        if (second) {
            nceMacroAddr += REPLY_16;
        }
        replyLen = REPLY_16;
        waiting++;
        byte[] bl = NceBinaryCommand.accMemoryRead(nceMacroAddr);
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
        int offset = 0;
        int numBytes = REPLY_16;
        if (secondRead) {
            offset = REPLY_16;
            numBytes = 4;
        }
        for (int i = 0; i < numBytes; i++) {
            nceMacroData[i + offset] = (byte) r.getElement(i);
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

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(NceMacroBackup.class.getName());
}
