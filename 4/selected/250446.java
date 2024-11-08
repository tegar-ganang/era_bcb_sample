package uk.ac.warwick.dcs.cokefolk.ui;

import uk.ac.warwick.dcs.cokefolk.util.ClientUtils;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;

public class XPFileChooser extends JFileChooser {

    public static final int EXPORT_DIALOG = -1;

    public boolean forceSwing = false;

    private final UserExperience xp = UserExperience.getPlatformExperience("");

    private FileFilter defaultFilter;

    private int type = JFileChooser.OPEN_DIALOG;

    private JLabel typeLabel;

    private File preSelectedFile = null;

    public XPFileChooser() {
        super();
        digInFileChooser();
    }

    public XPFileChooser(File currentDirectory) {
        super(currentDirectory);
        digInFileChooser();
    }

    public XPFileChooser(File currentDirectory, FileSystemView fsView) {
        super(currentDirectory, fsView);
        digInFileChooser();
    }

    public XPFileChooser(FileSystemView fsView) {
        super(fsView);
        digInFileChooser();
    }

    public XPFileChooser(String currentDirectoryPath) {
        super(currentDirectoryPath);
        digInFileChooser();
    }

    public XPFileChooser(String currentDirectoryPath, FileSystemView fsView) {
        super(currentDirectoryPath, fsView);
        digInFileChooser();
    }

    @Override
    public void addChoosableFileFilter(FileFilter filter) {
        super.addChoosableFileFilter(filter);
    }

    public FileFilter getDefaultFileFilter() {
        return defaultFilter;
    }

    @Override
    public int getDialogType() {
        return type;
    }

    @Override
    public File getSelectedFile() {
        return acceptChoice(super.getSelectedFile());
    }

    @Override
    public File[] getSelectedFiles() {
        File[] files = super.getSelectedFiles();
        for (int i = 0; i < files.length; i++) {
            files[i] = acceptChoice(files[i]);
        }
        return files;
    }

    @Override
    public boolean removeChoosableFileFilter(FileFilter filter) {
        if (defaultFilter != null && filter.equals(defaultFilter)) defaultFilter = null;
        return super.removeChoosableFileFilter(filter);
    }

    @Override
    public void resetChoosableFileFilters() {
        defaultFilter = null;
        super.resetChoosableFileFilters();
    }

    public void setDefaultFilter(FileFilter filter) {
        addChoosableFileFilter(filter);
        defaultFilter = filter;
    }

    @Override
    public void setSelectedFile(File file) {
        super.setSelectedFile(file);
        preSelectedFile = file;
    }

    @Override
    public void setDialogType(int type) {
        this.type = type;
        switch(type) {
            case JFileChooser.OPEN_DIALOG:
                {
                    super.setDialogType(JFileChooser.OPEN_DIALOG);
                    setAcceptAllFileFilterUsed(true);
                    String os = ClientUtils.getOS();
                    if (os == null || os.contains("windows")) {
                        if (defaultFilter != null) setFileFilter(defaultFilter);
                    } else {
                        setFileFilter(getAcceptAllFileFilter());
                    }
                    if (typeLabel != null) typeLabel.setText("Files of type: ");
                    break;
                }
            case JFileChooser.SAVE_DIALOG:
                {
                    super.setDialogType(JFileChooser.SAVE_DIALOG);
                    if (this.getChoosableFileFilters().length > 0) {
                        setAcceptAllFileFilterUsed(false);
                    } else {
                        setAcceptAllFileFilterUsed(true);
                    }
                    if (defaultFilter != null) setFileFilter(defaultFilter);
                    if (typeLabel != null) typeLabel.setText("Save as type: ");
                    break;
                }
            case XPFileChooser.EXPORT_DIALOG:
                {
                    super.setDialogType(JFileChooser.SAVE_DIALOG);
                    setApproveButtonText("Export");
                    if (getDialogTitle().equals("Save")) setDialogTitle("Export");
                    if (getChoosableFileFilters().length > 0) {
                        setAcceptAllFileFilterUsed(false);
                    } else {
                        setAcceptAllFileFilterUsed(true);
                    }
                    if (defaultFilter != null) setFileFilter(defaultFilter);
                    if (typeLabel != null) typeLabel.setText("Export as type: ");
                    break;
                }
            case JFileChooser.CUSTOM_DIALOG:
                {
                    super.setDialogType(JFileChooser.CUSTOM_DIALOG);
                    break;
                }
        }
    }

