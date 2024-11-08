package it.ame.permflow.IO.file;

import it.ame.permflow.SBCMain;
import it.ame.permflow.gui.HUD;
import it.ame.permflow.core.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import it.ame.permflow.util.*;

public class USBDumper {

    private final String USB_PEN_DEVICE = "/dev/sda1";

    private final String MOUNT_POINT = "/mnt/usb";

    private final String PERMFLOW_DIRECTORY = "PermFlow_Reports";

    private final String PERMFLOW_DEBUG_DIRECTORY = "/Users/eye/Desktop/Permflow_Reports";

    private File permDir;

    public USBDumper() {
    }

    public void init(String factory, String felt) throws IOException, InterruptedException {
        if (!SBCMain.DEBUG_MODE) {
            File sysDevice = new File("/sys/block/sda");
            try {
                while (!sysDevice.exists()) {
                    System.out.println("Il file " + sysDevice.getCanonicalPath() + " non esiste..per ora...");
                    Thread.sleep(1000);
                }
                Thread.sleep(2000);
                System.out.println("eseguo il mount di " + USB_PEN_DEVICE);
                Process mounter = Runtime.getRuntime().exec("mount " + USB_PEN_DEVICE + " " + MOUNT_POINT);
                mounter.waitFor();
                BufferedReader br = new BufferedReader(new InputStreamReader(mounter.getErrorStream()));
                String line = new String();
                System.out.println("OUTPUT DEL MOUNT");
                while ((line = br.readLine()) != null) System.out.println(line);
            } catch (Exception e) {
                Logger.reportException(e);
                e.printStackTrace();
            }
            System.out.println("Found USB device in " + MOUNT_POINT);
            File dir = new File(MOUNT_POINT);
            createPath(dir, new File(PERMFLOW_DIRECTORY));
            File baseDir = new File(MOUNT_POINT + File.separator + PERMFLOW_DIRECTORY);
            File factDir = new File(baseDir + File.separator + factory);
            createPath(baseDir, factDir);
            File feltDir = new File(factDir + File.separator + felt);
            createPath(factDir, feltDir);
            permDir = feltDir;
        } else {
            Thread.sleep(1000);
            permDir = new File(PERMFLOW_DEBUG_DIRECTORY);
        }
    }

