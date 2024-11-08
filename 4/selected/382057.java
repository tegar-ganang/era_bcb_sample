package edu.columbia.concerns.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.metrics.MetricsTable;
import edu.columbia.concerns.util.ProblemManager;

public class SaveMetricsAction extends Action {

    private MetricsTable metricsTable;

    private String suggestedPrefix = "";

    public SaveMetricsAction(MetricsTable metricsTable) {
        this.metricsTable = metricsTable;
        setText(ConcernTagger.getResourceString("actions.SaveMetricsAction.Label"));
        setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(ConcernTagger.ID_PLUGIN, "icons/action_save.gif"));
        setToolTipText(ConcernTagger.getResourceString("actions.SaveMetricsAction.ToolTip"));
    }

    public void setSuggestedPrefix(String suggestedPrefix) {
        this.suggestedPrefix = suggestedPrefix;
    }

    @Override
    public void run() {
        String fileExt = ConcernTagger.getResourceString("actions.SaveMetricsAction.FileExt");
        String path = "";
        boolean done = false;
        while (!done) {
            final FileDialog fileSaveDialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
            fileSaveDialog.setText(ConcernTagger.getResourceString("actions.SaveMetricsAction.DialogTitle"));
            fileSaveDialog.setFilterNames(new String[] { ConcernTagger.getResourceString("actions.SaveMetricsAction.DialogFilterName"), "All Files (*.*)" });
            fileSaveDialog.setFilterExtensions(new String[] { "*" + fileExt, "*.*" });
            String suggested = suggestedPrefix;
            if (!suggested.isEmpty()) suggested += ".";
            suggested += "metrics" + fileExt;
            fileSaveDialog.setFileName(suggested);
            path = fileSaveDialog.open();
            if (path == null || path.isEmpty()) return;
            if (path.indexOf('.') == -1) path += fileExt;
            if (new File(path).exists()) {
                done = MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Confirm File Overwrite", "The file already exists. Overwrite?");
            } else {
                done = true;
            }
        }
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(path);
            PrintStream out = new PrintStream(stream);
            metricsTable.output(out);
        } catch (IOException e) {
            ProblemManager.reportException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    ProblemManager.reportException(e);
                }
            }
        }
    }
}
