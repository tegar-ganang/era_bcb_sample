package org.eclipse.mylyn.tasks.tests;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.ArrayList;
import junit.framework.TestCase;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.mylyn.internal.context.core.InteractionContextManager;
import org.eclipse.mylyn.internal.monitor.core.util.ZipFileUtil;
import org.eclipse.mylyn.internal.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.internal.tasks.ui.WorkspaceAwareContextStore;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.TaskRepositoryManager;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;

/**
 * Tests unused code that was live up to Mylyn 1.0.1, {@link TasksUiPlugin}
 * 
 * @author Rob Elves
 */
public class TaskList06DataMigrationTest extends TestCase {

    private String sourceDir = "testdata/tasklistdatamigrationtest";

    private File sourceDirFile;

    private TaskListDataMigration migrator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sourceDirFile = TaskTestUtil.getLocalFile(sourceDir);
        assertNotNull(sourceDirFile);
        deleteAllFiles(sourceDirFile);
        migrator = new TaskListDataMigration(sourceDirFile);
        assertTrue(sourceDirFile.exists());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        deleteAllFiles(sourceDirFile);
    }

    public void testOldTasklistMigration() throws Exception {
        File oldTasklistFile = new File(sourceDirFile, "tasklist.xml");
        oldTasklistFile.createNewFile();
        assertTrue(new File(sourceDirFile, "tasklist.xml").exists());
        assertTrue(!new File(sourceDirFile, "tasklist.xml.zip").exists());
        assertTrue(migrator.migrateTaskList(new NullProgressMonitor()));
        assertFalse(new File(sourceDirFile, "tasklist.xml").exists());
        assertFalse(!new File(sourceDirFile, "tasklist.xml.zip").exists());
    }

    public void testOldRepositoriesMigration() throws Exception {
        File oldRepositoriesFile = new File(sourceDirFile, "repositories.xml");
        oldRepositoriesFile.createNewFile();
        assertTrue(new File(sourceDirFile, "repositories.xml").exists());
        assertTrue(!new File(sourceDirFile, "repositories.xml.zip").exists());
        assertTrue(migrator.migrateRepositoriesData(new NullProgressMonitor()));
        assertFalse(new File(sourceDirFile, "repositories.xml").exists());
        assertTrue(new File(sourceDirFile, "repositories.xml.zip").exists());
    }

    public void testOldContextMigration() throws Exception {
        String contextFileName1 = URLEncoder.encode("http://oldcontext1.xml", InteractionContextManager.CONTEXT_FILENAME_ENCODING);
        String contextFileName2 = URLEncoder.encode("http://oldcontext2.xml", InteractionContextManager.CONTEXT_FILENAME_ENCODING);
        String contextFileName3 = "task-1.xml";
        File oldContextFile1 = new File(sourceDirFile, contextFileName1);
        oldContextFile1.createNewFile();
        File oldContextFile2 = new File(sourceDirFile, contextFileName2);
        oldContextFile2.createNewFile();
        File oldContextFile3 = new File(sourceDirFile, contextFileName3);
        oldContextFile3.createNewFile();
        File contextFolder = new File(sourceDirFile, WorkspaceAwareContextStore.CONTEXTS_DIRECTORY);
        assertTrue(!contextFolder.exists());
        assertTrue(migrator.migrateTaskContextData(new NullProgressMonitor()));
        assertFalse(oldContextFile1.exists());
        assertFalse(oldContextFile2.exists());
        assertFalse(oldContextFile3.exists());
        assertTrue(contextFolder.exists());
        assertTrue(new File(contextFolder, contextFileName1 + ".zip").exists());
        assertTrue(new File(contextFolder, contextFileName2 + ".zip").exists());
        assertTrue(new File(contextFolder, contextFileName3 + ".zip").exists());
    }

    public void testOldActivityMigration() throws Exception {
        File oldActivityFile = new File(sourceDirFile, InteractionContextManager.OLD_CONTEXT_HISTORY_FILE_NAME + InteractionContextManager.CONTEXT_FILE_EXTENSION_OLD);
        oldActivityFile.createNewFile();
        File contextFolder = new File(sourceDirFile, WorkspaceAwareContextStore.CONTEXTS_DIRECTORY);
        assertTrue(!contextFolder.exists());
        assertTrue(migrator.migrateActivityData(new NullProgressMonitor()));
        assertFalse(oldActivityFile.exists());
        assertTrue(contextFolder.exists());
        assertTrue(new File(contextFolder, InteractionContextManager.CONTEXT_HISTORY_FILE_NAME + InteractionContextManager.CONTEXT_FILE_EXTENSION).exists());
    }

    private void deleteAllFiles(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                if (!file.getName().equals("CVS")) {
                    deleteAllFiles(file);
                    file.delete();
                }
            } else if (!file.getName().equals("empty.txt")) {
                file.delete();
            }
        }
    }
}

