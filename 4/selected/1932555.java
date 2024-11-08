package net.sourceforge.strategema.ui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Window;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

public class XPFileChooser extends JFileChooser {

    public static final int EXPORT_DIALOG = -1;

    private final UserExperience xp = UserExperience.getUserExperience();

    private FileFilter defaultFilter;

    private int type = JFileChooser.OPEN_DIALOG;

    private JLabel typeLabel;

    private File preSelectedFile = null;

    public XPFileChooser() {
        super();
        this.digInFileChooser();
    }

    public XPFileChooser(final File currentDirectory) {
        super(currentDirectory);
        this.digInFileChooser();
    }

    public XPFileChooser(final File currentDirectory, final FileSystemView fsView) {
        super(currentDirectory, fsView);
        this.digInFileChooser();
    }

    public XPFileChooser(final FileSystemView fsView) {
        super(fsView);
        this.digInFileChooser();
    }

    public XPFileChooser(final String currentDirectoryPath) {
        super(currentDirectoryPath);
        this.digInFileChooser();
    }

    public XPFileChooser(final String currentDirectoryPath, final FileSystemView fsView) {
        super(currentDirectoryPath, fsView);
        this.digInFileChooser();
    }

    @Override
    public void addChoosableFileFilter(final FileFilter filter) {
        super.addChoosableFileFilter(filter);
    }

    public FileFilter getDefaultFileFilter() {
        return this.defaultFilter;
    }

    @Override
    public int getDialogType() {
        return this.type;
    }

    @Override
    public File getSelectedFile() {
        return this.acceptChoice(super.getSelectedFile());
    }

    @Override
    public File[] getSelectedFiles() {
        final File[] files = super.getSelectedFiles();
        for (int i = 0; i < files.length; i++) {
            files[i] = this.acceptChoice(files[i]);
        }
        return files;
    }

    @Override
    public boolean removeChoosableFileFilter(final FileFilter filter) {
        if (this.defaultFilter != null && filter.equals(this.defaultFilter)) this.defaultFilter = null;
        return super.removeChoosableFileFilter(filter);
    }

    @Override
    public void resetChoosableFileFilters() {
        this.defaultFilter = null;
        super.resetChoosableFileFilters();
    }

    public void setDefaultFilter(final FileFilter filter) {
        this.addChoosableFileFilter(filter);
        this.defaultFilter = filter;
    }

    @Override
    public void setSelectedFile(final File file) {
        super.setSelectedFile(file);
        this.preSelectedFile = file;
    }

    @Override
    public void setDialogType(final int type) {
        this.type = type;
        switch(type) {
            case JFileChooser.OPEN_DIALOG:
                {
                    super.setDialogType(JFileChooser.OPEN_DIALOG);
                    this.setAcceptAllFileFilterUsed(true);
                    final String os = this.xp.platformName;
                    if (os.contains("windows")) {
                        if (this.defaultFilter != null) this.setFileFilter(this.defaultFilter);
                    } else {
                        this.setFileFilter(this.getAcceptAllFileFilter());
                    }
                    if (this.typeLabel != null) this.typeLabel.setText("Files of type: ");
                    break;
                }
            case JFileChooser.SAVE_DIALOG:
                {
                    super.setDialogType(JFileChooser.SAVE_DIALOG);
                    if (this.getChoosableFileFilters().length > 0) {
                        this.setAcceptAllFileFilterUsed(false);
                    } else {
                        this.setAcceptAllFileFilterUsed(true);
                    }
                    if (this.defaultFilter != null) this.setFileFilter(this.defaultFilter);
                    if (this.typeLabel != null) this.typeLabel.setText("Save as type: ");
                    break;
                }
            case XPFileChooser.EXPORT_DIALOG:
                {
                    super.setDialogType(JFileChooser.SAVE_DIALOG);
                    this.setApproveButtonText("Export");
                    if (this.getDialogTitle().equals("Save")) this.setDialogTitle("Export");
                    if (this.getChoosableFileFilters().length > 0) {
                        this.setAcceptAllFileFilterUsed(false);
                    } else {
                        this.setAcceptAllFileFilterUsed(true);
                    }
                    if (this.defaultFilter != null) this.setFileFilter(this.defaultFilter);
                    if (this.typeLabel != null) this.typeLabel.setText("Export as type: ");
                    break;
                }
            case JFileChooser.CUSTOM_DIALOG:
                {
                    super.setDialogType(JFileChooser.CUSTOM_DIALOG);
                    break;
                }
        }
    }

