package net.sf.myway.edit.ui.editors;

import java.util.EventObject;
import java.util.List;
import net.sf.myway.calibrator.da.entities.ScannedMap;
import net.sf.myway.calibrator.da.entities.props.ScannedMapPropertySource;
import net.sf.myway.edit.EditPlugin;
import net.sf.myway.edit.ui.editors.commands.LocationCommand;
import net.sf.myway.edit.ui.editors.palette.MapPalette;
import net.sf.myway.hibernate.NamedUuidEntity;
import net.sf.myway.map.da.entities.MapObject;
import net.sf.myway.map.profiles.Profile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * @author Andreas Beckers
 * @version $Revision: 1.5 $
 */
public class MapEditor extends EditorPart {

    public static final String ID = "net.sf.myway.edit.editor.map";

    private ScrollingGraphicalViewer _viewer;

    private ScalableRootEditPart _root;

    private EditDomain _editDomain;

    private boolean _dirty;

    private MapPalette _palette;

    /**
	 * 
	 */
    public static final String BACKGROUND_LAYER = "BackgroundLayer";

    public static final String LAYER1 = "Layer1";

    public static final String LAYER2 = "Layer2";

    public static final String LAYER3 = "Layer3";

    public static final String LAYER4 = "Layer4";

    public static final String LAYER5 = "Layer5";

    public static final String LAYER6 = "Layer6";

    public static final String LAYER7 = "Layer7";

    public static final String LAYER8 = "Layer8";

    public static final String LAYER9 = "Layer9";

    public static final String[] LAYERS = { LayerConstants.PRIMARY_LAYER, LAYER1, LAYER2, LAYER3, LAYER4, LAYER5, LAYER6, LAYER7, LAYER8, LAYER9, BACKGROUND_LAYER };

    private final CommandStackListener commandStackListener = new CommandStackListener() {

        @Override
        public void commandStackChanged(final EventObject event) {
            setDirty(getCommandStack().isDirty());
        }
    };

    private ActionRegistry _actionRegistry;

    private PropertySheetPage _propertySheetPage;

