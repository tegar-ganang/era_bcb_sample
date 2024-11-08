package org.python.antlr;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.python.core.PyType;
import org.python.antlr.ast.VisitorIF;
import java.util.ArrayList;
import java.util.List;

public class PythonTree extends AST {

    public boolean from_future_checked = false;

    private int charStartIndex = -1;

    private int charStopIndex = -1;

    private CommonTree node;

    /** Who is the parent node of this node; if null, implies node is root */
    private PythonTree parent;

    public PythonTree() {
        node = new CommonTree();
    }

    public PythonTree(PyType subType) {
        node = new CommonTree();
    }

    public PythonTree(Token t) {
        node = new CommonTree(t);
    }

    public PythonTree(int ttype, Token t) {
        CommonToken c = new CommonToken(ttype, t.getText());
        c.setLine(t.getLine());
        c.setTokenIndex(t.getTokenIndex());
        c.setCharPositionInLine(t.getCharPositionInLine());
        c.setChannel(t.getChannel());
        c.setStartIndex(((CommonToken) t).getStartIndex());
        c.setStopIndex(((CommonToken) t).getStopIndex());
        node = new CommonTree(c);
    }

    public PythonTree(PythonTree tree) {
        node = new CommonTree(tree.getNode());
        charStartIndex = tree.getCharStartIndex();
        charStopIndex = tree.getCharStopIndex();
    }

    public CommonTree getNode() {
        return node;
    }

    public Token getToken() {
        return node.getToken();
    }

    public PythonTree dupNode() {
        return new PythonTree(this);
    }

    public boolean isNil() {
        return node.isNil();
    }

    public int getAntlrType() {
        return node.getType();
    }

    public String getText() {
        return node.getText();
    }

    public int getLine() {
        if (node.getToken() == null || node.getToken().getLine() == 0) {
            if (getChildCount() > 0) {
                return getChild(0).getLine();
            }
            return 1;
        }
        return node.getToken().getLine();
    }

    public int getCharPositionInLine() {
        Token token = node.getToken();
        if (token == null || token.getCharPositionInLine() == -1) {
            if (getChildCount() > 0) {
                return getChild(0).getCharPositionInLine();
            }
            return 0;
        } else if (token != null && token.getCharPositionInLine() == -2) {
            return -1;
        }
        return token.getCharPositionInLine();
    }

    public int getTokenStartIndex() {
        return node.getTokenStartIndex();
    }

    public void setTokenStartIndex(int index) {
        node.setTokenStartIndex(index);
    }

    public int getTokenStopIndex() {
        return node.getTokenStopIndex();
    }

    public void setTokenStopIndex(int index) {
        node.setTokenStopIndex(index);
    }

    public int getCharStartIndex() {
        if (charStartIndex == -1 && node.getToken() != null) {
            return ((CommonToken) node.getToken()).getStartIndex();
        }
        return charStartIndex;
    }

    public void setCharStartIndex(int index) {
        charStartIndex = index;
    }

    public int getCharStopIndex() {
        if (charStopIndex == -1 && node.getToken() != null) {
            return ((CommonToken) node.getToken()).getStopIndex() + 1;
        }
        return charStopIndex;
    }

    public void setCharStopIndex(int index) {
        charStopIndex = index;
    }

    public int getChildIndex() {
        return node.getChildIndex();
    }

    public PythonTree getParent() {
        return parent;
    }

    public void setParent(PythonTree t) {
        this.parent = t;
    }

    public void setChildIndex(int index) {
        node.setChildIndex(index);
    }

    @Override
    public String toString() {
        if (isNil()) {
            return "None";
        }
        if (getAntlrType() == Token.INVALID_TOKEN_TYPE) {
            return "<errornode>";
        }
        if (node.getToken() == null) {
            return null;
        }
        return node.getToken().getText() + "(" + this.getLine() + "," + this.getCharPositionInLine() + ")";
    }

    public String toStringTree() {
        if (children == null || children.size() == 0) {
            return this.toString();
        }
        StringBuffer buf = new StringBuffer();
        if (!isNil()) {
            buf.append("(");
            buf.append(this.toString());
            buf.append(' ');
        }
        for (int i = 0; children != null && i < children.size(); i++) {
            PythonTree t = children.get(i);
            if (i > 0) {
                buf.append(' ');
            }
            buf.append(t.toStringTree());
        }
        if (!isNil()) {
            buf.append(")");
        }
        return buf.toString();
    }

    protected String dumpThis(String s) {
        return s;
    }

    protected String dumpThis(Object o) {
        if (o instanceof PythonTree) {
            return ((PythonTree) o).toStringTree();
        }
        return String.valueOf(o);
    }

    protected String dumpThis(Object[] s) {
        StringBuffer sb = new StringBuffer();
        if (s == null) {
            sb.append("null");
        } else {
            sb.append("(");
            for (int i = 0; i < s.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(dumpThis(s[i]));
            }
            sb.append(")");
        }
        return sb.toString();
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        throw new RuntimeException("Unexpected node: " + this);
    }

    public void traverse(VisitorIF<?> visitor) throws Exception {
        throw new RuntimeException("Cannot traverse node: " + this);
    }

