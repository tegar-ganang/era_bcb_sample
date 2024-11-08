package org.equivalence.server;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import org.equivalence.common.FileTransferHeader;

/**
 * This class allows the transfer of an arbitrary number of files from 
 * a disk to stream. 
 * @author gcc
 *
 */
class FileSender {

    private ArrayList<File> files;

    private ArrayList<File> removeFiles;

    private static final int BUFF_SIZE = 16384;

    private byte[] buffer = new byte[BUFF_SIZE];

    public FileSender() {
        files = new ArrayList<File>();
        removeFiles = new ArrayList<File>();
    }

    public void addFile(String filename) {
        File newFile = new File(filename);
        files.add(newFile);
    }

    public void deleteFile(String filename) {
        File newFile = new File(filename);
        removeFiles.add(newFile);
    }

    public void sendFiles(OutputStream os) throws Exception {
        BufferedOutputStream bos = new BufferedOutputStream(os);
        ObjectOutputStream objOut = new ObjectOutputStream(os);
        objOut.writeInt(removeFiles.size());
        objOut.writeInt(files.size());
        objOut.flush();
        for (File remFile : removeFiles) {
            objOut.writeObject(remFile.getPath());
        }
        objOut.flush();
        for (File file : files) {
            FileTransferHeader header;
            if (file.isDirectory()) {
                header = new FileTransferHeader(file.getPath(), -1);
            } else {
                header = new FileTransferHeader(file.getPath(), file.length());
            }
            objOut.writeObject(header);
        }
        objOut.flush();
        for (File file : files) {
            if (file.length() != 0L) {
                FileInputStream fis = new FileInputStream(file);
                int read = 0;
                while ((read = fis.read(buffer)) > 0) {
                    bos.write(buffer, 0, read);
                }
                bos.flush();
                fis.close();
            }
        }
        objOut.close();
        bos.close();
    }
}
