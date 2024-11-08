package org.sinaxe.graph;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.sinaxe.graph.selection.*;
import org.sinaxe.graph.graphicpanel.*;
import org.sinaxe.graph.model.*;
import org.sinaxe.graph.curve.NearestPointOnCurve;

public class GraphEditor implements MouseListener, MouseMotionListener, GraphModelListener {

    private GraphicPanel graphicPanel = null;

    private GraphModel model = null;

    private LinkedHashSet myGraphicObjects = new LinkedHashSet();

    private LinkedHashMap nodeListeners = new LinkedHashMap();

    private GraphEditorListener graphEditorListener = null;

    private GraphicObject pressedObj = null;

    private Point2D pressedPt = null;

    private Point2D dragPt = null;

    private Point2D pressedStart = null;

    private int pressedCtrl = -1;

    private GraphicObject pressedSource = null;

    private PortConnecter portConnecter = new PortConnecter();

    private int nextNode = 0;

    public GraphEditor() {
        this(null, null);
    }

    public GraphEditor(GraphModel model) {
        this(model, null);
    }

    public GraphEditor(GraphicPanel panel) {
        this(null, panel);
    }

    public GraphEditor(GraphModel model, GraphicPanel panel) {
        setModel(model != null ? model : createModel());
        setGraphicPanel(panel != null ? panel : createGraphicPanel());
        reload();
    }

    public synchronized void setModel(GraphModel model) {
        if (model == null) throw new UnsupportedOperationException("graph GraphModel cannot be null!");
        if (model == this.model) return;
        if (this.model != null) this.model.removeGraphModelListener(this);
        this.model = model;
        model.addGraphModelListener(this);
    }

    public GraphModel getModel() {
        return model;
    }

    public void setSelectionModel(SelectionModel selectionModel) {
        getGraphicPanel().setSelectionModel(selectionModel);
    }

    protected GraphModel createModel() {
        return new DefaultGraphModel();
    }

    public synchronized void setGraphicPanel(GraphicPanel graphicPanel) {
        if (graphicPanel == null) throw new UnsupportedOperationException("GraphEditor.setGraphicPanel() GraphicPanel cannot be null!");
        if (this.graphicPanel != null) {
            this.graphicPanel.removeInternalMouseListener(this);
            this.graphicPanel.removeInternalMouseMotionListener(this);
        }
        this.graphicPanel = graphicPanel;
        graphicPanel.addInternalMouseListener(this);
        graphicPanel.addInternalMouseMotionListener(this);
    }

    public GraphicPanel getGraphicPanel() {
        return graphicPanel;
    }

    protected GraphicPanel createGraphicPanel() {
        return new GraphicPanel();
    }

    protected void addGraphicObject(GraphicObject gob, int layer) {
        myGraphicObjects.add(gob);
        getGraphicPanel().addGraphicObject(gob, layer);
    }

    protected boolean removeGraphicObject(GraphicObject gob) {
        if (myGraphicObjects.remove(gob)) {
            getGraphicPanel().removeGraphicObject(gob);
            return true;
        }
        return false;
    }

    protected void removeAllGraphicObjects() {
        Iterator it = myGraphicObjects.iterator();
        while (it.hasNext()) getGraphicPanel().removeGraphicObject((GraphicObject) it.next());
        myGraphicObjects.clear();
    }

    public List getSelection() {
        Selectable selectionAry[] = getGraphicPanel().getSelectionModel().getSelection();
        ArrayList selection = new ArrayList();
        for (int i = 0; i < selectionAry.length; i++) {
            Selectable s = selectionAry[i];
            if (s instanceof GraphPortModel) {
                if (!(((GraphPortModel) s).getNode().isSelected() || ((GraphPortModel) s).getNode().getGraph().isSelected()) && myGraphicObjects.contains(s)) selection.add(s);
            } else if (s instanceof GraphModel) {
                if (myGraphicObjects.contains(s)) selection.add(s);
            } else if (s instanceof GraphNodeModel) {
                if (!((GraphNodeModel) s).getGraph().isSelected() && myGraphicObjects.contains(s)) selection.add(s);
            }
        }
        for (int i = 0; i < selectionAry.length; i++) {
            Selectable s = selectionAry[i];
            if (s instanceof GraphEdgeModel) {
                GraphEdgeModel edge = (GraphEdgeModel) s;
                if ((edge.getGraph() == null || !edge.getGraph().isSelected()) && myGraphicObjects.contains(s)) selection.add(s);
            }
        }
        return selection;
    }

