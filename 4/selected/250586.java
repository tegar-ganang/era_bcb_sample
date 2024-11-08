package justsftp.wizards;

import justsftp.Connection;
import justsftp.ConnectionStore;
import justsftp.SFtpFileHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import com.jcraft.jsch.JSchException;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "mpe". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */
public class JustFtpConnectionWizard extends Wizard implements INewWizard {

    private JustFtpConnectionWizardPage page;

    private boolean edit;

    /**
	 * Constructor for JustFtpConnectionWizard.
	 */
    public JustFtpConnectionWizard() {
        super();
        setNeedsProgressMonitor(true);
        this.edit = false;
    }

    /**
	 * Constructor for JustFtpConnectionWizard.
	 */
    public JustFtpConnectionWizard(boolean edit) {
        this();
        this.edit = edit;
    }

    /**
	 * Adding the page to the wizard.
	 */
    public void addPages() {
        page = new JustFtpConnectionWizardPage(edit);
        addPage(page);
    }

    /**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
    public boolean performFinish() {
        try {
            doFinish(page.getConnectionName().getText(), page.getProtocol().getSelection(), page.getPort().getText(), page.getUsername().getText(), page.getPassword().getText(), page.getHost().getText(), page.getDefaultDir().getText());
        } catch (CoreException e) {
            e.printStackTrace();
            MessageDialog.openError(getShell(), "Error", e.getMessage());
            return false;
        }
        return true;
    }

    /**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */
    private void doFinish(String connName, String[] protocols, String port, String userName, String pwd, String host, String defaultDir) throws CoreException {
        Connection connection = validateConnectionInfo(connName, protocols, port, userName, pwd, host, defaultDir);
        ConnectionStore.getConnectionStore().addConnection(connName, connection);
        SFtpFileHandler handle = new SFtpFileHandler();
        try {
            handle.getChannel(connection);
        } catch (JSchException e1) {
            if (!MessageDialog.openConfirm(getShell(), "Confirm !!!!!", "Unable to Connet to the host,\nDo you want to still Save?")) {
                throwCoreException("Did not Save the Connection");
            }
        }
        try {
            ConnectionStore.getConnectionStore().save();
        } catch (Exception e) {
            throwCoreException("Unable to save connection Data");
        }
    }

    public Connection validateConnectionInfo(String connectionName, String[] protocols, String port, String userName, String pwd, String host, String defaultDir) throws CoreException {
        Connection connection = null;
        validateIfEmpty(connectionName, "Connection Name");
        if (protocols.length != 1) {
            throwCoreException("Protocol is Required");
        }
        validateIfEmpty(protocols[0], "Protocol");
        validateIfEmpty(port, "Port");
        validateIfEmpty(userName, "Username");
        validateIfEmpty(pwd, "Password");
        validateIfEmpty(host, "Host");
        connection = new Connection();
        connection.setHost(host);
        connection.setPort(Integer.parseInt(port));
        connection.setProtocol(protocols[0]);
        connection.setPwd(pwd.getBytes());
        connection.setUserName(userName);
        connection.setDefaultDir(defaultDir.length() == 0 ? "." : defaultDir);
        return connection;
    }

    public void validateIfEmpty(String text, String label) throws CoreException {
        if (text.trim().length() == 0) {
            throwCoreException(label + " is Required");
        }
    }

    private void throwCoreException(String message) throws CoreException {
        IStatus status = new Status(IStatus.ERROR, "JustSFTP", IStatus.OK, message, null);
        throw new CoreException(status);
    }

    /**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }
}
