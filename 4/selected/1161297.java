package collabed.model.mset;

import collabed.util.Util;
import java.util.TreeMap;
import java.util.Scanner;

public class MSET<S extends Sequence> {

    private static int SUPER_USER = 0;

    TreeMap<INode.ID, INode<S>> iTree;

    STree<S> globalSTree;

    int nodeCount, userNumber;

    private INode<S> superNode;

    private ISegment<S> superSeg;

    private enum Insert {

        EXTEND, SPLIT, AFTER, BEFORE
    }

    public MSET(int userNumber, S emptySeq) {
        if (emptySeq.length() > 0) throw new RuntimeException("Trying to create a non-empty Super INode");
        this.userNumber = userNumber;
        nodeCount = 0;
        iTree = new TreeMap<INode.ID, INode<S>>();
        INode.ID id = new INode.ID(SUPER_USER, 0);
        superNode = new INode<S>(id, emptySeq, null, 0);
        iTree.put(superNode.id, superNode);
        superSeg = superNode.localSTree.root.isegment;
        globalSTree = new STree<S>(superSeg);
        superSeg.globalSNode = globalSTree.root;
    }

    /**
    * This method uses the global search tree to find the first ISegment that
    * contains offset. This means the resulting Cursor always points to the
    * MIDDLE or the END of an ISegment, with one exception -searching for the
    * ZERO offset.
    *
    * When do we update this Cursor to handle the tricky cases or to optimize?
    *
    * The Rules are simple:
    * R1) You can only extend your own INode at the real end- not just the
    * visible end.
    * R2) You may only locally insert at a position that does not previously
    * have an insertion.
    * R3) When a remote insertion clashes with your existing insertions, place
    * it among the others based on user id number.
    *
    * There are two possible meta cases to consider when updating a Cursor.
    * 1) Pointing to the middle of an ISegment is the simple case because we are
    *    guaranteed a new and legal insertion point.
    *    -> Don't update, Split-Insert!
    * 2) Pointing to the start (& end) of the first (& only) ISegment in an
    *    empty tree.
    *    This means you are pointing to the
    *    the start of the empty SUPER INode before it was split and you are
    *    guaranteed to be the first insertion.
    *    -> Don't update and Split-Insert!
    *
    * NOTE:
    * Every NON-ZERO offset search must yield a Cursor with an index!=start
    * of the Cursor's isegment b/c findVisible stops at the end of the last
    * visible isegment. This means NO START MARKERS returned by findVisible
    *
    * Every ZERO offset search MUST yield a Cursor pointing to the start&END
    * of superNode's first empty ISegment.
    * 3) Pointing to the end of an ISegment.
    *   a) IF it is the LAST ISegment in the INode (not just visible last)
    *      1) IF I own it
    *      -> Don't update, Extend!
    *      2) IF I don't own it
    *         Note: we can assume last iseg is not already an end marker because
    *         in that case cur would have pointed to the end of global-previous
    *      -> Don't update, Split-Insert creates an end marker!
    *   b) It is the NOT the LAST ISegment in the INode
    *      NOTE: optimizing by jumping to the end to extend instead of
    *      inserting unnecessarily BUT this is ONLY ALLOWED if you are NOT
    *      editing in DELETE_VIEW mode
    *     1) IF you own cur's inode and all the ISegments after cur.isegment
    *        are HIDDEN AND there are NO insertions (e.g. localNext == globalNext
    *        for every ISegment after cur.isegment)
    *     -> Update to end of last isegment in the localSTree and EXTEND
    *     2) IF the local-next is NOT the global-next then
    *        There is an insertion at the cursor so
    *     -> Uptate to the start of the global-next ISegment without an insertion
    *        at index ZERO (i.e. the next non-start-marker or the right most
    *        ISegment at that offset).
    *        Split-Insert will create a start marker. 
    *     3) Else no insertion at the cursor (i.e. must be a delete or something)
    *     -> Don't Update, insert-after;
    **/
    private Insert updateCursor(Cursor<S> cur) {
        if ((cur.index > cur.isegment.start && cur.index < cur.isegment.end) || (cur.index == cur.isegment.start && globalSTree.root.size == 0)) return Insert.SPLIT;
        STree<S> localSTree = cur.isegment.inode.localSTree;
        SNode<S> localNext = localSTree.next(cur.isegment.localSNode);
        if (localNext == null) {
            if (cur.isegment.inode.id.user == userNumber) return Insert.EXTEND;
            return Insert.SPLIT;
        }
        SNode<S> globalNext = globalSTree.next(cur.isegment.globalSNode);
        if (cur.isegment.inode.id.user == userNumber) {
            ISegment<S> lastSeg = getOptimizedEnd(localSTree, localNext, globalNext);
            if (lastSeg != null) {
                cur.isegment = lastSeg;
                cur.index = lastSeg.end;
                return Insert.EXTEND;
            }
        }
        if (localNext != globalNext) {
            setToRightSide(cur);
            return Insert.SPLIT;
        }
        return Insert.AFTER;
    }

