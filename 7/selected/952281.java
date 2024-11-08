package org.argouml.uml.diagram.botl_obj_src.layout;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import org.apache.log4j.Category;
import org.argouml.uml.diagram.layout.LayoutedObject;
import org.argouml.uml.diagram.layout.Layouter;
import org.argouml.uml.diagram.ui.UMLDiagram;
import org.tigris.gef.presentation.FigEdge;
import org.tigris.gef.presentation.FigNode;
import ru.novosoft.uml.foundation.core.MAbstraction;
import ru.novosoft.uml.foundation.core.MClassifier;
import ru.novosoft.uml.foundation.core.MDependency;
import ru.novosoft.uml.foundation.core.MGeneralizableElement;
import ru.novosoft.uml.foundation.core.MGeneralization;
import ru.novosoft.uml.foundation.core.MModelElement;
import ru.novosoft.uml.foundation.extension_mechanisms.MStereotype;

/**
 * This class implements a layout algoritms for class diagrams.
 */
public class BOTLObjectSourceDiagramLayouter implements Layouter {

    /** Category for logging events */
    public static final Category cat = Category.getInstance("org.argouml.uml.diagram.static_structure.layout.BOTLRuleDiagramLayouter");

    /** stores the current diagram *
     */
    private UMLDiagram diagram;

    /** stores all the nodes which will be layouted
     */
    Vector nodes;

