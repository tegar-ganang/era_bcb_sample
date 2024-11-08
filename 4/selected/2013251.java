package at.tuwien.minimee.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import eu.planets_project.pp.plato.util.OS;

public class LinuxCommandMonitor {

    private String tempDir = OS.getTmpPath();

    private ExecutionFootprintList taskPerformance = null;

    private String workingDirectory;

    private String monitorShellScript;

    private boolean topCalledImplicit = false;

    public LinuxCommandMonitor() {
        this.topCalledImplicit = false;
    }

    public LinuxCommandMonitor(boolean topCalledImplicit) {
        this.topCalledImplicit = topCalledImplicit;
    }

    public void prepareWorkingDirectory() throws Exception {
        workingDirectory = tempDir + "/profile_" + System.nanoTime();
        (new File(workingDirectory)).mkdir();
        String monitorCallShellScript = "data/scripts/monitorcall.sh";
        URL monitorCallShellScriptUrl = Thread.currentThread().getContextClassLoader().getResource(monitorCallShellScript);
        File inScriptFile = null;
        try {
            inScriptFile = new File(monitorCallShellScriptUrl.toURI());
        } catch (URISyntaxException e) {
            throw e;
        }
        monitorShellScript = workingDirectory + "/monitorcall.sh";
        File outScriptFile = new File(monitorShellScript);
        FileChannel inChannel = new FileInputStream(inScriptFile).getChannel();
        FileChannel outChannel = new FileOutputStream(outScriptFile).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
        try {
            LinuxCommandExecutor cmdExecutor = new LinuxCommandExecutor();
            cmdExecutor.runCommand("chmod 777 " + monitorShellScript);
        } catch (Exception e) {
            throw e;
        }
    }

    public void monitor(String command) {
        try {
            if (workingDirectory == "") {
                prepareWorkingDirectory();
            }
        } catch (Exception e) {
        }
        LinuxCommandExecutor cmdExecutor = new LinuxCommandExecutor();
        cmdExecutor.setWorkingDirectory(workingDirectory);
        String commandLine = "";
        if (topCalledImplicit) {
            commandLine = command;
        } else {
            commandLine = monitorShellScript + " " + workingDirectory + " 0 " + command;
            System.out.println("to execute: " + commandLine);
        }
        try {
            cmdExecutor.runCommand(commandLine);
            String error = cmdExecutor.getCommandError();
            String out = cmdExecutor.getCommandOutput();
            System.out.println("OUT: " + out);
            System.out.println("ERR: " + error);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            collectPerformanceValues();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @throws FileNotFoundException
     */
    private void collectPerformanceValues() throws FileNotFoundException {
        TopParser topParser = new TopParser(workingDirectory + "/top.log");
        topParser.parse();
        taskPerformance = topParser.getList();
    }

    public ExecutionFootprintList getTaskPerformance() {
        return taskPerformance;
    }

    public void setTaskPerformance(ExecutionFootprintList taskPerformance) {
        this.taskPerformance = taskPerformance;
    }

    public boolean isTopCalledImplicit() {
        return topCalledImplicit;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }
}
