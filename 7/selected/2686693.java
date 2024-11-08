package org.mitre.bio.phylo.tree;

/**
 * A class for representing a node (includes clade/branch) in a binary/non-binary
 * rooted/unrooted tree.
 *
 * <P>See  <code>SimpleNode</code> from <a href="http://www.cebl.auckland.ac.nz/pal-project/" PAL:Phylogenetic Analysis Library>
 *
 * @author Korbinian Strimmer
 * @author Alexei Drummond
 * @author Marc Colosimo
 */
public class SimpleNode implements Node {

    /** parent node */
    private Node parent;

    /** number of node as displayed */
    private int number;

    /** sequences associated with node */
    private byte[] sequence;

    /** length of branch to parent node */
    private double length;

    /** standard error of length of branch to parent node */
    private double lengthSE;

    /** height of this node */
    private double height;

    /** identifier of node/associated branch */
    private String identifier;

    private Node[] child;

    /** constructor default node */
    public SimpleNode() {
        parent = null;
        child = null;
        length = 0.0;
        lengthSE = 0.0;
        height = 0.0;
        identifier = "";
        number = 0;
        sequence = null;
    }

    public SimpleNode(String name, double branchLength) {
        this();
        identifier = name;
        length = branchLength;
    }

    /**
	 * Constructor
	 * @param children
	 * @param branchLength
	 * @throws IllegalArgumentException if only one child!
	 */
    protected SimpleNode(Node[] children, double branchLength) {
        this();
        this.child = children;
        if (children.length == 1) {
            throw new IllegalArgumentException("Must have more than one child!");
        }
        for (int i = 0; i < child.length; i++) {
            child[i].setParent(this);
        }
        this.length = branchLength;
    }

    /** constructor used to clone a node and all children */
    public SimpleNode(Node n) {
        this(n, true);
    }

    public void reset() {
        parent = null;
        child = null;
        length = 0.0;
        lengthSE = 0.0;
        height = 0.0;
        identifier = "";
        number = 0;
        sequence = null;
    }

    public SimpleNode(Node n, boolean keepIds) {
        init(n, keepIds);
        for (int i = 0; i < n.getChildCount(); i++) {
            addChild(new SimpleNode(n.getChild(i), keepIds));
        }
    }

    protected void init(Node n) {
        init(n, true);
    }

    /**
	 * Initialized node instance variables based on given Node.
	 * children are ignored.
	 */
    private void init(Node n, boolean keepId) {
        parent = null;
        length = n.getBranchLength();
        lengthSE = n.getBranchLengthSE();
        height = n.getNodeHeight();
        if (keepId) {
            identifier = n.getIdentifier();
        } else {
            identifier = "";
        }
        number = n.getNumber();
        sequence = n.getSequence();
        child = null;
    }

    /**
	 * Returns the parent node of this node.
	 */
    public Node getParent() {
        return parent;
    }

    /** Set the parent node of this node. */
    public void setParent(Node node) {
        parent = node;
    }

    /**
	 * removes parent.
	 */
    public void removeParent() {
        parent = null;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String id) {
        this.identifier = id;
    }

    /**
	 * Returns the sequence at this node, in the form of a String.
	 */
    public String getSequenceString() {
        return new String(sequence);
    }

    /**
	 * Returns the sequence at this node, in the form of an array of bytes.
	 */
    public byte[] getSequence() {
        return sequence;
    }

    /**
	 * Sets the sequence at this node, in the form of an array of bytes.
	 */
    public void setSequence(byte[] s) {
        sequence = s;
    }

    /**
	 * Get the length of the branch attaching this node to its parent.
	 */
    public final double getBranchLength() {
        return length;
    }

    /**
	 * Set the length of the branch attaching this node to its parent.
	 */
    public final void setBranchLength(double value) {
        length = value;
    }

    /**
	 * Get the length SE of the branch attaching this node to its parent.
	 */
    public final double getBranchLengthSE() {
        return lengthSE;
    }

