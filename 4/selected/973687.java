package J7Z;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Files {

    public static void showAbout() {
        showMessage(Settings.Messages.iInformation, Settings.Paths.Sys.sLineSep + "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -" + Settings.Paths.Sys.sLineSep);
        showMessage(Settings.Messages.iInformation, Settings.Application.sAbout1);
    }

    public static void showVariables() {
        showMessage(Settings.Messages.iInformation, "OS = " + Settings.Env.sOS);
        if (Settings.Env.sOS.contains("Linux")) {
            showMessage(Settings.Messages.iInformation, "KDE Version = " + Settings.Env.sKDESV);
            showMessage(Settings.Messages.iInformation, "KDE Config = " + Settings.Paths.KDE.sConfig);
            showMessage(Settings.Messages.iInformation, "KDE Prefix = " + Settings.Paths.KDE.sSPrefix);
            showMessage(Settings.Messages.iInformation, "KDE Local Prefix = " + Settings.Paths.KDE.sLPrefix);
        }
        showMessage(Settings.Messages.iInformation, Settings.Application.sName + " App = " + Settings.Paths.App.sJ7Z);
        showMessage(Settings.Messages.iInformation, Settings.Application.sName + " Config = " + Settings.Paths.App.sConfig);
        showMessage(Settings.Messages.iInformation, "Proxy = " + Settings.Env.sProxy + Settings.Paths.Sys.sLineSep);
    }

    public static boolean testDirectory(String sDir) {
        boolean bRet = true;
        File fDir = new File(sDir);
        if (!fDir.exists()) if (!fDir.mkdirs()) bRet = false;
        return bRet;
    }

    public static void testAllDirectories() {
        boolean bRet = true;
        String sDir = Settings.Paths.App.sConfig + Settings.Paths.Sys.sPathSep;
        bRet &= testDirectory(sDir + "Lists");
        bRet &= testDirectory(sDir + "Profiles");
        bRet &= testDirectory(sDir + "Externals");
        bRet &= testDirectory(sDir + "Output");
        if (!bRet) showMessage(Settings.Messages.iError, "The required directories can't be created.");
    }

    public static void testAllExternals() {
        boolean bRet = true;
        bRet &= testExternal("Lib-Dirs");
        if (Settings.Env.sOS.contains("Linux")) {
            bRet &= testExternal("P7Zip-CLI");
            bRet &= testExternal("P7Zip-GUI");
            bRet &= testExternal("Tar");
        }
        if (!bRet) showMessage(Settings.Messages.iError, "The required externals can't be created.");
    }

    private static boolean testExternal(String sName) {
        boolean bRet = true;
        String sMidPath = Settings.Paths.Sys.sPathSep + "Externals" + Settings.Paths.Sys.sPathSep;
        Externals.Info oEI = new Externals.Info();
        oEI.sName = sName;
        oEI.sPath = Settings.Paths.App.sJ7Z + sMidPath + Settings.Env.sOS.split(" ")[0] + Settings.Paths.Sys.sPathSep + oEI.sName + ".txt";
        if (!new File(Settings.Paths.App.sConfig + sMidPath + oEI.sName + ".txt").exists()) bRet = Externals.resetExternal(oEI);
        return bRet;
    }

    public static void testAllProfiles() {
        boolean bRet = true;
        bRet &= testProfile("Default");
        bRet &= testProfile("Flash");
        bRet &= testProfile("Local");
        bRet &= testProfile("Remote");
        bRet &= testProfile("Secure");
        bRet &= testProfile("Storage");
        if (!bRet) showMessage(Settings.Messages.iError, "The required profiles can't be created.");
    }

    private static boolean testProfile(String sName) {
        boolean bRet = true;
        String sMidPath = Settings.Paths.Sys.sPathSep + "Profiles" + Settings.Paths.Sys.sPathSep;
        Profiles.Info.Create oPI = new Profiles.Info.Create();
        oPI.sName = sName;
        oPI.sPath = Settings.Paths.App.sJ7Z + sMidPath + Settings.Env.sOS.split(" ")[0] + Settings.Paths.Sys.sPathSep + oPI.sName + ".txt";
        if (!new File(Settings.Paths.App.sConfig + sMidPath + oPI.sName + ".txt").exists()) bRet = Profiles.resetProfile(oPI);
        return bRet;
    }

    public static List findFiles(String sDir, final String sExt, boolean bRetFullPath) {
        File fDir = new File(sDir);
        List lPaths = new ArrayList<String>();
        FilenameFilter ffJAR = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return (name.toLowerCase().endsWith(sExt));
            }
        };
        for (int i = 0; i < fDir.list(ffJAR).length; i++) {
            if (bRetFullPath) lPaths.add(sDir + Settings.Paths.Sys.sPathSep + fDir.list(ffJAR)[i]); else lPaths.add(fDir.list(ffJAR)[i]);
        }
        return lPaths;
    }

    public static void deleteFiles(String sDir, final String sExt) {
        List lFiles = new ArrayList<String>();
        lFiles.addAll(Files.findFiles(sDir, sExt, true));
        for (int i = 0; i < lFiles.size(); i++) new File(lFiles.get(i).toString()).delete();
    }

    public static List getValidPaths(String sExternal) {
        String sLine;
        List lEntries = new ArrayList<String>();
        try {
            BufferedReader brTask = new BufferedReader(new FileReader(Settings.Paths.App.sConfig + Settings.Paths.Sys.sPathSep + "Externals" + Settings.Paths.Sys.sPathSep + sExternal + ".txt"));
            try {
                while ((sLine = brTask.readLine()) != null) if (!sLine.isEmpty() && !sLine.startsWith("#")) if (new File(sLine).exists()) {
                    lEntries.add(sLine);
                    showMessage(Settings.Messages.iInformation, "From the " + sExternal + " list, a valid path is: " + sLine);
                } else showMessage(Settings.Messages.iWarning, "From the " + sExternal + " list, an absent path is: " + sLine);
                brTask.close();
            } catch (IOException ex) {
                Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (lEntries);
    }

    public static class Exec {

        public String sCommand = "";

        public String sArgs = "";
    }

    public static boolean chooseBinary(Exec oExec, String sExternal, String sFallback) {
        boolean bFound = false;
        String sLine;
        try {
            BufferedReader brTask = new BufferedReader(new FileReader(Settings.Paths.App.sConfig + Settings.Paths.Sys.sPathSep + "Externals" + Settings.Paths.Sys.sPathSep + sExternal + ".txt"));
            try {
                while ((sLine = brTask.readLine()) != null) if (!sLine.isEmpty() && !sLine.startsWith("#")) {
                    String[] saLine = sLine.split(" ", 1);
                    Process pShell = Runtime.getRuntime().exec("which " + saLine[0]);
                    if (new BufferedReader(new InputStreamReader(pShell.getInputStream())).readLine() != null) {
                        bFound = true;
                        oExec.sCommand = saLine[0];
                        if (saLine.length > 1) oExec.sArgs = saLine[1];
                        showMessage(Settings.Messages.iInformation, "For the " + sExternal + " task, the chosen command is: " + sLine);
                        break;
                    } else showMessage(Settings.Messages.iWarning, "For the " + sExternal + " task, an absent command is: " + sLine);
                }
                brTask.close();
            } catch (IOException ex) {
                Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (bFound);
    }

    public static void copyFile(String sIn, String sOut) throws IOException {
        File fIn = new File(sIn);
        File fOut = new File(sOut);
        FileChannel fcIn = new FileInputStream(fIn).getChannel();
        FileChannel fcOut = new FileOutputStream(fOut).getChannel();
        try {
            fcIn.transferTo(0, fcIn.size(), fcOut);
        } catch (IOException e) {
            throw e;
        } finally {
            if (fcIn != null) fcIn.close();
            if (fcOut != null) fcOut.close();
        }
        fOut.setReadable(fIn.canRead());
        fOut.setWritable(fIn.canWrite());
        fOut.setExecutable(fIn.canExecute());
    }

    public static String stripHTML(String sMessage) {
        if (sMessage != null) {
            sMessage = sMessage.replace("<html>", "").replace("</html>", "");
            sMessage = sMessage.replace("<b>", "").replace("</b>", "");
        }
        return sMessage;
    }

    public static void showMessage(int iType, String sMessage) {
        sMessage = stripHTML(sMessage);
        if (Settings.Inits.bVerbose) try {
            FileWriter fwOutput = new FileWriter(Settings.Paths.App.sConfig + "/Output/" + Settings.Env.sDate + ".txt", true);
            switch(iType) {
                case Settings.Messages.iClear:
                    fwOutput.write(Settings.Paths.Sys.sLineSep);
                    break;
                case Settings.Messages.iStatus:
                    fwOutput.write("[S]: " + sMessage + Settings.Paths.Sys.sLineSep);
                    break;
                case Settings.Messages.iInformation:
                    fwOutput.write("[I]: " + sMessage + Settings.Paths.Sys.sLineSep);
                    break;
                case Settings.Messages.iWarning:
                    fwOutput.write("[W]: " + sMessage + Settings.Paths.Sys.sLineSep);
                    break;
                case Settings.Messages.iError:
                    fwOutput.write("[E]: " + sMessage + Settings.Paths.Sys.sLineSep);
                    break;
                case Settings.Messages.iQuestion:
                    fwOutput.write("[Q]: " + sMessage + Settings.Paths.Sys.sLineSep);
                    break;
            }
            fwOutput.close();
        } catch (IOException ex) {
            Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