    protected List<PythonTree> children;

    public PythonTree getChild(int i) {
        if (children == null || i >= children.size()) {
            return null;
        }
        return children.get(i);
    }

    /** Get the children internal List; note that if you directly mess with
     *  the list, do so at your own risk.
     */
    public List<PythonTree> getChildren() {
        return children;
    }

    public PythonTree getFirstChildWithType(int type) {
        for (int i = 0; children != null && i < children.size(); i++) {
            PythonTree t = children.get(i);
            if (t.getAntlrType() == type) {
                return t;
            }
        }
        return null;
    }

    public int getChildCount() {
        if (children == null) {
            return 0;
        }
        return children.size();
    }

    /** Add t as child of this node.
     *
     *  Warning: if t has no children, but child does
     *  and child isNil then this routine moves children to t via
     *  t.children = child.children; i.e., without copying the array.
     */
    public void addChild(PythonTree t) {
        if (t == null) {
            return;
        }
        PythonTree childTree = t;
        if (childTree.isNil()) {
            if (this.children != null && this.children == childTree.children) {
                throw new RuntimeException("attempt to add child list to itself");
            }
            if (childTree.children != null) {
                if (this.children != null) {
                    int n = childTree.children.size();
                    for (int i = 0; i < n; i++) {
                        PythonTree c = childTree.children.get(i);
                        this.children.add(c);
                        c.setParent(this);
                        c.setChildIndex(children.size() - 1);
                    }
                } else {
                    this.children = childTree.children;
                    this.freshenParentAndChildIndexes();
                }
            }
        } else {
            if (children == null) {
                children = createChildrenList();
            }
            children.add(t);
            childTree.setParent(this);
            childTree.setChildIndex(children.size() - 1);
        }
    }

    /** Add all elements of kids list as children of this node */
    public void addChildren(List<PythonTree> kids) {
        for (int i = 0; i < kids.size(); i++) {
            PythonTree t = kids.get(i);
            addChild(t);
        }
    }

    public void setChild(int i, PythonTree t) {
        if (t == null) {
            return;
        }
        if (t.isNil()) {
            throw new IllegalArgumentException("Can't set single child to a list");
        }
        if (children == null) {
            children = createChildrenList();
        }
        children.set(i, t);
        t.setParent(this);
        t.setChildIndex(i);
    }

    public Object deleteChild(int i) {
        if (children == null) {
            return null;
        }
        PythonTree killed = children.remove(i);
        this.freshenParentAndChildIndexes(i);
        return killed;
    }

    /** Delete children from start to stop and replace with t even if t is
     *  a list (nil-root tree).  num of children can increase or decrease.
     *  For huge child lists, inserting children can force walking rest of
     *  children to set their childindex; could be slow.
     */
    public void replaceChildren(int startChildIndex, int stopChildIndex, Object t) {
        if (children == null) {
            throw new IllegalArgumentException("indexes invalid; no children in list");
        }
        int replacingHowMany = stopChildIndex - startChildIndex + 1;
        int replacingWithHowMany;
        PythonTree newTree = (PythonTree) t;
        List<PythonTree> newChildren = null;
        if (newTree.isNil()) {
            newChildren = newTree.children;
        } else {
            newChildren = new ArrayList<PythonTree>(1);
            newChildren.add(newTree);
        }
        replacingWithHowMany = newChildren.size();
        int numNewChildren = newChildren.size();
        int delta = replacingHowMany - replacingWithHowMany;
        if (delta == 0) {
            int j = 0;
            for (int i = startChildIndex; i <= stopChildIndex; i++) {
                PythonTree child = newChildren.get(j);
                children.set(i, child);
                child.setParent(this);
                child.setChildIndex(i);
                j++;
            }
        } else if (delta > 0) {
            for (int j = 0; j < numNewChildren; j++) {
                children.set(startChildIndex + j, newChildren.get(j));
            }
            int indexToDelete = startChildIndex + numNewChildren;
            for (int c = indexToDelete; c <= stopChildIndex; c++) {
                PythonTree killed = children.remove(indexToDelete);
            }
            freshenParentAndChildIndexes(startChildIndex);
        } else {
            for (int j = 0; j < replacingHowMany; j++) {
                children.set(startChildIndex + j, newChildren.get(j));
            }
            int numToInsert = replacingWithHowMany - replacingHowMany;
            for (int j = replacingHowMany; j < replacingWithHowMany; j++) {
                children.add(startChildIndex + j, newChildren.get(j));
            }
            freshenParentAndChildIndexes(startChildIndex);
        }
    }

    /** Override in a subclass to change the impl of children list */
    protected List<PythonTree> createChildrenList() {
        return new ArrayList<PythonTree>();
    }

    /** Set the parent and child index values for all child of t */
    public void freshenParentAndChildIndexes() {
        freshenParentAndChildIndexes(0);
    }

    public void freshenParentAndChildIndexes(int offset) {
        int n = getChildCount();
        for (int c = offset; c < n; c++) {
            PythonTree child = getChild(c);
            child.setChildIndex(c);
            child.setParent(this);
        }
    }
}
