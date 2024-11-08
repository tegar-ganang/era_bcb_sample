package org.gello.client.threads;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.gello.client.manager.Project;
import org.gello.client.model.ObjectXMLSerializer;
import org.gello.client.views.Browser;

/**
 * A thread to export a project after it is ready to be exported.
 *
 */
public class ExportProjectThread implements Runnable {

    Project project = null;

    public ExportProjectThread(Project project) {
        this.project = project;
    }

    public void run() {
        String xml = new ObjectXMLSerializer().toXML(project);
        FileDialog fileDialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
        fileDialog.setFileName(project.getName());
        fileDialog.setFilterExtensions(new String[] { "*.xml" });
        String path = fileDialog.open();
        int resultCode = -1;
        while ((path != null) && (resultCode != SWT.YES) && new File(path).exists()) {
            MessageBox messageBox = new MessageBox(Display.getDefault().getActiveShell(), SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_QUESTION);
            messageBox.setMessage(path + " already exists, overwrite?");
            resultCode = messageBox.open();
            if (resultCode == SWT.NO) path = fileDialog.open(); else if (resultCode == SWT.CANCEL) path = null;
        }
        if (path != null) {
            try {
                FileWriter writer = new FileWriter(path);
                writer.write(xml);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Browser.progressMonitor.done();
    }
}
