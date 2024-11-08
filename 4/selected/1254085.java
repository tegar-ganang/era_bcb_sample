package net.sourceforge.processdash.tool.quicklauncher;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import net.sourceforge.processdash.FileBackupManager;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.util.FileUtils;

public class InstanceLauncherFactory {

    private static final Resources resources = QuickLauncher.resources;

    private boolean showMessageForUnrecognizedFile = false;

    public void setShowMessageForUnrecognizedFile(boolean showMessageForUnrecognizedFile) {
        this.showMessageForUnrecognizedFile = showMessageForUnrecognizedFile;
    }

    public DashboardInstance getLauncher(Component comp, File f) {
        String basename = f.getName().toLowerCase();
        if (f.isDirectory()) {
            if ("backup".equals(basename) || "cms".equals(basename)) f = f.getParentFile();
            return getDirLauncher(comp, f);
        }
        if (!f.isFile()) {
            if (showMessageForUnrecognizedFile) {
                JOptionPane.showMessageDialog(comp, resources.formatStrings("Errors.File_Not_Found_FMT", f.getPath()), resources.getString("Errors.No_Data_Found"), JOptionPane.ERROR_MESSAGE);
            }
            return null;
        }
        if (FileBackupManager.inBackupSet(f.getParentFile(), basename)) return getDirLauncher(comp, f.getParentFile());
        if (CompressedInstanceLauncher.isCompressedInstanceFilename(basename)) return getZipLauncher(comp, f);
        if (showMessageForUnrecognizedFile) {
            JOptionPane.showMessageDialog(comp, resources.formatStrings("Errors.Unrecognized_File_FMT", f.getPath()), resources.getString("Errors.No_Data_Found"), JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private DashboardInstance getDirLauncher(Component comp, File dir) {
        File testFile = new File(dir, DashboardInstance.DATA_DIR_FILE_ITEM);
        if (testFile.isFile()) return new DirectoryInstanceLauncher(dir);
        List dirs;
        try {
            dirs = DirectoryInstanceLauncher.getDataDirectoriesWithinDir(dir);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(comp, resources.formatStrings("Errors.Dir.Read_Error_FMT", dir.getAbsolutePath(), e.getLocalizedMessage()), resources.getString("Errors.Dialog_Title"), JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (dirs == null || dirs.isEmpty()) {
            String[] files = dir.list();
            if (files != null && files.length == 0) {
                if (offerToCreateNewDataset(comp, dir)) return new DirectoryInstanceLauncher(dir); else return null;
            }
            JOptionPane.showMessageDialog(comp, resources.formatStrings("Errors.Dir.No_Data_Found_FMT", dir.getAbsolutePath()), resources.getString("Errors.No_Data_Found"), JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (dirs.size() == 1) return new DirectoryInstanceLauncher((File) dirs.get(0));
        JList list = new JList(dirs.toArray());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        new JOptionPaneClickHandler().install(list);
        Object[] message = new Object[] { resources.formatStrings("Errors.Dir.Multiple_Data_Found_FMT", dir.getAbsolutePath()), new JScrollPane(list) };
        if (JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(comp, message, resources.getString("Multiple_Data_Found_Title"), JOptionPane.OK_CANCEL_OPTION)) return null;
        dir = (File) list.getSelectedValue();
        if (dir == null) return null; else return new DirectoryInstanceLauncher(dir);
    }

    private boolean offerToCreateNewDataset(Component comp, File dir) {
        String title = resources.getString("Create.Dialog_Title");
        String[] message = resources.formatStrings("Create.Message_FMT", dir.getPath());
        String team = resources.getString("Create.Team_Option");
        String personal = resources.getString("Create.Personal_Option");
        String cancel = resources.getString("Cancel");
        int userChoice = JOptionPane.showOptionDialog(comp, message, title, JOptionPane.OK_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] { team, personal, cancel }, cancel);
        if (userChoice == 0) {
            copyFile("teamdash.ini", dir, "pspdash.ini");
            copyFile("teamicon.ico", dir, "icon.ico");
            return true;
        } else if (userChoice == 1) {
            return true;
        } else {
            return false;
        }
    }

    private void copyFile(String resourceName, File dir, String filename) {
        try {
            File dest = new File(dir, filename);
            FileUtils.copyFile(InstanceLauncherFactory.class.getResourceAsStream(resourceName), dest);
        } catch (IOException ioe) {
        }
    }

    private DashboardInstance getZipLauncher(Component comp, File f) {
        List prefixes;
        try {
            prefixes = CompressedInstanceLauncher.getDataDirectoriesWithinZip(f);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(comp, resources.formatStrings("Errors.Zip.Read_Error_FMT", f.getAbsolutePath(), e.getLocalizedMessage()), resources.getString("Errors.Dialog_Title"), JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (prefixes == null || prefixes.isEmpty()) {
            JOptionPane.showMessageDialog(comp, resources.formatStrings("Errors.Zip.No_Data_Found_FMT", f.getAbsolutePath()), resources.getString("Errors.No_Data_Found"), JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (prefixes.size() == 1) return new CompressedInstanceLauncher(f, (String) prefixes.get(0));
        JList list = new JList(prefixes.toArray());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        new JOptionPaneClickHandler().install(list);
        Object[] message = new Object[] { resources.formatStrings("Errors.Zip.Multiple_Data_Found_FMT", f.getAbsolutePath()), new JScrollPane(list) };
        if (JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(comp, message, resources.getString("Multiple_Data_Found_Title"), JOptionPane.OK_CANCEL_OPTION)) return null;
        String prefix = (String) list.getSelectedValue();
        if (prefix == null) return null; else {
            DashboardInstance result = new CompressedInstanceLauncher(f, prefix);
            result.setDisplay(resources.format("Launcher.Zip_Display_FMT", prefix, result.getDisplay()));
            return result;
        }
    }
}