    public static void lineToBezier(Point2D[] L, int lIdx, Point2D[] R, int rIdx) {
        R[rIdx + 0] = L[lIdx + 0];
        double Ax = L[lIdx + 0].getX();
        double Ay = L[lIdx + 0].getY();
        double Bx = L[lIdx + 1].getX();
        double By = L[lIdx + 1].getY();
        double r = 1.0 / 3.0;
        double q = 2.0 / 3.0;
        R[rIdx + 1] = new Point2D.Double(q * Ax + r * Bx, q * Ay + r * By);
        R[rIdx + 2] = new Point2D.Double(r * Ax + q * Bx, r * Ay + q * By);
        R[rIdx + 3] = L[lIdx + 1];
    }

    public static Point2D[] getActualEdgeControls(GraphEdgeModel edge) {
        Point2D controlPoints[] = edge.getControlPoints();
        Point2D ctrls[] = new Point2D[controlPoints.length + 2];
        ctrls[0] = edge.getHead().getPosition();
        for (int i = 0; i < controlPoints.length; i++) ctrls[i + 1] = controlPoints[i];
        ctrls[ctrls.length - 1] = edge.getTail().getPosition();
        return ctrls;
    }

    public void splitEdgeNearPoint(GraphEdgeModel edge, Point2D pt) {
        Point2D edgeControls[] = getActualEdgeControls(edge);
        Point2D ctrls[];
        if (edgeControls.length < 4) {
            ctrls = new Point2D[4];
            lineToBezier(edgeControls, 0, ctrls, 0);
        } else ctrls = NearestPointOnCurve.splitBezierSetNearPoint(pt, edgeControls);
        Point2D newCtrls[] = new Point2D[ctrls.length - 2];
        for (int i = 0; i < newCtrls.length; i++) newCtrls[i] = ctrls[i + 1];
        edge.setControlPoints(newCtrls);
        repaint(edge.getChangeBBox());
    }

    public void removeControlsAt(GraphEdgeModel edge, int index) {
        Point2D[] newCtrls = null;
        Point2D[] controls = getActualEdgeControls(edge);
        if (controls.length == 2) return;
        if (controls.length == 4) {
            newCtrls = new Point2D[2];
            newCtrls[0] = controls[0];
            newCtrls[1] = controls[3];
        }
        if (controls.length > 4) {
            newCtrls = new Point2D[controls.length - 3];
            int idx = (index / 3) * 3;
            if (index == controls.length - 1) idx -= 3;
            if (idx == 0) idx++;
            for (int i = 0; i < idx; i++) newCtrls[i] = controls[i];
            for (int i = idx + 3; i < controls.length; i++) newCtrls[i - 3] = controls[i];
        }
        if (newCtrls != null) {
            Point2D ctrls[] = new Point2D[newCtrls.length - 2];
            for (int i = 0; i < ctrls.length; i++) ctrls[i] = newCtrls[i + 1];
            edge.setControlPoints(ctrls);
            repaint(edge.getChangeBBox());
        }
    }

    public Point2D getParallelMove(Point2D start, Point2D end, Point2D relative) {
        double x = end.getX() - start.getX() + relative.getX();
        double y = end.getY() - start.getY() + relative.getY();
        return new Point2D.Double(x, y);
    }

    protected void addPort(GraphPortModel port) {
        addGraphicObject(port, 30);
    }

    protected void removePort(GraphPortModel port) {
        removeGraphicObject(port);
    }

    public void movePort(GraphPortModel port, Point2D pos) {
        GraphNodeModel node = port.getNode();
        port.setPosition(pos);
        Rectangle2D bbox = port.getChangeBBox();
        GraphEdgeModel edges[] = port.getEdges();
        for (int i = 0; i < edges.length; i++) {
            edges[i].markDirty();
            bbox.add(edges[i].getChangeBBox());
        }
        repaint(bbox);
    }

    public void moveGraph(GraphModel graph, Point2D pos) {
        Point2D oldPos = graph.getPosition();
        GraphNodeModel nodes[] = graph.getNodes();
        for (int i = 0; i < nodes.length; i++) {
            Point2D nodePos = nodes[i].getPosition();
            nodePos = getParallelMove(oldPos, pos, nodePos);
            moveNode(nodes[i], nodePos);
        }
        GraphEdgeModel edges[] = graph.getEdges();
        for (int i = 0; i < edges.length; i++) {
            Point2D controlPoints[] = edges[i].getControlPoints();
            if (controlPoints.length > 0) {
                Point2D edgePos = controlPoints[0];
                edgePos = getParallelMove(oldPos, pos, edgePos);
                moveEdge(edges[i], edgePos);
            }
        }
        repaint(graph.getChangeBBox());
    }

