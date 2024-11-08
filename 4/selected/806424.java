package packjacket.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.logging.Level;
import packjacket.RunnerClass;
import packjacket.gui.create.ProgressFrame;

/**
 * The following code is a conversion of the phyton code for izpack2exe from Izpack utilities to java
 * Wraps a JAR in a LZMA-ed EXE
 * @author Manodasan Wingarajah
 */
public class Izpack2Exe {

    private File jarF, exeF;

    private ProgressFrame fr;

    private boolean crashed, stopped;

    private Process p;

    /**
     * Creates the task
     * @param jarF input jar file
     * @param exeF output exe file
     * @param fr frame showing progress
     */
    public Izpack2Exe(File jarF, File exeF, ProgressFrame fr) {
        this.jarF = jarF;
        this.exeF = exeF;
        this.fr = fr;
    }

    /**
     * Generates the exe
     * @throws IOException When an IOException occurs, it is thrown
     */
    public void generateExe() throws IOException {
        new File(System.getProperty("java.io.tmpdir") + "/config.txt").delete();
        new File(System.getProperty("java.io.tmpdir") + "/installer.7z").delete();
        if (System.getProperty("os.name").startsWith("Windows")) p = Runtime.getRuntime().exec(new String[] { RunnerClass.homedir + "utils/wrappers/izpack2exe/7za.exe", "a", "-t7z", "-mx=9", "-ms=off", System.getProperty("java.io.tmpdir") + "/installer.7z", jarF.getAbsolutePath() }, null, new File(RunnerClass.homedir + "utils/wrappers/izpack2exe")); else p = Runtime.getRuntime().exec(new String[] { "7za", "a", "-t7z", "-mx=9", "-ms=off", System.getProperty("java.io.tmpdir") + "/installer.7z", jarF.getAbsolutePath() }, null, new File(RunnerClass.homedir + "utils/wrappers/izpack2exe"));
        final BufferedReader inps = new BufferedReader(new InputStreamReader(p.getInputStream()));
        final BufferedReader errs = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    fr.taskMsg.setText("Compressing");
                    fr.taskPB.setIndeterminate(true);
                    String c;
                    long l = System.currentTimeMillis();
                    while ((c = inps.readLine()) != null) {
                        RunnerClass.logger.info((System.currentTimeMillis() - l) + " Input: " + c);
                        l = System.currentTimeMillis();
                    }
                    if (crashed) fr.crash();
                    jarF.delete();
                } catch (IOException ex) {
                    RunnerClass.logger.log(Level.SEVERE, null, ex);
                }
            }
        }).start();
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String c;
                    while ((c = errs.readLine()) != null) {
                        crashed = true;
                        RunnerClass.logger.severe("Error: " + c);
                    }
                } catch (IOException ex) {
                    RunnerClass.logger.log(Level.SEVERE, null, ex);
                }
            }
        }).start();
        try {
            p.waitFor();
        } catch (InterruptedException ex) {
        }
        if (stopped) return;
        PrintWriter out = new PrintWriter(new FileWriter(System.getProperty("java.io.tmpdir") + "/config.txt"));
        out.println(";!@Install@!UTF-8!");
        out.println("Title=\"Installer\"");
        out.println("InstallPath=\"%temp%\\\\packjacket-installer\"");
        out.println("ExtractDialogText=\"Extracting installer\"");
        out.println("Progress=\"yes\"");
        out.println("GUIFlags=\"4+32\"");
        out.println("GUIMode=\"1\"");
        out.println("ExecuteFile=\"" + jarF.getName() + "\"");
        out.println(";!@InstallEnd@!");
        out.close();
        exeF.delete();
        FileOutputStream outFile = new FileOutputStream(new File(exeF.getAbsolutePath()), true);
        writeFile(new FileInputStream(new File(RunnerClass.homedir + "utils/wrappers/izpack2exe/7zS.sfx")), outFile);
        writeFile(new FileInputStream(new File(System.getProperty("java.io.tmpdir") + "/config.txt")), outFile);
        writeFile(new FileInputStream(new File(System.getProperty("java.io.tmpdir") + "/installer.7z")), outFile);
        outFile.close();
        fr.setPB(100);
        fr.taskPB.setIndeterminate(false);
        fr.taskMsg.setText("Completed");
        new File(System.getProperty("java.io.tmpdir") + "/config.txt").delete();
        new File(System.getProperty("java.io.tmpdir") + "/installer.7z").delete();
    }

    /**
     * Gets data from input file, and appends it to output
     * @param inFile the input file to read from
     * @param outFile the output file to write to
     * @throws IOException When an IOException occurs, it is thrown
     */
    private void writeFile(FileInputStream inFile, FileOutputStream outFile) throws IOException {
        byte[] buf = new byte[2048];
        int read;
        while ((read = inFile.read(buf)) > 0 && !stopped) outFile.write(buf, 0, read);
        inFile.close();
    }

    /**
     * Stops the process
     */
    public void stop() {
        stopped = true;
        if (p != null) p.destroy();
        new File(System.getProperty("java.io.tmpdir") + "/config.txt").delete();
        new File(System.getProperty("java.io.tmpdir") + "/installer.7z").delete();
    }
}
