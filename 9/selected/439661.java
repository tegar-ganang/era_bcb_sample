package com.byterefinery.rmbench.editparts;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import com.byterefinery.rmbench.figures.TableStubTooltipFigure;
import com.byterefinery.rmbench.model.diagram.AbstractDTable;
import com.byterefinery.rmbench.model.diagram.DTableStub;
import com.byterefinery.rmbench.model.diagram.DTableStub.StubConnection;
import com.byterefinery.rmbench.model.schema.Table;
import com.byterefinery.rmbench.operations.AddStubbedTablesOperation;

/**
 * The Editpart for a TableStub, which represents tables from other diagrams that are referenced by
 * a table in the current diagram  
 * 
 * @author Hannes Niederhausen
 *
 */
public class TableStubEditPart extends AbstractTableEditPart {

    private static final int STUB_FIGURE_SIZE = 10;

    private static final int STUB_OWNER_DISTANCE = 15;

    private DTableStub dTableStub;

    private TableStubTooltipFigure tooltip;

    private TableEditPart connectedEditPart;

    private List<StubConnection> targetModelConnections = new ArrayList<StubConnection>();

    private class Listener implements PropertyChangeListener {

        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            if (evt.getPropertyName() == AbstractDTable.PROPERTY_LOCATION) {
                Point location = (Point) evt.getNewValue();
                updateLocation(location);
            } else if (evt.getPropertyName() == DTableStub.PROPERTY_TABLESLIST) {
                initializeTooltip();
            }
        }

        public void register() {
            dTableStub.addPropertyListener(this);
        }

        public void unregister() {
            dTableStub.removePropertyListener(this);
        }
    }

    ;

    private final Listener listener = new Listener();

    public TableStubEditPart(DTableStub table) {
        super();
        this.dTableStub = table;
        targetModelConnections.add(dTableStub.getStubConnection());
    }

    protected IFigure createFigure() {
        IFigure figure = new RectangleFigure();
        figure.setSize(STUB_FIGURE_SIZE, STUB_FIGURE_SIZE);
        figure.setLocation(getLocation());
        figure.setBackgroundColor(ColorConstants.orange);
        initializeTooltip();
        figure.setToolTip(tooltip);
        return figure;
    }

    public Point getLocation() {
        if (connectedEditPart == null) connectedEditPart = (TableEditPart) getViewer().getEditPartRegistry().get(dTableStub.getDTable().getTable());
        Point loc = new Point(connectedEditPart.getLocation());
        loc.x -= STUB_OWNER_DISTANCE;
        loc.y -= STUB_OWNER_DISTANCE;
        connectedEditPart.getFigure().translateToParent(loc);
        dTableStub.setLocation(loc);
        return loc;
    }

    private void initializeTooltip() {
        if (tooltip == null) tooltip = new TableStubTooltipFigure(); else tooltip.removeAll();
        if (!getDTableStub().isValid()) {
            return;
        }
        for (Iterator<Table> it = getDTableStub().foreignKeyTables(); it.hasNext(); ) {
            tooltip.addIncoming((Table) it.next());
        }
        for (Iterator<Table> it = getDTableStub().referenceTables(); it.hasNext(); ) {
            tooltip.addOutgoing((Table) it.next());
        }
        for (Iterator<Table> it = getDTableStub().referenceAndForeignKeyTables(); it.hasNext(); ) {
            tooltip.addInAndOutgoing((Table) it.next());
        }
    }

    public DTableStub getDTableStub() {
        return dTableStub;
    }

    public void activate() {
        super.activate();
        listener.register();
    }

    public void deactivate() {
        super.deactivate();
        listener.unregister();
    }

    public void performRequest(Request req) {
        if (req.getType().equals(REQ_OPEN)) {
            AddStubbedTablesOperation op = new AddStubbedTablesOperation(getDTableStub(), (GraphicalViewer) getViewer());
            op.execute(this);
        }
    }

    protected void createEditPolicies() {
    }

    public List<?> getModelTargetConnections() {
        return targetModelConnections;
    }

    protected AbstractDTable getModelDTable() {
        return dTableStub;
    }

    /**
     * cause this part to set its position relative to the bounds of the owning table parts figure
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the figure width
     * @param height the figure height
     */
    public void setOwnerBounds(int x, int y, int width, int height) {
        Point pos = new Point(x - STUB_OWNER_DISTANCE, y - STUB_OWNER_DISTANCE);
        getModelDTable().setLocation(pos);
    }

    protected void refreshVisuals() {
        getFigure().setLocation(getLocation());
    }
}
