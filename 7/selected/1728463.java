package org.jnet.modelset;

import org.jnet.viewer.JnetConstants;
import org.jnet.viewer.Token;
import org.jnet.viewer.Viewer;
import org.jnet.api.SymmetryInterface;
import org.jnet.g3d.Graphics3D;
import org.jnet.util.Point3fi;
import org.jnet.util.Quaternion;
import org.jnet.util.TextFormat;
import org.nbrowse.utils.ErrorUtil;
import org.nbrowse.views.JnetManager;
import java.io.Serializable;
import java.util.BitSet;
import javax.vecmath.Point2f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;

public final class Node extends Point3fi implements Serializable {

    static final long serialVersionUID = 1L;

    private static final boolean useRawData = false;

    static final byte VIBRATION_VECTOR_FLAG = 0x01;

    static final byte IS_HETERO_FLAG = 0x02;

    static final byte FORMAL_CHARGE_MASK = 0x1C;

    static final byte VALENCY_MASK = (byte) 0xE0;

    Group group;

    int nodeIndex;

    BitSet nodeSymmetry;

    int nodeSite;

    private float userDefinedVanDerWaalRadius;

    boolean collapsed = false;

    int collapsedGroupId = -1;

    int collapseLevel = 0;

    public void setNextCollapseLevel() {
        collapseLevel++;
        collapsed = true;
    }

    public void setPreviousCollapseLevel() {
        if (collapseLevel > 0) {
            collapseLevel--;
            if (collapseLevel == 0) collapsed = false;
        } else collapsed = false;
    }