    public void moveNode(GraphNodeModel node, Point2D pos) {
        Point2D oldPos = node.getPosition();
        node.setPosition(pos);
        Rectangle2D bbox = node.getChangeBBox();
        GraphPortModel ports[] = node.getPorts();
        for (int i = 0; i < ports.length; i++) {
            ports[i].markDirty();
            bbox.add(ports[i].getChangeBBox());
            GraphEdgeModel edges[] = ports[i].getEdges();
            for (int j = 0; j < edges.length; j++) {
                edges[j].markDirty();
                bbox.add(edges[j].getChangeBBox());
            }
        }
        getModel().markDirty();
        repaint(bbox);
    }

    protected void removeNode(GraphNodeModel node) {
        removeGraphicObject(node);
        GraphNodeModelListener listener = (GraphNodeModelListener) nodeListeners.get(node);
        node.removeGraphNodeModelListener(listener);
        GraphPortModel ports[] = node.getPorts();
        for (int i = 0; i < ports.length; i++) removePort(ports[i]);
    }

    private String getNextNodeName() {
        return "node" + nextNode++;
    }

    protected void addNode(GraphNodeModel node) {
        addNode(node, 10);
    }

    protected void addNode(GraphNodeModel node, int layer) {
        if (node.getName() == null) node.setName(getNextNodeName());
        addGraphicObject(node, layer);
        GraphPortModel ports[] = node.getPorts();
        for (int i = 0; i < ports.length; i++) addPort(ports[i]);
        GraphNodeModelListener listener = new DefaultGraphNodeModelListener() {

            public void portsRemoved(GraphEvent event) {
                Object ports[] = event.getObjects();
                for (int i = 0; i < ports.length; i++) {
                    GraphPortModel port = (GraphPortModel) ports[i];
                    removePort(port);
                    repaint(port.getBBox());
                }
            }

            public void portsInserted(GraphEvent event) {
                Object ports[] = event.getObjects();
                for (int i = 0; i < ports.length; i++) {
                    GraphPortModel port = (GraphPortModel) ports[i];
                    addPort(port);
                    repaint(port.getBBox());
                }
            }
        };
        node.addGraphNodeModelListener(listener);
        nodeListeners.put(node, listener);
    }

    public void moveEdge(GraphEdgeModel edge, Point2D pos) {
        Point2D controls[] = edge.getControlPoints();
        Point2D startPos = controls[0];
        for (int i = 0; i < controls.length; i++) {
            Point2D parMove = getParallelMove(startPos, pos, controls[i]);
            moveEdgeControl(edge, i, parMove);
        }
    }

    public void moveEdgeControl(GraphEdgeModel edge, int index, Point2D pos) {
        Point2D controls[] = edge.getControlPoints();
        controls[index] = pos;
        edge.controlsUpdated();
        repaint(edge.getChangeBBox());
    }

    protected void removeEdge(GraphEdgeModel edge) {
        removeGraphicObject(edge);
    }

    protected void addEdge(GraphEdgeModel edge) {
        addGraphicObject(edge, 20);
    }

    public synchronized void clear() {
        nextNode = 0;
        nodeListeners.clear();
        removeAllGraphicObjects();
        getGraphicPanel().repaint();
    }

    protected synchronized void load() {
        addGraphicObject(portConnecter, 100);
        addNode(getModel(), 8);
        GraphNodeModel nodes[] = getModel().getNodes();
        for (int i = 0; i < nodes.length; i++) addNode(nodes[i]);
        GraphEdgeModel edges[] = getModel().getEdges();
        for (int i = 0; i < edges.length; i++) addEdge(edges[i]);
        getGraphicPanel().repaint();
    }

    public synchronized void reload() {
        clear();
        load();
    }

    public void repaint() {
        getGraphicPanel().internalRepaint();
    }

    public void repaint(long tm) {
        getGraphicPanel().internalRepaint(tm);
    }

    public void repaint(Rectangle2D rect) {
        getGraphicPanel().internalRepaint(rect);
    }

    public void repaint(int x, int y, int width, int height) {
        getGraphicPanel().internalRepaint(x, y, width, height);
    }

