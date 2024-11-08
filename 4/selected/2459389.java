package org.openje.http.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public abstract class WarClassLoader extends ClassLoader {

    protected abstract void init(File war) throws HJasperException;

    public static WarClassLoader getWarClassLoader(File war) throws HJasperException {
        WarClassLoader loader = war.isDirectory() ? new WarDirClassLoader() : new WarFileClassLoader();
        loader.init(war);
        return loader;
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] b = loadClassImage(name);
        return defineClass(name, b, 0, b.length);
    }

    protected abstract byte[] loadClassImage(String name) throws ClassNotFoundException;

    public static class WarDirClassLoader extends WarClassLoader {

        private File warDir;

        public void init(File war) {
            this.warDir = war;
        }

        protected byte[] loadClassImage(String name) throws ClassNotFoundException {
            String path = warDir.getAbsolutePath() + "/WEB-INF/classes/" + name.replace(".", "/") + ".class";
            FileInputStream in = null;
            try {
                in = new FileInputStream(path);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte bs[] = new byte[1024];
                int count;
                while ((count = in.read(bs)) > 0) out.write(bs, 0, count);
                return out.toByteArray();
            } catch (FileNotFoundException e) {
                throw new ClassNotFoundException(name);
            } catch (IOException e) {
                throw new ClassNotFoundException(name);
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static class WarFileClassLoader extends WarClassLoader {

        private ZipFile zip;

        public void init(File war) throws HJasperException {
            try {
                zip = new ZipFile(war);
            } catch (ZipException e) {
                throw new HJasperException(e);
            } catch (IOException e) {
                throw new HJasperException(e);
            }
        }

        protected byte[] loadClassImage(String name) throws ClassNotFoundException {
            String path = "/WEB-INF/classes/" + name.replace(".", "/") + ".class";
            ZipEntry entry = zip.getEntry(path);
            InputStream in = null;
            try {
                in = zip.getInputStream(entry);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte bs[] = new byte[1024];
                int count;
                while ((count = in.read(bs)) > 0) out.write(bs, 0, count);
                return out.toByteArray();
            } catch (IOException e) {
                throw new ClassNotFoundException(name);
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
