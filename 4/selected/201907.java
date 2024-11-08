package de.uni_bremen.informatik.p2p.peeranha42.core.network.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.PropertyResourceBundle;
import de.uni_bremen.informatik.p2p.peeranha42.util.SystemInformation;

public class PeeranhaRunner {

    public static void main(final String[] args) {
        final String fs = File.separator;
        final String baseDir = SystemInformation.dirname(System.getProperty("java.class.path"));
        final String s_updates = baseDir + "update.properties";
        String[] cmd = new String[args.length + 3];
        cmd[0] = "java";
        cmd[1] = "-jar";
        if (baseDir.equals("")) cmd[2] = "peeranha42-dist.jar"; else cmd[2] = baseDir + "peeranha42-dist.jar";
        for (int i = 0; i < args.length; i++) {
            cmd[i + 3] = args[i];
        }
        Executor exec = new Executor(cmd);
        exec.run();
        FileInputStream res = null;
        try {
            res = new FileInputStream(s_updates);
        } catch (FileNotFoundException e) {
            System.out.println("No updates required");
            return;
        }
        PropertyResourceBundle updates = null;
        try {
            updates = new PropertyResourceBundle(res);
        } catch (IOException e) {
            System.out.println("Cannot access update file. Please check permissions.");
            return;
        }
        final Enumeration en = updates.getKeys();
        while (en.hasMoreElements()) {
            final String src = (String) en.nextElement();
            final String dest = updates.getString(src);
            System.out.println("About to copy " + src + " to " + dest);
            copy(src, dest);
            final File f_src = new File(src);
            f_src.delete();
        }
        final File f_updates = new File(s_updates);
        f_updates.delete();
    }

    public static void copy(final String source, final String dest) {
        final File s = new File(source);
        final File w = new File(dest);
        try {
            final FileInputStream in = new FileInputStream(s);
            final FileOutputStream out = new FileOutputStream(w);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (IOException ioe) {
            System.out.println("Error reading/writing files!");
        }
    }
}