class TaskListDataMigration implements IRunnableWithProgress {

    private File dataDirectory = null;

    public TaskListDataMigration(File sourceFolder) {
        this.dataDirectory = sourceFolder;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
            monitor.beginTask("Task Data Migration", IProgressMonitor.UNKNOWN);
            doMigration(monitor);
        } finally {
        }
    }

    public void doMigration(IProgressMonitor monitor) {
        try {
            if (dataDirectory == null || !dataDirectory.exists()) return;
            monitor.beginTask("Mylar Data Migration", 4);
            migrateTaskList(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
            monitor.worked(1);
            migrateRepositoriesData(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
            monitor.worked(1);
            migrateTaskContextData(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
            monitor.worked(1);
            migrateActivityData(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
            monitor.worked(1);
        } finally {
            monitor.done();
        }
    }

    public boolean migrateTaskList(IProgressMonitor monitor) {
        File oldTasklistFile = new File(dataDirectory, ITasksUiConstants.OLD_TASK_LIST_FILE);
        File newTasklistFile = new File(dataDirectory, ITasksUiConstants.DEFAULT_TASK_LIST_FILE);
        if (!oldTasklistFile.exists()) return false;
        if (newTasklistFile.exists()) {
            if (!newTasklistFile.delete()) {
                StatusHandler.fail(null, "Could not overwrite tasklist", false);
                return false;
            }
        }
        ArrayList<File> filesToZip = new ArrayList<File>();
        filesToZip.add(oldTasklistFile);
        try {
            monitor.beginTask("Migrate Tasklist Data", 1);
            ZipFileUtil.createZipFile(newTasklistFile, filesToZip, new SubProgressMonitor(monitor, 1));
            if (!oldTasklistFile.delete()) {
                StatusHandler.fail(null, "Could not remove old tasklist.", false);
                return false;
            }
            monitor.worked(1);
        } catch (Exception e) {
            StatusHandler.fail(e, "Error occurred while migrating old tasklist: " + e.getMessage(), true);
            return false;
        } finally {
            monitor.done();
        }
        return true;
    }

    public boolean migrateRepositoriesData(IProgressMonitor monitor) {
        File oldRepositoriesFile = new File(dataDirectory, TaskRepositoryManager.OLD_REPOSITORIES_FILE);
        File newRepositoriesFile = new File(dataDirectory, TaskRepositoryManager.DEFAULT_REPOSITORIES_FILE);
        if (!oldRepositoriesFile.exists()) return false;
        if (newRepositoriesFile.exists()) {
            if (!newRepositoriesFile.delete()) {
                StatusHandler.fail(null, "Could not overwrite repositories file. Check read/write permission on data directory.", false);
                return false;
            }
        }
        ArrayList<File> filesToZip = new ArrayList<File>();
        filesToZip.add(oldRepositoriesFile);
        try {
            monitor.beginTask("Migrate Repository Data", 1);
            ZipFileUtil.createZipFile(newRepositoriesFile, filesToZip, new SubProgressMonitor(monitor, 1));
            if (!oldRepositoriesFile.delete()) {
                StatusHandler.fail(null, "Could not remove old repositories file. Check read/write permission on data directory.", false);
                return false;
            }
            monitor.worked(1);
        } catch (Exception e) {
            StatusHandler.fail(e, "Error occurred while migrating old repositories data: " + e.getMessage(), true);
            return false;
        } finally {
            monitor.done();
        }
        return true;
    }

    public boolean migrateTaskContextData(IProgressMonitor monitor) {
        ArrayList<File> contextFiles = new ArrayList<File>();
        for (File file : dataDirectory.listFiles()) {
            if (file.getName().startsWith("http") || file.getName().startsWith("local") || file.getName().startsWith("task")) {
                if (!file.getName().endsWith(".zip")) {
                    contextFiles.add(file);
                }
            }
        }
        try {
            monitor.beginTask("Task Context Migration", contextFiles.size());
            File contextsFolder = new File(dataDirectory, WorkspaceAwareContextStore.CONTEXTS_DIRECTORY);
            if (!contextsFolder.exists()) {
                if (!contextsFolder.mkdir()) {
                    StatusHandler.fail(null, "Could not create contexts folder. Check read/write permission on data directory.", false);
                    return false;
                }
            }
            for (File file : contextFiles) {
                ArrayList<File> filesToZip = new ArrayList<File>();
                filesToZip.add(file);
                File newContextFile = new File(contextsFolder, file.getName() + ".zip");
                if (newContextFile.exists()) {
                    if (!newContextFile.delete()) {
                        StatusHandler.fail(null, "Could not overwrite context file. Check read/write permission on data directory.", false);
                        return false;
                    }
                }
                ZipFileUtil.createZipFile(newContextFile, filesToZip, new SubProgressMonitor(monitor, 1));
                if (!file.delete()) {
                    StatusHandler.fail(null, "Could not remove old context file. Check read/write permission on data directory.", false);
                    return false;
                }
                monitor.worked(1);
            }
        } catch (Exception e) {
            StatusHandler.fail(e, "Error occurred while migrating old repositories data: " + e.getMessage(), true);
            return false;
        } finally {
            monitor.done();
        }
        return true;
    }

    public boolean migrateActivityData(IProgressMonitor monitor) {
        File oldActivityFile = new File(dataDirectory, InteractionContextManager.OLD_CONTEXT_HISTORY_FILE_NAME + InteractionContextManager.CONTEXT_FILE_EXTENSION_OLD);
        if (!oldActivityFile.exists()) return false;
        File contextsFolder = new File(dataDirectory, WorkspaceAwareContextStore.CONTEXTS_DIRECTORY);
        if (!contextsFolder.exists()) {
            if (!contextsFolder.mkdir()) {
                StatusHandler.fail(null, "Could not create contexts folder. Check read/write permission on data directory.", false);
                return false;
            }
        }
        File newActivityFile = new File(contextsFolder, InteractionContextManager.CONTEXT_HISTORY_FILE_NAME + InteractionContextManager.CONTEXT_FILE_EXTENSION);
        if (newActivityFile.exists()) {
            if (!newActivityFile.delete()) {
                StatusHandler.fail(null, "Could not overwrite activity file. Check read/write permission on data directory.", false);
                return false;
            }
        }
        ArrayList<File> filesToZip = new ArrayList<File>();
        filesToZip.add(oldActivityFile);
        try {
            monitor.beginTask("Migrate Activity Data", 1);
            ZipFileUtil.createZipFile(newActivityFile, filesToZip, new SubProgressMonitor(monitor, 1));
            if (!oldActivityFile.delete()) {
                StatusHandler.fail(null, "Could not remove old activity file. Check read/write permission on data directory.", false);
                return false;
            }
            monitor.worked(1);
        } catch (Exception e) {
            StatusHandler.fail(e, "Error occurred while migrating old activity data: " + e.getMessage(), true);
            return false;
        } finally {
            monitor.done();
        }
        return true;
    }
}
