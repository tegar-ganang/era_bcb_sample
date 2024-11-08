package com.memoire.vainstall;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @version $Id: VALinkWindows.java,v 1.9 2005/10/11 09:51:55 deniger Exp $
 * @author Axel von Arnim
 */
public class VALinkWindows {

    public static boolean move(File _from, File _to) throws IOException {
        if (_from == null || !_from.exists()) return false;
        boolean b = _from.renameTo(_to);
        if (b) return true;
        copy(_from, _to);
        return _from.delete();
    }

    public static void copy(File _from, File _to) throws IOException {
        if (_from == null || !_from.exists()) return;
        FileOutputStream out = null;
        FileInputStream in = null;
        try {
            out = new FileOutputStream(_to);
            in = new FileInputStream(_from);
            byte[] buf = new byte[2048];
            int read = in.read(buf);
            while (read > 0) {
                out.write(buf, 0, read);
                read = in.read(buf);
            }
        } catch (IOException _e) {
            throw _e;
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    /**
	 * Create one Windows shortcut for each javalauncher script that was
	 * created.
	 * <p>
	 * Links will be named after the corresponding launch scripts.
	 * <p>
	 * Icons can be used. If in the install dir an icon file (suffix .ico)
	 * exists that matches the name of the launcher, it will be used as the icon
	 * for that launcher. Alternatively, if an icon file exists in the install
	 * dir that matches LINK_ENTRY_ICON, that icon file will be used. Otherwise,
	 * no icon is used.
	 *
	 * @see com.memoire.vainstall.Setup
	 */
    public static boolean create(VAShortcutEntry[] _launchparms, File sharedDir, String _installClassName, Set shortcuts) throws IOException {
        try {
            String iconfile;
            if (_launchparms == null) return false;
            String section = null;
            if ((!"applications".equals(VAGlobals.LINK_SECTION_NAME.toLowerCase())) && (!"utilities".equals(VAGlobals.LINK_SECTION_NAME.toLowerCase())) && (!"programs".equals(VAGlobals.LINK_SECTION_NAME.toLowerCase()))) section = VAGlobals.LINK_SECTION_NAME;
            if (section == null && (_launchparms.length > 1 || _launchparms.length == 1 && VAGlobals.CREATE_UNINSTALL_SHORTCUT)) {
                section = VAGlobals.APP_NAME;
            }
            String menulinkdir = null;
            Set shortcutToCreate = new HashSet();
            if (section != null) {
                menulinkdir = JNIWindowsShortcut.getShortcutDir(JNIWindowsShortcut.ON_START_MENU, JNIWindowsShortcut.EVERYBODY) + "\\" + section;
                File mld = new File(menulinkdir);
                if (!mld.exists()) {
                    if (!mld.mkdirs()) {
                        throw new IOException("unable to create " + menulinkdir);
                    }
                } else {
                    shortcuts.add(mld.getAbsolutePath());
                }
                if (VAGlobals.DEBUG) VAGlobals.printDebug("menu link dir=" + menulinkdir);
            }
            for (int i = 0; i < _launchparms.length; i++) {
                VAShortcutEntry parmset = _launchparms[i];
                if (parmset.getIconPath() == null) {
                    if (parmset.isUninstall()) {
                        iconfile = getWindowsIconFile("uninstall", false);
                    } else iconfile = getWindowsIconFile(parmset.getName(), true);
                    parmset.setIconPath(iconfile);
                }
                parmset.setWorkingDirectory(VAGlobals.DEST_PATH);
                if (parmset.isCreateOnDesktop()) {
                    String shortcut = create(parmset, JNIWindowsShortcut.ON_DESKTOP);
                    if (new File(shortcut).exists()) shortcuts.add(shortcut);
                }
                String shortcut = null;
                if (menulinkdir != null) {
                    if (VAGlobals.DEBUG) VAGlobals.printDebug("menu to write=" + parmset.getName());
                    try {
                        shortcut = createCustom(parmset, menulinkdir + "\\" + parmset.getName());
                    } catch (IOException _e1) {
                        shortcut = create(parmset, JNIWindowsShortcut.ON_START_MENU);
                        File shortcutFile = new File(shortcut);
                        if (shortcutFile.exists()) {
                            File destFile = new File(menulinkdir, shortcutFile.getName());
                            if (VAGlobals.DEBUG && shortcutFile.exists()) VAGlobals.printDebug("rename shortcut from=\n " + shortcutFile.getAbsolutePath() + "\nto\n" + destFile);
                            move(shortcutFile, destFile);
                            if (shortcutFile.exists()) shortcutFile.delete();
                            shortcut = destFile.getAbsolutePath();
                        }
                    }
                    if (VAGlobals.DEBUG) VAGlobals.printDebug("menu written=" + parmset.getName());
                } else {
                    shortcut = create(parmset, JNIWindowsShortcut.ON_START_MENU);
                }
                if (shortcut != null && new File(shortcut).exists()) {
                    shortcuts.add(shortcut);
                    shortcutToCreate.add(new File(shortcut));
                }
            }
            if (VAGlobals.SHORTCUTS_IN_INSTALLDIR) {
                File dest = new File(VAGlobals.DEST_PATH);
                for (Iterator it = shortcutToCreate.iterator(); it.hasNext(); ) {
                    File f = (File) it.next();
                    File shortcutInInstall = new File(dest, f.getName());
                    copy(f, shortcutInInstall);
                    shortcuts.add(shortcutInInstall.getAbsolutePath());
                }
            }
            return true;
        } catch (IOException _e) {
            _e.printStackTrace();
            throw _e;
        }
    }

    /**
	 * Create a short cut from the entry.
	 *
	 * @parm dest should be one of JNIWindowsShortcut.ON_DESKTOP or
	 *       JNIWindowsShortcut.ON_START_MENU.
	 *
	 */
    private static final String create(VAShortcutEntry parmset, int dest) throws IOException {
        return JNIWindowsShortcut.createShortcut(parmset.getExePath(), parmset.getWorkingDirectory(), "", JNIWindowsShortcut.SHOW_NORMAL, parmset.getName(), dest, JNIWindowsShortcut.EVERYBODY, parmset.getIconPath(), 0, parmset.getComment());
    }

    /**
	 * @parm dest should be one of JNIWindowsShortcut.ON_DESKTOP or
	 *       JNIWindowsShortcut.ON_START_MENU.
	 *
	 */
    private static final String createCustom(VAShortcutEntry parmset, String dest) throws IOException {
        return JNIWindowsShortcut.createShortcut(parmset.getExePath(), parmset.getWorkingDirectory(), "", JNIWindowsShortcut.SHOW_NORMAL, dest, JNIWindowsShortcut.CUSTOM_LOCATION, JNIWindowsShortcut.EVERYBODY, parmset.getIconPath(), 0, parmset.getComment());
    }

    /**
	 * Return the absolute path of the icon for the <code>name</code>
	 * executable. If this icon can't be found, the default icon
	 * <code>LINK_ENTRY_ICON</code> is tried.
	 */
    private static final String getWindowsIconFile(String name, boolean _useDefault) {
        File defaultIcon = null;
        if ((VAGlobals.LINK_ENTRY_ICON != null) && (VAGlobals.LINK_ENTRY_ICON.length() > 0)) {
            defaultIcon = new File(VAGlobals.DEST_PATH, VAGlobals.LINK_ENTRY_ICON.replace('/', File.separatorChar) + ".ico");
            VAGlobals.printDebug("default icon= " + defaultIcon);
        }
        File tryIcon;
        if (defaultIcon != null) tryIcon = new File(defaultIcon.getParentFile(), name + ".ico"); else tryIcon = new File(VAGlobals.DEST_PATH, name + ".ico");
        VAGlobals.printDebug("try icon= " + tryIcon);
        if (tryIcon.exists()) {
            return tryIcon.getAbsolutePath();
        }
        if (_useDefault && defaultIcon.exists()) return defaultIcon.getAbsolutePath();
        return null;
    }
}
