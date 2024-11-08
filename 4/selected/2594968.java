package net.sf.cclearly.prefs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.TreeMap;

public class PreferenceMap {

    TreeMap<String, String> prefs = new TreeMap<String, String>();

    private final File dataFile;

    public PreferenceMap(File file) {
        this.dataFile = file;
        if (dataFile.exists()) {
            readConfig();
        }
    }

    protected synchronized void writeConfig() {
        Thread writer = new Thread("Config_Writer") {

            public void run() {
                synchronized (PreferenceMap.this) {
                    try {
                        FileWriter out = new FileWriter(dataFile);
                        for (String key : prefs.keySet()) {
                            String value = prefs.get(key);
                            out.write(key + "=" + value + "\r\n");
                        }
                        out.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        writer.setDaemon(false);
        writer.start();
    }

    private synchronized void readConfig() {
        try {
            Scanner configScanner = new Scanner(dataFile);
            while (configScanner.hasNextLine()) {
                String line = configScanner.nextLine();
                String[] pair = line.split("=", 2);
                if (pair.length != 2) {
                    continue;
                }
                prefs.put(pair[0].trim().toLowerCase(), pair[1]);
            }
            configScanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public synchronized void put(String key, String value) {
        key = key.toLowerCase();
        prefs.put(key, value);
        writeConfig();
    }

    public synchronized String get(String key, String def) {
        key = key.toLowerCase();
        return prefs.containsKey(key) ? prefs.get(key) : def;
    }

    public synchronized long getLong(String key, int def) {
        key = key.toLowerCase();
        return prefs.containsKey(key) ? Long.parseLong(prefs.get(key)) : def;
    }

    public synchronized void putLong(String key, long value) {
        key = key.toLowerCase();
        prefs.put(key, Long.toString(value));
        writeConfig();
    }

    public synchronized boolean getBoolean(String key, boolean def) {
        key = key.toLowerCase();
        return prefs.containsKey(key) ? Boolean.parseBoolean(prefs.get(key)) : def;
    }

    public synchronized void putBoolean(String key, boolean value) {
        key = key.toLowerCase();
        prefs.put(key, Boolean.toString(value));
        writeConfig();
    }

    public synchronized void putInt(String key, int value) {
        key = key.toLowerCase();
        prefs.put(key, Integer.toString(value));
        writeConfig();
    }

    public synchronized int getInt(String key, int def) {
        key = key.toLowerCase();
        return prefs.containsKey(key) ? Integer.parseInt(prefs.get(key)) : def;
    }
}