    public int showExportDialog(final Component parent) throws HeadlessException {
        this.setDialogType(XPFileChooser.EXPORT_DIALOG);
        FileDialog nativeDialog = null;
        if (this.xp.preferAWTFileDialogs()) nativeDialog = this.toNativeDialog(parent);
        int success;
        boolean write = false;
        do {
            if (nativeDialog != null) {
                nativeDialog.setVisible(true);
                this.fromNativeDialog(nativeDialog);
                if (nativeDialog.getFile() != null) success = JFileChooser.APPROVE_OPTION; else success = JFileChooser.CANCEL_OPTION;
            } else {
                success = super.showSaveDialog(parent);
            }
            if (success == JFileChooser.APPROVE_OPTION) {
                write = this.checkWrite();
            }
        } while (success == JFileChooser.APPROVE_OPTION && !write);
        return success;
    }

    @Override
    public int showOpenDialog(final Component parent) throws HeadlessException {
        this.setDialogType(JFileChooser.OPEN_DIALOG);
        FileDialog nativeDialog = null;
        if (this.xp.preferAWTFileDialogs()) nativeDialog = this.toNativeDialog(parent);
        int success;
        boolean exists = false;
        do {
            if (nativeDialog != null) {
                nativeDialog.setVisible(true);
                this.fromNativeDialog(nativeDialog);
                if (nativeDialog.getFile() != null) success = JFileChooser.APPROVE_OPTION; else success = JFileChooser.CANCEL_OPTION;
            } else {
                success = super.showOpenDialog(parent);
            }
            if (success == JFileChooser.APPROVE_OPTION) {
                final File file = this.getSelectedFile();
                try {
                    exists = file.canRead();
                } catch (final SecurityException e) {
                    exists = false;
                }
                if (!exists) {
                    SystemSounds.error();
                    JOptionPane.showMessageDialog(this, file.getName() + " does not exist or cannot be accessed.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } while (success == JFileChooser.APPROVE_OPTION && !exists);
        return success;
    }

    @Override
    public int showSaveDialog(final Component parent) throws HeadlessException {
        this.setDialogType(JFileChooser.SAVE_DIALOG);
        FileDialog nativeDialog = null;
        if (this.xp.preferAWTFileDialogs()) nativeDialog = this.toNativeDialog(parent);
        int success;
        boolean write = false;
        do {
            if (nativeDialog != null) {
                nativeDialog.setVisible(true);
                this.fromNativeDialog(nativeDialog);
                if (nativeDialog.getFile() != null) success = JFileChooser.APPROVE_OPTION; else success = JFileChooser.CANCEL_OPTION;
            } else {
                success = super.showSaveDialog(parent);
            }
            if (success == JFileChooser.APPROVE_OPTION) {
                write = this.checkWrite();
            }
        } while (success == JFileChooser.APPROVE_OPTION && !write);
        return success;
    }

    @Override
    public int showDialog(final Component parent, final String approveButtonText) {
        FileDialog nativeDialog = null;
        if (this.xp.preferAWTFileDialogs()) nativeDialog = this.toNativeDialog(parent);
        if (nativeDialog != null) {
            switch(this.getDialogType()) {
                case JFileChooser.OPEN_DIALOG:
                    return this.showOpenDialog(parent);
                case JFileChooser.SAVE_DIALOG:
                    return this.showSaveDialog(parent);
                case XPFileChooser.EXPORT_DIALOG:
                    return this.showExportDialog(parent);
                default:
                    {
                        nativeDialog.setVisible(true);
                        this.fromNativeDialog(nativeDialog);
                        if (nativeDialog.getFile() != null) return JFileChooser.APPROVE_OPTION; else return JFileChooser.CANCEL_OPTION;
                    }
            }
        } else {
            return super.showDialog(parent, approveButtonText);
        }
    }

    private FileDialog toNativeDialog(final Component parent) {
        if (parent == null) return null;
        if (this.getChoosableFileFilters().length > 1) return null;
        if (this.getAccessory() != null) return null;
        if (this.getActionListeners().length > 0) return null;
        if (this.getFileSelectionMode() != JFileChooser.FILES_ONLY) return null;
        if (this.isFileHidingEnabled() == true) return null;
        if (this.isMultiSelectionEnabled() == true) return null;
        final Window parentWindow = this.findParentWindow(parent);
        if (parentWindow == null) return null;
        FileDialog dialog;
        if (parentWindow instanceof Frame) {
            dialog = new FileDialog((Frame) parentWindow, this.getDialogTitle());
        } else {
            dialog = new FileDialog((Dialog) parentWindow, this.getDialogTitle());
        }
        final File directory = this.getCurrentDirectory();
        if (directory != null) dialog.setDirectory(directory.getPath()); else dialog.setDirectory(null);
        if (this.preSelectedFile != null) dialog.setFile(this.preSelectedFile.getPath());
        final FileFilter[] filters = this.getChoosableFileFilters();
        if (filters.length == 1) dialog.setFilenameFilter(new FileFilterAdapter(filters[0]));
        final int dialogType = this.getDialogType();
        if (dialogType == JFileChooser.SAVE_DIALOG || dialogType == XPFileChooser.EXPORT_DIALOG) {
            dialog.setMode(FileDialog.SAVE);
        }
        return dialog;
    }

    private void fromNativeDialog(final FileDialog dialog) {
        this.setCurrentDirectory(new File(dialog.getDirectory()));
        final String filename = dialog.getFile();
        if (filename != null) this.setSelectedFile(new File(filename)); else this.setSelectedFile(null);
    }

    private Window findParentWindow(final Component comp) {
        if (comp instanceof Frame || comp instanceof Dialog) {
            return (Window) comp;
        } else {
            final Component parent = comp.getParent();
            if (parent != null) return this.findParentWindow(parent); else return null;
        }
    }

    private static class FileFilterAdapter implements FilenameFilter {

        private final FileFilter filter;

        public FileFilterAdapter(final FileFilter filter) {
            this.filter = filter;
        }

        public boolean accept(final File dir, final String name) {
            return this.filter.accept(new File(dir.getPath() + File.pathSeparator + name));
        }
    }

    private File acceptChoice(final File file) {
        if (file == null) {
            return null;
        } else {
            return new File(this.xp.getFilename(file.getPath(), this.getFileFilter()));
        }
    }

    private boolean checkWrite() {
        final File file = this.getSelectedFile();
        boolean isDir, exists;
        try {
            exists = file.exists();
            isDir = file.isDirectory();
        } catch (final SecurityException e) {
            exists = false;
            isDir = false;
        }
        if (!isDir) {
            boolean canWrite;
            try {
                canWrite = file.canWrite();
            } catch (final SecurityException e) {
                canWrite = false;
            }
            if (exists && canWrite) {
                SystemSounds.warning();
                return JOptionPane.showConfirmDialog(this, "The file " + file.getName() + " already exists.  Do you want to replace the existing file?", "Overwrite File?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
            } else if (!canWrite) {
                SystemSounds.error();
                JOptionPane.showMessageDialog(this, "The file " + file.getName() + " cannot be written to.\n" + "Check that the file is not marked as read-only and that you have sufficient security permissions.", "Access Denied", JOptionPane.ERROR_MESSAGE);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private void digInFileChooser() {
        for (final Component comp : this.getComponents()) {
            if (comp instanceof JPanel) {
                this.digInFileChooserPanel((JPanel) comp);
            }
        }
    }

    private void digInFileChooserPanel(final JPanel panel) {
        for (final Component comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                this.digInFileChooserPanel((JPanel) comp);
            } else {
                if (comp instanceof JLabel) {
                    final JLabel label = (JLabel) comp;
                    final String text = label.getText().toLowerCase();
                    if (text.startsWith("files of type") || text.startsWith("filter")) {
                        this.typeLabel = label;
                    }
                }
            }
        }
    }
}
