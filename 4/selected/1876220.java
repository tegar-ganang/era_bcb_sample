package com.netx.test.lib.jcifs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import jcifs.smb.*;

public class NTJcifs {

    public static void main(String[] args) throws Throwable {
        NTJcifs nt = new NTJcifs();
        nt.checkReadOnly();
        nt.println("done.");
    }

    public void print(Object o) {
        System.out.print(o);
    }

    public void println(Object o) {
        System.out.println(o);
    }

    public void reachFile() throws Exception {
        SmbFileInputStream in = new SmbFileInputStream("smb://boss:boss@boss/boss/Desktop/TEXTO.txt");
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) {
            System.out.write(b, 0, n);
        }
    }

    public void checkFileAccess() throws IOException {
        String fileName = "X:\\00 - Diversos\\ROKS.zip";
        File file = new File(fileName);
        println("Absolute path:  " + file.getAbsolutePath());
        println("Canonical path: " + file.getCanonicalPath());
        println("Name:           " + file.getName());
        println("Parent:         " + file.getParent());
        println("Path:           " + file.getPath());
        println(file.exists());
    }

    public void copyFile() throws Exception {
        SmbFile file = new SmbFile("smb://elsa:elsa@elsa/Elsa/Desktop/Ficheiros2/04-04-2066/How To Make a Flash Preloader.doc");
        println("length: " + file.length());
        SmbFileInputStream in = new SmbFileInputStream(file);
        println("available: " + in.available());
        File dest = new File("C:\\Documents and Settings\\Carlos\\Desktop\\Flash Preloader.doc");
        FileOutputStream out = new FileOutputStream(dest);
        int buffer_length = 1024;
        byte[] buffer = new byte[buffer_length];
        while (true) {
            int bytes_read = in.read(buffer, 0, buffer_length);
            if (bytes_read <= 0) {
                break;
            }
            out.write(buffer, 0, bytes_read);
        }
        in.close();
        out.close();
        println("done.");
    }

    public void listFiles() throws IOException {
        SmbFile file = new SmbFile("smb://Carlos_Da_S_Pereira:lpicldht6+@carlos-pereira.accenture.com/");
        println("listing files from: " + file.getCanonicalPath());
        SmbFile[] list = file.listFiles();
        for (int i = 0; i < list.length; i++) {
            println(list[i].getName());
        }
    }

    public void listWorkgroups() throws IOException {
        println(jcifs.Config.getProperty("jcifs.smb.client.domain"));
        SmbFile file = new SmbFile("smb://");
        SmbFile[] list = file.listFiles();
        for (int i = 0; i < list.length; i++) {
            println(list[i].getName());
        }
    }

    public void listServerInfo() throws IOException {
        SmbFile file = new SmbFile("smb://Carlos_Da_S_Pereira:lpicldht11+@carlos-pereira/Shared");
        println("getDiskFreeSpace: " + file.getDiskFreeSpace());
        println("getName: " + file.getName());
        println("getParent: " + file.getParent());
        println("getServer: " + file.getServer());
        println("getShare: " + file.getShare());
        println("getUncPath: " + file.getUncPath());
        println("URL:");
        URL url = file.getURL();
        println("getFile: " + url.getFile());
        println("getHost: " + url.getHost());
        println("getPath: " + url.getPath());
        println("getPort: " + url.getPort());
        println("getProtocol: " + url.getProtocol());
        println("getUserInfo: " + url.getUserInfo());
        println("InetAddress:");
        InetAddress[] list = InetAddress.getAllByName(url.getHost());
        for (int i = 0; i < list.length; i++) {
            println(list[i].toString());
        }
        println("localhost: " + InetAddress.getLocalHost());
    }

    public void accessVvpServer() throws IOException {
        SmbFile file = new SmbFile("smb://accenture:123@srvvvp005/");
        println("listing files from: " + file.getCanonicalPath());
        SmbFile[] list = file.listFiles();
        for (int i = 0; i < list.length; i++) {
            println(list[i].getName());
        }
    }

    public void checkReadOnly() throws Exception {
        SmbFile file = new SmbFile("smb://VAIO/BACKUP/20081130/Favorites.rar");
        println(file.getURL());
        println("archive: " + _checkAttribute(file, SmbFile.ATTR_ARCHIVE));
        println("directory: " + _checkAttribute(file, SmbFile.ATTR_DIRECTORY));
        println("hidden: " + _checkAttribute(file, SmbFile.ATTR_HIDDEN));
        println("read-only: " + _checkAttribute(file, SmbFile.ATTR_READONLY));
        println("system: " + _checkAttribute(file, SmbFile.ATTR_SYSTEM));
        println("volume: " + _checkAttribute(file, SmbFile.ATTR_VOLUME));
    }

    private boolean _checkAttribute(SmbFile file, int attr) throws SmbException {
        return (file.getAttributes() & attr) == attr;
    }
}
