package net.sf.ovanttasks.ovanttasks;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

public class stub {

    String __EXECUTABLE__NAME__ = null, __EXECUTABLE__PATH__ = null;

    boolean consoleMode = false;

    File tempDir = null;

    byte[] buffer = new byte[8192];

    JFrame jframe = null;

    JProgressBar progressBar = null;

    public static void main(String[] args) throws Exception {
        stub s = new stub();
        s.unjar(System.getProperty("java.class.path"), args);
    }

    int bytesToWrite = 0;

    int bytesWritten = 0;

    public void unjar(String jarFileName, String[] args) throws Exception {
        if (jarFileName.indexOf(File.separatorChar) == -1) tempDir = new File(new File(System.getProperty("java.io.tmpdir")), jarFileName); else tempDir = new File(new File(System.getProperty("java.io.tmpdir")), jarFileName.substring(jarFileName.lastIndexOf(File.separatorChar) + 1));
        JarFile jf = new JarFile(new File(jarFileName));
        Properties properties = new Properties();
        properties.load(stub.class.getResourceAsStream("stub.properties"));
        __EXECUTABLE__NAME__ = properties.getProperty("__EXECUTABLE__NAME__");
        __EXECUTABLE__PATH__ = properties.getProperty("__EXECUTABLE__PATH__");
        consoleMode = "console".equalsIgnoreCase(properties.getProperty("__MODE__"));
        if (!consoleMode) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            jframe = new JFrame("Preparing ...");
            jframe.setIconImage(new ImageIcon(stub.class.getResource("stub16x16.gif")).getImage());
            ((JComponent) jframe.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jframe.getContentPane().add(progressBar = new JProgressBar(JProgressBar.HORIZONTAL));
            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            jframe.pack();
            Dimension d = jframe.getSize();
            Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle pp = new Rectangle(0, 0, ss.width, ss.height);
            if (pp.x + (pp.width - d.width) / 2 < 0 || pp.y + (pp.height - d.height) / 2 < 0) pp = new Rectangle(0, 0, ss.width, ss.height);
            jframe.setBounds(pp.x + (pp.width - d.width) / 2, pp.y + (pp.height - d.height) / 2, d.width, d.height);
            jframe.setVisible(true);
        }
        ArrayList al = new ArrayList();
        Enumeration e = jf.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = (JarEntry) e.nextElement();
            if (!(entry.isDirectory() && entry.getName().startsWith("META-INF") && entry.getName().equals("com/roxes/tools/ant/stub.class") && entry.getName().equals("com/roxes/tools/ant/stub16x16.gif"))) {
                if (entry.isDirectory()) {
                    String name = entry.getName();
                    name = name.replace('/', File.separatorChar);
                    File file = new File(tempDir, name);
                    if (!file.exists()) {
                        boolean success = file.mkdirs();
                        if (!success) alert("Error occured while unpack distribution:\nCreating directory\n" + file.getAbsolutePath() + "\nfailed !");
                    }
                } else {
                    al.add(entry);
                    bytesToWrite += entry.getSize();
                }
            }
        }
        if (!consoleMode) {
            progressBar.setMaximum(bytesToWrite);
            progressBar.setMinimum(0);
            progressBar.setValue(0);
        }
        for (int i = 0; i < al.size(); i++) {
            JarEntry entry = (JarEntry) al.get(i);
            writeEntry(jf, entry);
        }
        System.out.println("");
        File execDir = tempDir;
        if (__EXECUTABLE__PATH__ != null) execDir = new File(tempDir, __EXECUTABLE__PATH__.replace('/', File.separatorChar));
        String osName = System.getProperty("os.name");
        if (!consoleMode && osName.indexOf("Windows") != -1) {
            __EXECUTABLE__NAME__ = "start /B " + __EXECUTABLE__NAME__;
        }
        StringTokenizer st = new StringTokenizer(__EXECUTABLE__NAME__);
        int count = st.countTokens() + args.length;
        String[] cmdarray = new String[count];
        count = 0;
        while (st.hasMoreTokens()) {
            cmdarray[count++] = st.nextToken();
        }
        for (String arg : args) {
            cmdarray[count++] = arg;
        }
        for (int i = 0; i < cmdarray.length; i++) {
            String string = cmdarray[i];
            System.out.println("cmd[" + i + "]=" + string);
        }
        System.out.println("executed in directory " + execDir);
        try {
            Process p = Runtime.getRuntime().exec(cmdarray, null, execDir);
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            alert("Error occured while executing\n" + __EXECUTABLE__NAME__ + "\nin path\n" + execDir);
        }
        if (!consoleMode) jframe.dispose();
    }

    public void writeEntry(JarFile jarFile, JarEntry entry) {
        String name = entry.getName();
        name = name.replace('/', File.separatorChar);
        File file = new File(tempDir, name);
        File dir = file.getParentFile();
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) alert("Error occured while unpack distribution:\nCreating directory\n" + dir.getAbsolutePath() + "\nfailed !");
        }
        try {
            BufferedInputStream bis = new BufferedInputStream(jarFile.getInputStream(entry));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            if (consoleMode) System.out.print(".");
            int read;
            while ((read = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
                if (progressBar != null) progressBar.setValue((bytesWritten += read));
            }
            bis.close();
            bos.close();
        } catch (Exception e) {
            alert("Error occured while unpack distribution:\nCopying resource\n" + entry.getName() + "\nto directory\n" + dir.getAbsolutePath() + "\nfailed !");
        }
    }

    public void alert(String s) {
        if (consoleMode) System.err.println("Error: " + s); else JOptionPane.showMessageDialog(jframe, s, "Error", JOptionPane.ERROR_MESSAGE);
        if (!consoleMode) jframe.dispose();
        System.exit(0);
    }
}
