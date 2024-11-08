package org.globaltester.testmanager.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;
import org.globaltester.logger.GTLogger;
import org.globaltester.testmanager.Activator;

/**
 * Wizard to create new GlobalTester Test Case.
 * 
 * @version Release 2.2.0
 * @author Holger Funke
 * 
 */
public class NewTestCaseSampleWizard extends BasicNewFileResourceWizard {

    private NewTestCaseSampleWizardMainpage mainPage;

    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        super.init(workbench, currentSelection);
        setWindowTitle("New GlobalTester test case sample");
        setNeedsProgressMonitor(true);
    }

    public void addPages() {
        mainPage = new NewTestCaseSampleWizardMainpage("page1", getSelection());
        mainPage.setTitle("New sample test case");
        mainPage.setDescription("Create new sample test case");
        addPage(mainPage);
    }

    public boolean performFinish() {
        IFile file = mainPage.createNewFile();
        if (file == null) {
            return false;
        }
        selectAndReveal(file);
        IWorkbenchWindow dw = getWorkbench().getActiveWorkbenchWindow();
        try {
            if (dw != null) {
                IWorkbenchPage page = dw.getActivePage();
                if (page != null) {
                    IDE.openEditor(page, file, true);
                }
            }
        } catch (PartInitException e) {
            GTLogger.getInstance().error(e);
            return false;
        }
        IPath projectPath = file.getLocation();
        projectPath = projectPath.removeLastSegments(1);
        IPath pluginDir = Activator.getPluginDir();
        if (pluginDir != null) {
            copy(new File(pluginDir + "stylesheets/testcase/testcase.dtd"), new File(projectPath + "/testcase.dtd"));
        }
        return true;
    }

    /**
	 * Overwrites the existing destination file with the source file
	 * 
	 * @param sourceFile
	 * @param destinationFile
	 */
    private void copy(File sourceFile, File destinationFile) {
        try {
            FileChannel in = new FileInputStream(sourceFile).getChannel();
            FileChannel out = new FileOutputStream(destinationFile).getChannel();
            try {
                in.transferTo(0, in.size(), out);
                in.close();
                out.close();
            } catch (IOException e) {
                GTLogger.getInstance().error(e);
            }
        } catch (FileNotFoundException e) {
            GTLogger.getInstance().error(e);
        }
    }
}
