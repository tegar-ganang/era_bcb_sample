package org.dbe.studio.core.security.signature.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.dbe.identity.utilities.DBEKeystore;
import org.dbe.studio.core.security.SecurityPlugin;
import org.dbe.studio.core.security.signature.dialogs.InputDialog;
import org.dbe.signature.xmldsig.XMLDsigSignatureHelper;
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.w3c.dom.Document;

/**
 * 
 * @author zboudjemil
 * 
 */
public class GenerateSignedFileAction implements IObjectActionDelegate {

    private ISelection currentSelection = null;

    private Document doc;

    char[] masterPwd;

    /**
	 * Constructor for GenerateSignedFileAction.
	 */
    public GenerateSignedFileAction() {
        super();
    }

    /**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    /**
	 * @see IActionDelegate#run(IAction)
	 */
    public void run(IAction action) {
        if (currentSelection != null) {
            try {
                IStructuredSelection structuredSelection = (IStructuredSelection) currentSelection;
                IFile fileSelection = (IFile) structuredSelection.getFirstElement();
                final String fileLocation = fileSelection.getLocation().toOSString();
                final String signedFileLocation = setExtension(fileLocation, SecurityPlugin.SIGNED_FILE_EXTENTION);
                final Shell shell = new Shell();
                shell.setText("Identification Input");
                shell.setBounds(100, 200, 300, 150);
                InputDialog dialog = new InputDialog(shell);
                if (dialog.open() == IDialogConstants.OK_ID) {
                    System.out.println("generating signature");
                    System.out.println("Master password iS:" + dialog.getMasterPassword());
                    masterPwd = dialog.getMasterPassword().toCharArray();
                    generateSignedDocument(fileLocation, dialog.getUserName(), dialog.getPassword());
                    dialog.close();
                    System.out.println("Saving file to " + signedFileLocation);
                    saveSignedFile(signedFileLocation);
                }
            } catch (Exception e) {
                ErrorDialog.openError(new Shell(), "Signin Plugin Error", "", new Status(IStatus.ERROR, SecurityPlugin.ID, IStatus.OK, e.getMessage(), e));
                e.printStackTrace();
            }
        }
    }

    /**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            return;
        }
        currentSelection = selection;
    }

    /**
	 * Build a new file path with the proposed extention
	 * 
	 * @param filepath -
	 *            the file location (path + name)
	 * @param newExt -
	 *            new extention
	 * @return
	 */
    private String setExtension(String filepath, String newExt) {
        String newfilepath = filepath;
        newfilepath = newfilepath + newExt;
        return newfilepath;
    }

    private Path getFilePath(String fileLocation) {
        Path path = new Path(fileLocation.substring(0, fileLocation.lastIndexOf("\\")));
        return path;
    }

    /**
	 * 
	 * @param fileLocation
	 * @param alias
	 * @param aliasPwd
	 */
    private void generateSignedDocument(String fileLocation, String alias, String aliasPwd) {
        try {
            System.out.println("Creating sighelper");
            DBEKeystore myKeystore = SecurityPlugin.getDefault().loadKeystore(masterPwd);
            if (myKeystore == null) {
                System.out.println("no keystore for master password ");
                throw new NullPointerException("no keystore for master password");
            }
            XMLDsigSignatureHelper sigHelper = new XMLDsigSignatureHelper(myKeystore);
            System.out.println("Created sighelper");
            doc = sigHelper.createExtendedXMLDsigSignature(transformToByteStream(fileLocation), alias, aliasPwd);
            System.out.println("DOM doc created");
        } catch (Exception e) {
            ErrorDialog.openError(new Shell(), "Signin Plugin Error", "", new Status(IStatus.ERROR, SecurityPlugin.ID, IStatus.OK, e.getMessage(), e));
            e.printStackTrace();
        }
    }

    /**
	 * 
	 * @param fileDest
	 * @return
	 */
    private File saveSignedFile(String fileDest) {
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
                trans.transform(new DOMSource(doc), new StreamResult(flux));
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

    /**
	 * 
	 * @param contractPath
	 * @return
	 * @throws ContractNotaryException
	 */
    private byte[] transformToByteStream(String contractPath) {
        byte[] data = null;
        try {
            FileInputStream fis = new FileInputStream(contractPath);
            FileChannel fc = fis.getChannel();
            data = new byte[(int) fc.size()];
            ByteBuffer bb = ByteBuffer.wrap(data);
            fc.read(bb);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
}
