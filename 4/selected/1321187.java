package org.semanticgov.repository.wsmx.actions;

import java.io.InputStream;
import javax.xml.parsers.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.omwg.mediation.language.objectmodel.api.MappingDocument;
import org.w3c.dom.*;
import org.wsmo.datastore.WsmoRepository;
import org.wsmostudio.repository.Registry;
import org.wsmostudio.repository.ui.actions.RepositoryAction;
import org.wsmostudio.repository.wsmx.WSMXRepository;
import org.wsmostudio.runtime.LogManager;

public class SendMappingAction extends RepositoryAction {

    public void run() {
        WsmoRepository repository = this._instance;
        if (false == Registry.getInstance().configureRepository(repository, false)) {
            return;
        }
        ResourceSelectionDialog dialog = new ResourceSelectionDialog(Display.getCurrent().getActiveShell());
        if (false == dialog.open()) {
            return;
        }
        IFile file = dialog.getSelectedFile();
        if (file == null) {
            return;
        }
        Shell statDialog = null;
        boolean success = false;
        try {
            statDialog = new Shell(Display.getCurrent().getActiveShell(), SWT.NONE);
            statDialog.setLayout(new GridLayout(1, false));
            new Label(statDialog, SWT.NONE).setText("Send Mapping");
            new Label(statDialog, SWT.NONE);
            Label lab = new Label(statDialog, SWT.NONE);
            lab.setText("Sending mapping file, please wait ...");
            lab.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
            statDialog.setSize(300, 100);
            Point parentLocation = Display.getCurrent().getActiveShell().getLocation();
            Point parentSize = Display.getCurrent().getActiveShell().getSize();
            statDialog.setLocation(parentLocation.x + parentSize.x / 2 - 150, parentLocation.y + parentSize.y / 2 - 75);
            statDialog.open();
            success = performSendMapping(file, (WSMXRepository) _instance);
        } catch (Throwable err) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "Error in sending mapping file:\n" + err.getMessage());
            LogManager.logWarning("ERROR " + err, err);
        }
        if (statDialog != null && false == statDialog.isDisposed()) {
            statDialog.dispose();
        }
        if (success) {
            MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Send Mapping Result", "Mapping file successfully sent to repository");
        }
    }

    private boolean performSendMapping(IFile file, WSMXRepository _instance) throws Exception {
        InputStream inStr = file.getContents();
        byte[] buffer = new byte[inStr.available()];
        inStr.read(buffer);
        inStr.close();
        String fileContent = new String(buffer, "UTF-8");
        final DocumentBuilderFactory FAC = DocumentBuilderFactory.newInstance();
        FAC.setValidating(false);
        FAC.setNamespaceAware(false);
        DocumentBuilder docBuilder = FAC.newDocumentBuilder();
        inStr = file.getContents();
        Document doc = docBuilder.parse(inStr);
        inStr.close();
        NodeList nl = doc.getElementsByTagName("dc:identifier");
        if (nl.getLength() == 0) {
            throw new Exception("Invalid mapping file - no ID detected");
        }
        String mappingID = ((Element) nl.item(0)).getAttribute("rdf:resource");
        boolean existsMapping = false;
        for (MappingDocument mDoc : _instance.retrieveMappings()) {
            if (mappingID.equals(mDoc.getId().toString())) {
                existsMapping = true;
                break;
            }
        }
        if (existsMapping == true && false == MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Mapping Overwrite?", "Mapping file with id '" + mappingID + "' already exists on the remote repository.\n" + "\nPlease confirm overwriting ?")) {
            return false;
        }
        _instance.storeMapping(fileContent);
        return true;
    }
}
