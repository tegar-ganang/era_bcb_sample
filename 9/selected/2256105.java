package thinwire_wysiwyg.editors;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.palette.CreationToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheetPage;
import thinwire.ui.Component;
import thinwire.ui.Container;
import thinwire_wysiwyg.Activator;
import thinwire_wysiwyg.ConfigurationAssistant;
import thinwire_wysiwyg.JavaProjectTools;
import thinwire_wysiwyg.editors.dialogs.MappingDialog;
import thinwire_wysiwyg.editors.gef.EditPartFigure;
import thinwire_wysiwyg.editors.gef.ThinwireComponentTypeResolver;
import thinwire_wysiwyg.editors.gef.actions.ComponentAction;
import thinwire_wysiwyg.editors.gef.designtime.DesignTimeComponent;
import thinwire_wysiwyg.editors.gef.designtime.DesignTimeGUIComponent;
import thinwire_wysiwyg.editors.gef.designtime.DesignTimeListener;
import thinwire_wysiwyg.editors.properties.ThinwireComponentPropertySheetPage;

public class ThinwireGEFEditor extends GraphicalEditorWithFlyoutPalette implements IPropertySourceProvider, IMenuListener {

    class DesignTimeGUIComponentDynamicCreationTool implements CreationFactory {

        IType type = null;

        public DesignTimeGUIComponentDynamicCreationTool() {
        }

        public Object getNewObject() {
            if (type == null) {
                return null;
            }
            DesignTimeGUIComponent comp = new DesignTimeGUIComponent(null);
            comp.setComponentType(type);
            return comp;
        }

        public Object getObjectType() {
            ThinwireComponentFinderDialog dlg = new ThinwireComponentFinderDialog(getSite().getShell(), SWT.APPLICATION_MODAL);
            dlg.setProject(JavaCore.create(((FileEditorInput) getEditorInput()).getFile().getProject()));
            dlg.open();
            type = dlg.getSelectedType();
            return type;
        }
    }

    class DesignTimeGUIComponentCreationTool implements CreationFactory {

        IType type;

        public DesignTimeGUIComponentCreationTool(IType type) {
            this.type = type;
        }

        public Object getNewObject() {
            DesignTimeGUIComponent comp = new DesignTimeGUIComponent(null);
            comp.setComponentType(type);
            return comp;
        }

        public Object getObjectType() {
            return type;
        }
    }

