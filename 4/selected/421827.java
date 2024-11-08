package net.sf.gridarta.textedit.scripteditor;

import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import net.sf.gridarta.textedit.textarea.JEditTextArea;
import net.sf.gridarta.textedit.textarea.TextAreaDefaults;
import net.sf.gridarta.utils.Exiter;
import net.sf.gridarta.utils.FileChooserUtils;
import net.sf.japi.swing.action.ActionBuilder;
import net.sf.japi.swing.action.ActionBuilderFactory;
import net.sf.japi.swing.action.ActionMethod;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ScriptEditControl - Manages events and data flow for the script editor
 * entity. There's always at most only one frame open. Additional files get
 * attached to the tab bar.
 * @author <a href="mailto:andi.vogl@gmx.net">Andreas Vogl</a>
 */
public class ScriptEditControl {

    /**
     * The Logger for printing log messages.
     */
    @NotNull
    private static final Category log = Logger.getLogger(ScriptEditControl.class);

    /**
     * Action Builder.
     */
    @NotNull
    private static final ActionBuilder ACTION_BUILDER = ActionBuilderFactory.getInstance().getActionBuilder("net.sf.gridarta");

    @Nullable
    private static CFPythonPopup activePopup = null;

    @NotNull
    private final ScriptEditView view;

    @NotNull
    private final List<String> tabs;

    /**
     * JFileChooser for opening script files.
     */
    @NotNull
    private final JFileChooser openFileChooser;

    @NotNull
    private final FileFilter scriptFileFilter;

    @NotNull
    private final String scriptSuffix;

    public ScriptEditControl(@NotNull final FileFilter scriptFileFilter, @NotNull final String scriptSuffix, @NotNull final Frame owner, final File defaultScriptDir, @NotNull final Preferences preferences, @NotNull final Exiter exiter) {
        this.scriptFileFilter = scriptFileFilter;
        this.scriptSuffix = scriptSuffix;
        tabs = new ArrayList<String>();
        view = new ScriptEditView(this, owner, preferences, exiter);
        openFileChooser = createOpenFileChooser(defaultScriptDir);
    }

    @Deprecated
    public void setTextAreaDefaults(@NotNull final TextAreaDefaults textAreaDefaults) {
        view.setTextAreaDefaults(textAreaDefaults);
    }

    /**
     * Register last active popup. When the script pad frame is hidden, this
     * popup will be closed (if still open).
     * @param activePopup active popup to register
     */
    public static void registerActivePopup(@NotNull final CFPythonPopup activePopup) {
        ScriptEditControl.activePopup = activePopup;
    }

    /**
     * Open a new empty script document.
     */
    @ActionMethod
    public void newScript() {
        tabs.add("<>");
        view.addTab("<New Script>", null);
    }

    /**
     * Open a new empty script document.
     */
    public void openScriptFile(@NotNull final String pathName) {
        final File file = new File(pathName);
        if (!file.exists() || !file.isFile()) {
            if (log.isInfoEnabled()) {
                log.info("Error in ScriptEditControl.openScriptFile():");
                log.info("   File '" + pathName + "' doesn't exist.");
            }
            return;
        }
        tabs.add(file.getAbsolutePath());
        view.addTab(file.getName(), file);
    }

