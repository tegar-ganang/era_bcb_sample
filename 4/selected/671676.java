package org.mhpbox.appman;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author Igor Gatis
 * @since 17/10/2004
 */
public class AppClassLoader extends ClassLoader {

    private Vector loaders;

    public AppClassLoader(File dir, String cp) {
        this.loaders = new Vector();
        File basedir = dir;
        StringTokenizer strToken = new StringTokenizer(cp, File.pathSeparator);
        while (strToken.hasMoreElements()) {
            File file = new File(basedir, strToken.nextToken());
            if (file.isDirectory()) {
                this.loaders.add(new FileLoader(file));
            }
        }
    }

    private static byte[] readClassData(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            while ((read = fis.read(buffer)) > 0) {
                baos.write(buffer, 0, read);
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                }
            }
        }
        return baos.toByteArray();
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        String clazzName = name.replace('.', File.separatorChar);
        byte[] data = null;
        for (int i = 0; i < this.loaders.size(); i++) {
            Loader loader = (Loader) loaders.get(i);
            data = loader.findClass(name);
            if (data != null) {
                return defineClass(name, data, 0, data.length, null);
            }
        }
        throw new ClassNotFoundException();
    }

    interface Loader {

        byte[] findClass(String name);
    }

    class FileLoader implements Loader {

        private File dir;

        public FileLoader(File d) {
            this.dir = d;
        }

        public byte[] findClass(String name) {
            String clazzName = name.replace('.', File.separatorChar);
            File file = new File(dir, clazzName + ".class");
            byte[] data = null;
            try {
                data = readClassData(file);
            } catch (IOException e) {
            }
            return data;
        }
    }

    class JarLoader implements Loader {

        public byte[] findClass(String name) {
            return null;
        }
    }
}
