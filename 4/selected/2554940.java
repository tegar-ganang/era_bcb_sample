package de.imedic.webrcp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Thread which unpacks files. The unpacking is moved in a seperate thread
 * because it can be done concurrently with the downloads.
 * 
 * @author Daniel Mendler <mendler@imedic.de>
 */
class UnpackThread extends Thread {

    private List fileList;

    private File destDir;

    private boolean finished, cleanUp;

    /**
	 * Constructor
	 */
    public UnpackThread(File destDir, boolean cleanUp) {
        this.destDir = destDir;
        this.cleanUp = cleanUp;
        fileList = new Vector();
        finished = false;
        start();
    }

    public void run() {
        if (cleanUp) cleanUpDestDir();
        while (!finished || !fileList.isEmpty()) {
            if (fileList.isEmpty()) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
            if (fileList.isEmpty()) return;
            unpackFile((File) fileList.remove(0));
        }
    }

    /**
	 * Add file to list of files which need unpacking
	 */
    public void addNextFile(File file) {
        fileList.add(file);
        synchronized (this) {
            notifyAll();
        }
    }

    /**
	 * Wait for thread to finish
	 */
    public void finish() {
        finished = true;
        synchronized (this) {
            notifyAll();
        }
        try {
            join();
        } catch (InterruptedException ex) {
        }
    }

    private void unpackFile(final File file) {
        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                File entryFile = new File(destDir, entry.getName());
                if (entryFile.exists()) continue;
                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                    if (!entryFile.exists() || !entryFile.isDirectory()) throw new IOException("Couldn't create directory");
                } else {
                    InputStream in = zipFile.getInputStream(entry);
                    OutputStream out = new FileOutputStream(entryFile);
                    byte[] buffer = new byte[32768];
                    int size;
                    while ((size = in.read(buffer)) > 0) out.write(buffer, 0, size);
                    in.close();
                    out.close();
                }
            }
            zipFile.close();
        } catch (final IOException ex) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        JOptionPane.showMessageDialog(null, "Temporary Zip-File " + file + " couldn't be extracted: " + ex, "Extraction Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex2) {
            }
            System.exit(1);
        }
    }

    private void cleanUpDestDir() {
        if (destDir.exists()) {
            File[] file = destDir.listFiles();
            for (int i = 0; i < file.length; ++i) {
                if (!file[i].getName().equals("workspace")) removeRecursively(file[i]);
            }
        }
    }

    private void removeRecursively(File dir) {
        if (!dir.exists()) return;
        if (!dir.isDirectory()) dir.delete();
        File[] file = dir.listFiles();
        if (file != null) {
            for (int i = 0; i < file.length; ++i) removeRecursively(file[i]);
        }
        dir.delete();
    }
}
