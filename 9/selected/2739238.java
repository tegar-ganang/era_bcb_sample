package net.sf.redsetter.editor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import net.sf.redsetter.action.MappingContextMenuProvider;
import net.sf.redsetter.dnd.DataEditDropTargetListener;
import net.sf.redsetter.jdt.importer.ModelValidator;
import net.sf.redsetter.model.Element;
import net.sf.redsetter.model.Mapping;
import net.sf.redsetter.model.Element.Position;
import net.sf.redsetter.sync.MyJavaElementChangeReporter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.actions.StackAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class MappingEditor extends GraphicalEditorWithFlyoutPalette implements CommandStackListener, ISelectionListener {

    private Mapping mapping;

    /** the list of action ids that are to CommandStack actions */
    private List stackActionIDs = new ArrayList();

    /** the list of action ids that are to EditPart actions */
    private List editPartActionIDs = new ArrayList();

    /** the list of action ids that are editor actions */
    private List editorActionIDs = new ArrayList();

    /** the <code>EditDomain</code> */
    private DefaultEditDomain editDomain;

    /** the graphical viewer */
    private GraphicalViewer graphicalViewer;

    /** the overview outline page */
    private OverviewOutlinePage overviewOutlinePage;

    private boolean isDirty;

    /** the editor's action registry */
    private ActionRegistry actionRegistry;

    public MappingEditor() {
        editDomain = new DefaultEditDomain(this);
        setEditDomain(editDomain);
    }

    public void dispose() {
        super.dispose();
    }

    public void doSave(IProgressMonitor monitor) {
        XStream streamer = getStreamer();
        IFile file = ((IFileEditorInput) getEditorInput()).getFile();
        String xmlString = streamer.toXML(this.mapping);
        byte currentXMLBytes[] = xmlString.getBytes();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(currentXMLBytes);
        try {
            file.setContents(byteArrayInputStream, true, false, monitor);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        getCommandStack().markSaveLocation();
    }

    private XStream getStreamer() {
        XStream streamer = new XStream(new DomDriver());
        return streamer;
    }

    /**
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        IFile file = ((IFileEditorInput) input).getFile();
        try {
            setPartName(file.getName());
            InputStream is = file.getContents(true);
            XStream streamer = getStreamer();
            InputStreamReader reader = new InputStreamReader(is);
            mapping = (Mapping) streamer.fromXML(reader);
            mapping.refreshAllMappings();
            ModelValidator modelValidator = new ModelValidator(mapping);
            isDirty = modelValidator.hasChanged();
        } catch (CoreException e) {
            e.printStackTrace();
        } catch (StreamException e2) {
            mapping = new Mapping();
            mapping.setElementA(new Element(mapping, Position.LEFT));
            mapping.getElementA().setName("A");
            mapping.getElementA().setVisible(true);
            mapping.setElementB(new Element(mapping, Position.RIGHT));
            mapping.getElementB().setName("B");
            mapping.getElementA().setVisible(true);
        }
    }

    public void doSaveAs() {
    }

    public boolean isDirty() {
        return isDirty;
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        getCommandStack().addCommandStackListener(this);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
        createActions();
    }

    /**
	 * The <code>CommandStackListener</code> that listens for
	 * <code>CommandStack </code> changes.
	 */
    public void commandStackChanged(EventObject event) {
        updateActions(stackActionIDs);
        setDirty(getCommandStack().isDirty());
    }

    protected void firePropertyChange(int propertyId) {
        super.firePropertyChange(propertyId);
        updateActions(editorActionIDs);
    }

    /**
	 * Sets the dirty state of this editor.
	 * 
	 * <p>
	 * An event will be fired immediately if the new state is different than the
	 * current one.
	 * 
	 * @param dirty
	 *            the new dirty state to set
	 */
    protected void setDirty(boolean dirty) {
        if (isDirty != dirty) {
            isDirty = dirty;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /**
	 * Returns the <code>GraphicalViewer</code> of this editor.
	 * 
	 * @return the <code>GraphicalViewer</code>
	 */
    public GraphicalViewer getGraphicalViewer() {
        return graphicalViewer;
    }

    /** the selection listener implementation */
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        updateActions(editPartActionIDs);
    }

    /**
	 * Returns the <code>CommandStack</code> of this editor's
	 * <code>EditDomain</code>.
	 * 
	 * @return the <code>CommandStack</code>
	 */
    public CommandStack getCommandStack() {
        return getEditDomain().getCommandStack();
    }

    protected void createGraphicalViewer(Composite parent) {
        IEditorSite editorSite = getEditorSite();
        GraphicalViewer viewer = new GraphicalViewerCreator(editorSite).createViewer(parent);
        GraphicalViewerKeyHandler graphicalViewerKeyHandler = new GraphicalViewerKeyHandler(viewer);
        KeyHandler parentKeyHandler = graphicalViewerKeyHandler.setParent(getCommonKeyHandler());
        viewer.setKeyHandler(parentKeyHandler);
        getEditDomain().addViewer(viewer);
        getSite().setSelectionProvider(viewer);
        viewer.setContents(mapping);
        ContextMenuProvider provider = new MappingContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
        DataEditDropTargetListener dropListener = new DataEditDropTargetListener(viewer);
        DropTarget dt = new DropTarget(viewer.getControl(), DND.DROP_COPY);
        Transfer transfer = dropListener.getTransfer();
        dt.setTransfer(new Transfer[] { transfer });
        dt.addDropListener(dropListener);
        JavaCore.addElementChangedListener(new MyJavaElementChangeReporter());
        this.graphicalViewer = viewer;
    }

    private KeyHandler getCommonKeyHandler() {
        KeyHandler sharedKeyHandler = new KeyHandler();
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));
        return sharedKeyHandler;
    }

    /**
	 * @return the preferences for the Palette Flyout
	 */
    protected FlyoutPreferences getPalettePreferences() {
        return new PaletteFlyoutPreferences();
    }

    public Mapping getMapping() {
        return mapping;
    }

    /**
	 * Creates a PaletteViewerProvider that will be used to create palettes for
	 * the view and the flyout.
	 * 
	 * @return the palette provider
	 */
    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new MappingPaletteViewerProvider(editDomain);
    }

    /**
	 * @return the PaletteRoot to be used with the PaletteViewer
	 */
    protected PaletteRoot getPaletteRoot() {
        return new PaletteViewerCreator().createPaletteRoot();
    }

    /**
	 * Returns the action registry of this editor.
	 * 
	 * @return the action registry
	 */
    protected ActionRegistry getActionRegistry() {
        if (actionRegistry == null) actionRegistry = new ActionRegistry();
        return actionRegistry;
    }

    /**
	 * Updates the specified actions.
	 * 
	 * @param actionIds
	 *            the list of ids of actions to update
	 */
    protected void updateActions(List actionIds) {
        for (Iterator ids = actionIds.iterator(); ids.hasNext(); ) {
            IAction action = getActionRegistry().getAction(ids.next());
            if (null != action && action instanceof UpdateAction) ((UpdateAction) action).update();
        }
    }

    /**
	 * Adaptable implementation for Editor
	 */
    public Object getAdapter(Class adapter) {
        if (adapter == GraphicalViewer.class || adapter == EditPartViewer.class) return getGraphicalViewer(); else if (adapter == CommandStack.class) return getCommandStack(); else if (adapter == EditDomain.class) return getEditDomain(); else if (adapter == ActionRegistry.class) return getActionRegistry(); else if (adapter == IContentOutlinePage.class) return getOverviewOutlinePage();
        return super.getAdapter(adapter);
    }

    /**
	 * Creates actions and registers them to the ActionRegistry.
	 */
    protected void createActions() {
        addStackAction(new UndoAction(this));
        addStackAction(new RedoAction(this));
        addEditPartAction(new DeleteAction((IWorkbenchPart) this));
        addEditorAction(new SaveAction(this));
        addEditorAction(new PrintAction(this));
    }

    /**
	 * Adds an <code>EditPart</code> action to this editor.
	 * 
	 * <p>
	 * <code>EditPart</code> actions are actions that depend and work on the
	 * selected <code>EditPart</code>s.
	 * 
	 * @param action
	 *            the <code>EditPart</code> action
	 */
    protected void addEditPartAction(SelectionAction action) {
        getActionRegistry().registerAction(action);
        editPartActionIDs.add(action.getId());
    }

    /**
	 * Adds an <code>CommandStack</code> action to this editor.
	 * 
	 * <p>
	 * <code>CommandStack</code> actions are actions that depend and work on
	 * the <code>CommandStack</code>.
	 * 
	 * @param action
	 *            the <code>CommandStack</code> action
	 */
    protected void addStackAction(StackAction action) {
        getActionRegistry().registerAction(action);
        stackActionIDs.add(action.getId());
    }

    /**
	 * Adds an editor action to this editor.
	 * 
	 * <p>
	 * <Editor actions are actions that depend and work on the editor.
	 * 
	 * @param action
	 *            the editor action
	 */
    protected void addEditorAction(WorkbenchPartAction action) {
        getActionRegistry().registerAction(action);
        editorActionIDs.add(action.getId());
    }

    /**
	 * Adds an action to this editor's <code>ActionRegistry</code>. (This is
	 * a helper method.)
	 * 
	 * @param action
	 *            the action to add.
	 */
    protected void addAction(IAction action) {
        getActionRegistry().registerAction(action);
    }

    private static ImageDescriptor create(String iconPath, String name) {
        return AbstractUIPlugin.imageDescriptorFromPlugin("net.sf.redsetter.editor.MappingEditor", iconPath + name);
    }

    /**
	 * Returns the overview for the outline view.
	 * 
	 * @return the overview
	 */
    protected OverviewOutlinePage getOverviewOutlinePage() {
        if (null == overviewOutlinePage && null != getGraphicalViewer()) {
            RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();
            if (rootEditPart instanceof ScalableFreeformRootEditPart) {
                overviewOutlinePage = new OverviewOutlinePage((ScalableFreeformRootEditPart) rootEditPart);
            }
        }
        return overviewOutlinePage;
    }
}
