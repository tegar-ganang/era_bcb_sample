package com.citizenhawk.antmakerunscript.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <u><b><font color="red">FOR INTERNAL USE ONLY.</font></b></u>
 * <p/>
 * This class uses a ProcessBuilder to run the ant command on your local system.  It depends on you having
 * a correctly configured ANT_HOME environment variable.  If you're on windows, it runs "%ANT_HOME%\bin\ant.bat",
 * otherwise, it runs "$ANT_HOME/bin/ant".
 * <p/>
 * Copyright (c)2007, Daniel Kaplan
 *
 * @author Daniel Kaplan
 * @since 7.10.5
 */
public class RunAnt {

    public static void main(String[] args) throws IOException {
        if (args.length == 2) {
            new RunAnt(args[0], args[1]).flushStreams(System.out);
        } else if (args.length == 1) {
            new RunAnt(args[0]).flushStreams(System.out);
        } else {
            System.err.println("Usage: RunAnt [workingdir] antfile");
            System.exit(-1);
        }
    }

    private Process p;

    public RunAnt(String workingDirectory, String antFile) throws IOException {
        deleteRunScripts();
        List command = new ArrayList();
        command.add(configureAntCommand());
        command.add("-f");
        command.add(antFile);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb = configureWorkingDirectory(workingDirectory, pb);
        p = pb.start();
    }

    private String configureAntCommand() {
        String os = System.getProperty("os.name");
        String suffix = "";
        if (os.toLowerCase().contains("windows")) {
            suffix = ".bat";
        }
        return "ant" + suffix;
    }

    private ProcessBuilder configureWorkingDirectory(String workingDirectory, ProcessBuilder pb) {
        if (workingDirectory == null) {
            pb = pb.directory(new File("src/test/com/citizenhawk/antmakerunscript/buildfiles"));
        } else {
            pb = pb.directory(new File(workingDirectory));
        }
        return pb;
    }

    public RunAnt(String antFile) throws IOException {
        this(null, antFile);
    }

    public Process getProcess() {
        validate();
        return p;
    }

    public int flushStreams() {
        return flushStreams(null);
    }

    public int flushStreams(OutputStream destination) {
        validate();
        int reads = flushStream(p.getErrorStream(), destination);
        flushStream(p.getInputStream(), destination);
        return reads;
    }

    private int flushStream(InputStream inputStream, OutputStream destination) {
        int reads = 0;
        try {
            int read;
            while ((read = inputStream.read()) != -1) {
                reads++;
                if (destination != null) destination.write(read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reads;
    }

    private void validate() {
        if (p == null) {
            throw new IllegalStateException("The process is null");
        }
    }

    private static void deleteRunScripts() throws IOException {
        File dir = new File(".");
        File[] files = dir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                if (isInCurrentDir(dir)) {
                    return false;
                }
                return name.endsWith(".bat") || name.endsWith(".sh");
            }

            private boolean isInCurrentDir(File dir) {
                return !dir.getAbsolutePath().equals(new File(".").getAbsolutePath());
            }
        });
        for (int i = 0; i < files.length; ++i) {
            boolean deleted = files[i].delete();
            if (!deleted) {
                throw new IOException("This file could not be deleted: " + files[i].getAbsolutePath());
            }
        }
    }
}
