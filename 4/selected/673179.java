package amaras.runtime;

import java.io.*;
import java.util.Vector;
import javax.swing.*;

public class PasteWorker extends Thread {

    private static int debugOn = 1;

    PasteQue pasteque = new PasteQue();

    public PasteWorker(PasteQue pasteque) {
        this.pasteque = pasteque;
    }

    public void run() {
        System.out.println("Starting Running Paste Thread");
        while (true) {
            System.out.println("Paste Thread Looping : checking wait() notify()");
            CopyCut currentJob = pasteque.getJob();
            int mode = currentJob.getMode();
            String source = currentJob.getSource();
            String dest = currentJob.getDestination();
            currentJob.setActive();
            System.out.println("" + mode + " : " + source + " : " + dest);
            int exitCode = pasteHardWork(mode, source, dest);
            System.out.println("PasteWorker.java -pasteHardWork- EXIT Code " + exitCode);
            if (exitCode == 0) {
                currentJob.setComplete();
            } else if (exitCode == -1) {
                currentJob.setCanceled();
            } else {
                currentJob.setError();
            }
            pasteque.completedJob(currentJob);
        }
    }

    public void pasteHardWorkDebug(String msg) {
        if (false) {
            System.out.println("PasteWorker.java:pasteHardWork " + msg);
        }
    }

    public int pasteHardWork(int mode, String file, String dest) {
        pasteHardWorkDebug("Enter pasteHardWork");
        int exitCode = -1;
        int lastFileSep = file.lastIndexOf(File.separator) + 1;
        int lastDot = file.lastIndexOf('.');
        if (lastDot == -1) {
            lastDot = file.length();
        }
        String pathfilename = file.substring(0, lastDot);
        String filename = file.substring(lastFileSep, lastDot);
        String fileExt = file.substring(lastDot, file.length());
        pasteHardWorkDebug("MODE is " + mode);
        if ((mode == CopyCut.COPY) || (mode == CopyCut.CUT)) {
            pasteHardWorkDebug("Entering COPY or CUT Mode");
            if ((false == true) && ((new File(file)).isDirectory() == false) && ((new File(dest)).isDirectory() == true)) {
                String destNew = new String(dest + filename + fileExt);
                for (int i = 1; ((new File(destNew)).exists() == true); i++) {
                    destNew = dest + filename + "_" + i + fileExt;
                }
                dest = destNew;
            }
            System.out.println("" + mode + " : " + file + " : " + dest);
            System.out.println("Testing Testing Testing");
            String[] command;
            try {
                if ((System.getProperty("os.name").contains("Mac")) | (System.getProperty("os.name").contains("nix"))) {
                    command = new String[4];
                    command[0] = "cp";
                    command[1] = "-R";
                    command[2] = file;
                    command[3] = dest;
                } else if (System.getProperty("os.name").contains("Win")) {
                    if ((new File(file)).isDirectory() == true) {
                        command = new String[14];
                        command[0] = "cmd.exe";
                        command[1] = "/c";
                        command[2] = "XCOPY";
                        command[5] = "/S";
                        command[6] = "/E";
                        command[7] = "/H";
                        command[8] = "/C";
                        command[9] = "/I";
                        command[10] = "/K";
                        command[11] = "/X";
                        command[12] = "/F";
                        command[13] = "/Y";
                        if ((new File(dest)).isDirectory() == true) {
                            if (dest.endsWith(File.separator) == true) {
                                dest = dest + filename;
                            } else {
                                dest = dest + File.separator + filename;
                            }
                        }
                        command[3] = file;
                        command[4] = dest;
                    } else {
                        command = new String[6];
                        command[0] = "cmd.exe";
                        command[1] = "/c";
                        command[2] = "COPY";
                        command[3] = "/Y";
                        command[4] = file;
                        command[5] = dest;
                    }
                } else {
                    command = new String[1];
                    command[0] = " ";
                }
                exitCode = runcommand(command);
                if (exitCode != 0) {
                    System.out.println("Copy Section of CUT exited uncleanly not safe to remove Origianl files");
                    JOptionPane.showMessageDialog(null, "COPY original File Failed" + file, "alert", JOptionPane.ERROR_MESSAGE);
                    return -2;
                }
                System.out.println(" Entering CUT Sequence");
                if (((System.getProperty("os.name").contains("Mac")) | (System.getProperty("os.name").contains("nix"))) & (mode == CopyCut.CUT)) {
                    command = new String[3];
                    command[0] = "rm";
                    command[1] = "-rf";
                    command[2] = file;
                    exitCode = runcommand(command);
                }
                if (System.getProperty("os.name").contains("Win") & (mode == CopyCut.CUT)) {
                    if ((new File(file)).isDirectory() == true) {
                        command = new String[6];
                        command[0] = "cmd.exe";
                        command[1] = "/c";
                        command[2] = "RD";
                        command[3] = "/S";
                        command[4] = "/Q";
                        command[5] = file;
                    } else {
                        command = new String[6];
                        command[0] = "cmd.exe";
                        command[1] = "/c";
                        command[2] = "DEL";
                        command[3] = "/F";
                        command[4] = "/Q";
                        command[5] = file;
                    }
                    exitCode = runcommand(command);
                }
                return exitCode;
            } catch (IOException e) {
                System.out.println("PasteWorker.java:pasteHardWork - IOException");
                System.err.println(e.getMessage());
                return -2;
            } catch (Exception e) {
                System.out.println("PasteWorker.java:pasteHardWork - Exception");
                e.printStackTrace();
                return -2;
            }
        }
        if (mode == CopyCut.DMG) {
            String command[] = new String[6];
            command[0] = "hdiutil";
            command[1] = "create";
            command[2] = pathfilename + ".dmg";
            command[3] = "-srcfolder";
            command[4] = file;
            command[5] = "-ov";
            try {
                exitCode = runcommand(command);
            } catch (IOException e) {
                System.out.println("PasteWorker.java:pasteHardWork - IOException - MODE DMG");
                System.err.println(e.getMessage());
                return -2;
            } catch (Exception e) {
                System.out.println("PasteWorker.java:pasteHardWork - Exception - MODE DMG");
                e.printStackTrace();
                return -2;
            }
            return exitCode;
        }
        pasteHardWorkDebug(" Defult return -2");
        return -2;
    }

    private int runcommand(String[] command) throws IOException, Exception {
        System.out.println();
        System.out.println();
        for (int i = 0; i < command.length - 1; i++) {
            System.out.print(command[i] + " ");
        }
        System.out.println(command[command.length - 1]);
        System.out.println();
        Process p = Runtime.getRuntime().exec(command);
        InputStream is = p.getInputStream();
        try {
            int b;
            while ((b = is.read()) != -1) System.out.write(b);
        } finally {
            is.close();
        }
        int exitCode = p.waitFor();
        debug("" + exitCode + ": PasteWorker exit code");
        return exitCode;
    }

    private void debug(String text) {
        if (debugOn == 1) {
            System.out.println(text);
        }
    }
}
