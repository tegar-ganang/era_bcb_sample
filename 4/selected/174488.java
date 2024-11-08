package net.sourceforge.jhelpdev.action;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.swing.AbstractAction;
import net.sourceforge.jhelpdev.ConfigHolder;

/**
 * CreateHelpJarFile
 * Copyright &copy; SATAC, 15 February 2006
 * @author medge
 */
public class CreateHelpJarFile extends AbstractAction {

    private static final int READ_BUF_SIZE = 1024;

    /** Creates a new instance of CreateHelpJarFile */
    public CreateHelpJarFile() {
        super();
        putValue(NAME, "Jar Help");
        URL url = getClass().getResource("/images/create_jar.gif");
        if (url != null) putValue(SMALL_ICON, new javax.swing.ImageIcon(url));
    }

    public CreateHelpJarFile(String name) {
        super(name);
    }

    public CreateHelpJarFile(String name, javax.swing.Icon icon) {
        super(name, icon);
    }

    private static void buildFileList(File top, List<File> files) {
        File[] fs = top.listFiles();
        for (File f : fs) {
            if (f.isDirectory()) buildFileList(f, files); else files.add(f);
        }
    }

    private static JarEntry makeJarEntry(File file, File top) {
        String act = file.getPath().substring(top.getAbsolutePath().length() + 1).replace(File.separatorChar, '/');
        JarEntry je = new JarEntry(act);
        return je;
    }

    private static void addFile(File file, File top, JarOutputStream jaroutput) throws FileNotFoundException, IOException {
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
        jaroutput.putNextEntry(makeJarEntry(file, top));
        byte[] readBuf = new byte[READ_BUF_SIZE];
        int readCount;
        while ((readCount = input.read(readBuf, 0, READ_BUF_SIZE)) != -1) jaroutput.write(readBuf, 0, readCount);
        input.close();
    }

    public static void createJarFile() {
        String proDir = ConfigHolder.CONF.getProjectDir() + File.separator;
        String jarFileName = ConfigHolder.CONF.getProjectName() + "_help.jar";
        File top = new File(proDir);
        List<File> files = new ArrayList<File>();
        buildFileList(top, files);
        File outFile = new File(proDir, jarFileName);
        try {
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outFile));
            JarOutputStream jaroutput = new JarOutputStream(output);
            for (File f : files) {
                if (!f.equals(outFile)) {
                    addFile(f, top, jaroutput);
                    jaroutput.flush();
                }
            }
            jaroutput.close();
            output.close();
            System.out.println("Help jar file created: " + outFile.getAbsolutePath());
        } catch (FileNotFoundException se) {
            System.out.println("Creation of jar file failed, file not found.");
            se.printStackTrace();
        } catch (IOException se) {
            System.out.println("Creation of jar file failed, IO Exception.");
            se.printStackTrace();
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent arg1) {
        doIt();
    }

    public static void doIt() {
        createJarFile();
    }
}
