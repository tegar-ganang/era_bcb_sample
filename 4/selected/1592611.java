package org.eclipse.mylyn.internal.tasks.ui.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.context.core.InteractionContextManager;
import org.eclipse.mylyn.internal.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.internal.tasks.ui.util.TaskDataExportJob;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * Wizard for exporting tasklist data files to the file system. This wizard uses a single page: TaskDataExportWizardPage
 * 
 * @author Wesley Coelho
 * @author Mik Kersten
 */
public class TaskDataExportWizard extends Wizard implements IExportWizard {

    /**
	 * The name of the dialog store's section associated with the task data export wizard
	 */
    private static final String SETTINGS_SECTION = "org.eclipse.mylyn.tasklist.ui.exportWizard";

    public static final String ZIP_FILE_PREFIX = "mylyndata";

    private static final String ZIP_FILE_EXTENSION = ".zip";

    private static final String WINDOW_TITLE = "Export";

    private TaskDataExportWizardPage exportPage = null;

    public static String getZipFileName() {
        String fomratString = "yyyy-MM-dd";
        SimpleDateFormat format = new SimpleDateFormat(fomratString, Locale.ENGLISH);
        String date = format.format(new Date());
        return ZIP_FILE_PREFIX + "-" + date + ZIP_FILE_EXTENSION;
    }

    public TaskDataExportWizard() {
        IDialogSettings masterSettings = TasksUiPlugin.getDefault().getDialogSettings();
        setDialogSettings(getSettingsSection(masterSettings));
        setNeedsProgressMonitor(true);
        setWindowTitle(WINDOW_TITLE);
    }

    /**
	 * Finds or creates a dialog settings section that is used to make the dialog control settings persistent
	 */
    public IDialogSettings getSettingsSection(IDialogSettings master) {
        IDialogSettings settings = master.getSection(SETTINGS_SECTION);
        if (settings == null) {
            settings = master.addNewSection(SETTINGS_SECTION);
        }
        return settings;
    }

    @Override
    public void addPages() {
        exportPage = new TaskDataExportWizardPage();
        exportPage.setWizard(this);
        addPage(exportPage);
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }

    @Override
    public boolean canFinish() {
        return exportPage.isPageComplete();
    }

    /**
	 * Called when the user clicks finish. Saves the task data. Waits until all overwrite decisions have been made
	 * before starting to save files. If any overwrite is canceled, no files are saved and the user must adjust the
	 * dialog.
	 */
    @Override
    public boolean performFinish() {
        boolean overwrite = exportPage.overwrite();
        boolean zip = exportPage.zip();
        Collection<AbstractTask> taskContextsToExport = TasksUiPlugin.getTaskListManager().getTaskList().getAllTasks();
        String destDir = exportPage.getDestinationDirectory();
        final File destDirFile = new File(destDir);
        if (!destDirFile.exists() || !destDirFile.isDirectory()) {
            StatusHandler.fail(new Exception("File Export Exception"), "Could not export data because specified location does not exist or is not a folder", true);
            return false;
        }
        final File destTaskListFile = new File(destDir + File.separator + ITasksUiConstants.DEFAULT_TASK_LIST_FILE);
        final File destActivationHistoryFile = new File(destDir + File.separator + InteractionContextManager.CONTEXT_HISTORY_FILE_NAME + InteractionContextManager.CONTEXT_FILE_EXTENSION);
        final File destZipFile = new File(destDir + File.separator + getZipFileName());
        if (!overwrite) {
            if (zip) {
                if (destZipFile.exists()) {
                    if (!MessageDialog.openConfirm(getShell(), "Confirm File Replace", "The zip file " + destZipFile.getPath() + " already exists. Do you want to overwrite it?")) {
                        return false;
                    }
                }
            } else {
                if (exportPage.exportTaskList() && destTaskListFile.exists()) {
                    if (!MessageDialog.openConfirm(getShell(), "Confirm File Replace", "The task list file " + destTaskListFile.getPath() + " already exists. Do you want to overwrite it?")) {
                        return false;
                    }
                }
                if (exportPage.exportActivationHistory() && destActivationHistoryFile.exists()) {
                    if (!MessageDialog.openConfirm(getShell(), "Confirm File Replace", "The task activation history file " + destActivationHistoryFile.getPath() + " already exists. Do you want to overwrite it?")) {
                        return false;
                    }
                }
                if (exportPage.exportTaskContexts()) {
                    for (AbstractTask task : taskContextsToExport) {
                        File contextFile = ContextCorePlugin.getContextManager().getFileForContext(task.getHandleIdentifier());
                        File destTaskFile = new File(destDir + File.separator + contextFile.getName());
                        if (destTaskFile.exists()) {
                            if (!MessageDialog.openConfirm(getShell(), "Confirm File Replace", "Task context files already exist in " + destDir + ". Do you want to overwrite them?")) {
                                return false;
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
        }
        TaskDataExportJob job = new TaskDataExportJob(exportPage.getDestinationDirectory(), exportPage.exportTaskList(), exportPage.exportActivationHistory(), exportPage.exportTaskContexts(), exportPage.zip(), destZipFile.getName(), taskContextsToExport);
        IProgressService service = PlatformUI.getWorkbench().getProgressService();
        try {
            service.run(true, false, job);
        } catch (InvocationTargetException e) {
            StatusHandler.fail(e, "Could not export files", true);
        } catch (InterruptedException e) {
            StatusHandler.fail(e, "Could not export files", true);
        }
        exportPage.saveSettings();
        return true;
    }
}
