package com.andrewj.parachute.network;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import com.andrewj.parachute.ui.UserInterface;

/**
 * This class handles sending of files and also copies/deletions
 * communications from this class are received by {@link Receiver}
 * @author Andy Jobson (andyjobson85@gmail.com)
 *
 */
public class Sender extends Transferer implements Runnable {

    private ArrayList<String> aActions;

    private DataOutputStream dos;

    public Sender(DataOutputStream out, File f, UserInterface u) {
        super(f, u);
        this.dos = out;
        super.setThread(new Thread(this), "Sender");
    }

    /**
	 * Set the queue of actions for this sender. After the queue is set, startTransferer should be called
	 * @param a An ArrayList of actions as determined by SynchronizerMaster
	 */
    public void setQueue(ArrayList<String> a) {
        aActions = a;
    }

    /**
	 * Convenience method to delete a File or Folder. In the case of a folder, this will recurse and delete all children
	 * @param f The file/folder to be deleted
	 */
    private void delete(File f) {
        File[] files = f.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    delete(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        f.delete();
    }

    /**
	 * Copy a file from source to destination
	 * @param s the source file to copy
	 * @param d the destination
	 * @throws IOException
	 */
    private void copyFile(File s, File d) throws IOException {
        d.getParentFile().mkdirs();
        FileChannel inChannel = new FileInputStream(s).getChannel();
        FileChannel outChannel = new FileOutputStream(d).getChannel();
        int maxCount = (64 * 1024 * 1024) - (32 * 1024);
        long size = inChannel.size();
        long position = 0;
        while (position < size) {
            position += inChannel.transferTo(position, maxCount, outChannel);
        }
        inChannel.close();
        outChannel.close();
        d.setLastModified(s.lastModified());
    }

    @Override
    public void run() {
        Iterator<String> it = aActions.iterator();
        while (it.hasNext() && !bStopped) {
            String sAction = it.next();
            switch(sAction.charAt(0)) {
                case SynchronizerMaster.SEND:
                    String relPath = sAction.substring(sAction.indexOf(";") + 1);
                    File f = new File(fSharedFolder.getPath() + File.separatorChar + relPath);
                    try {
                        dos.writeByte(RECEIVE);
                        dos.writeUTF(relPath);
                        if (f.isDirectory()) {
                            dos.writeBoolean(true);
                        } else {
                            dos.writeBoolean(false);
                            long size = f.length();
                            dos.writeLong(size);
                            dos.writeLong(f.lastModified());
                            FileInputStream fis = new FileInputStream(f);
                            byte[] b = new byte[Transferer.BUFF_SIZE];
                            long total = 0;
                            int bytesRead;
                            while ((bytesRead = fis.read(b)) != -1) {
                                dos.write(b, 0, bytesRead);
                                total += bytesRead;
                                ui.updateOutProgBar(relPath, (int) (((double) total / (double) size) * 100));
                                if (bStopped) {
                                    throw new Exception();
                                }
                            }
                            dos.flush();
                            fis.close();
                            ui.addOutMessage(relPath);
                        }
                    } catch (Exception e) {
                        ui.addErrorMessage("Sender error: " + e.getMessage());
                        bStopped = true;
                    } finally {
                        ui.updateOutProgBar("", 0);
                    }
                    break;
                case SynchronizerMaster.COPY:
                    File source = new File(fSharedFolder.getPath() + File.separatorChar + sAction.substring(sAction.indexOf(";") + 1, sAction.lastIndexOf(";")));
                    File dest = new File(fSharedFolder.getPath() + File.separatorChar + sAction.substring(sAction.lastIndexOf(";") + 1));
                    try {
                        copyFile(source, dest);
                        ui.addCopyMessage(sAction.substring(sAction.indexOf(";") + 1, sAction.lastIndexOf(";")), sAction.substring(sAction.lastIndexOf(";") + 1));
                        aRecord.add(ADDED_RECORD + ";" + sAction.substring(sAction.lastIndexOf(";") + 1));
                    } catch (IOException e) {
                        ui.addErrorMessage("failed to copy file: " + source.getPath() + " to " + dest.getPath());
                    }
                    try {
                        dos.writeByte(ADD_ELEMENT);
                        dos.writeUTF(sAction.substring(sAction.lastIndexOf(";") + 1));
                    } catch (Exception e) {
                        ui.addErrorMessage("failed to communicate to add an element - COPY");
                    }
                    break;
                case SynchronizerMaster.DELE:
                    delete(new File(fSharedFolder.getPath() + File.separator + sAction.substring(sAction.indexOf(";") + 1)));
                    ui.addDeleteMessage(sAction.substring(sAction.indexOf(";") + 1));
                    aRecord.add(DELETED_RECORD + ";" + sAction.substring(sAction.indexOf(";") + 1));
                    try {
                        dos.writeByte(DELETE);
                        dos.writeUTF(sAction.substring(sAction.indexOf(";") + 1));
                    } catch (Exception e) {
                    }
                    break;
            }
        }
        if (!bStopped) {
            try {
                dos.writeByte(END);
            } catch (Exception e) {
                ui.addErrorMessage("Sender could not send terminator");
            }
        }
        System.out.println("Sender shutting down");
    }
}
