package ti.plato.components.ui.oscript.hooks;

import java.io.File;
import java.io.IOException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import ti.mcore.u.FileUtil;
import ti.mcore.u.log.PlatoLogger;
import ti.oscript.eclipse.ui.OScriptEditorActionsUtil;
import ti.oscript.eclipse.ui.editor.OScriptEditor;
import ti.plato.components.ui.oscript.internal.file.NewFileDialog;
import ti.plato.components.ui.oscript.internal.file.PlatoEditorUtil;
import ti.plato.filehandler.hooks.IFileHandlerHook;
import ti.plato.filehandler.util.FileHandlerUtil;
import ti.plato.scripts.ScriptLocation;
import ti.plato.ui.views.manager.util.PlatoViewManagerUtil;
import ti.plato.util.PlatoPluginUtil;

public class FileEditableFileHook implements IFileHandlerHook {

    private static String scriptsExtension = "Scripts";

    private static String libraryExtension = "Library";

    private static String startupExtension = "Startup";

    private static final PlatoLogger LOGGER = PlatoLogger.getLogger(FileEditableFileHook.class);

    private IWorkbenchPage _page;

    public boolean isEnabled(int contribution) {
        return true;
    }

    public String run(Action action, String path) {
        if (action == Action.NEW) {
            return runNew();
        } else if (action == Action.OPEN) {
            return openFile(path, FileHandlerUtil.INVALID_LINE_NUMBER, false);
        } else if (action == Action.SAVE) {
            runSave();
            return null;
        } else if (action == Action.SAVE_AS) {
            runSaveAs();
            return null;
        } else if (action == Action.SAVE_ALL) {
        }
        return null;
    }