    public int getCollapseLevel() {
        return collapseLevel;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapseGroupId(int id) {
        collapsedGroupId = id;
    }

    public int getCollapseGroupId() {
        return collapsedGroupId;
    }

    public int getScreenRadius() {
        return screenDiameter / 2;
    }

    short modelIndex;

    private short nodeicAndIsotopeNumber;

    private byte formalChargeAndFlags;

    char alternateLocationID;

    private short madNode;

    /** madNode seems to be diameter? maybe in angstroms? */
    public short getMadNode() {
        return madNode;
    }

    short colixNode;

    short stepNode;

    boolean stepTranslucency = false;

    private short shapeNode = JnetConstants.SHAPE_TYPE_PILL;

    byte paletteID = JnetConstants.PALETTE_CPK;

    Edge[] edges;

    int nEdgesDisplayed = 0;

    int nBackbonesDisplayed = 0;

    public int getNBackbonesDisplayed() {
        return nBackbonesDisplayed;
    }

    int clickabilityFlags;

    int shapeVisibilityFlags;

    /** Whether node is hidden, NODE_IN_MODEL vis flag didnt work - was reset
   * by eval-tainted-setVis, alternatively this could be bit in shapeVisFlg?
   * refactor */
    boolean isHidden = false;

    boolean isSimple = false;

    /** used by polyhedra which nbrowse doesnt use - take out? */
    public boolean isSimple() {
        return isSimple;
    }

    /** this is used by polyhedra which i suspect nbrowse/jnet is not using */
    public Node(Point3f pt) {
        isSimple = true;
        this.x = pt.x;
        this.y = pt.y;
        this.z = pt.z;
        formalChargeAndFlags = 0;
        madNode = 0;
    }

    Node(Viewer viewer, int modelIndex, int nodeIndex, BitSet nodeSymmetry, int nodeSite, short nodeicAndIsotopeNumber, int size, int formalCharge, float x, float y, float z, boolean isHetero, char chainID, char alternateLocationID, float radius) {
        this.modelIndex = (short) modelIndex;
        this.nodeSymmetry = nodeSymmetry;
        this.nodeSite = nodeSite;
        this.nodeIndex = nodeIndex;
        this.nodeicAndIsotopeNumber = nodeicAndIsotopeNumber;
        if (isHetero) formalChargeAndFlags = IS_HETERO_FLAG;
        setFormalCharge(formalCharge);
        this.alternateLocationID = alternateLocationID;
        userDefinedVanDerWaalRadius = radius;
        setMadNode(viewer, size);
        set(x, y, z);
        setNodeInModelVisFlag(true);
    }

    public final void setShapeVisibilityFlags(int flag) {
        shapeVisibilityFlags = flag;
    }

    private void setNodeInModelVisFlag(boolean isVis) {
        setShapeVisibility(JnetConstants.NODE_IN_MODEL, isVis);
    }

    /** flag 1 is NODE_IN_MODEL... 8, 4, ... */
    public final void setShapeVisibility(int shapeVisibilityFlag, boolean isVisible) {
        if (isVisible) {
            shapeVisibilityFlags |= shapeVisibilityFlag;
        } else {
            shapeVisibilityFlags &= ~shapeVisibilityFlag;
        }
    }

    /** This takes jnetConstant int for vis, which converts constant to proper bit
   * that is 4 << shapeId
   * NODE_IN_MODEL should not be used here, its 1 unshifted!
   * @param shapeVisConstant shapeId from jnet constant
   * @param isVis true show, false hide */
    public void setShapeVisConstant(int shapeVisConstant, boolean isVis) {
        setShapeVisibility(JnetConstants.getShapeVisibilityFlag(shapeVisConstant), isVis);
    }

    /** convenience for oft used setting BALLS vis */
    public void setBallsShapeVis(boolean isVis) {
        setShapeVisConstant(JnetConstants.SHAPE_BALLS, isVis);
    }

    public boolean isEdgeed(Node nodeOther) {
        if (edges != null) for (int i = edges.length; --i >= 0; ) if (edges[i].getOtherNode(this) == nodeOther) return true;
        return false;
    }

    public boolean isEdgeed(Node nodeOther, short order) {
        if (edges != null) for (int i = edges.length; --i >= 0; ) if (edges[i].getOtherNode(this) == nodeOther) {
            edges[i].setOrder(order);
            return true;
        }
        return false;
    }

    public Edge getEdge(Node nodeOther) {
        if (edges != null) for (int i = edges.length; --i >= 0; ) if (edges[i].getOtherNode(nodeOther) != null) return edges[i];
        return null;
    }

    void addDisplayedEdge(int stickVisibilityFlag, boolean isVisible) {
        nEdgesDisplayed += (isVisible ? 1 : -1);
        setShapeVisibility(stickVisibilityFlag, isVisible);
    }

    public void addDisplayedBackbone(int backboneVisibilityFlag, boolean isVisible) {
        nBackbonesDisplayed += (isVisible ? 1 : -1);
        setShapeVisibility(backboneVisibilityFlag, isVisible);
    }

    void deleteEdge(Edge edge) {
        for (int i = edges.length; --i >= 0; ) if (edges[i] == edge) {
            deleteEdge(i);
            return;
        }
    }

    private void deleteEdge(int i) {
        int newLength = edges.length - 1;
        if (newLength == 0) {
            edges = null;
            return;
        }
        Edge[] edgesNew = new Edge[newLength];
        int j = 0;
        for (; j < i; ++j) edgesNew[j] = edges[j];
        for (; j < newLength; ++j) edgesNew[j] = edges[j + 1];
        edges = edgesNew;
    }

    void clearEdges() {
        edges = null;
    }

    int getEdgeedNodeIndex(int edgeIndex) {
        return edges[edgeIndex].getOtherNode(this).nodeIndex;
    }

    public void setMadNode(Viewer viewer, int size) {
        if (size == Viewer.DEFAULT_MAD_NODE_VALUE) madNode = Viewer.DEFAULT_NODE_SIZE_BALL; else {
            if (size < Short.MIN_VALUE) size = Short.MIN_VALUE;
            if (size > Short.MAX_VALUE) size = Short.MAX_VALUE;
            madNode = (short) size;
        }
    }

    public short convertEncodedMad(Viewer viewer, int size) {
        switch(size) {
            case 0:
                return 0;
            case -1000:
                int diameter = getBfactor100() * 10 * 2;
                if (diameter > 4000) diameter = 4000;
                size = diameter;
                break;
            case -1001:
                size = (getEdgeingMar() * 2);
                break;
            case -100:
                size = getVanderwaalsMad(viewer);
            default:
                if (size <= Short.MIN_VALUE) {
                    float d = 2000 * getADPMinMax(false);
                    if (size < Short.MIN_VALUE) size = (int) (d * (Short.MIN_VALUE - size) / 100f); else size = (int) d;
                    break;
                } else if (size < -2000) {
                    int iMode = (-size / 1000) - 2;
                    size = (-size) % 1000;
                    size = (int) (size / 50f * viewer.getVanderwaalsMar(nodeicAndIsotopeNumber % 128, iMode));
                } else if (size < 0) {
                    size = -size;
                    if (size > 200) size = 200;
                    size = (int) (size / 100f * getVanderwaalsMad(viewer));
                } else if (size >= Short.MAX_VALUE) {
                    float d = 2000 * getADPMinMax(true);
                    if (size > Short.MAX_VALUE) size = (int) (d * (size - Short.MAX_VALUE) / 100f); else size = (int) d;
                    break;
                } else if (size >= 10000) {
                    size = size - 10000 + getVanderwaalsMad(viewer);
                }
        }
        return (short) size;
    }

    public float getADPMinMax(boolean isMax) {
        Object[] ellipsoid = getEllipsoid();
        if (ellipsoid == null) return 0;
        return ((float[]) ellipsoid[1])[isMax ? 5 : 3];
    }

    public int getRasMolRadius() {
        return Math.abs(madNode / 8);
    }

    public int getCovalentEdgeCount() {
        if (edges == null) return 0;
        int n = 0;
        for (int i = edges.length; --i >= 0; ) if ((edges[i].order & JnetConstants.EDGE_COVALENT_MASK) != 0) ++n;
        return n;
    }

    int getCovalentHydrogenCount() {
        if (edges == null) return 0;
        int n = 0;
        for (int i = edges.length; --i >= 0; ) if ((edges[i].order & JnetConstants.EDGE_COVALENT_MASK) != 0 && (edges[i].getOtherNode(this).getElementNumber()) == 1) ++n;
        return n;
    }

    public Edge[] getEdges() {
        return edges;
    }

    public void setColixNode(short colixNode) {
        this.colixNode = colixNode;
    }

    public void setStepNode(short stepNode) {
        this.stepNode = stepNode;
    }

    public boolean getStepTranslucency() {
        return stepTranslucency;
    }

    public void setStepTranslucency(boolean value) {
        stepTranslucency = value;
    }

    public void setShapeNode(short shapeNode) {
        this.shapeNode = shapeNode;
    }

    public void setPaletteID(byte paletteID) {
        this.paletteID = paletteID;
    }

    public void setTranslucent(boolean isTranslucent, float translucentLevel) {
        colixNode = Graphics3D.getColixTranslucent(colixNode, isTranslucent, translucentLevel);
    }

    public boolean isTranslucent() {
        return Graphics3D.isColixTranslucent(colixNode);
    }

    public short getElementNumber() {
        return (short) (nodeicAndIsotopeNumber % 128);
    }

    public short getIsotopeNumber() {
        return (short) (nodeicAndIsotopeNumber >> 7);
    }

    public short getNodeicAndIsotopeNumber() {
        return nodeicAndIsotopeNumber;
    }

    public String getElementSymbol() {
        return JnetConstants.elementSymbolFromNumber(nodeicAndIsotopeNumber);
    }

    public char getAlternateLocationID() {
        return alternateLocationID;
    }

    boolean isAlternateLocationMatch(String strPattern) {
        if (strPattern == null) return (alternateLocationID == '\0');
        if (strPattern.length() != 1) return false;
        char ch = strPattern.charAt(0);
        return (ch == '*' || ch == '?' && alternateLocationID != '\0' || alternateLocationID == ch);
    }

    public boolean isHetero() {
        return (formalChargeAndFlags & IS_HETERO_FLAG) != 0;
    }

    void setFormalCharge(int charge) {
        if (charge > 7) charge = 7;
        formalChargeAndFlags = (byte) ((formalChargeAndFlags & ~FORMAL_CHARGE_MASK) | (charge << 2));
    }

    void setVibrationVector() {
        formalChargeAndFlags |= VIBRATION_VECTOR_FLAG;
    }

    public int getFormalCharge() {
        return formalChargeAndFlags >> 2;
    }

    public int getOccupancy() {
        byte[] occupancies = group.chain.modelSet.occupancies;
        return occupancies == null ? 100 : occupancies[nodeIndex];
    }

    public int getBfactor100() {
        short[] bfactor100s = group.chain.modelSet.bfactor100s;
        if (bfactor100s == null) return 0;
        return bfactor100s[nodeIndex];
    }

    public boolean setRadius(float radius) {
        return !Float.isNaN(userDefinedVanDerWaalRadius = (radius > 0 ? radius : Float.NaN));
    }

    public void setValency(int nEdges) {
        if (nEdges > 7) nEdges = 7;
        formalChargeAndFlags = (byte) ((formalChargeAndFlags & ~VALENCY_MASK) | ((byte) nEdges << 5));
    }

    public int getValence() {
        int n = (formalChargeAndFlags >> 5) & 7;
        if (n == 0 && edges != null) for (int i = edges.length; --i >= 0; ) n += edges[i].getValence();
        return n;
    }

    public float getDimensionValue(int dimension) {
        return (dimension == 0 ? x : (dimension == 1 ? y : z));
    }

    private int getVanderwaalsMad(Viewer viewer) {
        return (Float.isNaN(userDefinedVanDerWaalRadius) ? viewer.getVanderwaalsMar(nodeicAndIsotopeNumber % 128) * 2 : (int) (userDefinedVanDerWaalRadius * 2000f));
    }

    public float getVanderwaalsRadiusFloat() {
        return (Float.isNaN(userDefinedVanDerWaalRadius) ? group.chain.modelSet.getVanderwaalsMar(nodeicAndIsotopeNumber % 128) / 1000f : userDefinedVanDerWaalRadius);
    }

    short getEdgeingMar() {
        return JnetConstants.getEdgeingMar(nodeicAndIsotopeNumber % 128, getFormalCharge());
    }

    public float getEdgeingRadiusFloat() {
        return getEdgeingMar() / 1000f;
    }

    int getCurrentEdgeCount() {
        return edges == null ? 0 : edges.length;
    }

    public short getColix() {
        return colixNode;
    }

    public short getStep() {
        return stepNode;
    }

    public byte getPaletteID() {
        return paletteID;
    }

    public float getRadius() {
        return Math.abs(madNode / 2000f);
    }

    public short getShape() {
        return shapeNode;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public int getNodeSite() {
        return nodeSite;
    }

    public BitSet getNodeSymmetry() {
        return nodeSymmetry;
    }

    void setGroup(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

    public void transform(Viewer viewer) {
        Point3i screen;
        Vector3f[] vibrationVectors;
        if ((formalChargeAndFlags & VIBRATION_VECTOR_FLAG) == 0 || (vibrationVectors = group.chain.modelSet.vibrationVectors) == null) screen = viewer.transformPoint(this); else screen = viewer.transformPoint(this, vibrationVectors[nodeIndex]);
        if (useRawData) {
            if (!JnetManager.inst().getUseRawLoadData()) {
                screenX = screen.x;
                screenY = screen.y;
                screenZ = screen.z;
            }
        } else {
            screenX = screen.x;
            screenY = screen.y;
            screenZ = screen.z;
        }
        screenDiameter = viewer.scaleToScreen(screenZ, Math.abs(madNode));
    }

    public String getNodeName() {
        return group.chain.modelSet.nodeNames[nodeIndex];
    }

    public int getNodeNumber() {
        int[] nodeSerials = group.chain.modelSet.nodeSerials;
        return (nodeSerials != null ? nodeSerials[nodeIndex] : nodeIndex);
    }

    public boolean isModelVisible() {
        return ((shapeVisibilityFlags & JnetConstants.NODE_IN_MODEL) != 0);
    }

    public int getShapeVisibilityFlags() {
        return shapeVisibilityFlags;
    }

    public boolean isShapeVisible(int shapeVisibilityFlag) {
        return (isModelVisible() && (shapeVisibilityFlags & shapeVisibilityFlag) != 0);
    }

    public float getPartialCharge() {
        float[] partialCharges = group.chain.modelSet.partialCharges;
        return partialCharges == null ? 0 : partialCharges[nodeIndex];
    }

    public float getStraightness() {
        return group.getStraightness();
    }

    public Object[] getEllipsoid() {
        return group.chain.modelSet.getEllipsoid(nodeIndex);
    }

    /**
    * Given a symmetry operation number, the set of cells in the model, and the
    * number of operations, this method returns either 0 or the cell number (555, 666)
    * of the translated symmetry operation corresponding to this node.
    * 
    * nodeSymmetry is a bitset that is created in adapter.smarter.NodeSetCollection
    * 
    * It is arranged as follows:
    * 
    * |--overall--|---cell1---|---cell2---|---cell3---|...
    * 
    * |012..nOps-1|012..nOps-1|012..nOp-1s|012..nOps-1|...
    * 
    * If a bit is set, it means that the node was created using that operator
    * operating on the base file set and translated for that cell.
    * 
    * If any bit is set in any of the cell blocks, then the same
    * bit will also be set in the overall block. This allows for
    * rapid determination of special positions and also of
    * node membership in any operation set.
    * 
    *  Note that it is not necessarily true that an node is IN the designated
    *  cell, because one can load {nnn mmm 0}, and then, for example, the {-x,-y,-z}
    *  operator sends nodes from 555 to 444. Still, those nodes would be marked as
    *  cell 555 here, because no translation was carried out. 
    *  
    *  That is, the numbers 444 in symop=3444 do not refer to a cell, per se. 
    *  What they refer to is the file-designated operator plus a translation of
    *  {-1 -1 -1/1}. 
    * 
    * @param symop        = 0, 1, 2, 3, ....
    * @param cellRange    = {444, 445, 446, 454, 455, 456, .... }
    * @param nOps         = 2 for x,y,z;-x,-y,-z, for example
    * @return cell number such as 565
    */
    public int getSymmetryTranslation(int symop, int[] cellRange, int nOps) {
        int pt = symop;
        for (int i = 0; i < cellRange.length; i++) if (nodeSymmetry.get(pt += nOps)) return cellRange[i];
        return 0;
    }

    /**
    * Looks for a match in the cellRange list for this node within the specified translation set
    * select symop=0NNN for this
    * 
    * @param cellNNN
    * @param cellRange
    * @param nOps
    * @return     matching cell number, if applicable
    */
    public int getCellTranslation(int cellNNN, int[] cellRange, int nOps) {
        int pt = nOps;
        for (int i = 0; i < cellRange.length; i++) for (int j = 0; j < nOps; j++, pt++) if (nodeSymmetry.get(pt) && cellRange[i] == cellNNN) return cellRange[i];
        return 0;
    }

    private String getSymmetryOperatorList() {
        String str = "";
        ModelSet f = group.chain.modelSet;
        if (nodeSymmetry == null || f.unitCells == null || f.unitCells[modelIndex] == null) return "";
        int[] cellRange = f.getModelCellRange(modelIndex);
        if (cellRange == null) return "";
        int nOps = f.getModelSymmetryCount(modelIndex);
        int pt = nOps;
        for (int i = 0; i < cellRange.length; i++) for (int j = 0; j < nOps; j++) if (nodeSymmetry.get(pt++)) str += "," + (j + 1) + "" + cellRange[i];
        return str.substring(1);
    }

    public int getModelIndex() {
        return modelIndex;
    }

    public int getMoleculeNumber() {
        return (group.chain.modelSet.getMoleculeIndex(nodeIndex) + 1);
    }

    String getClientNodeStringProperty(String propertyName) {
        Object[] clientNodeReferences = group.chain.modelSet.clientNodeReferences;
        return ((clientNodeReferences == null || clientNodeReferences.length <= nodeIndex) ? null : (group.chain.modelSet.viewer.getClientNodeStringProperty(clientNodeReferences[nodeIndex], propertyName)));
    }

    public byte getSpecialNodeID() {
        byte[] specialNodeIDs = group.chain.modelSet.specialNodeIDs;
        return specialNodeIDs == null ? 0 : specialNodeIDs[nodeIndex];
    }

    public float getFractionalCoord(char ch) {
        Point3f pt = getFractionalCoord();
        return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
    }

    public Point3f getFractionalCoord() {
        SymmetryInterface[] c = group.chain.modelSet.unitCells;
        if (c == null) return this;
        Point3f pt = new Point3f(this);
        c[modelIndex].toFractional(pt);
        return pt;
    }

    void setFractionalCoord(int tok, float fValue) {
        SymmetryInterface[] c = group.chain.modelSet.unitCells;
        if (c != null) c[modelIndex].toFractional(this);
        switch(tok) {
            case Token.fracX:
                x = fValue;
                break;
            case Token.fracY:
                y = fValue;
                break;
            case Token.fracZ:
                z = fValue;
                break;
        }
        if (c != null) c[modelIndex].toCartesian(this);
    }

    void setFractionalCoord(Point3f ptNew) {
        set(ptNew);
        SymmetryInterface[] c = group.chain.modelSet.unitCells;
        if (c != null) c[modelIndex].toCartesian(this);
    }

    boolean isCursorOnTopOf(int xCursor, int yCursor, int minRadius, Node competitor) {
        int r = screenDiameter / 2;
        if (r < minRadius) r = minRadius;
        int r2 = r * r;
        int dx = screenX - xCursor;
        int dx2 = dx * dx;
        if (dx2 > r2) return false;
        int dy = screenY - yCursor;
        int dy2 = dy * dy;
        int dz2 = r2 - (dx2 + dy2);
        if (dz2 < 0) return false;
        if (competitor == null) return true;
        int z = screenZ;
        int zCompetitor = competitor.screenZ;
        int rCompetitor = competitor.screenDiameter / 2;
        if (z < zCompetitor - rCompetitor) return true;
        int dxCompetitor = competitor.screenX - xCursor;
        int dx2Competitor = dxCompetitor * dxCompetitor;
        int dyCompetitor = competitor.screenY - yCursor;
        int dy2Competitor = dyCompetitor * dyCompetitor;
        int r2Competitor = rCompetitor * rCompetitor;
        int dz2Competitor = r2Competitor - (dx2Competitor + dy2Competitor);
        return (z - Math.sqrt(dz2) < zCompetitor - Math.sqrt(dz2Competitor));
    }

    boolean isCursorOnTopOf(int xCursor, int yCursor, int minRadius, Node competitor, boolean isStretch) {
        if (isHidden()) return false;
        int r = screenDiameter / 2;
        if (isStretch) r = 12;
        if (r < minRadius) r = minRadius;
        int r2 = r * r;
        int dx = screenX - xCursor;
        int dx2 = dx * dx;
        if (dx2 > r2) return false;
        int dy = screenY - yCursor;
        int dy2 = dy * dy;
        int dz2 = r2 - (dx2 + dy2);
        if (dz2 < 0) return false;
        if (competitor == null) return true;
        int z = screenZ;
        int zCompetitor = competitor.screenZ;
        int rCompetitor = competitor.screenDiameter / 2;
        if (z < zCompetitor - rCompetitor) return true;
        int dxCompetitor = competitor.screenX - xCursor;
        int dx2Competitor = dxCompetitor * dxCompetitor;
        int dyCompetitor = competitor.screenY - yCursor;
        int dy2Competitor = dyCompetitor * dyCompetitor;
        int r2Competitor = rCompetitor * rCompetitor;
        int dz2Competitor = r2Competitor - (dx2Competitor + dy2Competitor);
        return (z - Math.sqrt(dz2) < zCompetitor - Math.sqrt(dz2Competitor));
    }

    public String getInfo() {
        return getIdentity(true);
    }

    String getInfoXYZ(boolean useChimeFormat) {
        if (useChimeFormat) {
            String group3 = getGroup3();
            char chainID = getChainID();
            Point3f pt = (group.chain.modelSet.unitCells == null ? null : getFractionalCoord());
            return "Node: " + (group3 == null ? getElementSymbol() : getNodeName()) + " " + getNodeNumber() + (group3 != null && group3.length() > 0 ? (isHetero() ? " Hetero: " : " Group: ") + group3 + " " + getResno() + (chainID != 0 && chainID != ' ' ? " Chain: " + chainID : "") : "") + " Model: " + getModelNumber() + " Coordinates: " + x + " " + y + " " + z + (pt == null ? "" : " Fractional: " + pt.x + " " + pt.y + " " + pt.z);
        }
        return getIdentity(true) + " " + x + " " + y + " " + z;
    }

    private String getIdentityXYZ() {
        return getIdentity(false) + " " + x + " " + y + " " + z;
    }

    private String getIdentity(boolean allInfo) {
        StringBuffer info = new StringBuffer();
        String group3 = getGroup3();
        String seqcodeString = getSeqcodeString();
        char chainID = getChainID();
        if (group3 != null && group3.length() > 0) {
            info.append("[");
            info.append(group3);
            info.append("]");
        }
        if (chainID != 0 && chainID != ' ') {
            info.append(":");
            info.append(chainID);
        }
        if (!allInfo) return info.toString();
        if (info.length() > 0) info.append(".");
        info.append(getNodeName());
        if (info.length() == 0) {
            info.append(getElementSymbol());
            info.append(" ");
            info.append(getNodeNumber());
        }
        if (alternateLocationID != 0) {
            info.append("%");
            info.append(alternateLocationID);
        }
        if (group.chain.modelSet.getModelCount() > 1) {
            info.append("/");
            info.append(getModelNumberForLabel());
        }
        info.append(" #");
        info.append(getNodeNumber());
        return info.toString();
    }

    String getGroup3() {
        return group.getGroup3();
    }

    String getGroup1() {
        char c = group.getGroup1();
        return (c == '\0' ? "" : "" + c);
    }

    boolean isGroup3(String group3) {
        return group.isGroup3(group3);
    }

    boolean isProtein() {
        return group.isProtein();
    }

    boolean isCarbohydrate() {
        return group.isCarbohydrate();
    }

    boolean isNucleic() {
        return group.isNucleic();
    }

    boolean isDna() {
        return group.isDna();
    }

    boolean isRna() {
        return group.isRna();
    }

    boolean isPurine() {
        return group.isPurine();
    }

    boolean isPyrimidine() {
        return group.isPyrimidine();
    }

    int getSeqcode() {
        return group.getSeqcode();
    }

    public int getResno() {
        return group.getResno();
    }

    public boolean isClickable() {
        if (!isVisible()) return false;
        int flags = shapeVisibilityFlags | group.shapeVisibilityFlags;
        return ((flags & clickabilityFlags) != 0);
    }

    public int getClickabilityFlags() {
        return clickabilityFlags;
    }

    public void setClickable(int flag) {
        if (flag == 0) clickabilityFlags = 0; else clickabilityFlags |= flag;
    }

    /** Whether node is hidden, NODE_IN_MODEL vis flag didnt work - was reset
   * by eval-tainted-setVis, alternatively this could be bit in shapeVisFlg?
   * refactor - and get isVis and isHidden on same page! */
    public boolean isHidden() {
        return isHidden;
    }

    public void setIsHidden(boolean hide) {
        isHidden = hide;
    }

    /**
   * determine if an node or its PDB group is visible
   * @return true if the node is in the "select visible" set
   */
    public boolean isVisible() {
        if (!isModelVisible() || group.chain.modelSet.isNodeHidden(nodeIndex)) return false;
        int flags = shapeVisibilityFlags;
        flags |= group.shapeVisibilityFlags;
        return ((flags & ~JnetConstants.NODE_IN_MODEL) != 0);
    }

    public float getGroupPhi() {
        return group.phi;
    }

    public float getGroupPsi() {
        return group.psi;
    }

    public char getChainID() {
        return group.chain.chainID;
    }

    public int getSurfaceDistance100() {
        return group.chain.modelSet.getSurfaceDistance100(nodeIndex);
    }

    public Vector3f getVibrationVector() {
        return group.chain.modelSet.getVibrationVector(nodeIndex);
    }

    public int getPolymerLength() {
        return group.getBioPolymerLength();
    }

    public Quaternion getQuaternion(char qtype) {
        return group.getQuaternion(qtype);
    }

    int getPolymerIndex() {
        return group.getBioPolymerIndex();
    }

    public int getSelectedGroupCountWithinChain() {
        return group.chain.getSelectedGroupCount();
    }

    public int getSelectedGroupIndexWithinChain() {
        return group.getSelectedGroupIndex();
    }

    public int getSelectedMonomerCountWithinPolymer() {
        return group.getSelectedMonomerCount();
    }

    public int getSelectedMonomerIndexWithinPolymer() {
        return group.getSelectedMonomerIndex();
    }

    Chain getChain() {
        return group.chain;
    }

    String getModelNumberForLabel() {
        return group.chain.modelSet.getModelNumberForNodeLabel(modelIndex);
    }

    public int getModelNumber() {
        return group.chain.modelSet.getModelNumber(modelIndex) % 1000000;
    }

    public int getModelFileIndex() {
        return group.chain.model.fileIndex;
    }

    public int getModelFileNumber() {
        return group.chain.modelSet.getModelFileNumber(modelIndex);
    }

    public byte getProteinStructureType() {
        return group.getProteinStructureType();
    }

    public int getProteinStructureID() {
        return group.getProteinStructureID();
    }

    public short getGroupID() {
        return group.groupID;
    }

    String getSeqcodeString() {
        return group.getSeqcodeString();
    }

    int getSeqNumber() {
        return group.getSeqNumber();
    }

    public char getInsertionCode() {
        return group.getInsertionCode();
    }

    public String formatLabel(String strFormat) {
        return formatLabel(strFormat, '\0', null);
    }

    public String formatLabel(String strFormat, char chNode, int[] indices) {
        if (strFormat == null || strFormat.length() == 0) return null;
        String strLabel = "";
        int cch = strFormat.length();
        int ich, ichPercent;
        for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) != -1; ) {
            if (ich != ichPercent) strLabel += strFormat.substring(ich, ichPercent);
            ich = ichPercent + 1;
            try {
                String strT = "";
                float floatT = Float.NaN;
                boolean alignLeft = false;
                if (strFormat.charAt(ich) == '-') {
                    alignLeft = true;
                    ++ich;
                }
                boolean zeroPad = false;
                if (strFormat.charAt(ich) == '0') {
                    zeroPad = true;
                    ++ich;
                }
                char ch;
                int width = 0;
                while ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
                    width = (10 * width) + (ch - '0');
                    ++ich;
                }
                int precision = Integer.MAX_VALUE;
                if (strFormat.charAt(ich) == '.') {
                    ++ich;
                    if ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
                        precision = ch - '0';
                        ++ich;
                    }
                }
                char ch0 = ch = strFormat.charAt(ich++);
                if (chNode != '\0' && ich < cch) {
                    if (strFormat.charAt(ich) != chNode) {
                        strLabel = strLabel + "%";
                        ich = ichPercent + 1;
                        continue;
                    }
                    ich++;
                }
                switch(ch) {
                    case 'A':
                        strT = (alternateLocationID != '\0' ? alternateLocationID + "" : "");
                        break;
                    case 'a':
                        strT = getNodeName();
                        break;
                    case 'C':
                        int formalCharge = getFormalCharge();
                        if (formalCharge > 0) strT = "" + formalCharge + "+"; else if (formalCharge < 0) strT = "" + -formalCharge + "-"; else strT = "0";
                        break;
                    case 'D':
                        strT = "" + (indices == null ? nodeIndex : indices[nodeIndex]);
                        break;
                    case 'e':
                        strT = getElementSymbol();
                        break;
                    case 'E':
                        ch = getInsertionCode();
                        strT = (ch == '\0' ? "" : "" + ch);
                        break;
                    case 'f':
                        floatT = getGroupPhi();
                        break;
                    case 'g':
                        strT = "" + getSelectedGroupIndexWithinChain();
                        break;
                    case 'I':
                        floatT = getEdgeingRadiusFloat();
                        break;
                    case 'i':
                        strT = "" + getNodeNumber();
                        break;
                    case 'L':
                        strT = "" + getPolymerLength();
                        break;
                    case 'l':
                        strT = "" + getElementNumber();
                        break;
                    case 'M':
                        strT = getModelNumberForLabel();
                        break;
                    case 'm':
                        strT = getGroup1();
                        break;
                    case 'N':
                        strT = "" + getMoleculeNumber();
                        break;
                    case 'n':
                        strT = getGroup3();
                        if (strT == null || strT.length() == 0) strT = "UNK";
                        break;
                    case 'o':
                        strT = getSymmetryOperatorList();
                        break;
                    case 'P':
                        floatT = getPartialCharge();
                        break;
                    case 'p':
                        floatT = getGroupPsi();
                        break;
                    case 'q':
                        strT = "" + getOccupancy();
                        break;
                    case 'Q':
                        floatT = getOccupancy() / 100f;
                        break;
                    case 'R':
                        strT = "" + getResno();
                        break;
                    case 'r':
                        strT = getSeqcodeString();
                        break;
                    case 'S':
                        strT = "" + nodeSite;
                        break;
                    case 's':
                    case 'c':
                        ch = getChainID();
                        strT = (ch == '\0' ? "" : "" + ch);
                        break;
                    case 'T':
                        floatT = getStraightness();
                        break;
                    case 't':
                    case 'b':
                        floatT = getBfactor100() / 100f;
                        break;
                    case 'U':
                        strT = getIdentity(true);
                        break;
                    case 'u':
                        floatT = getSurfaceDistance100() / 100f;
                        break;
                    case 'V':
                        floatT = getVanderwaalsRadiusFloat();
                        break;
                    case 'v':
                        ch = (ich < strFormat.length() ? strFormat.charAt(ich++) : '\0');
                        switch(ch) {
                            case 'x':
                            case 'y':
                            case 'z':
                                floatT = group.chain.modelSet.getVibrationCoord(nodeIndex, ch);
                                break;
                            default:
                                if (ch != '\0') --ich;
                                Vector3f v = getVibrationVector();
                                if (v == null) {
                                    floatT = 0;
                                    break;
                                }
                                strT = v.x + " " + v.y + " " + v.z;
                        }
                        break;
                    case 'W':
                        strT = getIdentityXYZ();
                        break;
                    case 'x':
                        floatT = x;
                        break;
                    case 'y':
                        floatT = y;
                        break;
                    case 'z':
                        floatT = z;
                        break;
                    case 'X':
                    case 'Y':
                    case 'Z':
                        floatT = getFractionalCoord(ch);
                        break;
                    case '%':
                        strT = "%";
                        break;
                    case '{':
                        int ichCloseBracket = strFormat.indexOf('}', ich);
                        if (ichCloseBracket > ich) {
                            String propertyName = strFormat.substring(ich, ichCloseBracket);
                            floatT = group.chain.modelSet.viewer.getDataFloat(propertyName, nodeIndex);
                            if (Float.isNaN(floatT)) strT = getClientNodeStringProperty(propertyName);
                            if (strT != null || !Float.isNaN(floatT)) {
                                ich = ichCloseBracket + 1;
                                break;
                            }
                        }
                    default:
                        strT = "%" + ch0;
                }
                if (!Float.isNaN(floatT)) strLabel += TextFormat.format(floatT, width, precision, alignLeft, zeroPad); else if (strT != null) strLabel += TextFormat.format(strT, width, precision, alignLeft, zeroPad);
            } catch (IndexOutOfBoundsException ioobe) {
                ich = ichPercent;
                break;
            }
        }
        strLabel += strFormat.substring(ich);
        if (strLabel.length() == 0) return null;
        return strLabel.intern();
    }

    /** This is failing to be called, the only reason i can think of is that
   * .equals must be final in Point3f
   * @param obj
   * @return
   */
    public boolean equals(Object obj) {
        System.out.println("modelset.Node.equals" + this + obj + (this == obj));
        return (this == obj);
    }

    public int hashCode() {
        return nodeIndex;
    }

    public Node findAromaticNeighbor(BitSet notNodes) {
        for (int i = edges.length; --i >= 0; ) {
            Edge edgeT = edges[i];
            Node a = edgeT.getOtherNode(this);
            if (edgeT.isAromatic() && (notNodes == null || !notNodes.get(a.nodeIndex))) return a;
        }
        return null;
    }

    public Node findAromaticNeighbor(int notNodeIndex) {
        for (int i = edges.length; --i >= 0; ) {
            Edge edgeT = edges[i];
            Node a = edgeT.getOtherNode(this);
            if (edgeT.isAromatic() && a.nodeIndex != notNodeIndex) return a;
        }
        return null;
    }

    public Point3f getScreenf() {
        return new Point3f(screenX, screenY, screenZ);
    }
}
