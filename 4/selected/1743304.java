package org.xaware.ide.xadev.gui.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jdom.Element;
import org.jdom.Namespace;
import org.xaware.ide.shared.DefaultUserPrefs;
import org.xaware.ide.shared.UserPrefs;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.datamodel.ObjectPair;
import org.xaware.ide.xadev.gui.XADialog;
import org.xaware.ide.xadev.gui.XADialogOperation;
import org.xaware.ide.xadev.gui.view.NavigatorView;
import org.xaware.ide.xadev.table.contentprovider.PreferencePageTableContentProvider;
import org.xaware.shared.i18n.Translator;

/**
 * Main Preference page for xa-designer.
 * 
 * @author T Vasu
 * @author Srinivas Reddy D
 * @author B Radhika
 * @version 1.0
 */
public class XADesignerPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, SelectionListener {

    /** Translator used for Localisation */
    public static final Translator translator = XA_Designer_Plugin.getTranslator();

    /** To hold Host Server */
    public Text hostTxt;

    /** To hold default source folder */
    public Text sourceTxt;

    /** Instance of host element */
    public Element host;

    /** Instance of default source element */
    public Element src;

    /** Instance to hold the composite of the page */
    private Composite parent;

    /** Instance of contentProvider */
    protected PreferencePageTableContentProvider contentProvider;

    /** Instance to hold Add Button */
    private Button addBtn;

    /** Instance to hold Edit Button */
    private Button editBtn;

    /** Instance to hold Delete Button */
    private Button deleteBtn;

    /** Instance to hold Display Composite Logical View button */
    private Button displayCompositeLogicalViewButton;

    /** Instance to hold Additional Namespaces table */
    protected Table table;

    /** Instance to hold rows of Additional Namespaces Table */
    private List objectPairs;

    /** Instance to hold resource folder state */
    private String resourceFolderState;

    /** Instance to hold list of content provider data */
    private List contentProviderList = new ArrayList();

    /** Instance to hold Group */
    private Group group;

    /**
     * Creates a new XADesignerPreferencePage object.
     */
    public XADesignerPreferencePage() {
        super(GRID);
        setPreferenceStore(XA_Designer_Plugin.getDefault().getPreferenceStore());
        setDescription("Enables configuration used by XA-Designer.");
    }

    /**
     * Creates the page's field editors.
     */
    @Override
    public void createFieldEditors() {
    }

    /**
     * Initializes this preference page for the given workbench.
     * 
     * @param workbench
     *            the workbench
     */
    public void init(final IWorkbench workbench) {
    }