    IMemento getComponentClasses() {
        IPath componentsPath = Activator.getDefault().getStateLocation().append("components.xml");
        File file = componentsPath.toFile();
        try {
            if (file != null) {
                return XMLMemento.createReadRoot(new FileReader(file));
            }
        } catch (WorkbenchException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Set<String> types = new HashSet<String>();
        XMLMemento ret = XMLMemento.createWriteRoot("components");
        try {
            IPackageFragment thinwireUIFragment = getProject().findType("thinwire.ui.Component").getPackageFragment();
            if (!thinwireUIFragment.isOpen()) {
                thinwireUIFragment.open(null);
            }
            for (IJavaElement comp : thinwireUIFragment.getChildren()) {
                if (isComponent(((IClassFile) comp).findPrimaryType())) {
                    ret.createChild("component").putTextData(((IClassFile) comp).findPrimaryType().getFullyQualifiedName());
                }
            }
            ret.save(new FileWriter(file));
        } catch (JavaModelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private boolean isComponent(IType type) {
        try {
            return Component.class.isAssignableFrom(getClassLoader().loadClass(type.getFullyQualifiedName()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<IType> getComponents() {
        List<IType> ret = new ArrayList<IType>();
        for (IMemento memento : getComponentClasses().getChildren("component")) {
            try {
                ret.add(getProject().findType(memento.getTextData()));
            } catch (JavaModelException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    private IJavaProject getProject() {
        return project;
    }

    private List<IType> getAllComponents() {
        return getComponents();
    }

    MenuManager contextMenu = new MenuManager();

    thinwire_wysiwyg.editors.gef.EditPartFactory fact = new thinwire_wysiwyg.editors.gef.EditPartFactory();

    IResourceChangeEvent lastEvent = null;

    ClassLoader loader = null;

    protected GEFContentOutlinePage olp = new GEFContentOutlinePage(this);

    PaletteRoot pr = null;

    ThinwireComponentPropertySheetPage propertyPage = new ThinwireComponentPropertySheetPage(this);

    IPropertySource propSrc = null;

    ThinwireEditor sourceEditor;

    Thread updateThread = null;

    public ThinwireGEFEditor(ThinwireEditor editor) {
        this.sourceEditor = editor;
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
    }

    @Override
    public void doSave(IProgressMonitor arg0) {
    }

    Set<ComponentAction> allActions = new HashSet<ComponentAction>();

    public static IMenuManager getSubMenu(IMenuManager menu, String group, boolean create) {
        for (IContributionItem item : menu.getItems()) {
            if (item instanceof MenuManager) {
                if (((MenuManager) item).getMenuText().equals(group)) {
                    return (IMenuManager) item;
                }
            }
        }
        if (create) {
            MenuManager ret = new MenuManager(group);
            menu.add(ret);
            return ret;
        }
        return null;
    }

    private final IAction mappingAction = new Action("Change Mapping") {

        @Override
        public void run() {
            doMappingEdit();
        }
    };

    protected void doMappingEdit() {
        MappingDialog md = new MappingDialog(new Shell(), SWT.APPLICATION_MODAL);
        md.setEditor(this.sourceEditor);
        md.open();
    }

    public void fillContextMenu(IMenuManager arg0) {
        if (sourceEditor.getServlet() != null) {
            arg0.add(mappingAction);
            arg0.add(new Separator());
        }
        for (ComponentAction action : allActions) {
            action.setParts(getViewer().getSelectedEditParts());
            if (action.canRun()) {
                IMenuManager menu = arg0;
                if (action.getGroupName() != null) {
                    for (StringTokenizer entryIT = new StringTokenizer(action.getGroupName(), "!"); entryIT.hasMoreTokens(); ) {
                        String menuName = entryIT.nextToken();
                        menu = getSubMenu(menu, menuName, true);
                    }
                }
                menu.add(action);
            }
        }
    }

    private Set<IType> findImplementors(final IJavaProject project, String type) throws CoreException {
        final Set<IType> results = new HashSet<IType>();
        int includeMask = IJavaSearchScope.SOURCES | IJavaSearchScope.APPLICATION_LIBRARIES | IJavaSearchScope.REFERENCED_PROJECTS;
        IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { project }, includeMask);
        SearchEngine se = new SearchEngine();
        SearchPattern pattern = SearchPattern.createPattern(project.findType(type), IJavaSearchConstants.IMPLEMENTORS);
        SearchRequestor requestor = new SearchRequestor() {

            @Override
            public void acceptSearchMatch(SearchMatch match) throws CoreException {
                results.add((IType) match.getElement());
            }
        };
        long start = System.currentTimeMillis();
        se.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, searchScope, requestor, null);
        List<IType> research = new ArrayList<IType>(results);
        for (IType c_type : research) {
            results.addAll(findImplementors(project, c_type.getFullyQualifiedName()));
        }
        return results;
    }

    @Override
    public Object getAdapter(Class type) {
        if (IContentOutlinePage.class == type) {
            return olp;
        }
        if (IPropertySheetPage.class == type) {
            return propertyPage;
        }
        return super.getAdapter(type);
    }

    private ClassLoader getClassLoader() {
        if (loader == null) {
            loader = JavaProjectTools.getClassLoader(getClass().getClassLoader(), JavaCore.create(((FileEditorInput) getEditorInput()).getFile().getProject()));
        }
        return loader;
    }

    public thinwire_wysiwyg.editors.gef.EditPartFactory getFactory() {
        return fact;
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        if (pr == null) {
            pr = new PaletteRoot();
            PaletteDrawer pd = new PaletteDrawer("default");
            pr.add(pd);
            pd.add(new SelectionToolEntry());
            PaletteDrawer customDrawer = new PaletteDrawer("Custom");
            pr.add(customDrawer);
            customDrawer.add(new CreationToolEntry("Select...", "Select...", new DesignTimeGUIComponentDynamicCreationTool(), null, null));
        }
        return pr;
    }

    private IPropertySource getPropertySource() {
        if (getViewer() == null || getViewer().getSelectedEditParts() == null) {
            return null;
        }
        if (getViewer().getSelectedEditParts().size() != 1) {
            return null;
        }
        Object element = ((EditPart) getViewer().getSelectedEditParts().get(0)).getModel();
        if (!(element instanceof DesignTimeComponent)) {
            return null;
        }
        DesignTimeComponent comp = (DesignTimeComponent) element;
        return comp.getPropertySource();
    }

    public IPropertySource getPropertySource(Object arg0) {
        if (arg0 instanceof DesignTimeComponent) {
            return ((DesignTimeComponent) arg0).getPropertySource();
        }
        return null;
    }

    public EditPart getRootEditPart() {
        return getGraphicalViewer().getRootEditPart();
    }

    private DesignTimeComponent getSelectedComponent() {
        if (getViewer() == null || getViewer().getSelectedEditParts() == null) {
            return null;
        }
        if (getViewer().getSelectedEditParts().size() != 1) {
            return null;
        }
        Object element = ((EditPart) getViewer().getSelectedEditParts().get(0)).getModel();
        if (!(element instanceof DesignTimeComponent)) {
            return null;
        }
        DesignTimeComponent comp = (DesignTimeComponent) element;
        return comp;
    }

    public GraphicalViewer getViewer() {
        return getGraphicalViewer();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        this.project = JavaCore.create(((FileEditorInput) input).getFile().getProject());
        setEditDomain(new DefaultEditDomain(this));
        super.init(site, input);
        loadComponents(getPaletteRoot());
        loadActions();
    }

    IJavaProject project = null;

    protected void loadActions() {
        allActions.addAll(ConfigurationAssistant.getComponentActions());
    }

    @Override
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        getGraphicalViewer().setEditPartFactory(fact);
        ITypeRoot tr = sourceEditor.getInputElement();
        try {
            getGraphicalViewer().setContents(tr);
            ((DesignTimeComponent) fact.getTopPart().getModelChildren().get(0)).addListener(new DesignTimeListener() {

                public void handleUpdate(DesignTimeComponent comp) {
                    updateSourceWithComponent(comp.getTopLevel());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
        getGraphicalViewer().addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent arg0) {
                if (getSelectedComponent() == null) {
                    return;
                }
                PropertySheetPage propertySheet = ((PropertySheetPage) getAdapter(IPropertySheetPage.class));
                propertySheet.selectionChanged(getSite().getPart(), new StructuredSelection(getSelectedComponent()));
                try {
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
        getGraphicalViewer().setContextMenu(contextMenu);
        contextMenu.addMenuListener(this);
    }

    private void loadComponents(PaletteRoot root) {
        PaletteDrawer componentDrawer = new PaletteDrawer("Thinwire:Components");
        PaletteDrawer containerDrawer = new PaletteDrawer("Thinwire:Containers");
        root.add(componentDrawer);
        root.add(containerDrawer);
        IJavaProject proj = JavaCore.create(((FileEditorInput) getEditorInput()).getFile().getProject());
        for (IType type : getAllComponents()) {
            try {
                Class hldClass = getClassLoader().loadClass(type.getFullyQualifiedName());
                EditPartFigure fig = ThinwireComponentTypeResolver.figureFor(hldClass);
                if (!hldClass.isInterface() && !Modifier.isAbstract(hldClass.getModifiers()) && Modifier.isPublic(hldClass.getModifiers())) {
                    if (Container.class.isAssignableFrom(getClassLoader().loadClass(type.getFullyQualifiedName()))) {
                        containerDrawer.add(new CreationToolEntry(type.getElementName(), type.getElementName(), new DesignTimeGUIComponentCreationTool(type), null, null));
                    } else if (Component.class.isAssignableFrom(getClassLoader().loadClass(type.getFullyQualifiedName()))) {
                        componentDrawer.add(new CreationToolEntry(type.getElementName(), type.getElementName(), new DesignTimeGUIComponentCreationTool(type), null, null));
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessError e) {
            } catch (Throwable th) {
            }
        }
    }

    public void menuAboutToShow(IMenuManager arg0) {
        arg0.removeAll();
        fillContextMenu(arg0);
    }

    private char[] readAll(InputStream stream) throws IOException {
        CharArrayWriter writer = new CharArrayWriter();
        int len = -1;
        byte[] buff = new byte[1024];
        while (((len = stream.read(buff)) > 0)) {
            writer.write(new String(buff, 0, len));
        }
        return writer.toCharArray();
    }

    public void refresh() {
        try {
            getSite().getShell().getDisplay().syncExec(new Runnable() {

                public void run() {
                    ITypeRoot tr = sourceEditor.getInputElement();
                    try {
                        getGraphicalViewer().setContents(tr);
                        ((DesignTimeComponent) fact.getTopPart().getModelChildren().get(0)).addListener(new DesignTimeListener() {

                            public void handleUpdate(DesignTimeComponent comp) {
                                updateSourceWithComponent(comp.getTopLevel());
                            }
                        });
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void updateSource(final DesignTimeComponent comp) {
        IType primaryType = sourceEditor.getInputElement().findPrimaryType();
        try {
            String code = "public " + primaryType.getElementName() + "()\n{\nsuper();\n" + "// This is GENERATED CODE any changes made here will be OVERWRITTEN\n" + comp.getTopLevel().convertToCode() + "if(getLayout()!=null){getLayout().apply();}\n" + "initialize();\n" + "}\n";
            IMethod init = primaryType.getMethod(primaryType.getElementName(), new String[] {});
            if (init != null && init.exists()) {
                init.delete(true, null);
            }
            primaryType.createMethod(code, null, true, null);
            if (!primaryType.getMethod("initialize", new String[] {}).exists()) {
                primaryType.createMethod("protected void initialize(){\n// This is an auto-generated method where developer startup code gets put\n}\n", null, true, null);
            }
            stopUdateFromSource();
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
    }

    public void stopUdateFromSource() {
        if (sourceEditor.updateThread != null) {
            sourceEditor.updateThread.interrupt();
            sourceEditor.updateThread = null;
        }
    }

    public synchronized void updateSourceWithComponent(final DesignTimeComponent comp) {
        if (updateThread != null && updateThread.isAlive()) {
            updateThread.interrupt();
        }
        updateThread = null;
        updateThread = new Thread() {

            @Override
            public void run() {
                try {
                    sleep(1);
                    updateSource(comp);
                } catch (InterruptedException ex) {
                }
            }
        };
        updateThread.start();
    }
}
