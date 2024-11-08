package de.perschon.utils.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class RecursiveCopy extends Task {

    private String srcdir;

    private String destdir;

    private String filename;

    private String regex;

    private HashMap<String, Object> copiedFiles = new HashMap<String, Object>();

    @Override
    public void execute() throws BuildException {
        if (srcdir == null || filename == null) throw new BuildException("dir and filename must be given!");
        File root = new File(srcdir);
        if (!root.exists() || !root.isDirectory()) throw new BuildException(srcdir + " does not exist or is not a directory!");
        try {
            doDir(root);
            copiedFiles.clear();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private void doDir(File dir) throws IOException, BuildException {
        for (File f : dir.listFiles()) {
            if (f.getName().equals(".") || f.getName().equals("..")) continue;
            if (f.isDirectory()) {
                doDir(f);
            } else {
                if (copiedFiles.containsKey(f.getName())) throw new BuildException("Found duplicate filename: " + f.getName());
                if (f.getName().matches(regex)) {
                    copyFile(f);
                    copiedFiles.put(f.getName(), null);
                }
            }
        }
    }

    private void copyFile(File f) throws IOException {
        File newFile = new File(destdir + "/" + f.getName());
        newFile.createNewFile();
        FileInputStream fin = new FileInputStream(f);
        FileOutputStream fout = new FileOutputStream(newFile);
        int c;
        while ((c = fin.read()) != -1) fout.write(c);
        fin.close();
        fout.close();
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
        regex = filename.replaceAll("[*]", ".*");
    }

    public String getSrcdir() {
        return srcdir;
    }

    public void setSrcdir(String srcdir) {
        this.srcdir = srcdir;
    }

    public String getDestdir() {
        return destdir;
    }

    public void setDestdir(String destdir) {
        this.destdir = destdir;
    }
}