    /**
	 * @param parent
	 */
    private ScrollingGraphicalViewer createGraphicalViewer(final Composite parent) {
        final ScrollingGraphicalViewer viewer = new ScrollingGraphicalViewer();
        viewer.createControl(parent);
        _root = new EditRootEditPart();
        viewer.setRootEditPart(_root);
        getEditDomain().addViewer(viewer);
        getSite().setSelectionProvider(viewer);
        viewer.setEditPartFactory(getEditPartFactory());
        final KeyHandler keyHandler = new GraphicalViewerKeyHandler(viewer) {

            @SuppressWarnings("unchecked")
            @Override
            public boolean keyPressed(final KeyEvent event) {
                if (event.stateMask == SWT.MOD1 && event.keyCode == SWT.DEL) {
                    final List<? extends EditorPart> objects = viewer.getSelectedEditParts();
                    if (objects == null || objects.isEmpty()) return true;
                    final GroupRequest deleteReq = new GroupRequest(RequestConstants.REQ_DELETE);
                    final CompoundCommand compoundCmd = new CompoundCommand("Delete");
                    for (int i = 0; i < objects.size(); i++) {
                        final EditPart object = (EditPart) objects.get(i);
                        deleteReq.setEditParts(object);
                        final Command cmd = object.getCommand(deleteReq);
                        if (cmd != null) compoundCmd.add(cmd);
                    }
                    getCommandStack().execute(compoundCmd);
                    return true;
                }
                if (event.stateMask == SWT.MOD3 && (event.keyCode == SWT.ARROW_DOWN || event.keyCode == SWT.ARROW_LEFT || event.keyCode == SWT.ARROW_RIGHT || event.keyCode == SWT.ARROW_UP)) {
                    final List<? extends EditorPart> objects = viewer.getSelectedEditParts();
                    if (objects == null || objects.isEmpty()) return true;
                    final GroupRequest moveReq = new ChangeBoundsRequest(RequestConstants.REQ_MOVE);
                    final CompoundCommand compoundCmd = new CompoundCommand("Move");
                    for (int i = 0; i < objects.size(); i++) {
                        final EditPart object = (EditPart) objects.get(i);
                        moveReq.setEditParts(object);
                        final LocationCommand cmd = (LocationCommand) object.getCommand(moveReq);
                        if (cmd != null) {
                            cmd.setLocation(new Point(event.keyCode == SWT.ARROW_LEFT ? -1 : event.keyCode == SWT.ARROW_RIGHT ? 1 : 0, event.keyCode == SWT.ARROW_DOWN ? 1 : event.keyCode == SWT.ARROW_UP ? -1 : 0));
                            cmd.setRelative(true);
                            compoundCmd.add(cmd);
                        }
                    }
                    getCommandStack().execute(compoundCmd);
                    return true;
                }
                return super.keyPressed(event);
            }
        };
        keyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));
        viewer.setKeyHandler(keyHandler);
        viewer.setContents(getEditorInput().getAdapter(NamedUuidEntity.class));
        viewer.addDropTargetListener(createTransferDropTargetListener(viewer));
        return viewer;
    }

    /**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
    @Override
    public void createPartControl(final Composite parent) {
        final Composite split = new Composite(parent, SWT.NONE);
        final GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        split.setLayout(layout);
        _viewer = createGraphicalViewer(split);
        _viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        final NamedUuidEntity data = (NamedUuidEntity) getEditorInput().getAdapter(NamedUuidEntity.class);
        final Profile profile = data instanceof ScannedMap ? EditPlugin.getBL().getProfile((ScannedMap) data) : EditPlugin.getBL().getDefaultProfile();
        _palette = new MapPalette(getEditDomain(), profile);
        final PaletteViewer paletteViewer = _palette.createPaletteViewer(split);
        final GridData gd = new GridData(GridData.FILL_VERTICAL);
        gd.widthHint = 175;
        paletteViewer.getControl().setLayoutData(gd);
        setPartName(data.getName());
    }

    private TransferDropTargetListener createTransferDropTargetListener(final ScrollingGraphicalViewer viewer) {
        return new TemplateTransferDropTargetListener(viewer) {

            @Override
            protected CreationFactory getFactory(final Object template) {
                if (template instanceof CreationFactory) return (CreationFactory) template;
                return null;
            }
        };
    }

    /**
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
    @Override
    public void dispose() {
        getCommandStack().removeCommandStackListener(getCommandStackListener());
        super.dispose();
    }

    /**
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        final EditMapEditPart editMap = (EditMapEditPart) _root.getContents();
        final List<MapObject> objs = editMap.getObjects();
        EditPlugin.getBL().save(objs);
        getCommandStack().markSaveLocation();
        _dirty = false;
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    /**
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
    @Override
    public void doSaveAs() {
        throw new UnsupportedOperationException();
    }

    /**
	 * Lazily creates and returns the action registry.
	 * 
	 * @return the action registry
	 */
    protected ActionRegistry getActionRegistry() {
        if (_actionRegistry == null) _actionRegistry = new ActionRegistry();
        return _actionRegistry;
    }

    /**
	 * @see org.eclipse.ui.part.WorkbenchPart#getAdapter(java.lang.Class)
	 */
    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(final Class adapter) {
        if (adapter == GraphicalViewer.class || adapter == EditPartViewer.class) return _viewer; else if (adapter == CommandStack.class) return getCommandStack(); else if (adapter == EditDomain.class) return getEditDomain(); else if (adapter == IPropertySheetPage.class) return getPropertySheetPage();
        return super.getAdapter(adapter);
    }

    public CommandStack getCommandStack() {
        return getEditDomain().getCommandStack();
    }

    protected CommandStackListener getCommandStackListener() {
        return commandStackListener;
    }

    public EditDomain getEditDomain() {
        if (_editDomain == null) _editDomain = new DefaultEditDomain(this);
        return _editDomain;
    }

    /**
	 * @return
	 */
    private EditPartFactory getEditPartFactory() {
        return new MapPartFactory();
    }

    /**
	 * @return
	 */
    private PropertySheetPage getPropertySheetPage() {
        if (null == _propertySheetPage) {
            _propertySheetPage = new PropertySheetPage();
            _propertySheetPage.setPropertySourceProvider(new IPropertySourceProvider() {

                @Override
                public IPropertySource getPropertySource(final Object object) {
                    return new ScannedMapPropertySource((ScannedMap) object);
                }
            });
            _propertySheetPage.setRootEntry(new UndoablePropertySheetEntry(getCommandStack()));
        }
        return _propertySheetPage;
    }

    /**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        getCommandStack().addCommandStackListener(getCommandStackListener());
    }

    /**
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
    @Override
    public boolean isDirty() {
        return _dirty;
    }

    /**
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
	 * @param dirty
	 *            the dirty to set
	 */
    public void setDirty(final boolean dirty) {
        if (_dirty != dirty) {
            _dirty = dirty;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /**
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
    @Override
    public void setFocus() {
        _viewer.setFocus(_root);
    }
}
