package org.kompiro.readviewer.ui.wizards;

import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.kompiro.readviewer.service.StatusHandler;
import org.kompiro.readviewer.ui.UIActivator;

public class ExportWizard extends Wizard implements IExportWizard {

    private ExportServicesPage servicesExportPage;

    public ExportWizard() {
        servicesExportPage = new ExportServicesPage();
        addPage(servicesExportPage);
    }

    @Override
    public boolean performFinish() {
        IPath path = UIActivator.getDefault().getStateLocation().append(UIActivator.SERVICE_XML);
        try {
            FileUtils.copyFile(path.toFile(), servicesExportPage.getFile());
        } catch (IOException e) {
            StatusHandler.fail(e, "Error is occured when exporting service.xml.", true);
            return false;
        }
        return true;
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }
}
