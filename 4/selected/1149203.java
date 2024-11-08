package net.sf.cclearly.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.StringTokenizer;
import net.sf.cclearly.launcher.md5.MD5;

public class Client {

    private static final byte OP_START = 1;

    private class FileDescriptor {

        String name = "";

        String hash = "";

        long size;
    }

    private File downloadFolder = new File("update");

    private LinkedList<FileDescriptor> descriptors = new LinkedList<FileDescriptor>();

    private LinkedList<FileDescriptor> newFiles = new LinkedList<FileDescriptor>();

    private LinkedList<FileDescriptor> updatedFiles = new LinkedList<FileDescriptor>();

    /**
     * @param args
     */
    public static void main(String[] args) {
        Client c = new Client();
        c.launch(args);
    }

    public boolean sendActivateMessage(int monitorPort) {
        try {
            Socket client = new Socket(InetAddress.getLocalHost(), monitorPort);
            OutputStream out = client.getOutputStream();
            out.write(OP_START);
            client.close();
            return true;
        } catch (UnknownHostException e) {
            return false;
        } catch (SocketException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public void launch(String[] args) {
        update();
        if (sendActivateMessage(34938)) {
            return;
        }
        waitForUnlock();
        try {
            StringBuilder command = new StringBuilder("cClearly.exe");
            for (int i = 0; i < args.length; i++) {
                command.append(" ");
                command.append(args[i]);
                if ("-minimized".equals(args[i])) {
                    command.append(" --l4j-no-splash");
                }
            }
            Runtime.getRuntime().exec(command.toString(), null, new File("."));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitForUnlock() {
        File dataFolder;
        if (System.getProperty("user.home") != null) {
            dataFolder = new File(System.getProperty("user.home") + "/cClearly");
        } else {
            dataFolder = new File("data");
        }
        File dbFolder = new File(dataFolder, "data");
        File lockFile = new File(dbFolder, "db.lck");
        if (!lockFile.exists()) {
            return;
        }
        while (lockFile.exists() && (!canObtainLock(lockFile))) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lockFile.delete();
    }

    private boolean canObtainLock(File lockFile) {
        try {
            RandomAccessFile in = new RandomAccessFile(lockFile, "rw");
            FileChannel channel = in.getChannel();
            FileLock lock = channel.tryLock();
            if (lock == null) {
                return false;
            }
            lock.release();
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void update() {
        if (!downloadFolder.exists()) {
            return;
        }
        if (!initializeHashList()) {
            System.out.println("Updater: Could not initialize hash list");
            return;
        }
        if (!compare()) {
            System.out.println("Updater: Could not compare.");
            return;
        }
        if (!verifyUpdate()) {
            System.out.println("Updater: Could not verify");
            return;
        }
        updateFolder(downloadFolder, new File("."));
    }

    public boolean compare() {
        boolean result = false;
        newFiles.clear();
        updatedFiles.clear();
        for (FileDescriptor file : descriptors) {
            File localFile = new File(file.name);
            String localHash = getFileHash(localFile);
            if (file.hash.equals(localHash)) {
                continue;
            }
            if (localFile.exists()) {
                updatedFiles.add(file);
            } else {
                newFiles.add(file);
            }
            result = true;
        }
        return result;
    }

    private boolean verifyUpdate() {
        for (FileDescriptor fileDesc : newFiles) {
            File file = new File(downloadFolder, fileDesc.name);
            if (!file.exists()) {
                return false;
            }
            String hash = getFileHash(file);
            if (!hash.equals(fileDesc.hash) || (file.length() != fileDesc.size)) {
                return false;
            }
        }
        for (FileDescriptor fileDesc : updatedFiles) {
            File file = new File(downloadFolder, fileDesc.name);
            if (!file.exists()) {
                return false;
            }
            String hash = getFileHash(file);
            if (!hash.equals(fileDesc.hash) || (file.length() != fileDesc.size)) {
                return false;
            }
        }
        return true;
    }

    private void updateFolder(File updateFolder, File parentFolder) {
        if (!parentFolder.exists()) {
            if (!parentFolder.mkdir()) {
                return;
            }
        }
        for (File file : updateFolder.listFiles()) {
            if (file.getName().equals("files.md5")) {
                continue;
            }
            File destFile = new File(parentFolder, file.getName());
            if (file.isDirectory()) {
                updateFolder(file, destFile);
                continue;
            }
            if (destFile.exists()) {
                if (!destFile.delete()) {
                    System.out.println("Could not delete " + destFile + " to update from " + file);
                    continue;
                }
            }
            if (file.renameTo(destFile)) {
                System.out.println("Updated " + file + " to " + destFile);
            } else {
                System.out.println("Could not update " + file + " to " + destFile);
            }
        }
    }

    private boolean initializeHashList() {
        File file = new File(downloadFolder, "files.md5");
        if (!file.exists()) {
            return false;
        }
        try {
            descriptors.clear();
            Scanner scanner = new Scanner(new FileInputStream(file));
            scanner.useDelimiter(System.getProperty("line.separator"));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                StringTokenizer tokenizer = new StringTokenizer(line, ",");
                int elmCount = 0;
                FileDescriptor descriptor = new FileDescriptor();
                while (tokenizer.hasMoreTokens()) {
                    String element = tokenizer.nextToken();
                    switch(elmCount) {
                        case 0:
                            descriptor.name = element;
                            break;
                        case 1:
                            descriptor.hash = element;
                            break;
                        case 2:
                            descriptor.size = Long.valueOf(element);
                            break;
                    }
                    elmCount++;
                }
                if ((descriptor.name.length() > 0) && (descriptor.hash.length() > 0)) {
                    descriptors.add(descriptor);
                }
            }
            scanner.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private String getFileHash(File file) {
        String hash = "";
        if (file.exists()) {
            try {
                hash = MD5.asHex(MD5.getHash(new File(file.getAbsolutePath())));
            } catch (IOException e) {
                System.out.println(e.toString());
            }
        }
        return hash;
    }
}
