package org.equivalence.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import org.equivalence.common.FileTransferHeader;

/**
 * This class allows the transfer of an arbitrary number of files from 
 * a stream to disk and vice versa. This means that the files can be sent
 * across a network using socket streams for example
 * The stream is strucutred with an integer first representing the number of files
 * being transferred, then that number of fileHeader objects which tell the
 * reciever what the files are and how large they are. This is then
 * followed by a byte stream containing all the files data. The headers are
 * required such that the program knows when to split the stream into its constituent
 * files
 * @author gcc
 *
 * now added truely atomic updates, no file deletions take place until all files
 * successfully transfer
 */
class FileReciever implements ActionListener {

    private static final int BUFF_SIZE = 16384;

    private static final char PATH_SEP = '/';

    private byte[] buffer = new byte[BUFF_SIZE];

    private boolean cancelled = false;

    private UpdateClient client;

    /**
	 * create a filereciever
	 * @param client a file reciever is owned by an update client
	 */
    public FileReciever(UpdateClient client) {
        this.client = client;
    }

    /**
	 * write files from an inputstream to disk
	 *
	 *@param in the input stream to read the files from
	 *@param tempDir the temporary directory (relative or absolute path) to use for storing files until the transfer has fully completed  
	 */
    public void recieveFiles(InputStream in, String tempDir) {
        File tempFolder = new File(tempDir);
        tempFolder.mkdir();
        try {
            ObjectInputStream objIn = new ObjectInputStream(in);
            BufferedInputStream bis = new BufferedInputStream(in);
            int fileRemCnt = objIn.readInt();
            int fileCnt = objIn.readInt();
            int total = fileRemCnt + fileCnt;
            int completion = 0;
            client.setProgress(0);
            ArrayList<File> removeFiles = getRemoveFileList(objIn, fileRemCnt, total, completion);
            completion = fileRemCnt;
            ArrayList<FileTransferHeader> fileHeaders = new ArrayList<FileTransferHeader>(fileCnt);
            for (int i = 0; i < fileCnt; ++i) {
                FileTransferHeader header = (FileTransferHeader) objIn.readObject();
                if (header.getFileName().indexOf("..") >= 0 || header.getFileName().indexOf(":") >= 0) {
                    throw new Exception("invalid characters found in file header");
                } else {
                    fileHeaders.add(header);
                }
            }
            for (FileTransferHeader fileHeader : fileHeaders) {
                client.transferEvent("adding " + fileHeader.getFileName() + " (" + fileHeader.getFileLength() / 1024 + "kB)");
                if (cancelled) {
                    throw new UserCancelledException();
                }
                long totalRemaining = fileHeader.getFileLength();
                if (totalRemaining == -1) {
                    new File(tempDir + PATH_SEP + fileHeader.getFileName()).mkdirs();
                } else if (totalRemaining == 0L) {
                    mkdirs(tempDir + PATH_SEP + fileHeader.getFileName());
                    new File(tempDir + PATH_SEP + fileHeader.getFileName()).createNewFile();
                } else {
                    mkdirs(tempDir + PATH_SEP + fileHeader.getFileName());
                    FileOutputStream toFile = new FileOutputStream(new File(tempDir + PATH_SEP + fileHeader.getFileName()));
                    while (totalRemaining > 0) {
                        int readLength = totalRemaining > buffer.length ? buffer.length : (int) totalRemaining;
                        totalRemaining -= readLength;
                        bis.read(buffer, 0, readLength);
                        toFile.write(buffer, 0, readLength);
                        if (cancelled) {
                            throw new UserCancelledException();
                        }
                        client.transferEvent("adding " + fileHeader.getFileName() + " (" + (fileHeader.getFileLength() - totalRemaining) / 1024 + "/" + fileHeader.getFileLength() / 1024 + "kB)");
                    }
                    toFile.close();
                }
                client.setProgress((float) ++completion / total);
            }
            if (total > 0) {
                client.completed("complete", "Update completed");
            } else {
                client.completed("noupdate", "Nothing to update");
            }
            copyToFinalDestination(tempDir, removeFiles, fileHeaders);
            objIn.close();
            bis.close();
        } catch (UserCancelledException e) {
            setErrorStatus(e.getMessage());
            cancelled = false;
        } catch (Exception e) {
            setErrorStatus(e.getMessage());
            e.printStackTrace();
        } finally {
            rmdir(tempFolder);
        }
    }

    /**
	 * this method copies all successfully transferred files to their correct destinations
	 * @param tempDir
	 * @param fileHeaders
	 */
    private void copyToFinalDestination(String tempDir, ArrayList<File> removeFiles, ArrayList<FileTransferHeader> fileHeaders) {
        for (File f : removeFiles) {
            rmdir(f);
        }
        for (FileTransferHeader f : fileHeaders) {
            File file = new File(tempDir + PATH_SEP + f.getFileName());
            if (file.exists()) {
                String newName = f.getFileName();
                mkdirs(newName);
                File newFile = new File(newName);
                if (newFile.exists() && !newFile.isDirectory()) {
                    newFile.delete();
                }
                file.renameTo(newFile);
            }
        }
    }

    /**
	 * this method removes a set of files
	 * @param objIn
	 * @param fileRemCnt files to remove
	 * @param total total number of files
	 * @param completion files completed
	 * @throws UserCancelledException
	 * @throws Exception
	 */
    private ArrayList<File> getRemoveFileList(ObjectInputStream objIn, int fileRemCnt, int total, int completion) throws UserCancelledException, Exception {
        ArrayList<File> remList = new ArrayList<File>();
        for (int i = 0; i < fileRemCnt; ++i) {
            String fileName = (String) objIn.readObject();
            client.transferEvent("removing " + fileName);
            if (cancelled) {
                throw new UserCancelledException();
            }
            if (fileName.indexOf("..") >= 0 || fileName.indexOf(":") >= 0) {
                throw new Exception("invalid characters found in file removal path");
            } else {
                File newFile = new File(fileName);
                remList.add(newFile);
            }
            client.setProgress((float) ++completion / total);
        }
        return remList;
    }

    private void setErrorStatus(String description) {
        client.completed("error", "Error: " + description);
    }

    /**
	 * make any directories nessecary to be able to add a file
	 * i.e a file dir1/dir2/file.txt may need the intermediary
	 * dirs created first before it can be created in the location
	 * @param fileName
	 */
    private void mkdirs(String fileName) {
        int pathSep = fileName.lastIndexOf("\\");
        if (pathSep < 0) {
            pathSep = fileName.lastIndexOf("/");
        }
        if (pathSep >= 0) {
            File f = new File(fileName.substring(0, pathSep));
            if (!f.exists()) {
                f.mkdirs();
            }
        }
    }

    /** 
	 *warning this method is dangerous, it recursivly deletes a directory
	 *make sure its told to delete the right place!!!
	 *@author gcconner
	 */
    private void rmdir(File path) {
        if (path.exists() && path.isDirectory()) {
            File[] files = path.listFiles();
            for (File element : files) {
                if (element.isDirectory()) {
                    rmdir(element);
                } else {
                    element.delete();
                }
            }
        }
        path.delete();
    }

    public void actionPerformed(ActionEvent e) {
        cancelled = true;
    }
}