    public int showExportDialog(Component parent) throws HeadlessException {
        setDialogType(XPFileChooser.EXPORT_DIALOG);
        FileDialog nativeDialog = null;
        if (xp.preferAWTFileDialogs()) nativeDialog = toNativeDialog(parent);
        int success;
        boolean write = false;
        do {
            if (nativeDialog != null) {
                nativeDialog.setVisible(true);
                fromNativeDialog(nativeDialog);
                if (nativeDialog.getFile() != null) success = JFileChooser.APPROVE_OPTION; else success = JFileChooser.CANCEL_OPTION;
            } else {
                success = super.showSaveDialog(parent);
            }
            if (success == JFileChooser.APPROVE_OPTION) {
                write = checkWrite();
            }
        } while (success == JFileChooser.APPROVE_OPTION && !write);
        return success;
    }

    @Override
    public int showOpenDialog(Component parent) throws HeadlessException {
        setDialogType(JFileChooser.OPEN_DIALOG);
        FileDialog nativeDialog = null;
        if (xp.preferAWTFileDialogs()) nativeDialog = toNativeDialog(parent);
        int success;
        boolean exists = false;
        do {
            if (nativeDialog != null) {
                nativeDialog.setVisible(true);
                fromNativeDialog(nativeDialog);
                if (nativeDialog.getFile() != null) success = JFileChooser.APPROVE_OPTION; else success = JFileChooser.CANCEL_OPTION;
            } else {
                success = super.showOpenDialog(parent);
            }
            if (success == JFileChooser.APPROVE_OPTION) {
                File file = getSelectedFile();
                exists = file.exists();
                if (!exists) {
                    SystemSounds.error();
                    JOptionPane.showMessageDialog(this, "The file " + file.getName() + " does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } while (success == JFileChooser.APPROVE_OPTION && !exists);
        return success;
    }

    @Override
    public int showSaveDialog(Component parent) throws HeadlessException {
        setDialogType(JFileChooser.SAVE_DIALOG);
        FileDialog nativeDialog = null;
        if (xp.preferAWTFileDialogs()) nativeDialog = toNativeDialog(parent);
        int success;
        boolean write = false;
        do {
            if (nativeDialog != null) {
                nativeDialog.setVisible(true);
                fromNativeDialog(nativeDialog);
                if (nativeDialog.getFile() != null) success = JFileChooser.APPROVE_OPTION; else success = JFileChooser.CANCEL_OPTION;
            } else {
                success = super.showSaveDialog(parent);
            }
            if (success == JFileChooser.APPROVE_OPTION) {
                write = checkWrite();
            }
        } while (success == JFileChooser.APPROVE_OPTION && !write);
        return success;
    }

    @Override
    public int showDialog(Component parent, String approveButtonText) {
        FileDialog nativeDialog = null;
        if (xp.preferAWTFileDialogs()) nativeDialog = toNativeDialog(parent);
        if (nativeDialog != null) {
            switch(getDialogType()) {
                case JFileChooser.OPEN_DIALOG:
                    return showOpenDialog(parent);
                case JFileChooser.SAVE_DIALOG:
                    return showSaveDialog(parent);
                case XPFileChooser.EXPORT_DIALOG:
                    return showExportDialog(parent);
                default:
                    {
                        nativeDialog.setVisible(true);
                        fromNativeDialog(nativeDialog);
                        if (nativeDialog.getFile() != null) return JFileChooser.APPROVE_OPTION; else return JFileChooser.CANCEL_OPTION;
                    }
            }
        } else {
            return super.showDialog(parent, approveButtonText);
        }
    }

    private FileDialog toNativeDialog(Component parent) {
        if (forceSwing) return null;
        if (parent == null) return null;
        if (getChoosableFileFilters().length > 1) return null;
        if (getAccessory() != null) return null;
        if (getActionListeners().length > 0) return null;
        if (getFileSelectionMode() != JFileChooser.FILES_ONLY) return null;
        if (isFileHidingEnabled() == true) return null;
        if (isMultiSelectionEnabled() == true) return null;
        Window parentWindow = findParentWindow(parent);
        if (parentWindow == null) return null;
        FileDialog dialog;
        if (parentWindow instanceof Frame) {
            dialog = new FileDialog((Frame) parentWindow, this.getDialogTitle());
        } else {
            dialog = new FileDialog((Dialog) parentWindow, this.getDialogTitle());
        }
        File directory = this.getCurrentDirectory();
        if (directory != null) dialog.setDirectory(directory.getPath()); else dialog.setDirectory(null);
        if (preSelectedFile != null) dialog.setFile(preSelectedFile.getPath());
        FileFilter[] filters = this.getChoosableFileFilters();
        if (filters.length == 1) dialog.setFilenameFilter(new FileFilterAdapter(filters[0]));
        int dialogType = this.getDialogType();
        if (dialogType == JFileChooser.SAVE_DIALOG || dialogType == XPFileChooser.EXPORT_DIALOG) {
            dialog.setMode(FileDialog.SAVE);
        }
        return dialog;
    }

    private void fromNativeDialog(FileDialog dialog) {
        setCurrentDirectory(new File(dialog.getDirectory()));
        String filename = dialog.getFile();
        if (filename != null) setSelectedFile(new File(filename)); else setSelectedFile(null);
    }

    private Window findParentWindow(Component comp) {
        if (comp instanceof Frame || comp instanceof Dialog) {
            return (Window) comp;
        } else {
            Component parent = comp.getParent();
            if (parent != null) return findParentWindow(parent); else return null;
        }
    }

    private static class FileFilterAdapter implements FilenameFilter {

        private final FileFilter filter;

        public FileFilterAdapter(FileFilter filter) {
            this.filter = filter;
        }

        public boolean accept(File dir, String name) {
            return filter.accept(new File(dir.getPath() + File.pathSeparator + name));
        }
    }

    private File acceptChoice(File file) {
        if (file == null) return null;
        String os = ClientUtils.getOS();
        FileFilter filter = getFileFilter();
        if (filter instanceof XPFileFilter) {
            return ((XPFileFilter) filter).acceptChoice(file);
        } else {
            if (os == null || os.contains("windows")) {
                String filename = file.getPath();
                if (filename.contains("\"")) {
                    return new File(filename.replaceAll("\"", ""));
                } else if (filename.endsWith(".")) {
                    return new File(filename.substring(0, filename.length() - 1));
                } else {
                    return file;
                }
            } else {
                return file;
            }
        }
    }

    private boolean checkWrite() {
        File file = getSelectedFile();
        if (file.exists() && !file.isDirectory()) {
            boolean canWrite;
            try {
                canWrite = file.canWrite();
            } catch (SecurityException e) {
                canWrite = false;
            }
            if (canWrite) {
                SystemSounds.warning();
                return JOptionPane.showConfirmDialog(this, "The file " + file.getName() + " already exists.  Do you want to replace the existing file?", "Overwrite File?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
            } else {
                SystemSounds.error();
                JOptionPane.showMessageDialog(this, "The file " + file.getName() + " already exists and cannot be overwritten.\n" + "Check that the file is not marked as read only, that you have sufficient security permissions to write data into the file " + "and that Java is allowed to write files.", "Access Denied", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else {
            return true;
        }
    }

    private void digInFileChooser() {
        for (Component comp : getComponents()) {
            if (comp instanceof JPanel) {
                digInFileChooserPanel((JPanel) comp);
            }
        }
    }

    private void digInFileChooserPanel(JPanel panel) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                digInFileChooserPanel((JPanel) comp);
            } else {
                if (comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    String text = label.getText().toLowerCase();
                    if (text.startsWith("files of type") || text.startsWith("filter")) {
                        typeLabel = label;
                    }
                }
            }
        }
    }
}
