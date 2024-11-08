package org.dbe.studio.core.security.signature.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.dbe.signature.xades.validation.XaDESSignatureValidator;
import org.dbe.signature.xmldsig.validation.XMLDsigSignatureValidator;
import org.dbe.studio.core.security.SecurityPlugin;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class RemoveSignatureAction implements IObjectActionDelegate {

    private ISelection currentSelection = null;

    private Document document;

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    public void run(IAction action) {
        if (currentSelection != null) {
            try {
                IStructuredSelection structuredSelection = (IStructuredSelection) currentSelection;
                IFile fileSelection = (IFile) structuredSelection.getFirstElement();
                instantiateDOMDocument(fileSelection.getContents());
                System.out.print(fileSelection.getFullPath());
                System.out.println("******" + document.getDocumentElement().getLastChild().getNodeName());
                if ("Signature".equals(document.getDocumentElement().getLastChild().getNodeName())) {
                    document.getDocumentElement().removeChild(document.getDocumentElement().getLastChild());
                    System.out.println("removed signature Element");
                }
                final String fileLocation = fileSelection.getLocation().toOSString();
                final String unsignedFileLocation = resetExtension(fileLocation, SecurityPlugin.SIGNED_FILE_EXTENTION);
                saveUNSignedFile(unsignedFileLocation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Reset the File extension by removing the x that signifies an added signature
	 * 
	 * @param filepath -
	 *            the file location (path + name)
	 * @param sigExt -
	 *           extra character added to the file Extension
	 * @return
	 */
    private String resetExtension(String filepath, String sigExt) {
        String newfilepath = filepath;
        if (filepath.endsWith(sigExt)) {
            newfilepath = newfilepath.substring(0, newfilepath.lastIndexOf(sigExt));
        }
        return newfilepath;
    }

    /**
	 * 
	 */
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            return;
        }
        currentSelection = selection;
    }

    /**
	 * 
	 * @param input
	 */
    private void instantiateDOMDocument(InputStream input) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            document = dbf.newDocumentBuilder().parse(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 * @param fileDest
	 * @return
	 */
    private File saveUNSignedFile(String fileDest) {
        File file = null;
        try {
            if (checkExistingFile(fileDest)) {
                boolean exportExistingDar = MessageDialog.openQuestion(new Shell(), "File location", "A signed file already exists :" + fileDest + " .\n Do you want to overwrite this signed file?");
                if (!exportExistingDar) {
                }
            }
            FileOutputStream flux = new FileOutputStream(fileDest);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            if (flux != null) {
                trans.transform(new DOMSource(document), new StreamResult(flux));
            }
            flux.flush();
            flux.close();
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            root.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (Exception e) {
            ErrorDialog.openError(new Shell(), "Signin Plugin Error", "", new Status(IStatus.ERROR, SecurityPlugin.ID, IStatus.OK, e.getMessage(), e));
            e.printStackTrace();
        }
        return file;
    }

    /**
	 * 
	 * @param fileLocation
	 * @return
	 */
    private boolean checkExistingFile(String fileLocation) {
        boolean existe = false;
        try {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IContainer[] containers = root.findContainersForLocation(getFilePath(fileLocation));
            for (int i = 0; i < containers.length; i++) {
                if ((containers[i].getType() == IContainer.FOLDER) || (containers[i].getType() == IContainer.PROJECT)) {
                    IResource[] resources = containers[i].members();
                    for (int j = 0; resources.length > j; j++) {
                        if (resources[j].getType() == IResource.FILE) {
                            IFile file = (IFile) resources[j];
                            if (file.getLocation().toOSString().equals(fileLocation)) {
                                existe = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            ErrorDialog.openError(new Shell(), "Signin Plugin Error", "", new Status(IStatus.ERROR, SecurityPlugin.ID, IStatus.OK, e.getMessage(), e));
            e.printStackTrace();
        }
        return existe;
    }

    private Path getFilePath(String fileLocation) {
        Path path = new Path(fileLocation.substring(0, fileLocation.lastIndexOf("\\")));
        return path;
    }
}
