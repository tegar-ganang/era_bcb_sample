package rallydemogef;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.EventObject;
import maseconnection.MaseConnection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import cards.CardConstants;
import filesystemaccess.FileSystemUtility;
import table.TableEditPartFactory;
import table.TableModel;
import table.TablePaletteRootFactory;

public class Editor extends GraphicalEditorWithPalette {

    public static final String ID = "rallydemogef.Editor";

    private static PaletteRoot TABLE_ROOT;

    private TableModel tableModel = new TableModel();

    private boolean dirty = false;

    private Editor editor = this;

    private String tooltip = CardConstants.APPLICATIONNAME;

    public Editor() {
        super();
        setEditDomain(new DefaultEditDomain(this));
        this.tableModel = new TableModel();
        tableModel.setEditor(this);
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new TableEditPartFactory());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        ContextMenuProvider provider = new MasePlannerContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
    }

    public void commandStackChanged(EventObject event) {
        dirty = true;
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    @SuppressWarnings("unused")
    private void createOutputStream(OutputStream os) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(getModel());
        oos.close();
    }

    /** This methode modified after the GEF shapes editor provided by 
     * 
     * @return
     */
    protected PaletteViewerProvider createPaletteViewProvider() {
        return new PaletteViewerProvider(getEditDomain()) {

            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }
        };
    }

    private TransferDropTargetListener createTransferDropTargetListener() {
        return new TemplateTransferDropTargetListener(getGraphicalViewer()) {

            protected CreationFactory getFactory(Object template) {
                return new SimpleFactory((Class) template);
            }
        };
    }

    protected void initializeGraphicalViewer() {
        EditPartViewer viewer = getGraphicalViewer();
        ProgressMonitorDialog progress = new ProgressMonitorDialog(null);
        progress.setCancelable(false);
        try {
            if (MaseConnection.useMaseServer() == false) {
                if (FileSystemUtility.getFileSystemUtility().getnewFile()) {
                    this.setPartName(CardConstants.APPLICATIONNAME);
                    tooltip = CardConstants.APPLICATIONNAME;
                } else {
                    tableModel = FileSystemUtility.getFileSystemUtility().getTableModel();
                    this.setPartName(FileSystemUtility.getFileSystemUtility().getFileName());
                    tooltip = FileSystemUtility.getFileSystemUtility().getAbsoluteFileName();
                }
            } else if (MaseConnection.useMaseServer()) {
                try {
                    MaseConnection connection;
                    connection = MaseConnection.getMaseConnection();
                    connection.setTableModel(tableModel);
                    tableModel = connection.getInitialState();
                    this.setPartName(MaseConnection.getMaseConnection().getProjectName());
                } catch (Exception e) {
                    System.err.println("There was an error connecting to mase!");
                    e.printStackTrace();
                }
            }
            tableModel.setEditor(editor);
        } catch (Exception e) {
        }
        viewer.setContents(tableModel);
        viewer.addDropTargetListener(createTransferDropTargetListener());
    }

    public TableModel getModel() {
        return tableModel;
    }

    protected PaletteRoot getPaletteRoot() {
        if (TABLE_ROOT == null) {
            TABLE_ROOT = TablePaletteRootFactory.createPalette();
        }
        return TABLE_ROOT;
    }

    public Object getAdapter(Class type) {
        return super.getAdapter(type);
    }

    @SuppressWarnings("unused")
    private void handleLoadException(Exception e) {
        tableModel = new TableModel();
    }

    /** ************************************************************************************* **/
    @SuppressWarnings("static-access")
    @Override
    public void doSave(IProgressMonitor monitor) {
        FileSystemUtility utility = FileSystemUtility.getFileSystemUtility();
        dirty = !utility.saveFile(this.tableModel);
        if (dirty == false) {
            updateTitleTab();
            this.firePropertyChange(this.PROP_DIRTY);
        }
    }

    @Override
    public void doSaveAs() {
        FileSystemUtility utility = FileSystemUtility.getFileSystemUtility();
        dirty = !utility.saveFileAs(this.tableModel);
        updateTitleTab();
    }

    @Override
    public boolean isSaveAsAllowed() {
        if (MaseConnection.useMaseServer()) return false;
        return true;
    }

    /**
     *  (non-Javadoc)
     * @see org.eclipse.ui.ISaveablePart#isDirty()
     * Removes the save option from the file when a change occures.
     */
    public boolean isDirty() {
        if (MaseConnection.useMaseServer()) return false;
        return dirty;
    }

    @SuppressWarnings("static-access")
    public void setDirty() {
        dirty = true;
        firePropertyChange(this.PROP_DIRTY);
    }

    private void updateTitleTab() {
        this.setPartName(FileSystemUtility.getFileSystemUtility().getFileName());
        if (!FileSystemUtility.getFileSystemUtility().getFileName().equals("")) {
            tooltip = FileSystemUtility.getFileSystemUtility().getAbsoluteFileName();
        }
        firePropertyChange(this.PROP_TITLE);
    }

    /**Warning, eclipse recomends that this method should not be overridden, 
	 * however this was not possible as teh setTitleToolTip() did not change the tooltip.
	 * **/
    public String getTitleToolTip() {
        return tooltip;
    }
}
