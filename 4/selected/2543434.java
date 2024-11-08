package com.qaessentials.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author rwall
 * 
 */
public class FileUtils {

    public class Permissions {

        private String owner = null;

        private String group = null;

        private String permissions = null;

        public Permissions(String permissions, String owner, String group) {
            this.permissions = permissions.trim();
            this.group = group.trim();
            this.owner = owner.trim();
        }

        public Permissions(boolean all, String owner, String group) {
            if (all) this.permissions = "-rwxrwxrwx"; else this.permissions = "-rwxr--r--";
            this.group = group.trim();
            this.owner = owner.trim();
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group.trim();
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner.trim();
        }

        public String getFilePermissions() {
            return permissions;
        }

        public void setFilePermissions(String permissions) {
            this.permissions = permissions.trim();
        }

        public String getUserPermissions() {
            String tmp = permissions.substring(1, 4).trim();
            return tmp.replaceAll("-", "");
        }

        public String getGroupPermissions() {
            String tmp = permissions.substring(4, 7).trim();
            return tmp.replaceAll("-", "");
        }

        public String getOtherPermissons() {
            String tmp = permissions.substring(7, 10).trim();
            return tmp.replaceAll("-", "");
        }
    }

    public static Permissions getFilePermissons(File in) throws Exception {
        String tmp = null;
        try {
            tmp = new Exec().exec(new String[] { "ls", "-la", in.getAbsolutePath() });
        } catch (Exception E) {
            E.printStackTrace();
            return null;
        }
        if (tmp == null) return null;
        String[] permissions = tmp.split(" ");
        return (new FileUtils()).new Permissions(permissions[0], permissions[2], permissions[3]);
    }

    public static void setFilePermissions(File in, Permissions set) throws Exception {
        try {
            new Exec().exec("chmod -R u=" + set.getUserPermissions() + ",g=" + set.getGroupPermissions() + ",o=" + set.getOtherPermissons() + " " + in.getAbsolutePath());
            new Exec().exec("chown -R " + set.getOwner() + ":" + set.getGroup() + " " + in.getAbsolutePath());
        } catch (Exception E) {
            E.printStackTrace();
            return;
        }
    }

    public static void setFileOwner(String in, String owner, String group) throws Exception {
        setFileOwner(new File(in), owner, group);
    }

    public static void setFileOwner(File in, String owner, String group) throws Exception {
        try {
            new Exec().exec("chown -R " + owner.trim() + ":" + group.trim() + " " + in.getAbsolutePath());
        } catch (Exception E) {
            E.printStackTrace();
            return;
        }
    }

    /**
	 * Copies a file.
	 * 
	 * @param in
	 * @param out
	 * @throws Exception
	 */
    public static void copyFile(File in, File out) throws Exception {
        Permissions before = getFilePermissons(in);
        FileChannel inFile = new FileInputStream(in).getChannel();
        FileChannel outFile = new FileOutputStream(out).getChannel();
        inFile.transferTo(0, inFile.size(), outFile);
        inFile.close();
        outFile.close();
        setFilePermissions(out, before);
    }

    /**
	 * Copies a file.
	 * 
	 * @param in
	 * @param out
	 * @throws Exception
	 */
    public static void copyFile(String in, String out) throws Exception {
        copyFile(new File(in), new File(out));
    }

    public static void deleteDir(File dir) {
        if (dir.isDirectory() && !dir.getAbsolutePath().matches("/")) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                deleteDir(new File(dir, children[i]));
            }
        }
        dir.delete();
        return;
    }

    public static void unzip(String zipfile, String outputDirectory) {
        try {
            byte[] buf = new byte[1024];
            ZipInputStream zipinputstream = null;
            ZipEntry zipentry;
            zipinputstream = new ZipInputStream(new FileInputStream(zipfile));
            zipentry = zipinputstream.getNextEntry();
            while (zipentry != null) {
                String entryName = zipentry.getName();
                System.out.println("entryname " + entryName);
                int n;
                FileOutputStream fileoutputstream;
                File newFile = new File(entryName);
                String directory = newFile.getParent();
                if (directory == null) {
                    if (newFile.isDirectory()) break;
                }
                fileoutputstream = new FileOutputStream(outputDirectory + entryName);
                while ((n = zipinputstream.read(buf, 0, 1024)) > -1) fileoutputstream.write(buf, 0, n);
                fileoutputstream.close();
                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();
            }
            zipinputstream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ungzip(String gzipfile, String outputDirectory) {
        try {
            File outputFile = new File(gzipfile.substring(0, gzipfile.indexOf(".gzip")));
            byte[] buf = new byte[1024];
            System.out.println(gzipfile);
            GZIPInputStream gzipinputstream = new GZIPInputStream(new FileInputStream(gzipfile));
            File newFile = new File(outputDirectory + "/" + outputFile.getName());
            FileOutputStream fileoutputstream = new FileOutputStream(newFile);
            int off = 0, len = 0;
            while ((len = gzipinputstream.read(buf, off, buf.length)) != 0) {
                fileoutputstream.write(buf, 0, len);
                len = 0;
            }
            fileoutputstream.close();
            gzipinputstream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void untar(String tarFile, String outputDirectory) throws Exception {
        try {
            new Exec().exec("tar xvf " + tarFile + " -C " + outputDirectory);
        } catch (Exception E) {
            E.printStackTrace();
            return;
        }
    }

    public static void replace(String file, String[] changes) throws Exception {
        File properties = null;
        if (!(properties = new File(file)).exists()) {
            System.out.println("File does not exist : " + file);
            return;
        }
        System.out.println("Copying file : " + properties.getAbsoluteFile() + " : to : " + properties.getAbsoluteFile() + ".bak");
        FileUtils.copyFile(properties, new File(properties.getAbsoluteFile() + ".bak"));
        changeNonProperty(properties, new File(properties.getAbsoluteFile() + ".bak"), changes);
    }

    public static void changeNonProperty(File outputfile, File inputfile, String[] changes) throws Exception {
        BufferedReader inputReader = null;
        FileOutputStream output = null;
        if (!inputfile.exists()) return;
        System.out.println("Changing file : " + outputfile.getAbsolutePath());
        System.out.flush();
        inputReader = new BufferedReader(new FileReader(inputfile));
        output = new FileOutputStream(outputfile);
        for (String t = inputReader.readLine(); t != null; t = inputReader.readLine()) {
            for (String change : changes) {
                String[] match = change.split("::");
                if (match.length > 2 || match.length < 2) {
                    System.out.println("Invalid entry : " + change);
                    continue;
                }
                String tmp = t;
                t = t.replaceAll(match[0], match[1]);
                if (tmp != t) {
                    System.out.println("File Changed");
                    System.out.println("BEFORE : " + tmp);
                    System.out.println("AFTER : " + t);
                    System.out.flush();
                    break;
                }
            }
            output.write((t + "\n").getBytes());
        }
        output.close();
        inputReader.close();
    }

    public static void main(String[] args) throws Exception {
        copyFile("/home/rwall/tmp/t.properties", "/home/rwall/tmp/t.properties.bak");
    }
}
