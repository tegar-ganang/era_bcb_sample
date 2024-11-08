package de.hu.gralog.gui.data;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarLoader extends ClassLoader {

    private boolean printLoadMessages = true;

    private Hashtable classArrays = new Hashtable();

    public JarLoader(ClassLoader parent) {
        super(parent);
    }

    private byte[] getClassBytes(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        boolean eof = false;
        while (!eof) {
            try {
                int i = bis.read();
                if (i == -1) eof = true; else baos.write(i);
            } catch (IOException e) {
                return null;
            }
        }
        return baos.toByteArray();
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String urlName = name.replace('.', '/');
        byte buf[];
        Class cl;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            int i = name.lastIndexOf('.');
            if (i >= 0) sm.checkPackageDefinition(name.substring(0, i));
        }
        buf = (byte[]) classArrays.get(urlName);
        if (buf != null) {
            cl = defineClass(name, buf, 0, buf.length);
            return cl;
        }
        throw new ClassNotFoundException(name);
    }

    public void readJarFile(JarFile jarFile) {
        try {
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = (JarEntry) entries.nextElement();
                String jarName = je.getName();
                if (jarName.endsWith(".class")) loadClassBytes(jarFile, je);
            }
        } catch (IOException ioe) {
            System.out.println("Badly formatted jar file");
        }
    }

    private void loadClassBytes(JarFile jis, JarEntry jarName) throws IOException {
        if (printLoadMessages) System.out.println("\t" + jarName);
        BufferedInputStream jarBuf = new BufferedInputStream(jis.getInputStream(jarName));
        ByteArrayOutputStream jarOut = new ByteArrayOutputStream();
        int b;
        try {
            while ((b = jarBuf.read()) != -1) jarOut.write(b);
            classArrays.put(jarName.getName().substring(0, jarName.getName().length() - 6), jarOut.toByteArray());
        } catch (IOException ioe) {
            System.out.println("Error reading entry " + jarName);
        }
    }
}
