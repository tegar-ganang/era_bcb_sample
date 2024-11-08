package org.hfbk.util;

import java.applet.Applet;
import java.applet.AppletStub;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.hfbk.vis.Prefs;

public class JARStarter extends Applet implements AppletStub {

    static String[] jars = new String[] { ".", "lwjgl.jar", "lwjgl_util.jar", "jna.jar", "ffmpeg.jar", "jinput.jar", "js.jar", "svg.jar" };

    static String mainclass = "org.hfbk.vis.VisClient";

    public void init() {
        try {
            List<String> args = new ArrayList<String>();
            for (Field f : Prefs.class.getFields()) {
                String key = f.getName();
                String value = getParameter(key);
                if (value != null) {
                    args.add(key);
                    args.add(value);
                }
            }
            main(args.toArray(new String[0]));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        String cp = join(File.pathSeparatorChar, jars);
        List<String> cmd = new LinkedList<String>();
        cmd.addAll(Arrays.asList(new String[] { "java", "-Xmx1000m", "-cp", cp, mainclass }));
        cmd.addAll(Arrays.asList(args));
        Map<String, String> env = System.getenv();
        String[] newenv = new String[env.size() + 1];
        int i = 0;
        for (Map.Entry e : env.entrySet()) newenv[i++] = e.getKey() + "=" + e.getValue();
        newenv[i++] = "LD_LIBRARY_PATH=.";
        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        Installer.install(tmpdir);
        String[] tmp = cmd.toArray(new String[0]);
        Process p = Runtime.getRuntime().exec(tmp, newenv, new File(tmpdir.getAbsolutePath(), File.separatorChar + "VisClient"));
        new Pipe(p.getInputStream(), System.out);
        new Pipe(p.getErrorStream(), System.err);
    }

    static String join(char c, String[] strings) {
        StringBuilder b = new StringBuilder();
        for (String s : strings) {
            b.append(s);
            b.append(c);
        }
        b.deleteCharAt(b.length() - 1);
        return b.toString();
    }

    /** a simple thread pumping data from an input stream into an output stream. */
    static class Pipe extends Thread {

        InputStream in;

        OutputStream out;

        public Pipe(InputStream in, OutputStream out) {
            setName("Pipe");
            this.in = in;
            this.out = out;
            start();
        }

        public void run() {
            int bytesRead;
            byte[] buffer = new byte[1024];
            try {
                while ((bytesRead = in.read(buffer)) > 0) out.write(buffer, 0, bytesRead);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void appletResize(int width, int height) {
    }
}
