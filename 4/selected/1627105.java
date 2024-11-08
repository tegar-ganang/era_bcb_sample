package net.chowda.castcluster.jnlp;

import net.chowda.castcluster.Main;
import javax.swing.*;
import java.net.URL;
import java.io.*;

/**
 * This class just kicks off the same thing that we would be using from the command line
 * it just wraps it in a pretty window and handles the delivery issues with JNLP...
 * like extracting the war file from the jar.
 */
public class JNLPMain {

    public static void main(String[] args) throws Exception {
        URL warFile = Main.class.getClassLoader().getResource("castcluster.war");
        File tempFile = File.createTempFile("castcluster", ".war");
        try {
            copyStream(warFile.openConnection().getInputStream(), new FileOutputStream(tempFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String warFilePath = tempFile.getAbsolutePath();
        System.setSecurityManager(null);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new GUIDialog().setVisible(true);
        Main.deployWar(warFilePath);
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }
}
