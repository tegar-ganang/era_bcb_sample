package com.jix.installer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import com.jix.JixException;

public class WindowsInstallerExec {

    private final File tempFile = File.createTempFile("jigen-installer", ".msi");

    private final File errorLogFile = File.createTempFile("jigen-installer", ".log");

    final String productGuid;

    final File javaHome, installDir;

    final boolean startMenuShortcutsDisabled, desktopShortcutsDisabled;

    final Collection notAssociatedExtensions;

    public WindowsInstallerExec(String productGuid, File javaHome, File installDir, boolean startMenuShortcutsDisabled, boolean desktopShortcutsDisabled, Collection notAssociatedExtensions) throws IOException {
        this.tempFile.deleteOnExit();
        this.errorLogFile.deleteOnExit();
        this.productGuid = productGuid;
        this.javaHome = javaHome;
        this.installDir = installDir;
        this.startMenuShortcutsDisabled = startMenuShortcutsDisabled;
        this.desktopShortcutsDisabled = desktopShortcutsDisabled;
        this.notAssociatedExtensions = notAssociatedExtensions;
        FileOutputStream fos = new FileOutputStream(tempFile);
        InputStream fileStream = WindowsInstallerExec.class.getResourceAsStream("/windows-installer.msi");
        byte buffer[] = new byte[200 * 1024];
        int length;
        while ((length = fileStream.read(buffer)) != -1) fos.write(buffer, 0, length);
        fileStream.close();
        fos.close();
    }

    public void install() throws JixException {
        String command = "msiexec.exe ";
        command += " /quiet /qn /passive";
        command += " /i \"" + tempFile.getAbsolutePath() + '"';
        command += " /lwemo \"" + errorLogFile.getAbsolutePath() + '"';
        command += " JAVA_HOME=\"" + javaHome.getAbsolutePath() + '"';
        command += " INSTALLDIR=\"" + installDir.getAbsolutePath() + '"';
        if (startMenuShortcutsDisabled) command += " STARTMENU_SHORTCUTS_DISABLED=true";
        if (desktopShortcutsDisabled) command += " DESKTOP_SHORTCUTS_DISABLED=true";
        Iterator i = notAssociatedExtensions.iterator();
        while (i.hasNext()) {
            String ext = (String) i.next();
            command += " DO_NOT_ASSOCIATE_" + ext.toUpperCase() + "=true";
        }
        int exitValue;
        try {
            Process process = Runtime.getRuntime().exec(command);
            exitValue = process.waitFor();
        } catch (Exception e) {
            try {
                String uninstallCommand = "msiexec.exe /quiet /qn /passive /x {" + productGuid + "}";
                Runtime.getRuntime().exec(uninstallCommand).waitFor();
            } catch (Exception e1) {
            }
            throw new JixException(e);
        }
        if (exitValue != 0) throw new JixException("Error during execution: " + exitValue, errorLogFile, "UTF-16");
    }

    public void dispose() {
        this.tempFile.delete();
        this.errorLogFile.delete();
    }
}
