package com.magneticreason.fitnium.api;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import com.thoughtworks.selenium.SeleniumException;

public class FitniumFileAPI {

    /****************************************************************************
     * File IO
     ****************************************************************************/
    public static final void createFileNamed(String name) throws SeleniumException {
        try {
            java.io.File file = new File(name);
            file.createNewFile();
        } catch (Exception e) {
            throw new SeleniumException("Failed to create file : " + name, e);
        }
    }

    public static final void createDirectoryNamed(String dir) {
        File file = new File(dir);
        file.mkdirs();
    }

    public static final void deleteFileNamed(String name) {
        java.io.File file = new File(name);
        file.delete();
    }

    public static final void deleteDirectoryNamed(String dir) {
        java.io.File file = new File(dir);
        file.delete();
    }

    public static final void copyFileNamedToFileNamed(String from, String to) throws SeleniumException {
        FitniumFileAPI.copyFile(from, to);
    }

    public static final void moveFileNamedToFileNamed(String from, String to) throws SeleniumException {
        FitniumFileAPI.copyFile(from, to);
        FitniumFileAPI.deleteFileNamed(from);
    }

    public static final boolean fileNamedExists(String name) {
        java.io.File file = new File(name);
        return file.exists();
    }

    public static final boolean directoryNamedExists(String name) {
        java.io.File file = new File(name);
        return file.exists();
    }

    protected static final void copyFile(String from, String to) throws SeleniumException {
        try {
            java.io.File fileFrom = new File(from);
            java.io.File fileTo = new File(to);
            FileReader in = new FileReader(fileFrom);
            FileWriter out = new FileWriter(fileTo);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (Exception e) {
            throw new SeleniumException("Failed to copy new file : " + from + " to : " + to, e);
        }
    }
}
