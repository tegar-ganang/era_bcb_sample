package mosinstaller.swing.sample;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.Timer;
import mosinstaller.swing.JProgressWizardPanel;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
* @author PLAGNIOL-VILLARD Jean-Christophe
* @version 1.00
*/
public class JZipCompress extends JProgressWizardPanel {

    private JZipCompressThread tr = null;

    private Timer timer = null;

    private JZipCompress parent = this;

    private class JZipCompressThread extends Thread {

        private boolean isdone = false;

        private String message = "";

        private File f[] = null;

        private ZipOutputStream dest = null;

        private int pos = 0;

        JZipCompressThread(File f[], File d, String comment) throws ZipException, IOException {
            comment += "\n" + JZipWizard.getString("createdBy");
            FileOutputStream ftmp = new FileOutputStream(d);
            CheckedOutputStream csum = new CheckedOutputStream(ftmp, new Adler32());
            dest = new ZipOutputStream(new BufferedOutputStream(csum));
            dest.setComment(comment);
            this.f = f;
        }

        public void compressDir(File f, String parentPath) throws IOException {
            if (!f.isDirectory()) return;
            String name = f.getAbsolutePath();
            name.replaceAll("\\\\", "/");
            if (name.startsWith(parentPath)) {
                name = name.substring(parentPath.length());
            } else name = f.getName();
            if (!name.startsWith("/")) {
                name = name.substring(1);
            }
            if (!name.endsWith("/")) {
                name += "/";
            }
            message = parent.getName() + " : " + name;
            ZipEntry entry = new ZipEntry(name);
            dest.putNextEntry(entry);
            entry.setTime(f.lastModified());
            dest.closeEntry();
            File list[] = f.listFiles();
            int i = 0;
            for (; i < list.length; i++) {
                if (list[i].isDirectory()) compressDir(list[i], parentPath); else {
                    name = list[i].getAbsolutePath();
                    name.replaceAll("\\\\", "/");
                    if (name.startsWith(parentPath)) {
                        name = name.substring(parentPath.length());
                    } else name = list[i].getName();
                    if (name.startsWith("\\")) {
                        name = name.substring(1);
                    }
                    message = parent.getName() + " : " + name;
                    compress(list[i], name);
                }
            }
        }

        public void compress(File f, String name) throws IOException {
            if (!f.isFile()) return;
            ZipEntry entry = new ZipEntry(name);
            entry.setTime(f.lastModified());
            FileInputStream in = new FileInputStream(f);
            byte[] buf = new byte[1024];
            int readed = 0;
            dest.putNextEntry(entry);
            while ((readed = in.read(buf)) > 0) {
                dest.write(buf, 0, readed);
            }
            in.close();
            dest.closeEntry();
        }

        public void compress(File f) throws IOException {
            if (f.isDirectory()) {
                String parentPath = f.getParentFile().getAbsolutePath();
                if (File.separatorChar == '\\') {
                    parentPath.replaceAll("\\\\", "/");
                }
                compressDir(f, parentPath);
            } else {
                message = f.getName();
                compress(f, f.getName());
            }
        }

        public void run() {
            isdone = false;
            try {
                int i = 0;
                for (; i < f.length; i++) {
                    compress(f[i]);
                    pos++;
                }
                dest.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            message = "";
            isdone = true;
        }

        public String getMessage() {
            return message;
        }

        public int getMaximum() {
            return f.length;
        }

        public int getCurrent() {
            return pos;
        }

        public boolean isDone() {
            return isdone;
        }
    }

    public JZipCompress(File f[], File dest, String comment) throws ZipException, IOException {
        _isFinishVisible = true;
        _isBackEnabled = false;
        _isNextEnabled = false;
        _isHelpEnabled = false;
        _isFinishEnabled = false;
        _isCancelEnabled = false;
        tr = new JZipCompressThread(f, dest, comment);
        setMaximum(tr.getMaximum());
        timer = new Timer(10, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                setValue(tr.getCurrent());
                setTitleProgress(tr.getMessage());
                if (tr.isDone()) {
                    Toolkit.getDefaultToolkit().beep();
                    timer.stop();
                    setCursor(null);
                }
            }
        });
        timer.start();
        tr.start();
        setFinishVisible(true);
        setFinishEnabled(true);
    }
}
