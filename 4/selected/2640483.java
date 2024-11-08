package edu.columbia.concerns.actions;

import java.io.File;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.util.ConcernARFFFile;

/**
 * @author eaddy
 * 
 */
public class ExportConcernsAction extends Action {

    private IConcernModelProvider concernModelProvider;

    private IStatusLineManager statusLineManager;

    private String suggestedPrefix = "";

    private boolean outputARFF = false;

    public ExportConcernsAction(IConcernModelProvider concernModelProvider, IStatusLineManager statusLineManager) {
        this.concernModelProvider = concernModelProvider;
        this.statusLineManager = statusLineManager;
        setText(ConcernTagger.getResourceString("actions.ExportConcernsAction.Label"));
        setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(ConcernTagger.ID_PLUGIN, "icons/action_save.gif"));
        setToolTipText(ConcernTagger.getResourceString("actions.ExportConcernsAction.ToolTip"));
    }

    public void setSuggestedPrefix(String suggestedPrefix) {
        this.suggestedPrefix = suggestedPrefix;
    }

    @Override
    public void run() {
        String arffFileExt = ConcernTagger.getResourceString("actions.ExportConcernsAction.FileExt1");
        String txtFileExt = ConcernTagger.getResourceString("actions.ExportConcernsAction.FileExt2");
        String path = "";
        boolean done = false;
        while (!done) {
            final FileDialog fileSaveDialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
            fileSaveDialog.setText(ConcernTagger.getResourceString("actions.ExportConcernsAction.DialogTitle"));
            fileSaveDialog.setFilterNames(new String[] { ConcernTagger.getResourceString("actions.ExportConcernsAction.DialogFilterName1"), ConcernTagger.getResourceString("actions.ExportConcernsAction.DialogFilterName2") });
            fileSaveDialog.setFilterExtensions(new String[] { "*" + arffFileExt, "*" + txtFileExt });
            String suggested = suggestedPrefix;
            if (suggested.isEmpty()) suggested = "concerns";
            suggested += arffFileExt;
            fileSaveDialog.setFileName(suggested);
            path = fileSaveDialog.open();
            if (path == null || path.isEmpty()) return;
            if (path.lastIndexOf('.') == -1) {
                path += arffFileExt;
            }
            if (new File(path).exists()) {
                done = MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Confirm File Overwrite", "The file already exists. Overwrite?");
            } else {
                done = true;
            }
        }
        int lastDot = path.lastIndexOf('.');
        if (lastDot > -1) {
            String fileExt = path.substring(lastDot);
            outputARFF = fileExt.compareToIgnoreCase(arffFileExt) == 0;
        } else {
            outputARFF = true;
        }
        final String pathForJob = path;
        Job job = new Job("Exporting concerns...") {

            @Override
            protected IStatus run(IProgressMonitor progressMonitor) {
                return saveConcernsToFile(pathForJob, progressMonitor, statusLineManager);
            }
        };
        job.setUser(true);
        job.schedule();
    }

    private IStatus saveConcernsToFile(final String path, IProgressMonitor progressMonitor, IStatusLineManager statusLineManager) {
        ConcernARFFFile asf = new ConcernARFFFile(path, concernModelProvider, progressMonitor, statusLineManager);
        if (outputARFF) asf.save(); else asf.saveWithIndention();
        return Status.OK_STATUS;
    }
}