    public void repaint(long tm, int x, int y, int width, int height) {
        getGraphicPanel().internalRepaint(tm, x, y, width, height);
    }

    protected GraphicObject pointToMyObject(Point2D pt) {
        GraphicObject gob = getGraphicPanel().pointToObject(new Point((int) pt.getX(), (int) pt.getY()));
        if (gob == null || !myGraphicObjects.contains(gob)) return null;
        return gob;
    }

    public void setGraphEditorListener(GraphEditorListener listener) {
        graphEditorListener = listener;
    }

    public GraphEditorListener getGraphEditorListener() {
        return graphEditorListener;
    }

    protected void fireAddGraphEdge(GraphPortModel port1, GraphPortModel port2) {
        graphEditorListener.addGraphEdge(port1, port2);
    }

    protected void fireAddGraphPort(GraphNodeModel node, boolean in, Point2D pos) {
        graphEditorListener.addGraphPort(node, in, pos);
    }

    public synchronized void nodesRemoved(GraphEvent event) {
        Rectangle2D bbox = null;
        Object objs[] = event.getObjects();
        for (int i = 0; i < objs.length; i++) {
            GraphNodeModel node = (GraphNodeModel) objs[i];
            removeNode(node);
            if (bbox == null) bbox = node.getBBox(); else bbox.add(node.getBBox());
        }
        if (model.isDirty()) repaint(model.getChangeBBox());
        repaint(bbox);
    }

    public synchronized void nodesInserted(GraphEvent event) {
        Rectangle2D bbox = null;
        Object objs[] = event.getObjects();
        for (int i = 0; i < objs.length; i++) {
            GraphNodeModel node = (GraphNodeModel) objs[i];
            addNode(node);
            if (bbox == null) bbox = node.getBBox(); else bbox.add(node.getBBox());
        }
        if (model.isDirty()) repaint(model.getChangeBBox());
        repaint(bbox);
    }

    public synchronized void nodesChanged(GraphEvent event) {
        Rectangle2D bbox = null;
        Object objs[] = event.getObjects();
        for (int i = 0; i < objs.length; i++) {
            GraphNodeModel node = (GraphNodeModel) objs[i];
            if (bbox == null) bbox = node.getChangeBBox(); else bbox.add(node.getChangeBBox());
        }
        if (model.isDirty()) repaint(model.getChangeBBox());
        repaint(bbox);
    }

    public synchronized void edgesRemoved(GraphEvent event) {
        Rectangle2D bbox = null;
        Object objs[] = event.getObjects();
        for (int i = 0; i < objs.length; i++) {
            GraphEdgeModel edge = (GraphEdgeModel) objs[i];
            removeEdge(edge);
            if (bbox == null) bbox = edge.getBBox(); else bbox.add(edge.getBBox());
        }
        if (model.isDirty()) repaint(model.getChangeBBox());
        repaint(bbox);
    }

    public synchronized void edgesInserted(GraphEvent event) {
        Rectangle2D bbox = null;
        Object objs[] = event.getObjects();
        for (int i = 0; i < objs.length; i++) {
            GraphEdgeModel edge = (GraphEdgeModel) objs[i];
            addEdge(edge);
            if (bbox == null) bbox = edge.getBBox(); else bbox.add(edge.getBBox());
        }
        if (model.isDirty()) repaint(model.getChangeBBox());
        repaint(bbox);
    }

