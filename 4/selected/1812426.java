package org.xaware.ide.xadev.gui.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
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
import org.jdom.Comment;
import org.jdom.Element;
import org.xaware.ide.shared.DefaultUserPrefs;
import org.xaware.ide.shared.UserPrefs;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.datamodel.ObjectPair;
import org.xaware.ide.xadev.gui.XADialog;
import org.xaware.ide.xadev.gui.XADialogOperation;
import org.xaware.ide.xadev.table.contentprovider.PreferencePageTableContentProvider;
import org.xaware.shared.i18n.Translator;

/**
 * Wizard Configuration Preference page for XA-Designer. which allow modification of BizDriver Protocol Types and Table
 * Types in UserConfig.xml
 * 
 * @author Aruna S
 * @author RamaKrishna B
 * @version 1.0
 */
public class WizardConfigurationPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, SelectionListener {

    /** Translator used for Localisation */
    public static final Translator translator = XA_Designer_Plugin.getTranslator();

    /** Instance to hold PreferencePageTableContentProvider */
    private static PreferencePageTableContentProvider contentProvider;

    /** Instance to hold Add Button */
    private Button addBtn;

    /** Instance to hold Edit Button */
    private Button editBtn;

    /** Instance to hold Delete Button */
    private Button deleteBtn;

    /** Instance to hold Composite */
    private Composite parent;

    /** Instance to hold List */
    private List objectPairs;

    /** Instance to hold Hashtable */
    private LinkedHashMap protoClassPair;

    /** Instance to hold Table */
    private Table table;

    /** Instance to hold SelectAll Button */
    private Button selectAllBtn;

    /** Instance to hold DeSelectAll Button */
    private Button deSelectAllBtn;

    /** Instance to hold Composite */
    private Composite checkComposite;

    /** Instance to hold List Object */
    private List tableTypesContent;

    /** Instance to hold GridaData */
    private GridData tableTypeGridData;

    /** Instance to hold Group */
    private Group tableTypeGroup;

    /** Tells whether the Cancel Button is pressed or not */
    protected boolean canceled;

    /** Instance to hold List */
    private List rowsList;

    /** Instance to hold ScrolledComposite */
    private ScrolledComposite scrolledComp;

    /** Instance to hold Group */
    private Group bizDriverGroup;