    /**
	 * Set the length SE of the branch attaching this node to its parent.
	 */
    public final void setBranchLengthSE(double value) {
        lengthSE = value;
    }

    /**
	 * Get the height of this node relative to the most recent node.
	 */
    public final double getNodeHeight() {
        return height;
    }

    /**
	 * Set the height of this node relative to the most recent node.
	 * @note corrects children branch lengths
	 */
    public final void setNodeHeight(double value) {
        if (value < 0) {
            height = -value;
        } else {
            height = value;
        }
    }

    /**
	 * Set the height of this node relative to the most recent node.
	 * @param adjustChildBranchLengths if true
	 */
    public final void setNodeHeight(double value, boolean adjustChildBranchLengths) {
        if (value < 0) {
            height = -value;
        } else {
            height = value;
        }
        if (adjustChildBranchLengths && child != null) {
            for (int i = 0; i < child.length; i++) {
                child[i].setBranchLength(height - child[i].getNodeHeight());
            }
        }
    }

    public void setNumber(int n) {
        number = n;
    }

    public int getNumber() {
        return number;
    }

    /**
	 * get child node
	 *
	 * @param n number of child
	 *
	 * @return child node
	 */
    public Node getChild(int n) {
        return child[n];
    }

    /**
	 * set child node
	 *
	 * @param n number
	 * @node node new child node
	 */
    public void setChild(int n, Node node) {
        child[n] = node;
        child[n].setParent(this);
    }

    /**
	 * check whether this node is an internal node
	 *
	 * @return result (true or false)
	 */
    public boolean hasChildren() {
        return !isLeaf();
    }

    /**
	 * check whether this node is an external node
	 *
	 * @return result (true or false)
	 */
    public boolean isLeaf() {
        return (getChildCount() == 0);
    }

    /**
	 * check whether this node is a root node
	 *
	 * @return result (true or false)
	 */
    public boolean isRoot() {
        if (parent == null) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * add new child node
	 *
	 * @param n new child node
	 */
    public void addChild(Node n) {
        insertChild(n, getChildCount());
    }

    /**
	 * add new child node (insertion at a specific position)
	 *
	 * @param n new child node
	 + @param pos position
	 */
    public void insertChild(Node n, int pos) {
        int numChildren = getChildCount();
        Node[] newChild = new Node[numChildren + 1];
        for (int i = 0; i < pos; i++) {
            newChild[i] = child[i];
        }
        newChild[pos] = n;
        for (int i = pos; i < numChildren; i++) {
            newChild[i + 1] = child[i];
        }
        child = newChild;
        n.setParent(this);
    }

    /**
	 * remove child
	 *
	 * @param n number of child to be removed
	 */
    public Node removeChild(int n) {
        int numChildren = getChildCount();
        if (n >= numChildren) {
            throw new IllegalArgumentException("Nonexistent child");
        }
        Node[] newChild = new Node[numChildren - 1];
        for (int i = 0; i < n; i++) {
            newChild[i] = child[i];
        }
        for (int i = n; i < numChildren - 1; i++) {
            newChild[i] = child[i + 1];
        }
        Node removed = child[n];
        removed.setParent(null);
        child = newChild;
        return removed;
    }

    /**
	 * determines the height of this node and its descendants
	 * from branch lengths, assuming contemporaneous tips.
	 */
    public void lengths2HeightsContemp() {
        double largestHeight = 0.0;
        if (!isLeaf()) {
            for (int i = 0; i < getChildCount(); i++) {
                NodeUtils.lengths2Heights(getChild(i));
                double newHeight = getChild(i).getNodeHeight() + getChild(i).getBranchLength();
                if (newHeight > largestHeight) {
                    largestHeight = newHeight;
                }
            }
        }
        setNodeHeight(largestHeight);
    }

    /**
	 * Returns the number of children this node has.
	 */
    public final int getChildCount() {
        if (child == null) return 0;
        return child.length;
    }
}
