package iwallet.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import iwallet.client.gui.ConnectionDetails;
import iwallet.client.requester.ServerCommunicationException;
import iwallet.client.requester.ServiceRequester;
import iwallet.client.transport.NetworkTransportClient;
import iwallet.client.transport.TransportException;
import iwallet.common.account.AccountUser;
import iwallet.common.account.IwalletAccount;
import iwallet.common.account.RegisterInfo;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Login dialog, which prompts for the user's account info, and has Login and
 * Cancel buttons.
 * @author ����
 */
public class LoginDialog extends Dialog {

    private Combo userIdText;

    private Text serverText;

    private Text passwordText;

    private ConnectionDetails connectionDetails;

    private HashMap savedDetails = new HashMap();

    private Image[] images;

    private static final String PASSWORD = "password";

    private static final String SERVER = "server";

    private static final String SAVED = "saved-connections";

    private static final String LAST_USER = "prefs_last_connection";

    public LoginDialog(Shell parentShell) {
        super(parentShell);
        loadDescriptors();
    }

    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("iWallet - ��¼");
        IProduct product = Platform.getProduct();
        if (product != null) {
            String[] imageURLs = parseCSL(product.getProperty(IProductConstants.WINDOW_IMAGES));
            if (imageURLs.length > 0) {
                images = new Image[imageURLs.length];
                for (int i = 0; i < imageURLs.length; i++) {
                    String url = imageURLs[i];
                    ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(product.getDefiningBundle().getSymbolicName(), url);
                    images[i] = descriptor.createImage(true);
                }
                newShell.setImages(images);
            }
        }
    }

    public static String[] parseCSL(String csl) {
        if (csl == null) return null;
        StringTokenizer tokens = new StringTokenizer(csl, ",");
        ArrayList array = new ArrayList(10);
        while (tokens.hasMoreTokens()) array.add(tokens.nextToken().trim());
        return (String[]) array.toArray(new String[array.size()]);
    }

    public boolean close() {
        if (images != null) {
            for (int i = 0; i < images.length; i++) images[i].dispose();
        }
        return super.close();
    }

    protected Control createDialogArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        composite.setLayout(layout);
        Label accountLabel = new Label(composite, SWT.NONE);
        accountLabel.setText("�û���Ϣ��");
        accountLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
        Label userIdLabel = new Label(composite, SWT.NONE);
        userIdLabel.setText("�û��� (&U)��");
        userIdLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        userIdText = new Combo(composite, SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, false);
        gridData.widthHint = convertHeightInCharsToPixels(20);
        userIdText.setLayoutData(gridData);
        Label serverLabel = new Label(composite, SWT.NONE);
        serverLabel.setText("������ (&S)��");
        serverLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        serverText = new Text(composite, SWT.BORDER);
        serverText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        Label passwordLabel = new Label(composite, SWT.NONE);
        passwordLabel.setText("���� (&P)��");
        passwordLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        passwordText = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        passwordText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        final Button autoLogin = new Button(composite, SWT.CHECK);
        autoLogin.setText("�������� (&A)");
        autoLogin.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, true, 2, 1));
        autoLogin.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                IEclipsePreferences prefs = new ConfigurationScope().getNode(Application.PLUGIN_ID);
                prefs.putBoolean(GeneralPreferencePage.AUTO_LOGIN, autoLogin.getSelection());
            }
        });
        IPreferencesService service = Platform.getPreferencesService();
        boolean auto_login = service.getBoolean(Application.PLUGIN_ID, GeneralPreferencePage.AUTO_LOGIN, true, null);
        autoLogin.setSelection(auto_login);
        userIdText.addListener(SWT.Modify, new Listener() {

            public void handleEvent(Event event) {
                ConnectionDetails d = (ConnectionDetails) savedDetails.get(userIdText.getText());
                if (d != null) {
                    serverText.setText(d.getServer());
                    if (autoLogin.getSelection()) {
                        passwordText.setText(d.getPassword());
                    } else {
                        passwordText.setText("");
                    }
                }
            }
        });
        String lastUser = "none";
        if (connectionDetails != null) lastUser = connectionDetails.getUserId();
        initializeUsers(lastUser);
        return composite;
    }

    protected void createButtonsForButtonBar(Composite parent) {
        Button createNewUser = createButton(parent, IDialogConstants.CLIENT_ID + 1, "ע�����û� (&R)", false);
        createNewUser.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                AccountUser user = new IwalletAccount(userIdText.getText(), passwordText.getText(), new RegisterInfo());
                if (GuiParameter.requester == null) {
                    GuiParameter.client = new NetworkTransportClient(serverText.getText());
                    try {
                        GuiParameter.client.connect();
                        GuiParameter.requester = new ServiceRequester(GuiParameter.client);
                    } catch (TransportException e2) {
                        MessageDialog.openError(getShell(), "iWallet - ע�����û�", "ע��ʧ�ܣ��޷����ӵ���������");
                        return;
                    }
                }
                boolean ret;
                try {
                    ret = GuiParameter.requester.addAccountObject(user);
                } catch (ServerCommunicationException e1) {
                    MessageDialog.openError(getShell(), "iWallet - ע�����û�", "ע��ʧ�ܣ��������ͨѶ�������");
                    return;
                }
                if (ret) {
                    MessageDialog.openInformation(getShell(), "iWallet - ע�����û�", "�û� " + user.getUserName() + " ע��ɹ���");
                } else {
                    MessageDialog.openInformation(getShell(), "iWallet - ע�����û�", "ע��ʧ�ܣ��û� " + userIdText.getText() + " �Ѵ��ڡ��볢��ʹ�������û���");
                }
            }
        });
        Button removeCurrentUser = createButton(parent, IDialogConstants.CLIENT_ID, "ɾ���û� (&D)", false);
        removeCurrentUser.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                AccountUser user = new IwalletAccount(userIdText.getText());
                if (GuiParameter.requester == null) {
                    GuiParameter.client = new NetworkTransportClient(serverText.getText());
                    try {
                        GuiParameter.client.connect();
                        GuiParameter.requester = new ServiceRequester(GuiParameter.client);
                    } catch (TransportException e2) {
                        MessageDialog.openError(null, "iWallet - �û���¼", "��¼ʧ�ܣ��������ͨѶ�������");
                    }
                }
                if (MessageDialog.openQuestion(getShell(), "iWallet - ɾ���û�", "���Ƿ����Ҫɾ���û� " + user.getUserName() + " ��")) {
                    try {
                        GuiParameter.requester.delAccountObject(user);
                    } catch (ServerCommunicationException sce) {
                        MessageDialog.openError(null, "iWallet - ɾ���û�", "ɾ��ʧ�ܣ��������ͨѶ�������");
                    }
                    MessageDialog.openInformation(getShell(), "iWallet - User deletion", "User " + user.getUserName() + " was deleted successfully.");
                    savedDetails.remove(userIdText.getText());
                    initializeUsers("");
                    saveDescriptors();
                }
            }
        });
        createButton(parent, IDialogConstants.OK_ID, "��¼ (&L)", true);
        createButton(parent, IDialogConstants.CANCEL_ID, "ȡ�� (&C)", false);
    }

    protected void initializeUsers(String defaultUser) {
        userIdText.removeAll();
        passwordText.setText("");
        serverText.setText("");
        for (Iterator it = savedDetails.keySet().iterator(); it.hasNext(); ) userIdText.add((String) it.next());
        int index = Math.max(userIdText.indexOf(defaultUser), 0);
        userIdText.select(index);
    }

    protected void okPressed() {
        if (userIdText.getText().equals("")) {
            MessageDialog.openError(getShell(), "iWallet - ��¼", "�����û�����Ϊ�ա�");
            return;
        }
        if (serverText.getText().equals("")) {
            MessageDialog.openError(getShell(), "iWallet - ��¼", "���󣺷���������Ϊ�ա�");
            return;
        }
        if (GuiParameter.requester == null) {
            GuiParameter.client = new NetworkTransportClient(serverText.getText());
            try {
                GuiParameter.client.connect();
                GuiParameter.requester = new ServiceRequester(GuiParameter.client);
            } catch (TransportException e2) {
                MessageDialog.openError(getShell(), "iWallet - ��¼", "��¼ʧ�ܣ��޷����ӵ���������");
                return;
            }
        }
        try {
            GuiParameter.logined = GuiParameter.requester.login(userIdText.getText(), passwordText.getText());
        } catch (ServerCommunicationException e) {
            MessageDialog.openError(getShell(), "iWallet - ��¼", "��¼ʧ�ܣ��������ͨѶ�������");
            return;
        }
        if (GuiParameter.logined) {
            connectionDetails = new ConnectionDetails(userIdText.getText(), serverText.getText(), passwordText.getText());
            savedDetails.put(userIdText.getText(), connectionDetails);
            saveDescriptors();
            super.okPressed();
        } else {
            MessageDialog.openError(getShell(), "iWallet - ��¼", "��¼ʧ�ܣ��û������ڻ��������");
        }
    }

    protected void buttonPressed(int buttonId) {
        super.buttonPressed(buttonId);
    }

    public void saveDescriptors() {
        Preferences preferences = new ConfigurationScope().getNode(Application.PLUGIN_ID);
        preferences.put(LAST_USER, connectionDetails.getUserId());
        Preferences connections = preferences.node(SAVED);
        for (Iterator it = savedDetails.keySet().iterator(); it.hasNext(); ) {
            String name = (String) it.next();
            ConnectionDetails d = (ConnectionDetails) savedDetails.get(name);
            Preferences connection = connections.node(name);
            connection.put(SERVER, d.getServer());
            connection.put(PASSWORD, d.getPassword());
        }
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            MessageDialog.openError(null, "iWallet - ������ѡ��", "����ʧ�ܣ���ѡ���ʧ�ܡ�");
        }
    }

    private void loadDescriptors() {
        try {
            Preferences preferences = new ConfigurationScope().getNode(Application.PLUGIN_ID);
            Preferences connections = preferences.node(SAVED);
            String[] userNames = connections.childrenNames();
            for (int i = 0; i < userNames.length; i++) {
                String userName = userNames[i];
                Preferences node = connections.node(userName);
                savedDetails.put(userName, new ConnectionDetails(userName, node.get(SERVER, ""), node.get(PASSWORD, "")));
            }
            connectionDetails = (ConnectionDetails) savedDetails.get(preferences.get(LAST_USER, ""));
        } catch (BackingStoreException e) {
            MessageDialog.openError(null, "iWallet - ������ѡ��", "����ʧ�ܣ���ѡ���������");
        }
    }

    /**
	 * Returns the connection details entered by the user, or <code>null</code>
	 * if the dialog was canceled.
	 */
    public ConnectionDetails getConnectionDetails() {
        return connectionDetails;
    }
}