    public RemoteInsert insertByLocal(int offset, S data) {
        Cursor<S> cur = globalSTree.findVisible(offset);
        Util.debugln("insertByLocal-1  " + cur);
        Insert type = updateCursor(cur);
        Util.debugln("insertByLocal- " + type + " - " + cur);
        if (type == Insert.EXTEND) {
            extendNode(cur, data);
            return new RemoteInsert<S>(data, cur.isegment.inode.id, cur.index, true);
        }
        INode.ID id = new INode.ID(userNumber, nodeCount++);
        INode<S> newNode = cur.isegment.inode.insert(id, data, cur.index);
        iTree.put(id, newNode);
        ISegment<S> newSeg = newNode.localSTree.root.isegment;
        switch(type) {
            case SPLIT:
                ISegment<S> rightSeg = splitSegment(cur.isegment, cur.index);
            case AFTER:
                newSeg.globalSNode = globalSTree.insertAfter(cur.isegment.globalSNode, newSeg);
                break;
            case BEFORE:
                throw new RuntimeException("updateCursor does not do Insert.Before");
        }
        globalSTree.updateSize(newSeg.globalSNode.parent.parent, data.length(), true, true);
        globalSTree.redBlackBalance(newSeg.globalSNode);
        return new RemoteInsert<S>(data, cur.isegment.inode.id, cur.index, false);
    }

    public void insertByRemote(RemoteInsert rInsert) {
    }

    /**
    * Returns null if any localNext is VISIBLE or NOT equal to its globalNext
    * but returns the LAST ISegment in the localSTree otherwise.
    **/
    private ISegment<S> getOptimizedEnd(STree<S> localSTree, SNode<S> localNext, SNode<S> globalNext) {
        while (!localNext.isegment.isVisible() && localNext == globalNext) {
            if ((localNext = localSTree.next(localNext)) == null) return globalNext.isegment;
            globalNext = globalSTree.next(globalNext);
        }
        return null;
    }

    /**
    * Returns true only if the Cursor points to the very end of an INode
    * (the end of the last ISegment)
    * owned by user and the Cursor's SNode is not an end-marker.
    **
   private boolean isExtend(Cursor<S> cursor) {
      return
	 //!isEndMarker(cursor.isegment) &&  // concern 1
	 // i am the owner
	 this.userNumber == cursor.isegment.inode.id.user &&
	 // cursor points to the end of the node
	 cursor.index == cursor.isegment.inode.data.length() &&
	 // cursor points to the last segment in the node
	 cursor.isegment.inode.localSTree.next(cursor.isegment.localSNode)!=null;
   }
*/
    private void extendNode(Cursor<S> cursor, S data) {
        INode<S> node = cursor.isegment.inode;
        ISegment<S> seg = cursor.isegment;
        int len = data.length();
        node.data.extend(data);
        seg.end += len;
        globalSTree.updateSize(seg.globalSNode, len, seg.isVisible(), true);
        node.localSTree.updateSize(seg.localSNode, len, seg.isVisible(), true);
    }

    /**
    * This assumes that cur is pointing to the end of the left most ISegment
    * at an offset that may or may not have many ISegment's of size 0 (markers)
    * and udpates cur to the start of the right most ISegment at that offset.
    *                       return
    * [---][][][][][---]     ===>     [---][][][][][---]
    *    _^_                                      _^_
    **/
    private void setToRightSide(Cursor<S> cur) {
        SNode<S> globalNext = globalSTree.next(cur.isegment.globalSNode);
        while (globalNext.isegment.size() == 0) globalNext = globalSTree.next(globalNext);
        cur.isegment = globalNext.isegment;
        cur.index = 0;
    }

    private ISegment<S> splitSegment(ISegment<S> iseg, int index) {
        if (index < iseg.start || index > iseg.end) throw new IndexOutOfBoundsException("Split index=" + index + " is outside of range: " + iseg);
        STree<S> localSTree = iseg.inode.localSTree;
        ISegment<S> newSeg = new ISegment<S>(iseg.inode, index, iseg.end, iseg.isVisible());
        iseg.setEnd(index);
        newSeg.localSNode = localSTree.insertAfter(iseg.localSNode, newSeg);
        localSTree.redBlackBalance(newSeg.localSNode);
        newSeg.globalSNode = globalSTree.insertAfter(iseg.globalSNode, newSeg);
        globalSTree.redBlackBalance(newSeg.globalSNode);
        return newSeg;
    }

    public String toString() {
        return "MSET {\n" + iTree + "\n}";
    }

    public static void main(String[] args) {
        String cmd, usage = "Usage:\n\t(i)nsert OFFSET TEXT\n\t" + "(p)rint aLL ISegments | vISIBLE ISegments | mSET INodes\n\t" + "(q)uit";
        MSET<StringSequence> mset = new MSET<StringSequence>(1, new StringSequence(""));
        Scanner in = new Scanner(System.in);
        System.out.println(usage);
        while (in.hasNext()) {
            cmd = in.next();
            if (cmd.equalsIgnoreCase("q") || cmd.equalsIgnoreCase("QUIT")) return; else if (cmd.equalsIgnoreCase("i") || cmd.equalsIgnoreCase("INSERT")) System.out.println(mset.insertByLocal(in.nextInt(), new StringSequence(in.nextLine().substring(1)))); else if (cmd.equalsIgnoreCase("p") || cmd.equalsIgnoreCase("PRINT")) {
                cmd = in.next();
                if (cmd.equalsIgnoreCase("visible") || cmd.equalsIgnoreCase("v")) System.out.println(mset.globalSTree.toString(true)); else if (cmd.equalsIgnoreCase("all") || cmd.equalsIgnoreCase("a")) System.out.println(mset.globalSTree.toString()); else System.out.println(mset);
            } else System.out.println(usage);
        }
    }
}