    /**
     * Creates a new WizardConfigurationPreferencePage object.
     */
    public WizardConfigurationPreferencePage() {
        super(GRID);
        setPreferenceStore(XA_Designer_Plugin.getDefault().getPreferenceStore());
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
     * Notifies that the OK button of this page's container has been pressed. All Changes done in the
     * WizardPerferencepage will be updated to UserConfig.xml file.
     * 
     * @return true will close the WizardPerferencepage.
     */
    @Override
    public boolean performOk() {
        performApply();
        return true;
    }

    /**
     * This method returns the root composite to the parent shell.
     * 
     * @param parent
     *            parent composite.
     * @return returns the root composite to the parent shell.
     */
    @Override
    protected Control createContents(final Composite parent) {
        final Composite parentComposite = new Composite(parent, SWT.NONE);
        final GridLayout layout1 = new GridLayout(1, false);
        final GridData gData1 = new GridData(GridData.FILL_BOTH);
        parentComposite.setLayout(layout1);
        parentComposite.setLayoutData(gData1);
        bizDriverGroup = new Group(parentComposite, SWT.NONE);
        bizDriverGroup.setText("BizDriver Protocol Types");
        final GridData bizDriverGridData = new GridData(GridData.FILL_BOTH);
        bizDriverGridData.heightHint = 220;
        bizDriverGroup.setLayoutData(bizDriverGridData);
        bizDriverGroup.setLayout(new GridLayout());
        populateSQLDrivers();
        table = contentProvider.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        final GridData tblData = new GridData(GridData.FILL_BOTH);
        table.setLayoutData(tblData);
        table.addSelectionListener(this);
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDoubleClick(final MouseEvent e) {
                handleEditButton();
            }
        });
        final Composite buttonCompoiste = new Composite(bizDriverGroup, SWT.NONE);
        final GridLayout btnCompositeLayout = new GridLayout(3, true);
        final GridData btnCompositeData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        buttonCompoiste.setLayout(btnCompositeLayout);
        buttonCompoiste.setLayoutData(btnCompositeData);
        final GridData btnData = new GridData();
        btnData.widthHint = 50;
        addBtn = ControlFactory.createButton(buttonCompoiste, "Add");
        addBtn.addSelectionListener(this);
        addBtn.setLayoutData(btnData);
        editBtn = ControlFactory.createButton(buttonCompoiste, "Edit");
        editBtn.addSelectionListener(this);
        editBtn.setLayoutData(btnData);
        deleteBtn = ControlFactory.createButton(buttonCompoiste, "Delete");
        deleteBtn.addSelectionListener(this);
        deleteBtn.setLayoutData(btnData);
        tableTypeGroup = new Group(parentComposite, SWT.NONE);
        tableTypeGroup.setText("Table Types in SQL Wizard");
        final GridLayout tableTypeGridLayout = new GridLayout();
        tableTypeGridLayout.numColumns = 1;
        tableTypeGroup.setLayout(tableTypeGridLayout);
        scrolledComp = new ScrolledComposite(tableTypeGroup, SWT.V_SCROLL | SWT.H_SCROLL);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        scrolledComp.setLayout(gridLayout);
        final GridData gridData = new GridData(GridData.FILL_BOTH);
        scrolledComp.setLayoutData(gridData);
        checkComposite = new Composite(scrolledComp, SWT.NONE);
        final GridLayout checkLayout = new GridLayout();
        checkComposite.setLayout(checkLayout);
        tableTypeGridData = new GridData(GridData.FILL_BOTH);
        final GridData mainGridData = new GridData(GridData.FILL_BOTH);
        mainGridData.heightHint = 200;
        checkComposite.setLayoutData(mainGridData);
        populateTableTypes();
        tableTypeGridData = new GridData(GridData.FILL_BOTH);
        tableTypeGridData.verticalIndent = 10;
        tableTypeGridData.widthHint = 550;
        tableTypeGridData.heightHint = 180;
        tableTypeGroup.setLayoutData(tableTypeGridData);
        final Composite buttonComposite = new Composite(tableTypeGroup, SWT.NONE);
        final GridLayout buttLayout = new GridLayout();
        buttLayout.numColumns = 2;
        buttonComposite.setLayout(buttLayout);
        selectAllBtn = ControlFactory.createButton(buttonComposite, "Select All");
        selectAllBtn.addSelectionListener(this);
        deSelectAllBtn = ControlFactory.createButton(buttonComposite, "Deselect All");
        deSelectAllBtn.addSelectionListener(this);
        tableTypeGridData = new GridData();
        tableTypeGridData.horizontalIndent = 20;
        buttonComposite.setLayoutData(tableTypeGridData);
        scrolledComp.setContent(checkComposite);
        scrolledComp.setExpandHorizontal(true);
        scrolledComp.setExpandVertical(true);
        return parentComposite;
    }

    /**
     * The method widgetSelected() holds action for Add,EDit,Delete,Select All and Deselect All buttons.
     * 
     * @param e
     *            holds SelectionEvent instance.
     */
    public void widgetSelected(final SelectionEvent e) {
        if (e.getSource() == addBtn) {
            final JDBCDriverInfoDlg jdbcDlg = new JDBCDriverInfoDlg(parent, "Add BizDriver Protocol Type", null);
            if (jdbcDlg.showDialog()) {
                if (contentProvider.contains(jdbcDlg.getObject())) {
                    final int result = ControlFactory.showConfirmDialog("BizDriver Protocol Type already exists, Do you want to Overwrite?");
                    if (result == Window.OK) {
                        final int oldIndex = contentProvider.findRow(jdbcDlg.getObject());
                        contentProvider.updateRow(oldIndex, jdbcDlg.getObject(), false);
                    }
                } else {
                    contentProvider.addRow(jdbcDlg.getObject());
                }
            }
        }
        if (e.getSource() == editBtn) {
            handleEditButton();
        }
        if (e.getSource() == deleteBtn) {
            final int selectedIndex = table.getSelectionIndex();
            if (selectedIndex == -1) {
                ControlFactory.showMessageDialog("Please verify a BizDriver Protocol Type is selected to perform Delete operation.", "Information");
            } else {
                contentProvider.removeRow(selectedIndex);
            }
        }
        if (e.getSource() == selectAllBtn) {
            final Control compositeChildArray[] = checkComposite.getChildren();
            for (int i = 0; i < compositeChildArray.length; i++) {
                final Object compositeChilsArrayObj = compositeChildArray[i];
                if (compositeChilsArrayObj instanceof Button) {
                    final Button select = (Button) compositeChildArray[i];
                    select.setSelection(true);
                }
            }
        }
        if (e.getSource() == deSelectAllBtn) {
            final Control compositeChildArray[] = checkComposite.getChildren();
            for (int i = 0; i < compositeChildArray.length; i++) {
                final Object compositeChilsArrayObj = compositeChildArray[i];
                if (compositeChilsArrayObj instanceof Button) {
                    final Button select = (Button) compositeChildArray[i];
                    select.setSelection(false);
                }
            }
        }
    }

    protected void handleEditButton() {
        final int selectedIndex = table.getSelectionIndex();
        if (selectedIndex == -1) {
            ControlFactory.showMessageDialog(translator.getString("Please verify a BizDriver Protocol Type is selected to perform Edit operation."), translator.getString("Information"));
        } else {
            final JDBCDriverInfoDlg jdbcDlg = new JDBCDriverInfoDlg(parent, "Edit BizDriver Protocol Type", contentProvider.getRow(selectedIndex));
            if (jdbcDlg.showDialog()) {
                if (contentProvider.contains(jdbcDlg.getObject())) {
                    final int oldIndex = contentProvider.findRow(jdbcDlg.getObject());
                    if (oldIndex != selectedIndex) {
                        final int result = ControlFactory.showConfirmDialog("BizDriver Protocol Type already exists, Do you want to Overwrite?");
                        if (result == Window.OK) {
                            contentProvider.updateRow(selectedIndex, jdbcDlg.getObject(), false);
                            contentProvider.removeRow(oldIndex);
                        }
                    } else {
                        contentProvider.updateSelectedRow(jdbcDlg.getObject(), false);
                    }
                } else {
                    contentProvider.updateRow(selectedIndex, jdbcDlg.getObject(), false);
                }
            }
        }
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
     */
    public void widgetDefaultSelected(final SelectionEvent e) {
    }

    /**
     * Gets the protocol and class from the UserConfig.xml and populates to the Table ContentProvider.
     */
    private void populateSQLDrivers() {
        final List tableColumnList = new ArrayList();
        tableColumnList.add("Protocol");
        tableColumnList.add("Class");
        objectPairs = new ArrayList();
        protoClassPair = UserPrefs.getSQLDriverHash();
        final Iterator protoClassPairKeys = protoClassPair.keySet().iterator();
        while (protoClassPairKeys.hasNext()) {
            final Object keyText = protoClassPairKeys.next();
            final Object valueText = protoClassPair.get(keyText);
            final ObjectPair objPair = new ObjectPair(keyText, valueText);
            objectPairs.add(objPair);
        }
        contentProvider = new PreferencePageTableContentProvider(bizDriverGroup, objectPairs, tableColumnList);
    }

    /**
     * Gets the tabletypes from the UserConfig.xml and populates to the composite.
     */
    private void populateTableTypes() {
        final Element tableTypesElement = UserPrefs.getTableTypes();
        tableTypesContent = tableTypesElement.getContent();
        final Iterator iterator = tableTypesContent.iterator();
        Button checkButton = null;
        while (iterator.hasNext()) {
            final Object tableTypeObj = iterator.next();
            tableTypeGridData = new GridData();
            tableTypeGridData.horizontalIndent = 20;
            if (tableTypeObj instanceof Comment) {
                checkButton = new Button(checkComposite, SWT.CHECK);
                final String commentText = ((Comment) tableTypeObj).getText();
                final int i = commentText.indexOf(">");
                final int j = commentText.lastIndexOf("<");
                checkButton.setText(commentText.substring(i + 1, j));
                checkButton.setSelection(false);
                checkButton.setLayoutData(tableTypeGridData);
            } else if (tableTypeObj instanceof Element) {
                checkButton = new Button(checkComposite, SWT.CHECK);
                final String elementText = ((Element) tableTypeObj).getText();
                checkButton.setText(elementText);
                checkButton.setSelection(true);
                checkButton.setLayoutData(tableTypeGridData);
            }
        }
        scrolledComp.setMinSize(checkComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
    }

    /**
     * Gets the protocol and class from the UserConfigDefault.xml and populates to the Table ContentProvider.
     */
    private void populateDefaultSQLDrivers() {
        final List tableColumnList = new ArrayList();
        tableColumnList.add("Protocol");
        tableColumnList.add("Class");
        objectPairs = new ArrayList();
        protoClassPair = DefaultUserPrefs.getSQLDriverHash();
        final Iterator protoClassPairKeys = protoClassPair.keySet().iterator();
        while (protoClassPairKeys.hasNext()) {
            final Object keyText = protoClassPairKeys.next();
            final Object valueText = protoClassPair.get(keyText);
            final ObjectPair objPair = new ObjectPair(keyText, valueText);
            objectPairs.add(objPair);
        }
        contentProvider.removeAllRows();
        contentProvider.addRows(objectPairs);
    }

    /**
     * Gets the tabletypes from the UserConfigDefault.xml and populates to the Table type composite .
     */
    private void populateDefaultTableTypes() {
        final Control[] controls = checkComposite.getChildren();
        for (int i = 0; i < controls.length; i++) {
            controls[i].dispose();
        }
        final Element tableTypesElement = DefaultUserPrefs.getTableTypes();
        tableTypesContent = tableTypesElement.getContent();
        final Iterator iterator = tableTypesContent.iterator();
        while (iterator.hasNext()) {
            final Object tableTypeObj = iterator.next();
            tableTypeGridData = new GridData();
            tableTypeGridData.horizontalIndent = 20;
            if (tableTypeObj instanceof Comment) {
                final Button checkButton = new Button(checkComposite, SWT.CHECK);
                final String commentText = ((Comment) tableTypeObj).getText();
                final int i = commentText.indexOf(">");
                final int j = commentText.lastIndexOf("<");
                checkButton.setText(commentText.substring(i + 1, j));
                checkButton.setSelection(false);
                checkButton.setLayoutData(tableTypeGridData);
            } else if (tableTypeObj instanceof Element) {
                final Button checkButton = new Button(checkComposite, SWT.CHECK);
                final String elementText = ((Element) tableTypeObj).getText();
                checkButton.setText(elementText);
                checkButton.setSelection(true);
                checkButton.setLayoutData(tableTypeGridData);
            }
        }
        checkComposite.layout();
        scrolledComp.layout();
        scrolledComp.setMinSize(checkComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
    }

    /**
     * WizardPerferencepage will be loaded from the UserConfigDefault.xml file.
     */
    @Override
    protected void performDefaults() {
        try {
            DefaultUserPrefs.getInstance();
            populateDefaultSQLDrivers();
            populateDefaultTableTypes();
        } catch (final Exception exception) {
            ControlFactory.showInfoDialog("Error loading from UserConfigDefault.xml.", exception.toString());
        }
    }

    /**
     * isSqlDriverModified method checks whether the contentprovider modified or not.
     * 
     * @return true if modified
     * @return false if not modified
     */
    protected boolean isSqlDriverModified() {
        rowsList = contentProvider.getRows();
        final Iterator rowListIterator = rowsList.iterator();
        objectPairs = new ArrayList();
        protoClassPair = UserPrefs.getSQLDriverHash();
        final Iterator protoClassPairKeys = protoClassPair.keySet().iterator();
        while (protoClassPairKeys.hasNext()) {
            final Object keyText = protoClassPairKeys.next();
            final Object valueText = protoClassPair.get(keyText);
            final ObjectPair objPair = new ObjectPair(keyText, valueText);
            objectPairs.add(objPair);
        }
        if (rowsList.size() != objectPairs.size()) {
            return true;
        } else {
            while (rowListIterator.hasNext()) {
                boolean found = false;
                final ObjectPair tableObject = (ObjectPair) rowListIterator.next();
                final String firstObjectValue = tableObject.getFirstObject().toString();
                final String secondObjectValue = tableObject.getSecondObject().toString();
                final Iterator objectPairsIterator = objectPairs.iterator();
                while (objectPairsIterator.hasNext()) {
                    final ObjectPair ConfigObject = (ObjectPair) objectPairsIterator.next();
                    final String firstConfigValue = ConfigObject.getFirstObject().toString();
                    final String secondConfigValue = ConfigObject.getSecondObject().toString();
                    if ((firstObjectValue.equals(firstConfigValue)) && secondObjectValue.equals(secondConfigValue)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * isTableTypesModified method checks whether the tabletypes modified or not.
     * 
     * @return true if modified
     * @return false if not modified
     */
    protected boolean isTableTypesModified() {
        final Control compositeChildArray[] = checkComposite.getChildren();
        final Element tableTypesElement = UserPrefs.getTableTypes();
        final int tableTypeSize = tableTypesElement.getContent().size();
        final int compositeChildArraySize = compositeChildArray.length;
        if (tableTypeSize != compositeChildArraySize) {
            return true;
        }
        for (int i = 0; i < compositeChildArray.length; i++) {
            final Button select = (Button) compositeChildArray[i];
            boolean check = select.getSelection();
            final String buttonText = select.getText();
            tableTypesContent = tableTypesElement.getContent();
            final Iterator iterator = tableTypesContent.iterator();
            while (iterator.hasNext()) {
                final Object tableTypesContentObject = iterator.next();
                if (tableTypesContentObject instanceof Comment) {
                    final String commentText = ((Comment) tableTypesContentObject).getText();
                    final int firstIndex = commentText.indexOf(">");
                    final int lastIndex = commentText.lastIndexOf("<");
                    final String xmlText = commentText.substring(firstIndex + 1, lastIndex);
                    if (buttonText.equals(xmlText) && check) {
                        return true;
                    }
                } else if (tableTypesContentObject instanceof Element) {
                    final String elementText = ((Element) tableTypesContentObject).getText();
                    if (buttonText.equals(elementText)) {
                        if (!check) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * All Changes done in the WizardPerferencepage will be updated to UserConfig.xml file.
     */
    @Override
    protected void performApply() {
        boolean changed = false;
        if (isSqlDriverModified()) {
            UserPrefs.setSQLDriverHash(rowsList);
            changed = true;
        }
        if (isTableTypesModified()) {
            setTableTypes();
            changed = true;
        }
        if (changed) {
            try {
                UserPrefs.updateUserConfig();
            } catch (final Exception exception) {
                ControlFactory.showInfoDialog("Error updating UserConfig.xml. ", exception.toString());
            }
        }
    }

    /**
     * Sets the SQL Table Types.
     */
    private void setTableTypes() {
        rowsList = contentProvider.getRows();
        final Element tableTypesElement1 = UserPrefs.getTableTypes();
        final List tableTypesElementContent = tableTypesElement1.getContent();
        final Iterator tableTypesElementIterator = tableTypesElementContent.iterator();
        while (tableTypesElementIterator.hasNext()) {
            final Object tableTypesObject = tableTypesElementIterator.next();
            if ((tableTypesObject instanceof Comment) || (tableTypesObject instanceof Element)) {
                tableTypesElementIterator.remove();
            }
        }
        final Control compositeChildArray[] = checkComposite.getChildren();
        for (int i = 0; i < compositeChildArray.length; i++) {
            final Button select = (Button) compositeChildArray[i];
            final boolean var = select.getSelection();
            final String buttonText = select.getText();
            UserPrefs.setTableTypes(buttonText, var);
        }
    }

    /**
     * JDBCDriverInfoDlg for Wizard Configuration Preference page. which display the BizDriverProtocol dialog.
     */
    class JDBCDriverInfoDlg implements XADialogOperation {

        /** Instance to hold Composite */
        private Composite dialogComposite;

        /** Instance to hold Label */
        private Label protocolLbl;

        /** Instance to hold Label */
        private Label classLbl;

        /** Instance to hold Text */
        protected Text protocolTxt;

        /** Instance to hold Text */
        protected Text classTxt;

        /** Instance to hold Shell */
        private Shell shell;

        /** Instance to hold ObjectPair */
        private ObjectPair protoClassObject;

        /**
         * Creates a new JDBCDriverInfoDlg object.
         * 
         * @param parent
         *            Composite instance.
         */
        public JDBCDriverInfoDlg(final Composite parent) {
            shell = XA_Designer_Plugin.getShell();
            dialogComposite = new Composite(shell, SWT.NONE);
            final GridLayout dialogLayout = new GridLayout(3, false);
            final GridData dialogGridData = new GridData(GridData.FILL_BOTH);
            dialogComposite.setLayout(dialogLayout);
            dialogComposite.setLayoutData(dialogGridData);
            createContents();
            final Point point = new Point(400, 150);
            final XADialog dialog = new XADialog(shell, this, dialogComposite, "Add BizDriver Protocol Type", true, true, point);
            dialog.open();
        }

        /**
         * Creates a new JDBCDriverInfoDlg object.
         * 
         * @param parent
         *            Composite instance.
         * @param title
         *            String Value.
         * @param protoClassObject
         *            Object instance.
         */
        public JDBCDriverInfoDlg(final Composite parent, final String title, final Object protoClassObject) {
            shell = XA_Designer_Plugin.getShell();
            dialogComposite = new Composite(shell, SWT.NONE);
            final GridLayout dialogCompositeLayout = new GridLayout(3, false);
            final GridData dialogCompositeGridData = new GridData(GridData.FILL_BOTH);
            dialogComposite.setLayout(dialogCompositeLayout);
            dialogComposite.setLayoutData(dialogCompositeGridData);
            createContents();
            if (protoClassObject != null) {
                final ObjectPair protoClassObjectPair = (ObjectPair) protoClassObject;
                protocolTxt.setText(protoClassObjectPair.getFirstObject().toString());
                classTxt.setText(protoClassObjectPair.getSecondObject().toString());
            }
            final Point point = new Point(400, 150);
            final XADialog dialog = new XADialog(shell, this, dialogComposite, title, true, true, point);
            dialog.open();
        }

        private void createContents() {
            final GridData textData = new GridData(GridData.FILL_HORIZONTAL);
            textData.horizontalSpan = 2;
            protocolLbl = new Label(dialogComposite, SWT.NONE);
            protocolLbl.setText("Driver Protocol:");
            protocolTxt = ControlFactory.createText(dialogComposite, SWT.BORDER);
            protocolTxt.setLayoutData(textData);
            classLbl = new Label(dialogComposite, SWT.NONE);
            classLbl.setText("Driver Class:");
            classTxt = ControlFactory.createText(dialogComposite, SWT.BORDER);
            classTxt.setLayoutData(textData);
        }

        /**
         * Gets the object which contains protocol and class contents
         * 
         * @return Object
         */
        public Object getObject() {
            return protoClassObject;
        }

        /**
         * Returns true if OK pressed otherwise false will be returned.
         * 
         * @return boolean
         */
        public boolean okPressed() {
            final String protocolText = protocolTxt.getText().trim();
            final String classText = classTxt.getText().trim();
            final Object object1 = protocolText;
            final Object object2 = classText;
            if (protocolText.equals("")) {
                ControlFactory.showMessageDialog("Driver Protocol should not be empty.", "Information");
                Display.getCurrent().asyncExec(new Runnable() {

                    public void run() {
                        if (protocolTxt != null) {
                            protocolTxt.forceFocus();
                        }
                    }
                });
                return false;
            } else if (classText.equals("")) {
                ControlFactory.showMessageDialog("Driver Class should not be empty.", "Information");
                Display.getCurrent().asyncExec(new Runnable() {

                    public void run() {
                        if (classTxt != null) {
                            classTxt.forceFocus();
                        }
                    }
                });
                return false;
            }
            protoClassObject = new ObjectPair(object1, object2);
            canceled = false;
            return true;
        }

        /**
         * Returns true if Cancel pressed otherwise false will be returned.
         * 
         * @return boolean
         */
        public boolean cancelPressed() {
            canceled = true;
            return true;
        }

        /**
         * Returns true if OK pressed otherwise false will be returned.
         * 
         * @return boolean
         */
        public boolean showDialog() {
            if (canceled) {
                return false;
            } else {
                return true;
            }
        }
    }
}
