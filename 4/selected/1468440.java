package de.jassda.util.installer.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import de.jassda.util.installer.InstallManager;
import de.jassda.util.installer.Preferences;
import de.jassda.util.installer.ui.InstallerUI;

/**
 * The installer copies files from the installer archive to the destination
 * directory. Doing this it updates the user interface and registers 
 * every installed file. <br />
 * Internal directory structures are kept.
 * 
 * @author riejo
 */
public class Installer {

    private int blocksize;

    private InstallManager installManager;

    private InstallerUI ui;

    private File installationDirectory;

    static final String RESOURCE_BASE_PATH = "FILES/";

    public Installer(InstallManager installManager, File installationDirectory) {
        this.installManager = installManager;
        this.installationDirectory = installationDirectory;
        ui = installManager.getUi();
        blocksize = 1024;
    }

    /**
	 * Perform the installation which contains of two step per file.
	 * <br />(1) Copy file to the desired location.
	 * <br />(2) Add file to internal registry so it can be uninstalled.
	 * <br />While performing above steps the user interface is
	 * being updated.
	 * @param files
	 */
    public synchronized void asynchPerform() {
        new Worker().start();
    }

    public void synchPerform() {
        new Worker().run();
    }

    private final class Worker extends Thread {

        public void run() {
            String[] archiveFiles = installManager.getFiles();
            String toolsJar = installManager.getToolsJar();
            int max = archiveFiles.length + (toolsJar != null ? 1 : 0);
            ui.updateProgress(0, max);
            int total = 1;
            for (int i = 0; i < archiveFiles.length; i++) {
                try {
                    installFile(archiveFiles[i], new BufferedInputStream(ClassLoader.getSystemResource(RESOURCE_BASE_PATH + archiveFiles[i]).openStream()));
                    ui.updateProgress(total++, max);
                    ui.appendMessage("Copied from archive: '" + archiveFiles[i] + "'");
                } catch (Exception e) {
                    e.printStackTrace();
                    ui.showError(e, "Could not copy archive file: " + archiveFiles[i]);
                    return;
                }
            }
            if (toolsJar != null) {
                try {
                    installFile("lib/tools.jar", new BufferedInputStream(new FileInputStream(toolsJar)));
                    ui.updateProgress(total++, max);
                    ui.appendMessage("Copied '" + toolsJar + "'");
                } catch (Exception e) {
                    e.printStackTrace();
                    ui.showError(e, "Could not copy " + toolsJar);
                    return;
                }
            }
            Preferences.addInstallation(installManager.getProgramName(), installationDirectory.getAbsolutePath());
            Preferences.setVersion(installManager.getProgramName(), installationDirectory.getAbsolutePath(), installManager.getProgramVersion());
            if (toolsJar != null) {
                Preferences.setToolsJar(installManager.getProgramName(), installationDirectory.getAbsolutePath(), toolsJar);
            }
            ui.updateButtons(InstallerUI.Buttons.FWD);
        }

        private void installFile(String destination, BufferedInputStream reader) throws Exception {
            File tmpDir = new File(installationDirectory + File.separator + extractDirectories(destination));
            tmpDir.mkdirs();
            File tmpFile = new File(installationDirectory + File.separator + destination);
            copy(reader, tmpFile);
            Preferences.addInstalledFile(installManager.getProgramName(), installationDirectory.getAbsolutePath(), destination);
        }

        private void copy(InputStream in, File destination) throws IOException {
            int read;
            byte[] block = new byte[blocksize];
            FileOutputStream writer = new FileOutputStream(destination);
            while ((read = in.read(block)) != -1) {
                writer.write(block, 0, read);
            }
            in.close();
            writer.flush();
            writer.close();
        }

        private String extractDirectories(String file) {
            int index = file.lastIndexOf('/');
            return index != -1 ? file.substring(0, index + 1) : "";
        }
    }
}