    /**
     * This constructor is mainly for convenience, so we don't have
     * add every node manually to the layouter.
     */
    public BOTLObjectSourceDiagramLayouter(UMLDiagram theDiagram) {
        this.diagram = theDiagram;
        nodes = diagram.getLayer().getContents();
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.elementAt(i) instanceof FigNode) {
                add(new BOTLObjectSourceDiagramNode((FigNode) (nodes.elementAt(i))));
            }
            if (nodes.elementAt(i) instanceof FigEdge) {
                add(BOTLObjectSourceDiagramModelElementFactory.SINGLETON.getInstance(nodes.elementAt(i)));
            }
        }
        _layoutedClassNodes = getBOTLRuleDiagramNodes();
    }

    /**
     * Add a object to layout.
     *
     * @param obj represents the object to layout.
     */
    public void add(LayoutedObject obj) {
        _layoutedObjects.add(obj);
    }

    /**
     * Add a BOTLRuleDiagramNode to layout.
     *
     * @param obj represents the object to layout.
     */
    public void add(BOTLObjectSourceDiagramNode obj) {
        _layoutedObjects.add(obj);
    }

    /**
     * Remove a object from the layout process.
     *
     * @param obj represents the object to remove.
     */
    public void remove(LayoutedObject obj) {
        _layoutedObjects.remove(obj);
    }

    /**
     * Operation getObjects returns all the objects
     * currently participating in the layout process.
     *
     * @return An array holding all the object in the layouter.
     */
    public LayoutedObject[] getObjects() {
        LayoutedObject[] result = new LayoutedObject[_layoutedObjects.size()];
        _layoutedObjects.copyInto(result);
        return result;
    }

    /**
     * Operation getObject returns a object with a given index from the layouter.
     *
     * @param index represents the index of this object in the layouter.
     * @return The LayoutedObject for the given index.
     */
    public LayoutedObject getObject(int index) {
        return (LayoutedObject) (_layoutedObjects.elementAt(index));
    }

    /**
     * Get a BOTLRuleDiagramNode from the layouted objects.
     *
     * @param index represents the index of this BOTLRuleDiagramNode.
     * @return The BOTLRuleDiagramNode for this index.
     */
    public BOTLObjectSourceDiagramNode getBOTLRuleDiagramNode(int index) {
        return (BOTLObjectSourceDiagramNode) (_layoutedClassNodes.elementAt(index));
    }

    /** extract the BOTLRuleDiagramNodes from all layouted objects */
    private Vector getBOTLRuleDiagramNodes() {
        Vector classNodes = new Vector();
        for (int i = 0; i < _layoutedObjects.size(); i++) if (_layoutedObjects.elementAt(i) instanceof BOTLObjectSourceDiagramNode) classNodes.add(_layoutedObjects.elementAt(i));
        return classNodes;
    }

    /**
     * Operation layout implements the actual layout algorithm
     */
    public void layout() {
        for (int i = 0; i < _layoutedClassNodes.size(); i++) {
            BOTLObjectSourceDiagramNode BOTLRuleDiagramNode = getBOTLRuleDiagramNode(i);
            if (!BOTLRuleDiagramNode.isPackage()) {
                Object node = BOTLRuleDiagramNode.getFigure().getOwner();
                if (node instanceof MModelElement) {
                    Vector specs = new Vector(((MModelElement) node).getClientDependencies());
                    specs.addAll(((MModelElement) node).getSupplierDependencies());
                    for (Iterator iter = specs.iterator(); iter.hasNext(); ) {
                        MDependency dep = (MDependency) iter.next();
                        if (dep instanceof MAbstraction) {
                            MAbstraction abstr = (MAbstraction) dep;
                            MStereotype stereotype = abstr.getStereotype();
                            if ((stereotype != null) && "realize".equals(stereotype.getName())) {
                                Collection clients = abstr.getClients();
                                for (Iterator iter2 = clients.iterator(); iter2.hasNext(); ) {
                                    MModelElement me = (MModelElement) iter2.next();
                                    if (node == me) {
                                        Collection suppliers = abstr.getSuppliers();
                                        for (Iterator iter3 = suppliers.iterator(); iter3.hasNext(); ) {
                                            MModelElement me2 = (MModelElement) iter3.next();
                                            if (me2 instanceof MClassifier) {
                                                BOTLObjectSourceDiagramNode superNode = getBOTLRuleDiagramNode4owner((MClassifier) me2);
                                                if (superNode != null) {
                                                    BOTLRuleDiagramNode.addUplink(superNode);
                                                }
                                            }
                                        }
                                    }
                                }
                                Collection suppliers = abstr.getSuppliers();
                                for (Iterator iter2 = suppliers.iterator(); iter2.hasNext(); ) {
                                    MModelElement me = (MModelElement) iter2.next();
                                    if (node == me) {
                                        clients = abstr.getClients();
                                        for (Iterator iter3 = clients.iterator(); iter3.hasNext(); ) {
                                            MModelElement me2 = (MModelElement) iter3.next();
                                            if (me2 instanceof MClassifier) {
                                                BOTLObjectSourceDiagramNode subNode = getBOTLRuleDiagramNode4owner((MClassifier) me2);
                                                if (subNode != null) {
                                                    BOTLRuleDiagramNode.addDownlink(subNode);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (node instanceof MGeneralizableElement) {
                    Collection gn = ((MClassifier) node).getGeneralizations();
                    for (Iterator iter = gn.iterator(); iter.hasNext(); ) {
                        MGeneralization g = (MGeneralization) iter.next();
                        BOTLObjectSourceDiagramNode superNode = getBOTLRuleDiagramNode4owner((MClassifier) (g.getParent()));
                        if (superNode != null) {
                            BOTLRuleDiagramNode.addUplink(superNode);
                        }
                    }
                    Collection sp = ((MClassifier) node).getSpecializations();
                    for (Iterator iter = sp.iterator(); iter.hasNext(); ) {
                        MGeneralization s = (MGeneralization) iter.next();
                        BOTLObjectSourceDiagramNode subNode = getBOTLRuleDiagramNode4owner((MClassifier) (s.getChild()));
                        if (subNode != null) {
                            BOTLRuleDiagramNode.addDownlink(subNode);
                        }
                    }
                }
            }
        }
        rankPackagesAndMoveClassesBelow();
        layoutPackages();
        weightAndPlaceClasses();
        centerPlacedRows();
        BOTLObjectSourceDiagramEdge.setVGap(_vGap);
        BOTLObjectSourceDiagramEdge.setHGap(_hGap);
        for (int i = 0; i < _layoutedObjects.size(); i++) if (_layoutedObjects.elementAt(i) instanceof BOTLObjectSourceDiagramEdge) ((BOTLObjectSourceDiagramEdge) _layoutedObjects.elementAt(i)).layout();
    }

    private void rankPackagesAndMoveClassesBelow() {
        int currentColumnPosition = 0;
        int currentRow = 0;
        for (int i = 0; i < _layoutedClassNodes.size(); i++) {
            BOTLObjectSourceDiagramNode node = getBOTLRuleDiagramNode(i);
            if (node.isPackage()) {
                if (currentColumnPosition <= _vMax) {
                    node.setRank(currentRow);
                    currentColumnPosition++;
                } else {
                    node.setRank(++currentRow);
                    currentColumnPosition = 0;
                }
            }
        }
        for (int i = 0; i < _layoutedClassNodes.size(); i++) {
            if (getBOTLRuleDiagramNode(i).isPackage() && (getBOTLRuleDiagramNode(i).getRank() > _maxPackageRank)) {
                _maxPackageRank = getBOTLRuleDiagramNode(i).getRank();
            }
        }
        _maxPackageRank++;
        for (int i = 0; i < _layoutedClassNodes.size(); i++) {
            if (!getBOTLRuleDiagramNode(i).isPackage()) getBOTLRuleDiagramNode(i).addRank(_maxPackageRank);
        }
    }

    private void weightAndPlaceClasses() {
        int rows = getRows();
        for (int curRow = _maxPackageRank; curRow < rows; curRow++) {
            xPos = getHGap() / 2;
            BOTLObjectSourceDiagramNode[] rowObject = getObjectsInRow(curRow);
            for (int i = 0; i < rowObject.length; i++) {
                if (curRow == _maxPackageRank) {
                    int nDownlinks = rowObject[i].getDownlinks().size();
                    rowObject[i].setWeight((nDownlinks > 0) ? (1 / nDownlinks) : 2);
                } else {
                    Vector uplinks = rowObject[i].getUplinks();
                    int nUplinks = uplinks.size();
                    if (nUplinks > 0) {
                        float average_col = 0;
                        for (int j = 0; j < uplinks.size(); j++) {
                            average_col += ((BOTLObjectSourceDiagramNode) (uplinks.elementAt(j))).getColumn();
                        }
                        average_col /= nUplinks;
                        rowObject[i].setWeight(average_col);
                    } else {
                        rowObject[i].setWeight(1000);
                    }
                }
            }
            int[] pos = new int[rowObject.length];
            for (int i = 0; i < pos.length; i++) {
                pos[i] = i;
            }
            boolean swapped = true;
            while (swapped) {
                swapped = false;
                for (int i = 0; i < pos.length - 1; i++) {
                    if (rowObject[pos[i]].getWeight() > rowObject[pos[i + 1]].getWeight()) {
                        int temp = pos[i];
                        pos[i] = pos[i + 1];
                        pos[i + 1] = temp;
                        swapped = true;
                    }
                }
            }
            for (int i = 0; i < pos.length; i++) {
                rowObject[pos[i]].setColumn(i);
                if ((i > _vMax) && (rowObject[pos[i]].getUplinks().size() == 0) && (rowObject[pos[i]].getDownlinks().size() == 0)) {
                    if (getColumns(rows - 1) > _vMax) {
                        rows++;
                    }
                    rowObject[pos[i]].setRank(rows - 1);
                } else {
                    rowObject[pos[i]].setLocation(new Point(xPos, yPos));
                    xPos += rowObject[pos[i]].getSize().getWidth() + getHGap();
                }
            }
            yPos += getRowHeight(curRow) + getVGap();
        }
    }

    /** center the rows according to the biggest/widest row.
     * Instead of placing the rows like this:
     * <pre>
     * ABC
     * DEFGH
     * I
     * </pre>
     * place them like this:
     * <pre>
     *   ABC
     *  DEFGH
     *    I
     * </pre>
     *
     * @author Markus Klink
     * @since 0.9.7
     */
    private void centerPlacedRows() {
        double maxRowSizeInPixels = 0;
        int rows = getRows();
        double rowLengths[] = new double[rows];
        for (int curRow = 0; curRow < rows; curRow++) {
            double thisRowLength = 0;
            thisRowLength = getRowWidth(curRow);
            rowLengths[curRow] = thisRowLength;
            if (thisRowLength > maxRowSizeInPixels) maxRowSizeInPixels = thisRowLength;
        }
        int shiftRight = 0;
        for (int curRow = 0; curRow < rows; curRow++) {
            shiftRight = (int) ((maxRowSizeInPixels - rowLengths[curRow]) / 2);
            BOTLObjectSourceDiagramNode[] rowObjects = getObjectsInRow(curRow);
            for (int i = 0; i < rowObjects.length; i++) rowObjects[i].setLocation(new Point((int) (rowObjects[i].getLocation().getX() + shiftRight), (int) (rowObjects[i].getLocation().getY())));
        }
    }

    /** position the packages of the diagram
     */
    void layoutPackages() {
        int rows = getRows();
        xPos = getHGap() / 2;
        yPos = getVGap() / 2;
        cat.debug("Number of rows in layout process: " + rows);
        for (int curRow = 0; curRow < _maxPackageRank; curRow++) {
            cat.debug("Processing row nr: " + curRow);
            xPos = getHGap() / 2;
            BOTLObjectSourceDiagramNode[] rowObject = getObjectsInRow(curRow);
            cat.debug("Objects in this row: " + rowObject.length);
            for (int i = 0; i < rowObject.length; i++) {
                rowObject[i].setColumn(i);
                rowObject[i].setLocation(new Point(xPos, yPos));
                xPos += rowObject[i].getSize().getWidth() + getHGap();
            }
            yPos += getRowHeight(curRow) + getVGap();
        }
    }

    /**
     * Search the nodes in this BOTLRuleDiagram for a node
     * with a given owner.
     *
     * @return The BOTLRuleDiagram node for this owner, if it's in this
     *         diagram, or null if not.
     */
    private BOTLObjectSourceDiagramNode getBOTLRuleDiagramNode4owner(MClassifier m) {
        for (int i = 0; i < _layoutedClassNodes.size(); i++) {
            if (_layoutedClassNodes.elementAt(i) instanceof BOTLObjectSourceDiagramNode) if (getBOTLRuleDiagramNode(i).getFigure().getOwner() == m) return getBOTLRuleDiagramNode(i);
        }
        return null;
    }

    /**
     * Operation getMinimumDiagramSize returns the minimum diagram
     * size after the layout process.
     *
     * @return The minimum diagram size after the layout process.
     */
    public Dimension getMinimumDiagramSize() {
        int width = 0, height = 0;
        for (int i = 0; i < _layoutedObjects.size(); i++) {
            BOTLObjectSourceDiagramNode node = getBOTLRuleDiagramNode(i);
            if (node.getLocation().x + node.getSize().getWidth() + getHGap() / 2 >= width) width = (int) (node.getLocation().x + node.getSize().getWidth() + getHGap() / 2);
            if (node.getLocation().y + node.getSize().getHeight() + getVGap() / 2 >= height) height = (int) (node.getLocation().y + node.getSize().getHeight() + getVGap() / 2);
        }
        return new Dimension(width, height);
    }

    /**
     * Get the number of rows in this diagram.
     *
     * @return The number of rows in this layout.
     */
    private int getRows() {
        int result = 0;
        for (int i = 0; i < _layoutedClassNodes.size(); i++) {
            BOTLObjectSourceDiagramNode node = getBOTLRuleDiagramNode(i);
            if (node.getRank() >= result) result = node.getRank() + 1;
        }
        return result;
    }

    /**
     * calculate the height of the row
     *
     * @param row the row to calculate
     * @return the height
     */
    private int getRowHeight(int row) {
        int currentHeight = 0;
        for (int i = 0; i < _layoutedClassNodes.size(); i++) {
            if ((getBOTLRuleDiagramNode(i)).getRank() == row) {
                if ((getBOTLRuleDiagramNode(i)).getSize().height > currentHeight) currentHeight = (getBOTLRuleDiagramNode(i)).getSize().height;
            }
        }
        return currentHeight;
    }

    /** calculate the width of the row
     *
     * @param row the row to calculate
     * @return the width
     */
    private int getRowWidth(int row) {
        int currentWidth = 0;
        for (int i = 0; i < _layoutedClassNodes.size(); i++) {
            if ((getBOTLRuleDiagramNode(i)).getRank() == row) {
                currentWidth += (getBOTLRuleDiagramNode(i).getSize().width + getHGap());
            }
        }
        return currentWidth;
    }

    /**
     * Get the number of elements in a given row
     *
     * @param row The row to check.
     * @return The number of elements in the given row.
     */
    private int getColumns(int row) {
        int result = 0;
        for (int i = 0; i < _layoutedClassNodes.size(); i++) {
            if ((getBOTLRuleDiagramNode(i)).getRank() == row) result++;
        }
        return result;
    }

    /**
     * Operation getObject InRow returns all the objects for a given row.
     *
     * @param row represents the row of the returned objects.
     */
    private BOTLObjectSourceDiagramNode[] getObjectsInRow(int row) {
        Vector resultBuffer = new Vector();
        for (int i = 0; i < _layoutedClassNodes.size(); i++) {
            if ((getBOTLRuleDiagramNode(i)).getRank() == row) resultBuffer.add(getBOTLRuleDiagramNode(i));
        }
        BOTLObjectSourceDiagramNode[] result = new BOTLObjectSourceDiagramNode[resultBuffer.size()];
        if (resultBuffer.size() > 0) resultBuffer.copyInto(result);
        return result;
    }

    /**
     * Get the vertical gap between nodes.
     *
     * @return The vertical gap between nodes.
     */
    protected int getVGap() {
        return _vGap;
    }

    /**
     * Get the horizontal gap between nodes.
     *
     * @return The horizontal gap between nodes.
     */
    protected int getHGap() {
        return _hGap;
    }

    /**
     * AttributeImpl _layoutedObjects holds the objects to layout.
     */
    private Vector _layoutedObjects = new Vector();

    /** _layoutedClassNodes is a convenience which holds a
     * subset of _layoutedObjects (only ClassNodes)
     */
    private Vector _layoutedClassNodes = new Vector();

    private int _maxPackageRank = -1;

    /**
     * The horizontal gap between nodes.
     */
    private int _hGap = 80;

    /**
     * The vertical gap between nodes.
     */
    private int _vGap = 80;

    /**
     * The maximum of elements in a particular row
     */
    private int _vMax = 5;

    /** internal */
    private int xPos;

    /** internal */
    private int yPos;
}
