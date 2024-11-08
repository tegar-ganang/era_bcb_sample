package com.safi.workshop.exportwiz;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import com.safi.db.SafiDriverManager;
import com.safi.db.Variable;
import com.safi.db.server.config.Saflet;
import com.safi.db.server.config.SafletProject;
import com.safi.server.manager.SafiServerRemoteManager;
import com.safi.server.plugin.SafiServerPlugin;
import com.safi.server.saflet.importing.ImportArchiveException;
import com.safi.server.saflet.importing.OverwriteMode;
import com.safi.server.saflet.util.FileUtils;
import com.safi.workshop.importwiz.ImportUtils;
import com.safi.workshop.navigator.db.RetrieveSafletAction;
import com.safi.workshop.sqlexplorer.plugin.SQLExplorerPlugin;
import com.safi.workshop.util.SafletPersistenceManager;

public class ImportWorkspaceArchiveWizard extends Wizard {

    private SelectArchivePage selectArchivePage;

    private ViewArchiveContentsPage viewContentsPage;

    private ImportDestinationPage importDestinationPage;

    private SelectGlobalsPage selectGlobalsPage;

    public ImportWorkspaceArchiveWizard() {
    }

    @Override
    public boolean performFinish() {
        String filePath = selectArchivePage.getFilePath();
        if (StringUtils.isBlank(filePath)) {
            MessageDialog.openError(getShell(), "No File Selected", "Please select a workspace archive file (.sar) file to import");
            return false;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            MessageDialog.openError(getShell(), "File Not Found", "File " + filePath + " could not be found.  Please select a valid workspace archive file (.sar) file to import");
            return false;
        }
        if (importDestinationPage != null && importDestinationPage.isRemote()) {
            try {
                SafiServerRemoteManager.getInstance().transferWorkspaceArchive(file.getName(), FileUtils.readFile(filePath), OverwriteMode.FAIL);
            } catch (ImportArchiveException e) {
                e.printStackTrace();
                if (e.getType() == ImportArchiveException.Type.SYSTEM) {
                    MessageDialog.openError(getShell(), "Import Failed", "File " + filePath + " could not be imported: " + e.getLocalizedMessage());
                    return false;
                } else {
                    MessageDialog dialog = new MessageDialog(getShell(), "Resource Exists", null, "Name conflict: " + e.getLocalizedMessage() + ". Do you want to\nSkip: " + "(Resources sharing the same name as existing resources will not be imported)\n" + "or Overwrite (Resources sharing the same name as existing resources will be replaced", SWT.ICON_QUESTION, new String[] { "SKIP", "OVERWRITE", "CANCEL" }, 2);
                    int result = dialog.open();
                    if (result == 2) return false;
                    try {
                        SafiServerRemoteManager.getInstance().transferWorkspaceArchive(file.getName(), FileUtils.readFile(filePath), result == 0 ? OverwriteMode.SKIP : OverwriteMode.OVERWRITE);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        MessageDialog.openError(getShell(), "Import Failed", "File " + filePath + " could not be imported: " + e.getLocalizedMessage());
                        return false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                MessageDialog.openError(getShell(), "Import Failed", "File " + filePath + " could not be imported: " + e.getLocalizedMessage());
                return false;
            }
            List<String> saflets = getViewContentsPage().getSaflets();
            if (saflets != null && !saflets.isEmpty()) {
                if (MessageDialog.openQuestion(getShell(), "Retrieve Saflets?", saflets.size() + " Saflets were imported and uploaded to the SafiServer.\nWould you like to load them into your workspace now?")) {
                    RetrieveSafletAction action = new RetrieveSafletAction();
                    action.setSelectedSaflets(saflets);
                    action.run(null);
                }
            }
            try {
                SafiDriverManager driverManager = SafiServerPlugin.getDefault().getDriverManager();
                SafiDriverManager newManager = getViewContentsPage().getDBResources();
                boolean cancel = ImportUtils.mergeDriverManager(getShell(), driverManager, newManager);
                if (!cancel) {
                    SQLExplorerPlugin.getDefault().rebuildDBNavModel();
                }
            } catch (Exception e) {
                e.printStackTrace();
                MessageDialog.openError(getShell(), "DB Import Failed", "Database resources could not be imported: " + e.getLocalizedMessage());
                return false;
            }
            return true;
        } else {
            try {
                IWorkspace ws = ResourcesPlugin.getWorkspace();
                IProject[] projects = ws.getRoot().getProjects();
                List<IProject> plist = new ArrayList<IProject>(Arrays.asList(projects));
                List<Saflet> saflets = new ArrayList<Saflet>();
                Map<SafletProject, IProject> projectToResourceMap = new HashMap<SafletProject, IProject>();
                List<SafletProject> sps = getViewContentsPage().getSafletProjects();
                SafletPersistenceManager.getInstance().prepareProjects(getShell(), (List) sps, projectToResourceMap, plist);
                for (SafletProject sp : sps) {
                    for (Saflet s : sp.getSaflets()) {
                        saflets.add(s);
                    }
                }
                Map<IProject, List<Saflet>> perProjectMap = new HashMap<IProject, List<Saflet>>();
                for (Saflet saflet : saflets) {
                    SafletProject proj = saflet.getProject();
                    IProject p = projectToResourceMap.get(proj);
                    if (p != null) {
                        List<Saflet> sl = perProjectMap.get(p);
                        if (sl == null) {
                            sl = new ArrayList<Saflet>();
                            perProjectMap.put(p, sl);
                        }
                        sl.add(saflet);
                    }
                }
                for (Map.Entry<IProject, List<Saflet>> entry : perProjectMap.entrySet()) {
                    SafletPersistenceManager.getInstance().addOrUpdateSaflets(entry.getKey(), entry.getValue(), true, true);
                }
                if (SafiServerPlugin.getDefault().isConnected()) {
                    List<Variable> vars = getViewContentsPage().getGlobalVariables();
                    boolean overwriteAll = false;
                    for (Variable v : vars) {
                        Variable var = SafiServerPlugin.getDefault().getGlobalVariable(v.getName());
                        if (var != null) {
                            if (!overwriteAll) {
                                MessageDialog dialog = new MessageDialog(getShell(), "Global Variable Exists", null, "Variable " + var.getName() + " exists. Do you want to\nSkip, Overwrite, or Overwrite all?", SWT.ICON_QUESTION, new String[] { "SKIP", "OVERWRITE", "OVERWRITE ALL" }, 0);
                                int result = dialog.open();
                                if (result == 2) overwriteAll = true; else if (result == 0) continue;
                            }
                            SafiServerPlugin.getDefault().deleteGlobalVariable(var);
                        }
                        SafiServerPlugin.getDefault().addGlobalVariable(v);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                MessageDialog.openError(getShell(), "Import Failed", "Resources could not be imported: " + e.getLocalizedMessage());
                return false;
            }
            try {
                SafiDriverManager driverManager = SafiServerPlugin.getDefault().getDriverManager();
                SafiDriverManager newManager = getViewContentsPage().getDBResources();
                boolean cancel = ImportUtils.mergeDriverManager(getShell(), driverManager, newManager);
                if (!cancel) {
                    SQLExplorerPlugin.getDefault().rebuildDBNavModel();
                }
            } catch (Exception e) {
                e.printStackTrace();
                MessageDialog.openError(getShell(), "DB Import Failed", "Database resources could not be imported: " + e.getLocalizedMessage());
                return false;
            }
            return true;
        }
    }

    public SelectArchivePage getSelectArchivePage() {
        if (selectArchivePage == null) addPage(selectArchivePage = new SelectArchivePage());
        return selectArchivePage;
    }

    public ViewArchiveContentsPage getViewContentsPage() {
        if (viewContentsPage == null) addPage(viewContentsPage = new ViewArchiveContentsPage());
        return viewContentsPage;
    }

    public ImportDestinationPage getImportDestinationPage() {
        if (importDestinationPage == null) addPage(importDestinationPage = new ImportDestinationPage());
        return importDestinationPage;
    }

    @Override
    public void addPages() {
        getSelectArchivePage();
        getViewContentsPage();
        if (SafiServerPlugin.getDefault().isConnected()) getImportDestinationPage();
        super.addPages();
    }
}
