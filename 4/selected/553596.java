package org.nexopenframework.ide.eclipse.jee.wizards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog2;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ComboDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.nexopenframework.ide.eclipse.commons.io.IOUtils;
import org.nexopenframework.ide.eclipse.commons.log.Logger;
import org.nexopenframework.ide.eclipse.commons.util.StringUtils;
import org.nexopenframework.ide.eclipse.jee.JeeServiceComponentUIPlugin;
import org.nexopenframework.ide.eclipse.jee.model.ISpringController;
import org.nexopenframework.ide.eclipse.jee.wizards.AbstractWebTypeWizardPage.FacadeWrapper;
import org.nexopenframework.ide.eclipse.jst.JstActivator;
import org.nexopenframework.ide.eclipse.jst.datamodel.web.NexOpenFacetInstallDataModelProvider;
import org.nexopenframework.ide.eclipse.velocity.VelocityEngineHolder;
import org.nexopenframework.ide.eclipse.velocity.VelocityEngineUtils;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>Wizard for creating a JSP associated to a Spring MVC controller and associated the i18n
 * messages</p>
 * 
 * @see NewTypeWizardPage
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public class NewSpringJSPControllerWizardPage extends NewTypeWizardPage {

    /**separator of JSP pages*/
    public static final String SEPARATOR = "/";

    /***/
    private static final String HIBERNATE3_ADAPTER = "Hibernate3 Adapter";

    /***/
    private static final String PERSISTENCE_MANAGER_ADAPTER = "Persistence Manager Adapter";

    /***/
    private static final String FACADE_ADAPTER = "Facade Adapter";

    /***/
    private static final String FORM_JSP_PAGE = "Form JSP page";

    /**JSTL forEach JSP page*/
    private static final String JSP_PAGE_JSTL = "JSP page with forEach JSTL tag";

    /**ValueList JSP page with tag support*/
    private static final String JSP_PAGE_VLH = "JSP page with ValueList tag";

    /**Simple JSP page*/
    private static final String SIMPLE_JSP_PAGE = "Simple JSP page";

    /**CRUD JSP page*/
    private static final String CRUD_JSP_PAGE = "JSPs for CRUD development";

    private static final Map TEMPLATES = new HashMap();

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("org.nexopenframework.ide.eclipse.jee.wizards.jsp");

    static {
        TEMPLATES.put(SIMPLE_JSP_PAGE, "simple_view.vm");
        TEMPLATES.put(JSP_PAGE_JSTL, "foreach_view.vm");
        TEMPLATES.put(FORM_JSP_PAGE, "form_view.vm");
        TEMPLATES.put(JSP_PAGE_VLH, "vlh_view.vm");
    }

    /**Eclipse IStructuredSelection*/
    private IStructuredSelection selection;

    /**JSP Name*/
    private StringDialogField fJspNameDialogField;

    /**JSP templates*/
    private ComboDialogField cTemplatesTypeDialogField;

    /**Items Name, the name of the collection or similar to be stored in a given scope*/
    private StringDialogField fItemsNameDialogField;

    /**valueList table identifier*/
    private StringDialogField fTableIdNameDialogField;

    /**valueList table identifier*/
    private StringDialogField fAdapterNameDialogField;

    /**JSP templates*/
    private ComboDialogField cAdaptersTypeDialogField;

    /**custom facade creation enabling*/
    private SelectionButtonDialogFieldGroup fFacadeAdapterEnabledCheckbox;

    /**Facade selection*/
    private StringButtonDialogField fFacadeClassTypeField;

    /**Dispaly info about selected JSP*/
    private Text displayJspInfo;

    /**model which represents some aspects of the controller*/
    private ISpringController spring;

    /**The parent for retrieve data*/
    NewSpringControllerWizardPage springPage;

    /**
	 * <p></p>
	 */
    public NewSpringJSPControllerWizardPage() {
        super(true, "wizardAddJSPControllerWizard");
        setTitle("Add a JSP page to Spring MVC Controller");
        setDescription("This wizard creates a new JSP page for " + "Spring controller and provides ValueList support if selected");
        setImageDescriptor(JeeServiceComponentUIPlugin.getImageDescriptor("/icons/banners/newjspfile_wiz.gif"));
        fJspNameDialogField = new StringDialogField();
        fJspNameDialogField.setLabelText("JSP Name");
        fJspNameDialogField.setText("default.jsp");
        fJspNameDialogField.setDialogFieldListener(new IDialogFieldListener() {

            public void dialogFieldChanged(DialogField field) {
                if (field == fJspNameDialogField) {
                    String n = fJspNameDialogField.getText();
                    if (spring != null && (n == null || n.trim().length() == 0 || n.trim().length() == 1)) {
                        String[] mappings = spring.getURLMappings();
                        String name = (mappings != null && mappings.length > 0) ? mappings[0] : "default";
                        StringBuffer sb = new StringBuffer(name);
                        sb.append(".jsp");
                        fJspNameDialogField.setText(sb.toString());
                    }
                }
            }
        });
        cTemplatesTypeDialogField = new ComboDialogField(1);
        cTemplatesTypeDialogField.setLabelText("JSP Template");
        String[] items = new String[] { SIMPLE_JSP_PAGE, JSP_PAGE_JSTL, FORM_JSP_PAGE, JSP_PAGE_VLH };
        cTemplatesTypeDialogField.setItems(items);
        cTemplatesTypeDialogField.selectItem(0);
        IDialogFieldListener listener = new IDialogFieldListener() {

            public void dialogFieldChanged(DialogField field) {
                String template = cTemplatesTypeDialogField.getText();
                if (!template.equals(SIMPLE_JSP_PAGE)) {
                    fItemsNameDialogField.setEnabled(true);
                    fItemsNameDialogField.setText("items");
                    if (template.equals(JSP_PAGE_JSTL)) {
                        disableValueListSupport();
                        displayJspInfo.setText(BUNDLE.getString("jsp.forEach"));
                    } else if (template.equals(FORM_JSP_PAGE)) {
                        disableValueListSupport();
                        displayJspInfo.setText(BUNDLE.getString("jsp.form"));
                    } else if (template.equals(JSP_PAGE_VLH)) {
                        displayJspInfo.setText(BUNDLE.getString("jsp.vlh"));
                        fTableIdNameDialogField.setEnabled(true);
                        fTableIdNameDialogField.setFocus();
                        fTableIdNameDialogField.setText("MyTable");
                        fAdapterNameDialogField.setEnabled(true);
                        fAdapterNameDialogField.setText("list");
                        cAdaptersTypeDialogField.selectItem(0);
                        cAdaptersTypeDialogField.setEnabled(true);
                    }
                    return;
                }
                fItemsNameDialogField.setText("");
                fItemsNameDialogField.setEnabled(false);
                disableValueListSupport();
                displayJspInfo.setText(BUNDLE.getString("jsp.simple"));
            }
        };
        cTemplatesTypeDialogField.setDialogFieldListener(listener);
        fItemsNameDialogField = new StringDialogField();
        fItemsNameDialogField.setLabelText("Items name");
        fItemsNameDialogField.setEnabled(false);
        fTableIdNameDialogField = new StringDialogField();
        fTableIdNameDialogField.setLabelText("Table Identifier");
        fTableIdNameDialogField.setEnabled(false);
        fAdapterNameDialogField = new StringDialogField();
        fAdapterNameDialogField.setLabelText("Adapter Name");
        fAdapterNameDialogField.setEnabled(false);
        cAdaptersTypeDialogField = new ComboDialogField(1);
        cAdaptersTypeDialogField.setLabelText("Adapter Type");
        String[] _items = new String[] { PERSISTENCE_MANAGER_ADAPTER, HIBERNATE3_ADAPTER, FACADE_ADAPTER };
        cAdaptersTypeDialogField.setItems(_items);
        cAdaptersTypeDialogField.selectItem(0);
        IDialogFieldListener _listener = new IDialogFieldListener() {

            public void dialogFieldChanged(DialogField field) {
                if (field == cAdaptersTypeDialogField) {
                    String adapter = cAdaptersTypeDialogField.getText();
                    if (adapter.equals(FACADE_ADAPTER)) {
                        fFacadeAdapterEnabledCheckbox.setEnabled(true);
                    } else {
                        fFacadeAdapterEnabledCheckbox.setEnabled(false);
                        fFacadeAdapterEnabledCheckbox.setSelection(0, false);
                        fFacadeClassTypeField.setEnabled(false);
                        fFacadeClassTypeField.setText("");
                    }
                }
            }
        };
        cAdaptersTypeDialogField.setEnabled(false);
        cAdaptersTypeDialogField.setDialogFieldListener(_listener);
        IDialogFieldListener adapterEnable = new IDialogFieldListener() {

            public void dialogFieldChanged(DialogField field) {
                if (field == fFacadeAdapterEnabledCheckbox) {
                    if (fFacadeAdapterEnabledCheckbox.isSelected(0)) {
                        fFacadeClassTypeField.setEnabled(true);
                        List facades = springPage.getFacades();
                        if (!facades.isEmpty()) {
                            FacadeWrapper base = (FacadeWrapper) facades.get(0);
                            fFacadeClassTypeField.setText(base.facadeName);
                        }
                    } else {
                        fFacadeClassTypeField.setEnabled(false);
                        fFacadeClassTypeField.setText("");
                    }
                }
            }
        };
        fFacadeAdapterEnabledCheckbox = new SelectionButtonDialogFieldGroup(SWT.CHECK, new String[] { "" }, 1);
        fFacadeAdapterEnabledCheckbox.setDialogFieldListener(adapterEnable);
        fFacadeAdapterEnabledCheckbox.setLabelText("Facade Adapter creation");
        fFacadeAdapterEnabledCheckbox.setSelection(0, false);
        IStringButtonAdapter adapter = new IStringButtonAdapter() {

            public void changeControlPressed(DialogField field) {
                if (field == fFacadeClassTypeField) {
                    IType type = chooseClass();
                    if (type != null) {
                        fFacadeClassTypeField.setText(JavaModelUtil.getFullyQualifiedName(type));
                    }
                }
            }
        };
        fFacadeClassTypeField = new StringButtonDialogField(adapter);
        fFacadeClassTypeField.setLabelText("Business Facade");
        fFacadeClassTypeField.setButtonLabel("Browse ...");
        fFacadeClassTypeField.setEnabled(false);
    }

    /**
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        int nColumns = 4;
        GridLayout layout = new GridLayout();
        layout.numColumns = nColumns;
        composite.setLayout(layout);
        this.createTemplatesTypeControls(composite, nColumns);
        this.createJSPNameControls(composite, nColumns);
        this.createItemsNameControls(composite, nColumns);
        DialogField.createEmptySpace(composite);
        this.createSeparator(composite, nColumns);
        this.createTableIdControls(composite, nColumns);
        this.createAdapterNameControls(composite, nColumns);
        this.createAdaptersTypeControls(composite, nColumns);
        this.createAdapterFacadeEnablingControls(composite, nColumns);
        this.createAdapterFacadeNameControls(composite, nColumns);
        DialogField.createEmptySpace(composite);
        displayJspInfo = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
        displayJspInfo.setText(BUNDLE.getString("jsp.simple"));
        displayJspInfo.setEditable(false);
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_FILL);
        gridData.heightHint = 20;
        gridData.widthHint = 20;
        displayJspInfo.setLayoutData(gridData);
        DialogField.createEmptySpace(composite);
        setControl(composite);
        Dialog.applyDialogFont(composite);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.NEW_CLASS_WIZARD_PAGE);
    }

    /**
	 * @param selection
	 */
    public void init(IStructuredSelection selection) {
        this.selection = selection;
        IJavaElement element = getInitialJavaElement(this.selection);
        initContainerPage(element);
        initTypePage(element);
    }

    /**
	 * @param page
	 */
    public void setJspPage(String page) {
        String jsp = (page != null) ? (page.indexOf(".jsp") > -1) ? page : new StringBuffer(page).append(".jsp").toString() : "default.jsp";
        if (jsp.startsWith("/")) {
            jsp = jsp.substring(jsp.indexOf("/") + 1);
        }
        this.fJspNameDialogField.setText(jsp);
    }

    /**
	 * 
	 */
    public void setFormJspPage() {
        this.cTemplatesTypeDialogField.selectItem(2);
        this.cTemplatesTypeDialogField.setEnabled(false);
        this.fItemsNameDialogField.setEnabled(false);
    }

    /**
	 * <p></p>
	 * 
	 */
    public void unsetFormJspPage() {
        this.cTemplatesTypeDialogField.selectItem(0);
        this.cTemplatesTypeDialogField.setEnabled(true);
        this.fItemsNameDialogField.setEnabled(true);
    }

    /**
	 * <p>Generate CRUD context</p>
	 */
    public void setCRUDContext() {
        displayJspInfo.setText(BUNDLE.getString("jsp.crud"));
        this.fJspNameDialogField.setLabelText("JSP Form" + SEPARATOR + "ValueList Names");
        this.fJspNameDialogField.setText("default_form.jsp" + SEPARATOR + "default_vl.jsp");
        String[] items = new String[] { CRUD_JSP_PAGE };
        this.cTemplatesTypeDialogField.setItems(items);
        this.cTemplatesTypeDialogField.selectItem(0);
        this.cTemplatesTypeDialogField.setEnabled(false);
        fTableIdNameDialogField.setEnabled(true);
        fTableIdNameDialogField.setText("MyTable");
        fAdapterNameDialogField.setEnabled(true);
        fAdapterNameDialogField.setText("list");
        cAdaptersTypeDialogField.setEnabled(true);
        fFacadeAdapterEnabledCheckbox.setEnabled(false);
    }

    public void unsetCRUDContext() {
        displayJspInfo.setText(BUNDLE.getString("jsp.simple"));
        this.fJspNameDialogField.setLabelText("JSP Name");
        this.fJspNameDialogField.setText("default.jsp");
        String[] items = new String[] { SIMPLE_JSP_PAGE, JSP_PAGE_JSTL, FORM_JSP_PAGE, JSP_PAGE_VLH };
        this.cTemplatesTypeDialogField.setItems(items);
        this.cTemplatesTypeDialogField.selectItem(0);
        this.cTemplatesTypeDialogField.setEnabled(true);
        this.fItemsNameDialogField.setEnabled(true);
        fTableIdNameDialogField.setEnabled(false);
        fAdapterNameDialogField.setEnabled(false);
        cAdaptersTypeDialogField.setEnabled(false);
    }

    /**
	 * @param controller
	 */
    protected void setSpringController(ISpringController controller) {
        this.spring = controller;
    }

    /**
	 * @return
	 */
    protected String getItemsName() {
        return this.fItemsNameDialogField.getText();
    }

    /**
	 * @return
	 */
    protected String getJspName() {
        return this.fJspNameDialogField.getText();
    }

    /**
	 * @return
	 */
    protected String getTableId() {
        return this.fTableIdNameDialogField.getText();
    }

    /**
	 * @return
	 */
    protected String getAdapterName() {
        return this.fAdapterNameDialogField.getText();
    }

    /**
	 * @return
	 */
    protected boolean isValueListSelected() {
        return this.cTemplatesTypeDialogField.getText().equals(JSP_PAGE_VLH);
    }

    /**
	 * @return
	 */
    protected boolean isCRUDSelected() {
        return this.cTemplatesTypeDialogField.getText().equals(CRUD_JSP_PAGE);
    }

    /**
	 * Creates the controls for the type Spring JSP name field. Expects a
	 * <code>GridLayout</code> with at least 2 columns.
	 * 
	 * @param composite
	 *            the parent composite
	 * @param nColumns
	 *            number of columns to span
	 */
    protected void createJSPNameControls(final Composite composite, int nColumns) {
        fJspNameDialogField.doFillIntoGrid(composite, nColumns - 1);
        DialogField.createEmptySpace(composite);
        Text text = fJspNameDialogField.getTextControl(null);
        LayoutUtil.setWidthHint(text, getMaxFieldWidth());
        TextFieldNavigationHandler.install(text);
    }

    /**
	 * Creates the controls for the type Items name field. Expects a
	 * <code>GridLayout</code> with at least 2 columns.
	 * 
	 * @param composite
	 *            the parent composite
	 * @param nColumns
	 *            number of columns to span
	 */
    protected void createItemsNameControls(final Composite composite, int nColumns) {
        fItemsNameDialogField.doFillIntoGrid(composite, nColumns - 1);
        DialogField.createEmptySpace(composite);
        Text text = fItemsNameDialogField.getTextControl(null);
        LayoutUtil.setWidthHint(text, getMaxFieldWidth());
        TextFieldNavigationHandler.install(text);
    }

    /**
	 * Creates the controls for the type Item Table identifier (VL support) name field. Expects a
	 * <code>GridLayout</code> with at least 2 columns.
	 * 
	 * @param composite
	 *            the parent composite
	 * @param nColumns
	 *            number of columns to span
	 */
    protected void createTableIdControls(final Composite composite, int nColumns) {
        fTableIdNameDialogField.doFillIntoGrid(composite, nColumns - 1);
        DialogField.createEmptySpace(composite);
        Text text = fTableIdNameDialogField.getTextControl(null);
        LayoutUtil.setWidthHint(text, getMaxFieldWidth());
        TextFieldNavigationHandler.install(text);
    }

    /**
	 * Creates the controls for the type Item Adapter name (VL support) name field. Expects a
	 * <code>GridLayout</code> with at least 2 columns.
	 * 
	 * @param composite
	 *            the parent composite
	 * @param nColumns
	 *            number of columns to span
	 */
    protected void createAdapterNameControls(final Composite composite, int nColumns) {
        fAdapterNameDialogField.doFillIntoGrid(composite, nColumns - 1);
        DialogField.createEmptySpace(composite);
        Text text = fAdapterNameDialogField.getTextControl(null);
        LayoutUtil.setWidthHint(text, getMaxFieldWidth());
        TextFieldNavigationHandler.install(text);
    }

    /**
	 * @param composite
	 * @param nColumns
	 */
    protected void createTemplatesTypeControls(Composite composite, int nColumns) {
        cTemplatesTypeDialogField.doFillIntoGrid(composite, nColumns - 1);
        DialogField.createEmptySpace(composite);
        Control control = cTemplatesTypeDialogField.getComboControl(composite);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = nColumns - 2;
        control.setLayoutData(gd);
    }

    /**
	 * @param composite
	 * @param nColumns
	 */
    protected void createAdaptersTypeControls(Composite composite, int nColumns) {
        cAdaptersTypeDialogField.doFillIntoGrid(composite, nColumns - 1);
        DialogField.createEmptySpace(composite);
        Control control = cAdaptersTypeDialogField.getComboControl(composite);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = nColumns - 2;
        control.setLayoutData(gd);
    }

    /**
	 * Creates the controls for Adapter Facade creation. Expects a
	 * <code>GridLayout</code> with at least 3 columns.
	 * 
	 * @param composite
	 *            the parent composite
	 * @param nColumns
	 *            number of columns to span
	 */
    protected void createAdapterFacadeEnablingControls(Composite composite, int nColumns) {
        LayoutUtil.setHorizontalSpan(fFacadeAdapterEnabledCheckbox.getLabelControl(composite), 1);
        Control control = fFacadeAdapterEnabledCheckbox.getSelectionButtonsGroup(composite);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = nColumns - 2;
        control.setLayoutData(gd);
        DialogField.createEmptySpace(composite);
    }

    /**
	 * Creates the controls for the type Item Adapter name (VL support) name field. Expects a
	 * <code>GridLayout</code> with at least 2 columns.
	 * 
	 * @param composite
	 *            the parent composite
	 * @param nColumns
	 *            number of columns to span
	 */
    protected void createAdapterFacadeNameControls(final Composite composite, int nColumns) {
        fFacadeClassTypeField.doFillIntoGrid(composite, nColumns - 1);
        DialogField.createEmptySpace(composite);
        Text text = fFacadeClassTypeField.getTextControl(null);
        LayoutUtil.setWidthHint(text, getMaxFieldWidth());
        TextFieldNavigationHandler.install(text);
    }

    /**
	 * Opens a selection dialog that allows to select a super class. 
	 * 
	 * @return returns the selected type or <code>null</code> if the dialog has been canceled.
	 * The caller typically sets the result to the super class input field.
	 * 	<p>
	 * Clients can override this method if they want to offer a different dialog.
	 * </p>
	 * 
	 * @since 3.2
	 */
    protected IType chooseClass() {
        IPackageFragmentRoot root = getPackageFragmentRoot();
        if (root == null) {
            return null;
        }
        IJavaElement[] elements = new IJavaElement[] { root.getJavaProject() };
        IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements);
        TypeSelectionDialog2 dialog = new TypeSelectionDialog2(getShell(), false, getWizard().getContainer(), scope, IJavaSearchConstants.INTERFACE);
        dialog.setTitle("Business Facade Selection");
        dialog.setMessage("Choose a Business Facade type");
        List facades = springPage.getFacades();
        if (!facades.isEmpty()) {
            FacadeWrapper base = (FacadeWrapper) facades.get(0);
            dialog.setFilter(base.facadeName);
        } else {
            dialog.setFilter("java.lang.Object");
        }
        if (dialog.open() == Window.OK) {
            return (IType) dialog.getFirstResult();
        }
        return null;
    }

    /**
	 * @param monitor
	 * @param model
	 * @throws CoreException
	 */
    protected void createJSPType(final IProgressMonitor monitor, final Map model) throws CoreException {
        if (this.getJspName() == null || this.getJspName().length() == 0) {
            Logger.getLog().info("No crea");
            return;
        }
        final String webResourcePath = NexOpenFacetInstallDataModelProvider.WEB_SRC_MAIN_WEBAPP;
        StringBuffer sb = new StringBuffer(webResourcePath);
        if (!webResourcePath.endsWith("/")) {
            sb.append("/");
        }
        sb.append("WEB-INF/jsp");
        String jspName = this.getJspName();
        String[] tokens = StringUtils.tokenizeToStringArray(jspName, "/");
        if (tokens != null && tokens.length > 1) {
            sb.append("/");
            for (int k = 0; k < tokens.length - 1; k++) {
                String tkn = tokens[k];
                if (tkn != null && tkn.trim().length() > 0) {
                    sb.append(tkn);
                    if (k != tokens.length - 2) {
                        sb.append("/");
                    }
                }
            }
            jspName = tokens[tokens.length - 1];
        }
        final IPackageFragmentRoot pfr = this.getPackageFragmentRoot();
        if (pfr == null) {
            Logger.getLog().error("No PackageFragmentRoot present into " + this + ". You must select a root in the project");
            throw new IllegalStateException("No project selected, please ");
        }
        final JavaProject project = (JavaProject) pfr.getParent();
        final IProject pj = project.getJavaProject().getProject();
        IPath jspPath = new Path(sb.toString());
        IFolder folder = pj.getFolder(jspPath);
        if (!folder.exists()) {
            folder.create(true, true, null);
        }
        String jspFolder = new StringBuffer(sb.toString()).append("/").append(jspName).toString();
        IFile jsp = pj.getFile(new Path(jspFolder));
        if (!jsp.exists()) {
            createJSPTypeInternal(monitor, pj, model, jspName, jsp);
        }
    }

    /**
	 * <p>First element is Form name</p>
	 * <p>Second element is ValueList name</p>
	 * @return
	 */
    protected String[] getCRUDViews() {
        String jspNames = this.getJspName();
        String[] tokens = StringUtils.tokenizeToStringArray(jspNames, SEPARATOR);
        if (tokens.length == 2) {
            String formPage = tokens[0];
            String vlPage = tokens[1];
            String[] values = new String[2];
            {
                String formName = formPage.substring(0, formPage.indexOf(".jsp"));
                values[0] = formName;
                String vlName = vlPage.substring(0, vlPage.indexOf(".jsp"));
                values[1] = vlName;
            }
            return values;
        }
        return null;
    }

    /**
	 * <p></p>
	 * 
	 * @param monitor
	 * @param model
	 * @throws CoreException
	 */
    protected void createJSPCRUDType(IProgressMonitor monitor, Map model) throws CoreException {
        if (this.getJspName() == null || this.getJspName().length() == 0) {
            return;
        }
        IProject ipj = getPackageFragment().getJavaProject().getProject();
        String projectName = ipj.getName();
        model.put("projectName", projectName);
        final String webResourcePath = NexOpenFacetInstallDataModelProvider.WEB_SRC_MAIN_WEBAPP;
        StringBuffer sb = new StringBuffer(webResourcePath);
        if (!webResourcePath.endsWith("/")) {
            sb.append("/");
        }
        sb.append("WEB-INF/jsp");
        String jspNames = this.getJspName();
        String[] tokens = StringUtils.tokenizeToStringArray(jspNames, SEPARATOR);
        if (tokens != null && tokens.length == 2) {
            JavaProject project = (JavaProject) this.getPackageFragmentRoot().getParent();
            IProject pj = project.getJavaProject().getProject();
            IPath jspPath = new Path(sb.toString());
            IFolder folder = pj.getFolder(jspPath);
            if (!folder.exists()) {
                folder.create(true, true, null);
            }
            String formPage = tokens[0];
            String vlPage = tokens[1];
            String jspFormPageFolder = new StringBuffer(sb.toString()).append("/").append(formPage).toString();
            String jspVLPageFolder = new StringBuffer(sb.toString()).append("/").append(vlPage).toString();
            IFile jspForm = pj.getFile(new Path(jspFormPageFolder));
            if (!jspForm.exists()) {
                {
                    model.put("pageName", getCRUDViews()[0]);
                }
                VelocityEngine engine = VelocityEngineHolder.getEngine();
                String source;
                try {
                    source = VelocityEngineUtils.mergeTemplateIntoString(engine, "form_view.vm", model);
                    createi18nMessages(pj, "form_view.vm", getCRUDViews()[0]);
                } catch (VelocityException e) {
                    IStatus status = new Status(IStatus.ERROR, JeeServiceComponentUIPlugin.PLUGIN_ID, IStatus.OK, "Velocity exception", e);
                    throw new CoreException(status);
                }
                final InputStream in = new ByteArrayInputStream(source.getBytes());
                try {
                    jspForm.create(in, true, monitor);
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
            IFile jspVL = pj.getFile(new Path(jspVLPageFolder));
            if (!jspVL.exists()) {
                {
                    model.put("pageName", getCRUDViews()[1]);
                }
                VelocityEngine engine = VelocityEngineHolder.getEngine();
                String source;
                try {
                    source = VelocityEngineUtils.mergeTemplateIntoString(engine, "vlh_view.vm", model);
                    createi18nMessages(pj, "vlh_view.vm", getCRUDViews()[1]);
                } catch (VelocityException e) {
                    IStatus status = new Status(IStatus.ERROR, JeeServiceComponentUIPlugin.PLUGIN_ID, IStatus.OK, "Velocity exception", e);
                    throw new CoreException(status);
                }
                final InputStream in = new ByteArrayInputStream(source.getBytes());
                try {
                    jspVL.create(in, true, monitor);
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    /**
	 * @param monitor
	 * @param pack
	 * @param model
	 * @throws CoreException
	 */
    protected void createFacadeAdapterType(IProgressMonitor monitor, IPackageFragment pack, Map model) throws CoreException {
        if (!this.cAdaptersTypeDialogField.getText().equals(FACADE_ADAPTER)) {
            return;
        }
        String fcn = this.fFacadeClassTypeField.getText();
        if (fcn == null || fcn.length() == 0) {
            return;
        }
        FacadeWrapper wrapper = new FacadeWrapper(this.fFacadeClassTypeField.getText());
        model.put("facade", wrapper);
        try {
            String source = null;
            try {
                VelocityEngine engine = VelocityEngineHolder.getEngine();
                source = VelocityEngineUtils.mergeTemplateIntoString(engine, "FacadeAdapter.vm", model);
            } catch (VelocityException e) {
                IStatus status = new Status(IStatus.ERROR, JeeServiceComponentUIPlugin.PLUGIN_ID, IStatus.OK, "Velocity exception", e);
                throw new CoreException(status);
            }
            String className = new StringBuffer(wrapper.simpleClassName()).append("Adapter.java").toString();
            pack.createCompilationUnit(className, source, false, monitor);
            return;
        } catch (JavaModelException e) {
            Logger.log(Logger.INFO, "Facade class " + wrapper + " already exists . Not created");
        }
    }

    /**
	 * @param monitor
	 * @param pack
	 * @param model
	 * @throws CoreException
	 */
    protected void createValueListAnnotation(IProgressMonitor monitor, IPackageFragment pack, Map model) throws CoreException {
        IProject pj = pack.getJavaProject().getProject();
        QualifiedName qn = new QualifiedName(JstActivator.PLUGIN_ID, JstActivator.PACKAGE_INFO_LOCATION);
        String location = pj.getPersistentProperty(qn);
        if (location != null) {
            IFolder javaFolder = pj.getFolder(new Path(NexOpenFacetInstallDataModelProvider.WEB_SRC_MAIN_JAVA));
            IFolder packageInfo = javaFolder.getFolder(location);
            if (!packageInfo.exists()) {
                Logger.log(Logger.INFO, "package-info package [" + location + "] does not exists.");
                Logger.log(Logger.INFO, "ValueList annotation will not be added by this wizard. " + "You must add manually in your package-info class if exist " + "or create a new one at location " + location);
                return;
            }
            IFile pkginfo = packageInfo.getFile("package-info.java");
            if (!pkginfo.exists()) {
                Logger.log(Logger.INFO, "package-info class at location [" + location + "] does not exists.");
                return;
            }
            InputStream in = pkginfo.getContents();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                IOUtils.copy(in, baos);
                String content = new String(baos.toByteArray());
                VelocityEngine engine = VelocityEngineHolder.getEngine();
                model.put("adapterType", getAdapterType());
                model.put("packageInfo", location.replace('/', '.'));
                model.put("defaultNumberPerPage", "5");
                model.put("defaultSortDirection", "asc");
                if (isFacadeAdapter()) {
                    model.put("facadeType", "true");
                }
                if (content.indexOf("@ValueLists({})") > -1) {
                    appendValueList(monitor, model, pkginfo, content, engine, true);
                    return;
                } else if (content.indexOf("@ValueLists") > -1) {
                    appendValueList(monitor, model, pkginfo, content, engine, false);
                    return;
                }
                String vl = VelocityEngineUtils.mergeTemplateIntoString(engine, "ValueList.vm", model);
                ByteArrayInputStream bais = new ByteArrayInputStream(vl.getBytes());
                try {
                    pkginfo.setContents(bais, true, false, monitor);
                } finally {
                    bais.close();
                }
                return;
            } catch (IOException e) {
                IStatus status = new Status(IStatus.ERROR, JeeServiceComponentUIPlugin.PLUGIN_ID, IStatus.OK, "I/O exception", e);
                throw new CoreException(status);
            } catch (VelocityException e) {
                IStatus status = new Status(IStatus.ERROR, JeeServiceComponentUIPlugin.PLUGIN_ID, IStatus.OK, "Velocity exception", e);
                throw new CoreException(status);
            } finally {
                try {
                    baos.close();
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        Logger.log(Logger.INFO, "package-info location property does not exists.");
    }

    /**
	 * @param monitor
	 * @param model
	 * @param pkginfo
	 * @param content
	 * @param engine
	 * @param firstTime 
	 * @throws VelocityException
	 * @throws CoreException
	 * @throws IOException
	 */
    void appendValueList(IProgressMonitor monitor, Map model, IFile pkginfo, String content, VelocityEngine engine, boolean firstTime) throws VelocityException, CoreException, IOException {
        if (!firstTime) {
            model.put("exists", Boolean.TRUE);
        }
        String vlf = VelocityEngineUtils.mergeTemplateIntoString(engine, "ValueListFragment.vm", model);
        StringBuffer sb = new StringBuffer(content);
        int index = content.indexOf("{");
        sb.insert(index + "{".length(), vlf);
        FacadeWrapper w = (FacadeWrapper) model.get("facade");
        if (w != null) {
            StringBuffer bfa = new StringBuffer("import ").append((String) model.get("packageName"));
            bfa.append(".").append(w.simpleClassName()).append("Adapter");
            bfa.append(";");
            sb.append("\n");
            if (!(sb.indexOf(bfa.toString()) > -1)) {
                sb.append(bfa);
            }
        } else {
            StringBuffer bfa = new StringBuffer("import ").append("org.nexopenframework.web.vlh.annotations.MatchModeType;");
            if (!(sb.indexOf(bfa.toString()) > -1)) {
                sb.append("\n");
                sb.append(bfa);
            }
            bfa = new StringBuffer("import ").append("org.nexopenframework.web.vlh.annotations.QueryType;");
            if (!(sb.indexOf(bfa.toString()) > -1)) {
                sb.append("\n");
                sb.append(bfa);
            }
            sb.append("\n");
        }
        if (firstTime) {
            sb.append("\n").append("import org.nexopenframework.web.vlh.annotations.AdapterType;").append("\n");
            sb.append("import org.nexopenframework.web.vlh.annotations.Query;").append("\n");
            sb.append("import org.nexopenframework.web.vlh.annotations.ValueList;").append("\n");
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes());
        try {
            pkginfo.setContents(bais, true, false, monitor);
        } finally {
            bais.close();
        }
        return;
    }

    /**
	 * @param monitor
	 * @param pj 
	 * @param model
	 * @param jspName
	 * @param jsp
	 * @throws CoreException
	 */
    void createJSPTypeInternal(final IProgressMonitor monitor, final IProject pj, Map model, final String jspName, final IFile jsp) throws CoreException {
        String projectName = pj.getName();
        VelocityEngine engine = VelocityEngineHolder.getEngine();
        String template = (String) TEMPLATES.get(this.cTemplatesTypeDialogField.getText());
        String name = jspName.substring(0, jspName.indexOf(".jsp"));
        {
            model.put("projectName", projectName);
            model.put("pageName", name);
        }
        String source;
        try {
            source = VelocityEngineUtils.mergeTemplateIntoString(engine, template, model);
        } catch (VelocityException e) {
            IStatus status = new Status(IStatus.ERROR, JeeServiceComponentUIPlugin.PLUGIN_ID, IStatus.OK, "Velocity exception", e);
            throw new CoreException(status);
        }
        final InputStream in = new ByteArrayInputStream(source.getBytes());
        try {
            jsp.create(in, true, monitor);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
        createi18nMessages(pj, template, name);
    }

    /**
	 * @param pj
	 * @param template
	 * @param name
	 * @throws CoreException
	 */
    void createi18nMessages(IProject pj, String template, String name) throws CoreException {
        final String webResourcePath = NexOpenFacetInstallDataModelProvider.WEB_SRC_MAIN_RESOURCES;
        StringBuffer sb = new StringBuffer(webResourcePath);
        if (!webResourcePath.endsWith("/")) {
            sb.append("/");
        }
        sb.append("applicationMessages.properties");
        IPath msgPath = new Path(sb.toString());
        IFile i18n = pj.getFile(msgPath);
        if (i18n.exists()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                InputStream is = i18n.getContents();
                Properties props = new Properties();
                props.load(is);
                props.put(new StringBuffer(name).append(".main.title").toString(), "Welcome main title");
                if (template.equals(JSP_PAGE_VLH) || isCRUDSelected()) {
                    props.put(new StringBuffer(name).append(".item.noresults").toString(), "No results found");
                    props.put(new StringBuffer(name).append(".item.try").toString(), "Try again");
                }
                StringBuffer header = new StringBuffer("here we specify i18n messages for application pages of ");
                header.append(name).append(" application");
                props.store(baos, header.toString());
                final InputStream in = new ByteArrayInputStream(baos.toByteArray());
                i18n.setContents(in, true, false, null);
                return;
            } catch (IOException e) {
                IStatus status = new Status(IStatus.ERROR, JeeServiceComponentUIPlugin.PLUGIN_ID, IStatus.OK, "I/O exception loading/storing i18n file", e);
                throw new CoreException(status);
            } finally {
                try {
                    baos.close();
                } catch (IOException e) {
                }
            }
        }
        Logger.log(Logger.INFO, "No i18n properties found at location " + sb);
        Logger.log(Logger.INFO, "Please, create a i18n properties at location " + sb);
    }

    /**
	 * Disable value list support
	 */
    void disableValueListSupport() {
        {
            fTableIdNameDialogField.setEnabled(false);
            fTableIdNameDialogField.setText("");
            fAdapterNameDialogField.setEnabled(false);
            fAdapterNameDialogField.setText("");
            cAdaptersTypeDialogField.selectItem(0);
            cAdaptersTypeDialogField.setEnabled(false);
            fFacadeAdapterEnabledCheckbox.setEnabled(false);
            fFacadeClassTypeField.setEnabled(false);
            fFacadeClassTypeField.setText("");
        }
    }

    /**
	 * @return
	 */
    protected boolean isFacadeAdapter() {
        String adapter = this.cAdaptersTypeDialogField.getText();
        return adapter.equals(FACADE_ADAPTER);
    }

    /**
	 * @return
	 */
    private String getAdapterType() {
        String adapter = this.cAdaptersTypeDialogField.getText();
        if (adapter.equals(FACADE_ADAPTER)) {
            return "FACADE_ADAPTER";
        } else if (adapter.equals(PERSISTENCE_MANAGER_ADAPTER)) {
            return "PERSISTENCE_MANAGER_ADAPTER";
        } else if (adapter.equals(HIBERNATE3_ADAPTER)) {
            return "HIBERNATE3_ADAPTER";
        }
        return null;
    }
}
