package com.bukkit.epicsaga.EpicManager.auth;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

/**
 * A file based whitelist.
 *
 * The file is cached in memory, and checked for modification date during a call
 *  to isAllowed().
 *
 * @author _sir_maniac
 *
 */
public class FileWhitelist implements PlayerAuthenticator {

    private File file;

    private long fileTime;

    private Set<String> users;

    public FileWhitelist(File file) throws IOException {
        this.file = file;
        fileTime = file.lastModified();
        if (!file.exists() && file.createNewFile()) throw new IOException("Cannot create file: " + file.toString());
        users = new HashSet<String>();
        readFile();
    }

    public void deny(String name, String reason) {
        deny(name);
    }

    public void deny(String name) {
        checkFile();
        name = name.toLowerCase();
        if (!users.contains(name)) return;
        try {
            removeFromFile(name);
        } catch (IOException e) {
            throw new RuntimeException("Error removing from file: " + file.getPath(), e);
        }
        users.remove(name);
    }

    public String getBannedReason(String name) {
        return null;
    }

    public void accept(String name) {
        checkFile();
        name = name.toLowerCase();
        if (users.contains(name)) return;
        try {
            Writer out = new BufferedWriter(new FileWriter(file, true));
            try {
                out.append(name + "\n");
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error adding to file: " + file.getPath(), e);
        }
        users.add(name);
    }

    public boolean isAllowed(String name) {
        checkFile();
        return users.contains(name.toLowerCase());
    }

    private void readFile() {
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            assert false : "File should never be nonexistant here";
        }
        users.clear();
        try {
            while (scanner.hasNextLine()) {
                users.add(scanner.nextLine().toLowerCase());
            }
        } finally {
            scanner.close();
        }
    }

    private void checkFile() {
        long last = file.lastModified();
        if (last > fileTime) readFile();
        fileTime = last;
    }

    private void removeFromFile(String name) throws IOException {
        List<String> names = new Vector<String>();
        Scanner scanner = new Scanner(file);
        String line;
        try {
            while (scanner.hasNextLine()) {
                line = scanner.nextLine().trim().toLowerCase();
                if (!line.isEmpty()) names.add(line);
            }
        } finally {
            scanner.close();
        }
        names.remove(name.toLowerCase());
        FileOutputStream fs = new FileOutputStream(file);
        fs.getChannel().truncate(0);
        fs.close();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        Iterator<String> i = names.iterator();
        try {
            while (i.hasNext()) {
                writer.write(i.next() + "\n");
            }
        } finally {
            writer.close();
        }
    }
}
