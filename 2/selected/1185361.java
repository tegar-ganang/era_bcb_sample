package org.xsocket.stream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
*
* @author grro@xsocket.org
*/
final class JavaProcess {

    private ConsoleReader reader = null;

    private Process p = null;

    public void start(String classname, String jarFileURI, String... params) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                terminate();
            }
        });
        System.out.println("loading jar file " + jarFileURI + " ...");
        URL url = new URL(jarFileURI);
        InputStream is = url.openStream();
        File jarFile = File.createTempFile("test", "test");
        jarFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(jarFile);
        byte[] buffer = new byte[4096];
        int bytes_read;
        while ((bytes_read = is.read(buffer)) != -1) {
            fos.write(buffer, 0, bytes_read);
        }
        fos.close();
        is.close();
        String[] args = new String[params.length + 4];
        args[0] = "java";
        args[1] = "-cp";
        args[2] = jarFile.getAbsolutePath();
        args[3] = classname;
        System.arraycopy(params, 0, args, 4, params.length);
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg + " ");
        }
        System.out.println("execute " + sb);
        ProcessBuilder pb = new ProcessBuilder(args);
        p = pb.start();
        reader = new ConsoleReader(p.getInputStream());
        new Thread(reader).start();
    }

    public void terminate() {
        try {
            p.destroy();
            reader.terminate();
        } catch (Exception ignore) {
        }
    }

    private static final class ConsoleReader implements Runnable {

        private Thread t = null;

        private InputStream is = null;

        ConsoleReader(InputStream is) {
            this.is = is;
        }

        public void run() {
            t = Thread.currentThread();
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line + "\r\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void terminate() {
            t.interrupt();
        }
    }
}
