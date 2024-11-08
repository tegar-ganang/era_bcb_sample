package org.jbrt.client.net.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import org.jbrt.client.JThrowable;
import org.jbrt.client.config.JConfigAtom;
import org.jbrt.client.net.JFileCashedSender;
import org.jbrt.client.net.JBrtFormatter;
import org.jbrt.client.JResponseBean;

/**
 *
 * @author Cipov Peter
 */
public class JFileCashedSenderImpl implements JFileCashedSender {

    private static final String ENCODING = "utf-8";

    private static final String DATA_EXTENTION = ".dat";

    private static final String CONFIG_EXTENTION = ".conf";

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private File rootDir = null;

    private long cashSize = DEFAULT_BUFFER_SIZE;

    public boolean setRootDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return false;
            }
        }
        if (!dir.canRead()) {
            return false;
        }
        if (!dir.canWrite()) {
            return false;
        }
        rootDir = dir;
        return true;
    }

    public File getRootDir() {
        return rootDir;
    }

    /**
     * Set cash size (per file)
     * @param size 0 - no cash
     *             &lt; 0 ulimited
     *             &gt; 0 file size in bytes.
     */
    public void setCasheSize(long size) {
        cashSize = size;
    }

    public long getCashSize() {
        return cashSize;
    }

    public void store(List<JThrowable> throwables, JConfigAtom atom) throws IOException {
        if (cashSize != 0) {
            storeData(throwables, atom);
            storeConfig(atom);
        }
    }

    public JResponseBean post() throws Exception {
        JResponseBean bean = new JResponseBean();
        if (rootDir == null) {
            return bean;
        }
        List<JConfigAtom> atoms = loadConfigs();
        bean.merge(postFiles(atoms));
        return bean;
    }

    public JResponseBean post(List<JThrowable> throwables, JConfigAtom atom) throws Exception {
        JResponseBean bean = new JResponseBean();
        try {
            if (cashSize != 0) {
                bean.merge(post());
            }
        } finally {
            boolean fail = true;
            try {
                bean.merge(postThrowables(throwables, atom));
                fail = false;
            } finally {
                if (fail) {
                    store(throwables, atom);
                }
            }
            return bean;
        }
    }

    private JConfigAtom loadConfig(File file) throws Exception {
        FileInputStream in = null;
        ObjectInputStream stream = null;
        JConfigAtom atom;
        try {
            in = new FileInputStream(file);
            stream = new ObjectInputStream(in);
            atom = (JConfigAtom) stream.readObject();
        } finally {
            try {
                stream.close();
            } finally {
                in.close();
            }
        }
        return atom;
    }

    private List<JConfigAtom> loadConfigs() {
        LinkedList<JConfigAtom> atoms = new LinkedList<JConfigAtom>();
        File[] files = rootDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(CONFIG_EXTENTION);
            }
        });
        for (int i = 0; i < files.length; i++) {
            try {
                atoms.add(loadConfig(files[i]));
            } catch (Exception ex) {
                continue;
            }
        }
        return atoms;
    }

    private JResponseBean postFile(File file, JConfigAtom atom) throws Exception {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int readed;
        JMultipartHttpFilePost connection = new JMultipartHttpFilePost(atom.getUrl());
        FileInputStream stream = new FileInputStream(file);
        connection.startFile("file" + atom.hashCode(), "file.xml");
        try {
            OutputStream out = connection.getOutputStream();
            while ((readed = stream.read(buffer, 0, buffer.length)) >= 0) {
                out.write(buffer, 0, readed);
            }
            out.flush();
            JBrtFormatter.writeLogEnd(new PrintStream(out, true, ENCODING));
            out.flush();
        } finally {
            try {
                connection.endFile();
            } finally {
                stream.close();
            }
        }
        return JBrtFormatter.convertXMLToJResponse(connection.post());
    }

    private JResponseBean postFiles(List<JConfigAtom> atoms) throws IOException {
        JResponseBean bean = new JResponseBean();
        File dataFile;
        File configFile;
        boolean mistake = false;
        Exception exception = null;
        for (JConfigAtom atom : atoms) {
            dataFile = new File(rootDir, atom.hashCode() + DATA_EXTENTION);
            configFile = new File(rootDir, atom.hashCode() + CONFIG_EXTENTION);
            try {
                bean.merge(postFile(dataFile, atom));
                dataFile.delete();
                configFile.delete();
            } catch (Exception ex) {
                mistake = true;
                exception = ex;
            }
        }
        if (bean.getResponses().isEmpty() && mistake) {
            throw new IOException(exception);
        }
        return bean;
    }

    private JResponseBean postThrowables(List<JThrowable> throwables, JConfigAtom atom) throws Exception {
        PrintStream stream = null;
        JMultipartHttpFilePost connection = null;
        try {
            connection = new JMultipartHttpFilePost(atom.getUrl());
            connection.startFile("file" + atom.hashCode(), "file.xml");
            stream = new PrintStream(connection.getOutputStream(), false, ENCODING);
            JBrtFormatter.writeLog(stream, atom, throwables);
            connection.endFile();
            return JBrtFormatter.convertXMLToJResponse(connection.post());
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private void storeConfig(JConfigAtom atom) throws IOException {
        File file = new File(rootDir, atom.hashCode() + CONFIG_EXTENTION);
        FileOutputStream out = null;
        ObjectOutputStream stream = null;
        try {
            out = new FileOutputStream(file, false);
            stream = new ObjectOutputStream(out);
            stream.writeObject(atom);
            stream.flush();
        } finally {
            try {
                stream.close();
            } finally {
                out.close();
            }
        }
    }

    private void storeData(List<JThrowable> throwables, JConfigAtom atom) throws IOException {
        File file = new File(rootDir, atom.hashCode() + DATA_EXTENTION);
        boolean append = false;
        if (cashSize == 0) {
            return;
        }
        if (file.exists()) {
            if (cashSize > 0) {
                if (file.length() > cashSize) {
                    append = false;
                } else {
                    append = true;
                }
            } else {
                append = true;
            }
        }
        PrintStream stream = null;
        try {
            FileOutputStream fos = new FileOutputStream(file, append);
            stream = new PrintStream(fos, false, ENCODING);
            if (!append) {
                JBrtFormatter.writeLogStart(stream, atom);
                JBrtFormatter.writeContent(stream, throwables);
            } else {
                JBrtFormatter.writeContent(stream, throwables);
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    public void clean() throws IOException {
        if (rootDir == null) {
            return;
        }
        File[] files = rootDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(CONFIG_EXTENTION) || name.endsWith(DATA_EXTENTION);
            }
        });
        if (files == null) {
            return;
        }
        for (File file : files) {
            file.delete();
        }
    }

    public JResponseBean simplePost(List<JThrowable> throwables, JConfigAtom atom) throws Exception {
        return postThrowables(throwables, atom);
    }
}