    public String runNew() {
        Shell shell = PlatoPluginUtil.getShell();
        if (shell == null) {
            LOGGER.logError(new Throwable("shell == null"));
            return null;
        }
        NewFileDialog dialog = new NewFileDialog(shell);
        dialog.open();
        String dirPathStr = dialog.getDirectoryPath();
        if (dirPathStr == null) {
            return null;
        }
        boolean isDirExists = false;
        try {
            isDirExists = FileUtil.exists(dirPathStr);
        } catch (SecurityException e) {
            String msg = "Directory is not accessible";
            errorDialog_runNew(shell, msg, msg + ": \"" + dirPathStr + "\".");
            return null;
        }
        if (!isDirExists || !FileUtil.isDirectory(dirPathStr)) {
            if (dialog.isAutoCreateDirectory()) {
                File dirFile = new File(dirPathStr);
                try {
                    dirFile.mkdirs();
                } catch (Exception e) {
                    String title = "Unable to create directory";
                    errorDialog_runNew(shell, title, title + ": \"" + dirPathStr + "\" \n" + "Reason: " + e.toString());
                    return null;
                }
            } else {
                errorDialog_runNew(shell, "Invalid directory path", "Directory '" + dirPathStr + "' does not exist.");
                return null;
            }
        }
        String fileNameStr = dialog.getFileName();
        String invalidFileNameMsg = PlatoEditorUtil.isValidFileName(fileNameStr);
        if (invalidFileNameMsg != null) {
            errorDialog_runNew(shell, "Invalid file name", invalidFileNameMsg);
            return null;
        }
        File file = new File(dirPathStr, fileNameStr);
        if (!file.exists()) {
            String errorMsg = "Unable to create file '" + dirPathStr + '/' + fileNameStr + "'";
            String invalidPathMsg = PlatoEditorUtil.isValidPath(file.getAbsolutePath());
            if (invalidPathMsg != null) {
                errorDialog_runNew(shell, "Invalid directory path", errorMsg + "; \n" + invalidPathMsg);
                return null;
            }
            try {
                boolean isCreated = file.createNewFile();
                if (!isCreated) {
                    errorDialog_runNew(shell, errorMsg, errorMsg + " \"" + file.getAbsolutePath() + "\"");
                    return null;
                }
            } catch (IOException e) {
                errorDialog_runNew(shell, errorMsg, errorMsg + " \"" + file.getAbsolutePath() + "\"; \n" + "Reason:" + e.toString());
                return null;
            }
        }
        IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(dirPathStr));
        fileStore = fileStore.getChild(fileNameStr);
        String editorId = PlatoEditorUtil.getEditorId(fileStore);
        IEditorInput input = PlatoEditorUtil.createEditorInput(fileStore);
        try {
            _page = PlatoEditorUtil.openEditor(PlatoViewManagerUtil.getActivePage(), input, dirPathStr + File.separator + fileNameStr, editorId);
        } catch (PartInitException e) {
            String errorMsg = "Unable to open file in editor";
            errorDialog_runNew(shell, errorMsg, errorMsg + " \"" + file.getAbsolutePath() + "\"; \n" + "Reason:" + e.toString());
            LOGGER.logError(e);
            return null;
        }
        return dirPathStr + File.separator + fileNameStr;
    }

    private void errorDialog_runNew(Shell shell, String title, String msg) {
        MessageDialog.openError(shell, title, msg);
        runNew();
    }

    public void runSave() {
        OScriptEditorActionsUtil.runEditorAction(ITextEditorActionConstants.SAVE);
    }

    private void runSaveAs() {
        OScriptEditor e = PlatoEditorUtil.getActiveOScriptEditor();
        if (e == null) {
            return;
        }
        Shell shell = PlatoPluginUtil.getShell();
        FileDialog dialog = new FileDialog(shell, SWT.SAVE | SWT.SYSTEM_MODAL);
        dialog.setText("Save As...");
        String OS_EXT = ".os";
        dialog.setFilterExtensions(new String[] { "*" + OS_EXT, "*" });
        dialog.setFilterNames(new String[] { "Object Script File (*" + OS_EXT + ")", "All Files (*)" });
        File currentFile = e.getEditedFile();
        if (currentFile == null) {
            dialog.setFileName("Untitled" + OS_EXT);
        } else {
            dialog.setFileName(currentFile.getAbsolutePath());
        }
        dialog.setFilterPath(System.getProperty("user.home") + File.separator + "Desktop");
        final String name = dialog.open();
        if (name == null || name.equals("")) {
            return;
        }
        File newFile = addFileExtension(currentFile, name);
        if (currentFile.equals(newFile)) {
            return;
        }
        boolean isOverride = true;
        if (newFile.exists() && !currentFile.equals(newFile)) {
            MessageBox mbox = new MessageBox(shell, SWT.OK | SWT.CANCEL | SWT.ICON_WARNING | SWT.APPLICATION_MODAL);
            mbox.setText("Warning");
            mbox.setMessage("This file already exists. Do you want to overwrite it?");
            if (mbox.open() == SWT.CANCEL) {
                isOverride = false;
            }
        }
        boolean isSaveChanges = true;
        if (e.isDirty()) {
            MessageBox mbox = new MessageBox(shell, SWT.OK | SWT.CANCEL | SWT.ICON_WARNING | SWT.APPLICATION_MODAL);
            mbox.setText("Warning");
            mbox.setMessage("Save current changes?");
            if (mbox.open() == SWT.CANCEL) {
                isSaveChanges = false;
            }
        }
        if (isSaveChanges) {
            runSave();
        }
        String errorMsg = FileUtil.cp(currentFile, newFile, isOverride);
        if (errorMsg == null) {
            openFile(newFile.getAbsolutePath(), FileHandlerUtil.INVALID_LINE_NUMBER, false);
        } else {
            MessageBox mbox = new MessageBox(shell, SWT.OK | SWT.CANCEL | SWT.ICON_WARNING | SWT.APPLICATION_MODAL);
            mbox.setText("Warning");
            mbox.setMessage("Unable to create a new file.\nReason: " + errorMsg);
        }
    }

    private File addFileExtension(File sourceFile, String name) {
        if (name == null || name.equals("")) {
            return null;
        }
        IPath originalPath = new Path(sourceFile.getAbsolutePath());
        IPath destinationPath = new Path(name);
        if (destinationPath.getFileExtension() == null && originalPath != null && originalPath.getFileExtension() != null) {
            destinationPath = destinationPath.addFileExtension(originalPath.getFileExtension());
        }
        return destinationPath.toFile();
    }

    public void dispose() {
        if (_page != null) {
            _page.close();
            _page = null;
        }
    }

    /**
	 * Un-mangle the name to something that is presentable to the user, ie
	 * "2_Run_Something.os" becomes "Run Something".
	 */
    private static String sanitizeScript(String name) {
        if (name.endsWith(".os")) {
            name = name.substring(0, name.length() - 3);
        }
        boolean isParseable = false;
        int idx = name.indexOf('_');
        if (idx != -1) {
            String prefix = name.substring(0, idx);
            try {
                Integer.parseInt(prefix);
                isParseable = true;
            } catch (Throwable t) {
                isParseable = false;
            }
            if (isParseable) {
                name = name.substring(idx + 1);
            }
        }
        if (isParseable) {
            name = name.replaceAll("_", " ");
        }
        return name;
    }

    /**
	 * Un-mangle the name to something that is presentable to the user, ie
	 * "2_Run_Something.os" becomes "Run Something".
	 */
    private static String sanitizeLibraryStartup(String name) {
        if (name.endsWith(".os")) {
            name = name.substring(0, name.length() - 3);
        }
        return name;
    }

    private static String getScriptLocation(String path) {
        String result = null;
        ScriptLocation[] locations = ScriptLocation.getScriptLocations();
        for (int i = 0; i < locations.length; i++) {
            String absolutePath = locations[i].getPath().getAbsolutePath();
            if (path.startsWith(absolutePath)) {
                if ((result == null) || (result.length() < absolutePath.length())) {
                    result = absolutePath;
                }
            }
        }
        return result;
    }

    public String getSanitizedDisplayName(String path) {
        if (!path.endsWith(".os")) {
            return null;
        }
        String name = (new File(path)).getName();
        String scriptLocation = getScriptLocation(path);
        if (scriptLocation == null) {
            return sanitizeLibraryStartup(name);
        }
        String relativePath = path.substring(scriptLocation.length() + 1);
        if (relativePath.startsWith(scriptsExtension)) {
            return sanitizeScript(name);
        } else if (relativePath.startsWith(libraryExtension)) {
            return sanitizeLibraryStartup(name);
        } else if (relativePath.startsWith(startupExtension)) {
            return sanitizeLibraryStartup(name);
        }
        return null;
    }

    public String openFile(String path, int lineNumber, boolean useSystemHandler) {
        if (useSystemHandler) {
            return openSystemFile(path);
        }
        IWorkbenchPage page;
        try {
            page = PlatoEditorUtil.openExternalFile(path);
        } catch (Throwable t) {
            LOGGER.logWarning(t);
            page = null;
        }
        if (page == null) {
            return openSystemFile(path);
        }
        PlatoEditorUtil.gotoLine(page.getActiveEditor(), lineNumber);
        return path;
    }

    private String openSystemFile(String path) {
        if (PlatoEditorUtil.openInSystemEditor(path, true)) {
            return path;
        }
        return null;
    }
}
