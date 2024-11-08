package com.abso.sunlight.explorer.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.program.Program;
import com.abso.sunlight.api.Legislator;
import com.abso.sunlight.api.SunlightException;
import com.abso.sunlight.explorer.CongressExplorerPlugin;

/**
 * Base class for wizards able to export a list of legislators into a file.
 */
public abstract class ExportFileWizard extends Wizard {

    /** The list of all legislators returned by a specific query and listed into a editor. */
    private List<Legislator> allLegislators;

    /** The list of legislators selected into a editor. */
    private List<Legislator> selectedLegislators;

    /** Indicates if the initial option must be either to export all legislators or the selected ones only. */
    private boolean initialExportAll;

    /**
     * Constructs a new wizard.
     * 
     * @param allLegislators
     *            the list of all legislators returned by a specific query and listed into a editor.
     * @param selectedLegislators
     *            the list of legislators selected into a editor.
     * @param initialExportAll
     *            indicates if the initial option must be either to export all legislators or the selected ones only.
     */
    public ExportFileWizard(List<Legislator> allLegislators, List<Legislator> selectedLegislators, boolean initialExportAll) {
        super();
        this.allLegislators = allLegislators;
        this.selectedLegislators = selectedLegislators;
        this.initialExportAll = initialExportAll;
        setWindowTitle(getWizardTitle());
        setDialogSettings(CongressExplorerPlugin.getDefault().getDialogSettings(this.getClass().getSimpleName()));
    }

    @Override
    public final void addPages() {
        super.addPages();
        ExportFileWizardPage page = createExportPage();
        page.setExportOptions(initialExportAll, !selectedLegislators.isEmpty());
        addPage(page);
    }

    @Override
    public final boolean performFinish() {
        final ExportFileWizardPage wizardPage = ((ExportFileWizardPage) getPages()[0]);
        final File file = wizardPage.getDestinationFile();
        if (file.isDirectory()) {
            MessageDialog.openError(getShell(), "Export Problems", "Export destination must be a file, not a directory.");
            return false;
        }
        if (file.exists()) {
            if (!MessageDialog.openQuestion(getShell(), "Overwrite", "Target file already exists. Would you like to overwrite it?")) {
                return false;
            }
        }
        try {
            wizardPage.saveSettings();
            final List<Legislator> legislators = wizardPage.isExportAll() ? allLegislators : selectedLegislators;
            final boolean includePhotos = wizardPage.isIncludePhotos();
            IRunnableWithProgress runnable = new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        doExport(legislators, file, includePhotos, monitor);
                        if (!monitor.isCanceled()) {
                            Program.launch(file.getAbsolutePath());
                        }
                    } catch (SunlightException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            };
            new ProgressMonitorDialog(getShell()).run(true, true, runnable);
        } catch (InvocationTargetException e) {
            Throwable e2 = (e.getCause() != null) ? e.getCause() : e;
            CongressExplorerPlugin.getDefault().showErrorDialog(getShell(), "Export Problems", "Unable to export", e2.getMessage(), e2);
            return false;
        } catch (InterruptedException e) {
        }
        return true;
    }

    /**
     * Creates the unique page composing the wizard.
     * 
     * @return the export page.
     */
    protected abstract ExportFileWizardPage createExportPage();

    /**
     * Returns the wizard's title.
     * 
     * @return the wizard's dialog title.
     */
    protected abstract String getWizardTitle();

    /**
     * Performs the export.
     * 
     * @param legislators
     *            the list of legislators to export.
     * @param file
     *            the destination files.
     * @param includePhotos
     *            if <code>true</code> the exported data also include photos.
     * @param monitor
     *            the progress monitor.
     * @throws SunlightException
     *             if an exception occurred performing the export.
     */
    protected abstract void doExport(List<Legislator> legislators, File file, boolean includePhotos, IProgressMonitor monitor) throws SunlightException;
}
