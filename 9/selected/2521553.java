package net.sf.myway.calibrator.ui.editors;

import java.util.EventObject;
import net.sf.myway.calibrator.CalibratorPlugin;
import net.sf.myway.calibrator.da.entities.ScannedMap;
import net.sf.myway.calibrator.da.entities.props.ScannedMapPropertySource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
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
 * @version $Revision: 1.6 $
 */
public class ScannedMapEditor extends EditorPart {

    public static final String ID = "net.sf.myway.calibrator.editor.scannedMap";

    private ScrollingGraphicalViewer _viewer;

    private ScalableRootEditPart _root;

    private EditDomain _editDomain;

    private boolean _dirty;

    private final CommandStackListener commandStackListener = new CommandStackListener() {

        @Override
        public void commandStackChanged(final EventObject event) {
            setDirty(getCommandStack().isDirty());
        }
    };

    private PropertySheetPage _propertySheetPage;

    /**
	 * @param parent
	 */
    private ScrollingGraphicalViewer createGraphicalViewer(final Composite parent) {
        final ScrollingGraphicalViewer viewer = new ScrollingGraphicalViewer();
        viewer.createControl(parent);
        _root = new ScalableRootEditPart();
        viewer.setRootEditPart(_root);
        getEditDomain().addViewer(viewer);
        getSite().setSelectionProvider(viewer);
        viewer.setEditPartFactory(getEditPartFactory());
        viewer.setContents(getEditorInput().getAdapter(ScannedMap.class));
        return viewer;
    }

    @Override
    public void createPartControl(final Composite parent) {
        _viewer = createGraphicalViewer(parent);
        setPartName(((ScannedMap) getEditorInput().getAdapter(ScannedMap.class)).getName());
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
        final ScannedMapEditorInput editorInput = (ScannedMapEditorInput) getEditorInput();
        final ScannedMap map = (ScannedMap) editorInput.getAdapter(ScannedMap.class);
        CalibratorPlugin.getDA().save(map);
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
        return new ScannedMapPartFactory();
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

    protected void setDirty(final boolean dirty) {
        if (_dirty != dirty) {
            _dirty = dirty;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    @Override
    public void setFocus() {
        _viewer.setFocus(_root);
    }
}