    /**
     * Creates contents of the preference page.
     */
    @Override
    protected Control createContents(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        final GridLayout pageLayout = new GridLayout(1, false);
        GridData pageGridData = new GridData(GridData.FILL_BOTH);
        composite.setLayout(pageLayout);
        composite.setLayoutData(pageGridData);
        displayCompositeLogicalViewButton = new Button(composite, SWT.CHECK);
        displayCompositeLogicalViewButton.setText("Display Composite Logical View");
        displayCompositeLogicalViewButton.addSelectionListener(this);
        final Composite compo = new Composite(composite, SWT.NONE);
        final GridLayout hostLayout = new GridLayout(2, false);
        pageGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        compo.setLayoutData(pageGridData);
        compo.setLayout(hostLayout);
        final Label hostLbl = new Label(compo, SWT.NONE);
        hostLbl.setText("Host Server:");
        hostTxt = new Text(compo, SWT.BORDER);
        hostTxt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        final Label sourceLbl = new Label(compo, SWT.NONE);
        sourceLbl.setText("Source Folder:");
        sourceTxt = new Text(compo, SWT.BORDER);
        sourceTxt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        group = new Group(composite, SWT.NONE);
        group.setText("Available Namespaces");
        final GridData gData = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gData);
        group.setLayout(new GridLayout());
        populateGeneralConfigValues();
        table = contentProvider.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        final GridData tblData = new GridData(GridData.FILL_BOTH);
        tblData.heightHint = 190;
        table.setLayoutData(tblData);
        table.addSelectionListener(this);
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDoubleClick(final MouseEvent e) {
                final int selectedIndex = table.getSelectionIndex();
                if (selectedIndex != -1 && !((ObjectPair) contentProvider.getRow(selectedIndex)).getFirstObject().toString().equals("xa")) {
                    handleEditButton();
                }
            }
        });
        final Composite buttonCompoiste = new Composite(group, SWT.NONE);
        final GridLayout btnCompositeLayout = new GridLayout(3, true);
        final GridData btnCompositeData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        buttonCompoiste.setLayout(btnCompositeLayout);
        buttonCompoiste.setLayoutData(btnCompositeData);
        final GridData btnData = new GridData();
        btnData.widthHint = 50;
        sourceTxt.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                if (sourceTxt.getText() != null) {
                    validateResourceFolder(sourceTxt.getText());
                }
            }
        });
        addBtn = ControlFactory.createButton(buttonCompoiste, "Add");
        addBtn.addSelectionListener(this);
        addBtn.setLayoutData(btnData);
        editBtn = ControlFactory.createButton(buttonCompoiste, "Edit");
        editBtn.addSelectionListener(this);
        editBtn.setLayoutData(btnData);
        deleteBtn = ControlFactory.createButton(buttonCompoiste, "Delete");
        deleteBtn.addSelectionListener(this);
        deleteBtn.setLayoutData(btnData);
        return parent;
    }

    /**
     * checks whether resource folder is valid or not.
     */
    protected void validateResourceFolder(final String resourceFolder) {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IStatus resourceStatus = workspace.validateName(resourceFolder, IResource.FOLDER);
        if (!resourceStatus.isOK()) {
            if (resourceFolder.trim().equals("")) {
                setErrorMessage(null);
                setValid(true);
            } else {
                setErrorMessage(resourceStatus.getMessage());
                setValid(false);
            }
        } else {
            setErrorMessage(null);
            setValid(true);
        }
    }

    /**
     * Functionality to be invoked if a widget which has a selction Listener is selected.
     * 
     * @param e
     *            object which holds the SelectionEvent.
     */
    public void widgetSelected(final SelectionEvent e) {
        int selectedIndex = table.getSelectionIndex();
        if (selectedIndex != -1 && ((ObjectPair) contentProvider.getRow(selectedIndex)).getFirstObject().toString().equals("xa")) {
            editBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
        } else {
            editBtn.setEnabled(true);
            deleteBtn.setEnabled(true);
        }
        if (e.getSource() == addBtn) {
            selectedIndex = table.getItemCount() + 1;
            final AddNamespacesInfoDlg addDlg = new AddNamespacesInfoDlg(parent, "Add Namespace", new ObjectPair("", ""));
            final ObjectPair updatedColumn = addDlg.getUpdateRowData();
            final Object firstObj = updatedColumn.getFirstObject();
            if (firstObj != null) {
                final String updatedPrefix = firstObj.toString().trim();
                if (!(updatedPrefix.equals("xa"))) {
                    if (contentProvider.contains(updatedColumn)) {
                        final int result = ControlFactory.showConfirmDialog("Namespace already exists, Do you want to Overwrite?");
                        if (result == Window.OK) {
                            contentProvider.updateRow(contentProvider.findRow(updatedColumn), updatedColumn, false);
                        }
                    } else {
                        contentProvider.addRow(updatedColumn);
                    }
                }
            }
        }
        if (e.getSource() == editBtn) {
            handleEditButton();
        }
        if (e.getSource() == deleteBtn) {
            if (selectedIndex == -1) {
                ControlFactory.showMessageDialog("Please verify an Available Namespace is selected to perform Delete operation.", "Information");
            } else {
                contentProvider.removeRow(selectedIndex);
            }
        }
    }

    /**
     * Functionality to be invoked if a edit button selection Listener is invoked.
     */
    protected void handleEditButton() {
        final int selectedIndex = table.getSelectionIndex();
        if (selectedIndex == -1) {
            ControlFactory.showMessageDialog("Please verify an Available Namespace is selected to perform Edit operation.", "Information");
        } else {
            final AddNamespacesInfoDlg editDlg = new AddNamespacesInfoDlg(parent, "Edit Namespace", (ObjectPair) contentProvider.getRow(selectedIndex));
            final ObjectPair updatedColumn = editDlg.getUpdateRowData();
            final Object firstObj = updatedColumn.getFirstObject();
            if (firstObj != null) {
                final String updatedPrefix = firstObj.toString().trim();
                if (contentProvider.contains(updatedColumn)) {
                    final int oldIndex = contentProvider.findRow(updatedColumn);
                    if (oldIndex != selectedIndex) {
                        final int result = ControlFactory.showConfirmDialog("Namespace already exists, Do you want to Overwrite?");
                        if (result == Window.OK) {
                            contentProvider.updateRow(selectedIndex, updatedColumn, false);
                            contentProvider.removeRow(oldIndex);
                        }
                    } else {
                        contentProvider.updateSelectedRow(updatedColumn, false);
                    }
                } else if (!(updatedPrefix.equals("xa"))) {
                    contentProvider.updateRow(selectedIndex, updatedColumn, false);
                }
            }
        }
    }

    /**
     * invoked when the widget is selected by default. no functionality is needed.
     * 
     * @param e
     *            selection event.
     */
    public void widgetDefaultSelected(final SelectionEvent e) {
    }

    /**
     * Populates General Configuration values into preference page
     */
    private void populateGeneralConfigValues() {
        displayCompositeLogicalViewButton.setSelection(UserPrefs.isLogicalViewShown());
        hostTxt.setText(UserPrefs.getDefaultServerHost());
        if (UserPrefs.getResourceFolder().getContent() != null) {
            sourceTxt.setText(UserPrefs.getResourceFolder().getText());
        }
        final List columns = new ArrayList();
        columns.add("Prefix");
        columns.add("URI");
        objectPairs = new ArrayList();
        final Vector prefixVector = UserPrefs.getAvailablePrefixes();
        final Vector uriVector = UserPrefs.getAvailableURIs();
        for (int j = 0; j < prefixVector.size(); j++) {
            final Object obj1 = prefixVector.get(j);
            final Object obj2 = uriVector.get(j);
            final ObjectPair objPair = new ObjectPair(obj1, obj2);
            objectPairs.add(objPair);
        }
        final int[] columnWidths = { 60, 100 };
        contentProvider = new PreferencePageTableContentProvider(group, false, objectPairs, columns, columnWidths);
    }

    /**
     * performDefaults method sets the Default values in to the wizard from the UserconfigDefault.xml file.
     */
    @Override
    protected void performDefaults() {
        try {
            DefaultUserPrefs.getInstance();
            if (DefaultUserPrefs.getDisplayCompositeLogicalView().equalsIgnoreCase("yes")) {
                displayCompositeLogicalViewButton.setSelection(true);
            } else if (DefaultUserPrefs.getDisplayCompositeLogicalView().equalsIgnoreCase("no")) {
                displayCompositeLogicalViewButton.setSelection(false);
            }
            hostTxt.setText(DefaultUserPrefs.getDefaultServerHost());
            sourceTxt.setText(DefaultUserPrefs.getResourceFolder().getText());
            final List columns = new ArrayList();
            columns.add("Prefix");
            columns.add("URI");
            objectPairs = new ArrayList();
            final Vector prefixVector = DefaultUserPrefs.getAvailablePrefixes();
            final Vector uriVector = DefaultUserPrefs.getAvailableURIs();
            for (int j = 0; j < prefixVector.size(); j++) {
                final Object obj1 = prefixVector.get(j);
                final Object obj2 = uriVector.get(j);
                final ObjectPair objPair = new ObjectPair(obj1, obj2);
                objectPairs.add(objPair);
            }
            contentProvider.removeAllRows();
            contentProvider.addRows(objectPairs);
        } catch (final Exception exception) {
            ControlFactory.showInfoDialog("Error loading from UserConfigDefault.xml.", exception.toString());
        }
    }

    /**
     * checks whether Display Composite Logical View State is changed or not.
     * 
     * @returns true if changed else false
     */
    protected boolean isDisplayCompositeLogicalViewModified() {
        return displayCompositeLogicalViewButton.getSelection() != UserPrefs.isLogicalViewShown();
    }

    /**
     * checks whether Host Server Text is changed or not. returns boolean
     * 
     * @returns true if changed else false
     */
    protected boolean isHostServerModified() {
        if (hostTxt.getText().equalsIgnoreCase(UserPrefs.getDefaultServerHost())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * checks whether Resource Folder Text is changed or not. returns boolean
     * 
     * @returns true if changed else false
     */
    protected boolean isResourceFolderModified() {
        resourceFolderState = sourceTxt.getText();
        if (UserPrefs.getResourceFolder().getContent() != null) {
            if (resourceFolderState.equalsIgnoreCase(UserPrefs.getResourceFolder().getText())) {
                return false;
            } else {
                return true;
            }
        } else {
            if (resourceFolderState.equals("")) {
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * checks whether Available Namespaces are changed or not. returns boolean
     * 
     * @returns true if changed else false
     */
    protected boolean isAvailableNamespacesModified() {
        contentProviderList = contentProvider.getRows();
        final Vector userConfigVector = UserPrefs.getAvailableNamespaces();
        if (contentProviderList.size() != userConfigVector.size()) {
            return true;
        } else {
            final Vector userConfigPrefixes = UserPrefs.getAvailablePrefixes();
            final Vector userConfigUris = UserPrefs.getAvailableURIs();
            final Iterator contentProviderListIter = contentProviderList.iterator();
            int i = 0;
            while (contentProviderListIter.hasNext()) {
                final ObjectPair rowObject = (ObjectPair) contentProviderListIter.next();
                if (!(rowObject.getFirstObject().equals(userConfigPrefixes.get(i)) && rowObject.getSecondObject().equals(userConfigUris.get(i)))) {
                    return true;
                }
                i++;
            }
            return false;
        }
    }

    /**
     * performApply method sets the updated values in to the Userconfig.xml file.
     * 
     * @returns true if changed else false
     */
    @Override
    protected void performApply() {
        boolean updated = false;
        isDisplayCompositeLogicalViewModified();
        if (isDisplayCompositeLogicalViewModified()) {
            UserPrefs.setLogicalViewShown(displayCompositeLogicalViewButton.getSelection());
            final NavigatorView navigatorView = XA_Designer_Plugin.getNavigatorView();
            if (navigatorView != null) {
                navigatorView.applyContentProvider();
            }
            updated = true;
        }
        if (isHostServerModified()) {
            UserPrefs.setDefaultServerHost(hostTxt.getText());
            updated = true;
        }
        if (isResourceFolderModified()) {
            UserPrefs.setResourceFolder(sourceTxt.getText());
            updated = true;
        }
        if (isAvailableNamespacesModified()) {
            final List rowData = contentProvider.getRows();
            UserPrefs.setAvailableNamespaces(rowData);
            updated = true;
        }
        if (updated) {
            try {
                UserPrefs.updateUserConfig();
            } catch (final IOException exception) {
                ControlFactory.showInfoDialog("Error updating UserConfig.xml. ", exception.toString());
            }
        }
    }

    /**
     * performOk method stores the values from the preference page to the Userconfig.xml file.
     * 
     * @returns true if changed else false
     */
    @Override
    public boolean performOk() {
        performApply();
        return true;
    }

    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);
        ControlFactory.tableResize(table, false);
    }
}

/**
 * Creates a new AddNamespacesInfoDlg object.
 */
class AddNamespacesInfoDlg implements XADialogOperation {

    /** holds the instance of dialog composite */
    private Composite composite;

    /** holds URI label */
    private Label uriLbl;

    /** holds Prefix Label */
    private Label prefixLbl;

    /** holds URI Text value */
    protected Text uriTxt;

    /** holds Prefix text value */
    protected Text prefixTxt;

    /** holds instance of dialog shell */
    private Shell shell;

    /** holds value of selected prefix */
    private String prefix;

    /** holds value of selected uri */
    private String uri;

    /** holds Updated row data */
    private final ObjectPair updatedRowData = new ObjectPair();

    /**
     * Creates a new AddNamespacesInfoDlg object.
     * 
     * @param parent
     *            Composite instance.
     * @param title
     *            String Value.
     * @param selectedRowData
     *            ObjectPair instance.
     */
    public AddNamespacesInfoDlg(final Composite parent, final String operation, final ObjectPair selectedRowData) {
        prefix = selectedRowData.getFirstObject().toString();
        uri = selectedRowData.getSecondObject().toString();
        createDialog(operation);
    }

    /**
     * Creates a new XADialog object.
     * 
     * @param columnData
     *            String value.
     */
    private void createDialog(final String operation) {
        shell = XA_Designer_Plugin.getShell();
        composite = new Composite(shell, SWT.NONE);
        final GridLayout layout = new GridLayout(2, false);
        final GridData gData = new GridData(GridData.FILL_BOTH);
        composite.setLayout(layout);
        composite.setLayoutData(gData);
        createContents();
        final XADialog dialog = new XADialog(shell, this, composite, operation, true, true, new Point(400, 150));
        dialog.open();
    }

    /**
     * Creates contents of XADialog object.
     */
    private void createContents() {
        final GridData textData = new GridData(GridData.FILL_HORIZONTAL);
        textData.widthHint = 60;
        prefixLbl = new Label(composite, SWT.NONE);
        prefixLbl.setText("Prefix:");
        prefixTxt = ControlFactory.createText(composite, SWT.BORDER);
        prefixTxt.setLayoutData(textData);
        uriLbl = new Label(composite, SWT.NONE);
        uriLbl.setText("URI:");
        uriTxt = ControlFactory.createText(composite, SWT.BORDER);
        uriTxt.setLayoutData(textData);
        prefixTxt.setText(prefix);
        uriTxt.setText(uri);
    }

    /**
     * Updates updatedRowData Object.
     * 
     * @returns updated row data.
     */
    public ObjectPair getUpdateRowData() {
        return updatedRowData;
    }

    /**
     * Invoked when user clicks the cancel button.
     * 
     * @return true indicating cancel was pressed.
     */
    public boolean cancelPressed() {
        return true;
    }

    /**
     * Invoked when the user clicks the OK button.
     * 
     * @return retVal return value indiacating the dialog can be closed or not.
     */
    public boolean okPressed() {
        final String prefixStr = prefixTxt.getText().trim();
        final String uriStr = uriTxt.getText().trim();
        if (prefixStr.equals("")) {
            ControlFactory.showMessageDialog("Prefix should not be empty.", "Information");
            Display.getCurrent().asyncExec(new Runnable() {

                public void run() {
                    if (prefixTxt != null) {
                        prefixTxt.forceFocus();
                    }
                }
            });
            return false;
        } else if (uriStr.equals("")) {
            ControlFactory.showMessageDialog("URI should not be empty.", "Information");
            Display.getCurrent().asyncExec(new Runnable() {

                public void run() {
                    if (uriTxt != null) {
                        uriTxt.forceFocus();
                    }
                }
            });
            return false;
        } else if (prefixStr.equals("xa")) {
            ControlFactory.showMessageDialog("Cannot add xa Namespace.", "Information");
            Display.getCurrent().asyncExec(new Runnable() {

                public void run() {
                    if (prefixTxt != null) {
                        prefixTxt.forceFocus();
                    }
                }
            });
            return false;
        } else {
            try {
                Namespace.getNamespace(prefixStr, uriStr);
            } catch (final Exception ex) {
                ControlFactory.showInfoDialog("Prefix or Namespace URI is invalid.", ex.getMessage());
                Display.getCurrent().asyncExec(new Runnable() {

                    public void run() {
                        if (prefixTxt != null) {
                            prefixTxt.forceFocus();
                        }
                    }
                });
                return false;
            }
        }
        updatedRowData.setFirstObject(prefixStr);
        updatedRowData.setSecondObject(uriStr);
        return true;
    }
}