    private void createPath(File basePath, File newDir) throws IOException {
        boolean found = false;
        System.out.println(basePath.getCanonicalPath());
        File[] fileList = basePath.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return true; else return false;
            }
        });
        if (fileList.length != 0) {
            for (File file : fileList) {
                if (file.getName().equals(newDir.getName())) {
                    found = true;
                    System.out.println("Trovata la directory " + newDir.getName() + " nel path " + basePath.getName());
                    return;
                }
            }
        }
        if (!found) {
            System.out.println("Creo la directory " + basePath.getName() + File.separator + newDir.getName());
            newDir = new File(basePath + File.separator + newDir.getName());
            newDir.mkdir();
            return;
        }
    }

    public boolean dumpToUsb(File[] fileArray) throws IOException, InterruptedException {
        for (File file : fileArray) {
            dumpToUsb(file, null);
        }
        return true;
    }

    public void writeHeader(DataOutputStream dos, Report r) throws IOException {
        for (int i = 0; i < SBCMain.profiles.size(); ++i) {
            PaperFactory f = SBCMain.profiles.get(i);
            for (int j = 0; j < f.size(); ++j) {
                Felt fl = f.getFelt(j);
                for (int w = 0; w < fl.getReportCount(); ++w) {
                    if (fl.getReport(w).equals(r)) {
                        dos.writeBytes("cartiera; " + f.name + "\r\n");
                        dos.writeBytes("feltro; " + fl.name + "\r\n");
                        dos.writeBytes("data; " + r.dateString() + "\r\n");
                        dos.writeBytes("flag1; " + ((r.isAC) ? ("AC") : ("BC")) + "\r\n");
                        dos.writeBytes("flag2; " + ((r.isCD) ? ("CD") : ("MD")) + "\r\n");
                    }
                }
            }
        }
    }

    public boolean dumpToUsb(File file, Report report) throws IOException, InterruptedException {
        System.out.println("Sto copiando " + file.getCanonicalPath() + " in " + permDir.getCanonicalPath().replace(" ", "\\ "));
        System.out.println("cp " + file.getCanonicalPath() + " " + permDir.getCanonicalPath().replace(" ", "\\ "));
        if (HUD.settings.getCurrentFiltering() == 1) {
            Process copier = Runtime.getRuntime().exec("cp " + file.getCanonicalPath() + " " + permDir.getCanonicalPath());
            copier.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(copier.getErrorStream()));
            String line = new String();
            System.out.println("OUTPUT COPIA");
            while ((line = br.readLine()) != null) System.out.println(line);
        } else {
            File in = file;
            File out = new File(permDir, in.getName());
            System.out.println("Copia filtrata da '" + in.getCanonicalPath() + "' a '" + out.getCanonicalPath() + "' ogni " + HUD.settings.getCurrentFiltering() + " pacchetti.");
            BufferedReader br = new BufferedReader(new FileReader(in));
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(out));
            if (report != null) writeHeader(dos, report);
            float curMean1 = 0.0f, curMean2 = 0.0f;
            int opticalValue = 0;
            int count = 0;
            String curLine = br.readLine();
            StringTokenizer tokenizer;
            while (curLine != null) {
                tokenizer = new StringTokenizer(curLine, ",;");
                curMean1 += Float.parseFloat(tokenizer.nextToken());
                curMean2 += Float.parseFloat(tokenizer.nextToken());
                opticalValue += Integer.valueOf(tokenizer.nextToken());
                ++count;
                if (count == HUD.settings.getCurrentFiltering()) {
                    curMean1 /= (float) HUD.settings.getCurrentFiltering();
                    curMean2 /= (float) HUD.settings.getCurrentFiltering();
                    if (file.getName().endsWith(".perm")) {
                        dos.writeBytes(Integer.toString((int) curMean1));
                        dos.writeByte(';');
                        dos.writeBytes(Float.toString(curMean2));
                        dos.writeByte(';');
                        if (opticalValue >= 1) {
                            dos.writeByte('1');
                            opticalValue = 0;
                        } else dos.writeByte('0');
                        dos.writeBytes("\r\n");
                    } else {
                        dos.writeBytes(Float.toString(curMean1));
                        dos.writeByte(';');
                        dos.writeBytes(Float.toString(curMean2));
                        dos.writeByte(';');
                        if (opticalValue >= 1) {
                            dos.writeByte('1');
                            opticalValue = 0;
                        } else dos.writeByte('0');
                        dos.writeBytes("\r\n");
                    }
                    curMean1 = 0.0f;
                    curMean2 = 0.0f;
                    count = 0;
                }
                curLine = br.readLine();
            }
            if (count > 0) {
                curMean1 /= (float) count;
                curMean2 /= (float) count;
                if (file.getName().endsWith(".perm")) {
                    dos.writeBytes(Integer.toString((int) curMean1));
                    dos.writeByte(';');
                    dos.writeBytes(Float.toString(curMean2));
                    dos.writeByte(';');
                    if (opticalValue >= 1) {
                        dos.writeByte('1');
                        opticalValue = 0;
                    } else dos.writeByte('0');
                    dos.writeBytes("\r\n");
                } else {
                    dos.writeBytes(Float.toString(curMean1));
                    dos.writeByte(';');
                    dos.writeBytes(Float.toString(curMean2));
                    dos.writeByte(';');
                    if (opticalValue >= 1) {
                        dos.writeByte('1');
                        opticalValue = 0;
                    } else dos.writeByte('0');
                    dos.writeBytes("\r\n");
                }
            }
            br.close();
            dos.close();
        }
        return true;
    }

    private void copy(File fromFile, File toFile) throws IOException {
        String fromFileName = fromFile.getName();
        File tmpFile = new File(fromFileName);
        String toFileName = toFile.getName();
        if (!tmpFile.exists()) throw new IOException("FileCopy: " + "no such source file: " + fromFileName);
        if (!tmpFile.isFile()) throw new IOException("FileCopy: " + "can't copy directory: " + fromFileName);
        if (!tmpFile.canRead()) throw new IOException("FileCopy: " + "source file is unreadable: " + fromFileName);
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(tmpFile);
            File toF = new File(toFile.getCanonicalPath());
            if (!toF.exists()) ;
            toF.createNewFile();
            if (!SBCMain.DEBUG_MODE) to = new FileOutputStream(toFile); else to = new FileOutputStream(toF);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    public void umount() throws Exception {
        if (SBCMain.DEBUG_MODE) {
            Thread.sleep(1000);
        } else {
            System.out.println("Umounting Device...");
            String line;
            Process proc = Runtime.getRuntime().exec("umount " + USB_PEN_DEVICE);
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            proc.waitFor();
            System.out.println("OUTPUT DEL UMOUNT");
            while ((line = br.readLine()) != null) System.out.println(line);
        }
    }
}