    public synchronized void edgesChanged(GraphEvent event) {
        Rectangle2D bbox = null;
        Object objs[] = event.getObjects();
        for (int i = 0; i < objs.length; i++) {
            GraphEdgeModel edge = (GraphEdgeModel) objs[i];
            if (bbox == null) bbox = edge.getChangeBBox(); else bbox.add(edge.getChangeBBox());
        }
        if (model.isDirty()) repaint(model.getChangeBBox());
        repaint(bbox);
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == e.BUTTON2) {
            getGraphicPanel().snapToScale();
            getGraphicPanel().snapToDrawing();
        }
        if (e.getClickCount() == 2 && e.getButton() == e.BUTTON1) {
            GraphicObject gob = pointToMyObject(e.getPoint());
            if (gob instanceof GraphEdgeModel) {
                GraphEdgeModel edge = (GraphEdgeModel) gob;
                if (e.isControlDown()) splitEdgeNearPoint(edge, e.getPoint());
                if (e.isShiftDown()) {
                    int index = edge.getIntersectingControlPoint(e.getPoint());
                    removeControlsAt(edge, index);
                }
            }
        }
        if (model.isDirty()) repaint(model.getChangeBBox());
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        pressedPt = e.getPoint();
        dragPt = pressedPt;
        if (e.getButton() == e.BUTTON1) {
            pressedObj = pointToMyObject(e.getPoint());
            pressedCtrl = -1;
            if (pressedObj instanceof GraphEdgeModel) pressedCtrl = ((GraphEdgeModel) pressedObj).getIntersectingControlPoint(pressedPt); else if ((pressedObj instanceof GraphPortModel || pressedObj instanceof GraphNodeModel) && e.isShiftDown()) {
                pressedSource = pressedObj;
                portConnecter.setLine(pressedPt, pressedPt);
                portConnecter.enable();
            } else if (e.isShiftDown()) pressedObj = null;
        } else pressedObj = null;
    }

    public GraphPortModel makePort(GraphNodeModel node, boolean in, Point2D pos) {
        Point2D nodePt = node.getPosition();
        Point2D edgePt = node.getClosestPointOnEdge(pos);
        Point2D relativePt = new Point2D.Double(edgePt.getX() - nodePt.getX(), edgePt.getY() - nodePt.getY());
        fireAddGraphPort(node, in, relativePt);
        Object newObject = pointToMyObject(edgePt);
        if (newObject instanceof GraphPortModel) return (GraphPortModel) newObject;
        System.out.println("Warning: New port not found at (" + pos + ")!");
        return null;
    }

    public void mouseReleased(MouseEvent e) {
        if (portConnecter.isEnabled()) {
            portConnecter.disable();
            repaint(portConnecter.getChangeBBox());
            Object dest = pointToMyObject(e.getPoint());
            if (dest != null && pressedSource != null && dest != pressedSource) {
                GraphPortModel sourcePort = null;
                GraphPortModel destPort = null;
                if (pressedSource instanceof GraphPortModel) sourcePort = (GraphPortModel) pressedSource; else if (pressedSource instanceof GraphNodeModel) sourcePort = makePort((GraphNodeModel) pressedSource, false, pressedPt);
                if (dest instanceof GraphPortModel) destPort = (GraphPortModel) dest; else if (dest instanceof GraphNodeModel) destPort = makePort((GraphNodeModel) dest, true, e.getPoint());
                if (sourcePort != null && destPort != null) fireAddGraphEdge(sourcePort, destPort);
            }
        }
        pressedObj = null;
        pressedPt = null;
        pressedCtrl = -1;
        pressedSource = null;
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        if (pressedObj != null) {
            if (pressedCtrl != -1 && pressedObj instanceof GraphEdgeModel) {
                GraphEdgeModel edge = (GraphEdgeModel) pressedObj;
                Point2D pressedStart = edge.getControlPoints()[pressedCtrl];
                Point2D newPos = e.getPoint();
                newPos = getParallelMove(dragPt, e.getPoint(), pressedStart);
                moveEdgeControl(edge, pressedCtrl, newPos);
            } else if (pressedSource != null) {
                portConnecter.setLine(null, e.getPoint());
                repaint(portConnecter.getChangeBBox());
            } else if (graphicPanel.getSelectionBox() == null) {
                Selectable selection[] = getGraphicPanel().getSelectionModel().getSelection();
                for (int i = 0; i < selection.length; i++) {
                    Selectable obj = selection[i];
                    if (obj instanceof GraphModel) {
                    } else if (obj instanceof GraphNodeModel) {
                        GraphNodeModel node = (GraphNodeModel) obj;
                        pressedStart = node.getPosition();
                        Point2D newPos = getParallelMove(dragPt, e.getPoint(), pressedStart);
                        moveNode(node, newPos);
                    } else if (obj instanceof GraphEdgeModel) {
                        GraphEdgeModel edge = (GraphEdgeModel) obj;
                        Point2D controlPoints[] = edge.getControlPoints();
                        if (controlPoints.length > 0) {
                            pressedStart = controlPoints[0];
                            Point2D newPos = getParallelMove(dragPt, e.getPoint(), pressedStart);
                            moveEdge(edge, newPos);
                        }
                    } else if (obj instanceof GraphPortModel) {
                        GraphPortModel port = (GraphPortModel) obj;
                        if (!port.getNode().isSelected() && selection.length == 1) movePort(port, e.getPoint());
                    }
                }
            }
            if (model.isDirty()) repaint(model.getChangeBBox());
            dragPt = e.getPoint();
        }
    }
}
