package co.edu.unal.ungrid.admin.controller;

import java.io.File;
import java.io.FileInputStream;
import net.jini.core.lease.Lease;
import co.edu.unal.space.util.SpaceProxy;
import co.edu.unal.ungrid.client.controller.App;
import co.edu.unal.ungrid.client.controller.CommManager;
import co.edu.unal.ungrid.client.controller.GroupManager;
import co.edu.unal.ungrid.client.model.ByteArrayEntry;
import co.edu.unal.ungrid.client.util.FileType;

public class InstallManager {

    protected void removeInstallation() {
        SpaceProxy proxy = AdminApp.getInstance().getProxy();
        try {
            ByteArrayEntry jar = new ByteArrayEntry(FileType.JAR_TYPE, GroupManager.DEFAULT, App.ADMIN_NAME, Integer.valueOf(0), App.WORKER_JAR_FILE, null);
            proxy.takeIfExists(jar, null, Lease.FOREVER);
            ByteArrayEntry pol = new ByteArrayEntry(FileType.POL_TYPE, GroupManager.DEFAULT, App.ADMIN_NAME, Integer.valueOf(0), App.POLICY_FILE, null);
            proxy.takeIfExists(pol, null, Lease.FOREVER);
            ByteArrayEntry win = new ByteArrayEntry(FileType.WIN_TYPE, GroupManager.DEFAULT, App.ADMIN_NAME, Integer.valueOf(0), FileType.WN_SHL_FILE, null);
            proxy.takeIfExists(win, null, Lease.FOREVER);
            ByteArrayEntry unx = new ByteArrayEntry(FileType.UNX_TYPE, GroupManager.DEFAULT, App.ADMIN_NAME, Integer.valueOf(0), FileType.UX_SHL_FILE, null);
            proxy.takeIfExists(unx, null, Lease.FOREVER);
            ByteArrayEntry inw = new ByteArrayEntry(FileType.WIN_TYPE, GroupManager.DEFAULT, App.ADMIN_NAME, Integer.valueOf(0), FileType.WN_SHL_INSTALL, null);
            proxy.takeIfExists(inw, null, Lease.FOREVER);
            ByteArrayEntry inu = new ByteArrayEntry(FileType.UNX_TYPE, GroupManager.DEFAULT, App.ADMIN_NAME, Integer.valueOf(0), FileType.UX_SHL_INSTALL, null);
            proxy.takeIfExists(inu, null, Lease.FOREVER);
        } catch (Exception exc) {
            System.out.println("InstallManager::removeInstallation(): " + exc);
        }
    }

    protected boolean writeInstallFile(final File f) {
        boolean b = false;
        if (f.canRead()) {
            byte[] ba = new byte[(int) f.length()];
            if (ba != null) {
                try {
                    FileInputStream fisJar = new FileInputStream(f);
                    fisJar.read(ba);
                    fisJar.close();
                    ByteArrayEntry bae = new ByteArrayEntry(FileType.JAR_TYPE, GroupManager.DEFAULT, App.ADMIN_NAME, Integer.valueOf(0), App.WORKER_JAR_FILE, ba);
                    CommManager.getInstance().writeEntry(bae, Lease.FOREVER);
                    b = true;
                } catch (Exception exc) {
                    App.getInstance().showMessageDialog("InstallManager::writeInstallation(): f=" + f.getAbsolutePath() + ": write entry failed.");
                }
            }
        } else {
            App.getInstance().showMessageDialog("InstallManager::writeInstallation(): f=" + f.getAbsolutePath() + ": can't read.");
        }
        return b;
    }

    protected boolean writeInstallation() {
        removeInstallation();
        String sUsrDir = System.getProperty("user.home");
        String sFilSep = System.getProperty("file.separator");
        File fJar = new File(sUsrDir + sFilSep + App.INSTALL_DIR + sFilSep + App.WORKER_JAR_FILE);
        File fPol = new File(sUsrDir + sFilSep + App.INSTALL_DIR + sFilSep + App.POLICY_FILE);
        File fWin = new File(sUsrDir + sFilSep + App.INSTALL_DIR + sFilSep + FileType.WN_SHL_FILE);
        File fUnx = new File(sUsrDir + sFilSep + App.INSTALL_DIR + sFilSep + FileType.UX_SHL_FILE);
        File fInw = new File(sUsrDir + sFilSep + App.INSTALL_DIR + sFilSep + FileType.WN_SHL_INSTALL);
        File fInu = new File(sUsrDir + sFilSep + App.INSTALL_DIR + sFilSep + FileType.UX_SHL_INSTALL);
        boolean b = writeInstallFile(fJar);
        if (b) {
            b = writeInstallFile(fPol);
            if (b) {
                b = writeInstallFile(fWin);
                if (b) {
                    b = writeInstallFile(fUnx);
                    if (b) {
                        b = writeInstallFile(fInw);
                        if (b) {
                            b = writeInstallFile(fInu);
                        }
                    }
                }
            }
        }
        return b;
    }
}