    /**
     * Creates the {@link JFileChooser} for opening a script file.
     * @param defaultScriptDir the initial directory for the file chooser; will
     * be ignored if invalid
     * @return the file chooser
     */
    @NotNull
    private JFileChooser createOpenFileChooser(@NotNull final File defaultScriptDir) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(scriptFileFilter);
        if (defaultScriptDir.exists() && defaultScriptDir.isDirectory()) {
            FileChooserUtils.setCurrentDirectory(fileChooser, defaultScriptDir);
        } else {
            FileChooserUtils.sanitizeCurrentDirectory(fileChooser);
        }
        return fileChooser;
    }

    /**
     * Open a file which is chosen by the user.
     */
    public void openUser() {
        openFileChooser.setDialogTitle("Open Script File");
        FileChooserUtils.sanitizeCurrentDirectory(openFileChooser);
        final int returnVal = openFileChooser.showOpenDialog(view);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File file = openFileChooser.getSelectedFile();
        if (file.exists() && !file.isDirectory()) {
            openScriptFile(file.getAbsolutePath());
        } else {
            newScript();
        }
    }

    /**
     * Close the active script-tab.
     * @return <code>true</code> if the tab was closed, or <code>false</code> if
     *         the user has canceled
     */
    public boolean closeActiveTab() {
        final String title = view.getActiveTitle();
        if (title == null) {
            return true;
        }
        final JEditTextArea activeTextArea = getActiveTextArea();
        if (activeTextArea != null && activeTextArea.isModified() && !ACTION_BUILDER.showQuestionDialog(view, "scriptEdit.confirmClose", title)) {
            return false;
        }
        if (view.getSelectedIndex() >= 0 && !tabs.isEmpty()) {
            tabs.remove(view.getSelectedIndex());
        }
        view.closeActiveTab();
        if (view.getTabCount() <= 0) {
            if (activePopup != null && (activePopup.isShowing() || activePopup.getMenu().isShowing())) {
                activePopup.getMenu().setVisible(false);
            }
            view.setVisible(false);
        }
        return true;
    }

    /**
     * Close all opened script-tabs.
     * @return <code>true</code> if all tabs have been closed, or
     *         <code>false</code> if the user has canceled.
     */
    public boolean closeAllTabs() {
        while (view.getSelectedIndex() >= 0 || !tabs.isEmpty()) {
            if (!closeActiveTab()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Open a file browser and prompt the user for a location/name to store this
     * file. If everything goes fine, the file is saved.
     */
    public void saveAsActiveTab() {
        final String activePath = getActiveFilePath();
        final JEditTextArea activeTextArea = getActiveTextArea();
        if (activeTextArea == null) {
            return;
        }
        final String text = activeTextArea.getText();
        final int tabIndex = view.getSelectedIndex();
        openFileChooser.setDialogTitle("Save Script File As");
        if (activePath != null) {
            final File file = new File(activePath);
            if (file.getParentFile().exists() && file.getParentFile().isDirectory()) {
                openFileChooser.setCurrentDirectory(file.getParentFile());
                openFileChooser.setSelectedFile(file);
            }
        }
        final int returnVal = openFileChooser.showSaveDialog(view);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = openFileChooser.getSelectedFile();
        if (!file.getName().endsWith(scriptSuffix)) {
            final String fileName = file.getAbsolutePath();
            file = new File(fileName + scriptSuffix);
        }
        if (!file.exists() || (activePath != null && file.getAbsolutePath().equals(activePath)) || view.askConfirm("Overwrite?", "A file named \"" + file.getName() + "\" already exists.\n" + "Are you sure you want to overwrite it?")) {
            if (saveTextToFile(file, text)) {
                activeTextArea.resetModified();
            }
            if (tabIndex >= 0 && tabs.size() > tabIndex) {
                tabs.set(tabIndex, file.getAbsolutePath());
                view.setTitleAt(tabIndex, file.getName());
            }
        }
    }

    /**
     * Save the active script-tab to the stored file path.
     */
    public void saveActiveTab() {
        if (getActiveFilePath() != null) {
            final File file = new File(getActiveFilePath());
            final JEditTextArea activeTextArea = getActiveTextArea();
            if (activeTextArea != null && saveTextToFile(file, activeTextArea.getText())) {
                activeTextArea.resetModified();
            }
        } else {
            log.error("ScriptEditControl.saveActiveTab(): Cannot save file without name!");
            saveAsActiveTab();
        }
    }

    /**
     * Write the given text into the specified file.
     * @param file text gets saved into this file
     * @param text text to be saved
     * @return <code>true</code> if the file was saved, or <code>false</code> if
     *         an error occurred
     */
    public boolean saveTextToFile(@NotNull final File file, @NotNull final String text) {
        try {
            final FileOutputStream fos = new FileOutputStream(file);
            try {
                final OutputStreamWriter osw = new OutputStreamWriter(fos);
                try {
                    osw.write(text);
                } finally {
                    osw.close();
                }
            } finally {
                fos.close();
            }
        } catch (final IOException e) {
            view.showMessage("Write Error", "The file \"" + file.getName() + "\" could not be written.\nPlease use the 'Save As...' menu.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * @return currently active JEditTextArea, or null if none are open
     */
    @Nullable
    JEditTextArea getActiveTextArea() {
        return view.getActiveTextArea();
    }

    /**
     * @return file path of active tab, null if no path is available
     */
    @Nullable
    String getActiveFilePath() {
        if (view.getSelectedIndex() < 0 || tabs.size() <= 0) {
            return null;
        }
        final String path = tabs.get(view.getSelectedIndex());
        if (path == null || path.length() == 0 || path.equals("<>")) {
            return null;
        } else {
            return path;
        }
    }
}
