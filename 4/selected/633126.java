package mosinstaller.swing.sample;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import mosinstaller.swing.JMOSI;
import mosinstaller.swing.JProgressWizardPanel;
import mosinstaller.swing.MultiLineLabelUI;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
* @author PLAGNIOL-VILLARD Jean-Christophe
* @version 1.00
*/
public class JZipExtract extends JProgressWizardPanel {

    private JZipExtractThread tr = null;

    private Timer timer = null;

    private JZipExtract parent = this;

    private class JZipExtractThread extends Thread {

        private boolean isdone = false;

        private String message = "";

        private ZipInputStream zis = null;

        private ZipFile zf = null;

        private File dest = null;

        private int i = 0;

        private int isoverwrite = -1;

        private static final int Yes = 0;

        private static final int YesToAll = 1;

        private static final int No = 2;

        private static final int NoToAll = 3;

        private Exception exp = null;

        JZipExtractThread(File f, File dest) throws ZipException, IOException {
            zf = new ZipFile(f);
            FileInputStream fi = new FileInputStream(f);
            CheckedInputStream csumi = new CheckedInputStream(fi, new Adler32());
            zis = new ZipInputStream(new BufferedInputStream(csumi));
            if (!dest.isDirectory()) throw new IOException("[dest]:\"" + dest.getAbsolutePath() + "\" is NOT a directory");
            if (!dest.exists()) dest.mkdirs();
            this.dest = dest;
        }

        public void extract(ZipEntry ze) throws IOException {
            File d = null;
            InputStream in = zf.getInputStream(ze);
            d = new File(dest, ze.getName());
            if (ze.isDirectory() && !d.exists()) d.mkdir(); else if (d.isFile() && d.exists()) {
                d.getParentFile().mkdirs();
                if (isoverwrite == NoToAll) return;
                if (isoverwrite != YesToAll) {
                    JLabel l = new JLabel(JZipWizard.getString("ExtractOverWriteText"));
                    l.setUI(new MultiLineLabelUI());
                    l.setFont(JMOSI.getDefaultFont());
                    Object[] options = { JZipWizard.getString("ExtractOverWriteYes"), JZipWizard.getString("ExtractOverWriteYesToAll"), JZipWizard.getString("ExtractOverWriteNo"), JZipWizard.getString("ExtractOverWriteNoToAll") };
                    isoverwrite = JOptionPane.showOptionDialog(parent, l, JZipWizard.getString("ExtractOverWriteTitle"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    switch(isoverwrite) {
                        case No:
                        case NoToAll:
                            return;
                    }
                    d.createNewFile();
                }
            }
            FileOutputStream out;
            out = new FileOutputStream(d);
            byte[] buf = new byte[1024];
            int readed = 0;
            while ((readed = in.read(buf)) > 0) {
                out.write(buf, 0, readed);
            }
            out.close();
            in.close();
        }

        public void run() {
            isdone = false;
            ZipEntry entry = null;
            try {
                while ((entry = zis.getNextEntry()) != null) {
                    message = entry.getName();
                    extract(entry);
                    i++;
                }
                isdone = true;
            } catch (IOException ioe) {
                exp = ioe;
                ioe.printStackTrace();
            }
        }

        public String getMessage() {
            return message;
        }

        public int getMaximum() {
            return zf.size();
        }

        public int getCurrent() {
            return i;
        }

        public boolean isDone() {
            return isdone;
        }

        public void isExecption() throws Exception {
            if (exp != null) throw exp;
        }
    }

    public JZipExtract(File f, File dest) throws Exception {
        tr = new JZipExtractThread(f, dest);
        setMaximum(tr.getMaximum());
        timer = new Timer(10, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                setValue(tr.getCurrent());
                setTitleProgress(tr.getMessage());
                if (tr.isDone()) {
                    Toolkit.getDefaultToolkit().beep();
                    timer.stop();
                }
            }
        });
        timer.start();
        tr.start();
    }
}
