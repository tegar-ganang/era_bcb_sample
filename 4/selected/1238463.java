package com.duroty.utils.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * DOCUMENT ME!
 *
 * @author jordi marques
 */
public class CopyFiles {

    /**
     * Creates a new instance of CopyFiles
     *
     * @param inputFileName DOCUMENT ME!
     * @param outputFileName DOCUMENT ME!
     * @param overWrite DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public static void start(String inputFileName, String outputFileName, boolean overWrite) throws IOException {
        File inf = new File(inputFileName);
        if (!inf.exists()) {
            throw new IOException("The input file no exists");
        }
        if (inf.isDirectory()) {
            throw new IOException("The input file is directory");
        }
        if (!inf.canRead()) {
            throw new IOException("The input file can't read");
        }
        File outf = new File(outputFileName);
        if (outf.exists() && !overWrite) {
            throw new IOException("The out file exist, user overWrite = true");
        }
        RandomAccessFile randomWrite = new RandomAccessFile(outf, "rw");
        RandomAccessFile randomRead = new RandomAccessFile(inf, "rw");
        byte[] by = new byte[500];
        int byteread = 0;
        while ((byteread = randomRead.read(by)) != -1) {
            randomWrite.write(by, 0, byteread);
        }
        randomRead.close();
        randomRead = null;
        randomWrite.close();
        randomWrite = null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CopyFiles.start("c:/work/html/merda.html", "c:/rana.html", true);
        } catch (IOException ioe) {
        }
    }
}
