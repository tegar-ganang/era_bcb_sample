package org.jazzteam.edu.utillDirectory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Utils {

    public Utils() {
        ourFiles = new ArrayList();
        newFiles = new ArrayList();
        nameNewFiles = new ArrayList();
    }

    private final ArrayList ourFiles;

    private final ArrayList newFiles;

    private final ArrayList nameNewFiles;

    public ArrayList getOurFiles() {
        return ourFiles;
    }

    public ArrayList getNewFiles() {
        return newFiles;
    }

    public ArrayList getNameNewFiles() {
        return nameNewFiles;
    }

    public void showOur(String startPath) {
        System.out.println("Our Files");
        File startFile = new File(startPath);
        File srcFiles[] = startFile.listFiles();
        if (startFile.exists()) {
            File afile[];
            int j = (afile = srcFiles).length;
            for (int i = 0; i < j; i++) {
                File f = afile[i];
                if (f.isDirectory()) showOur(f.getPath()); else if (f.isFile()) {
                    System.out.println((new StringBuilder(String.valueOf(f.getName()))).append(" [").append(f.length()).append(" bytes]").toString());
                    ourFiles.add(f.getName());
                }
            }
        }
    }

    public void scan(String startPath) {
        System.out.println("New Files");
        File startFile = new File(startPath);
        File srcFiles[] = startFile.listFiles();
        if (startFile.exists()) {
            File afile[];
            int k = (afile = srcFiles).length;
            for (int j = 0; j < k; j++) {
                File f = afile[j];
                if (f.isDirectory()) scan(f.getPath()); else if (f.isFile()) {
                    for (int i = 0; i < ourFiles.size(); i++) {
                        if (ourFiles.contains(f.getName())) continue;
                        System.out.println((new StringBuilder(String.valueOf(f.getName()))).append(" [").append(f.length()).append(" bytes]").toString());
                        newFiles.add(f.getPath());
                        nameNewFiles.add(f.getName());
                        break;
                    }
                }
            }
        }
    }

    public void updateFiles(String ourPath) {
        System.out.println("Update");
        DataInputStream dis = null;
        DataOutputStream dos = null;
        for (int i = 0; i < newFiles.size() && i < nameNewFiles.size(); i++) {
            try {
                dis = new DataInputStream(new FileInputStream((String) newFiles.get(i)));
                dos = new DataOutputStream(new FileOutputStream((new StringBuilder(String.valueOf(ourPath))).append("\\").append((String) nameNewFiles.get(i)).toString()));
            } catch (IOException e) {
                System.out.println(e.toString());
                System.exit(0);
            }
            try {
                do dos.writeChar(dis.readChar()); while (true);
            } catch (EOFException e) {
            } catch (IOException e) {
                System.out.println(e.toString());
            }
        }
    }
}
