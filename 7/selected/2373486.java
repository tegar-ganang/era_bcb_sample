package de.jardas.ictk.boardgame;

import de.jardas.ictk.boardgame.ContinuationList;
import de.jardas.ictk.boardgame.Move;

/**
 * ContinuationArrayList uses arrays internally to represent the 
 * banching structure.  Arrays are compacted when null items are
 * created.
 */
public class ContinuationArrayList implements ContinuationList {

    /**the move that precedes all the variations*/
    protected Move departureMove;

    /**array of next moves, allowing for treeing from main line*/
    protected Move[] branches;

    public ContinuationArrayList(Move m) {
        branches = new Move[1];
        setDepartureMove(m);
    }

    public ContinuationArrayList() {
        this(null);
    }

    protected void setDepartureMove(Move m) {
        departureMove = m;
    }

    public Move getDepartureMove() {
        return departureMove;
    }

    public boolean isTerminal() {
        return getMainLine() == null && sizeOfVariations() == 0;
    }

    public boolean setMainLineTerminal() {
        if (branches == null || branches[0] == null) return false; else {
            add(null, true);
            return true;
        }
    }

    public boolean exists(int variation) {
        try {
            if (branches != null && branches[variation] != null) return true; else return false;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    public boolean exists(Move move) {
        return getIndex(move) != -1;
    }

    public boolean hasMainLine() {
        return exists(0);
    }

    public Move getMainLine() {
        if (branches == null) return null; else return branches[0];
    }

    public boolean hasVariations() {
        return sizeOfVariations() > 0;
    }

    public Move get(int i) {
        if (i == 0 && branches == null) return null; else return branches[i];
    }

    public int size() {
        int len = 0;
        if (branches == null) return 0; else {
            len = branches.length;
            if (len == 1 && branches[0] == null) len = 0;
        }
        return len;
    }

    public int sizeOfVariations() {
        if (branches == null) return 0; else return branches.length - 1;
    }

    public void add(Move m, boolean isMain) {
        Move[] tmp = null;
        assert (departureMove == null || departureMove.getHistory().getCurrentMove() == departureMove) : "You cannot add a continuation to moves other than the " + "last move played on the board (History.getCurrentMove())";
        if (branches == null) {
            if (isMain) {
                branches = new Move[1];
                branches[0] = m;
            } else {
                branches = new Move[2];
                branches[1] = m;
            }
        } else if (isMain && branches[0] == null) branches[0] = m; else {
            tmp = new Move[branches.length + 1];
            if (isMain) {
                tmp[0] = m;
                System.arraycopy(branches, 0, tmp, 1, branches.length);
            } else {
                System.arraycopy(branches, 0, tmp, 0, branches.length);
                tmp[tmp.length - 1] = m;
            }
            branches = tmp;
        }
        if (m != null) m.prev = departureMove;
    }

    public void add(Move m) {
        Move[] tmp = null;
        assert (departureMove.getHistory().getCurrentMove() == departureMove) : "You cannot add a continuation to moves other than the " + "last move played on the board (History.getCurrentMove())";
        if (branches == null) {
            branches = new Move[1];
            branches[0] = m;
        } else if (branches.length == 1 && branches[0] == null) branches[0] = m; else {
            tmp = new Move[branches.length + 1];
            System.arraycopy(branches, 0, tmp, 0, branches.length);
            tmp[tmp.length - 1] = m;
        }
        m.prev = departureMove;
    }

    public int getIndex(Move m) {
        int i = 0;
        if (branches == null) return -1;
        for (i = 0; i < branches.length; i++) {
            if (branches[i] == m) break;
        }
        if (i < branches.length) return i; else return -1;
    }

    public Move[] find(Move m) {
        Move[] moves = null;
        int[] idx = findIndex(m);
        if (idx != null) {
            moves = new Move[idx.length];
            for (int i = 0; i < idx.length; i++) moves[i] = branches[idx[i]];
        }
        return moves;
    }

    public int[] findIndex(Move m) {
        int i = 0;
        int count = 0;
        int[] rvalue = null, tmp = null;
        if (branches == null) return null; else {
            tmp = new int[branches.length];
            for (i = 0; i < branches.length; i++) {
                if (branches[i] != null && branches[i].equals(m)) tmp[count++] = i;
            }
            if (count == 0) return null; else if (count == branches.length) return tmp; else {
                rvalue = new int[count];
                System.arraycopy(tmp, 0, rvalue, 0, rvalue.length);
                return rvalue;
            }
        }
    }

    /** compresses the branches array so there are no nulls
    *  in the list except for possibily the main line (0).  
    *  If the list is already
    *  full the list is not affected.  If the list is empty
    *  then no action occurs.
    *  
    *  @return true if compression occured
    *  @return false compression was not needed
    */
    protected boolean compressVariations() {
        boolean compressed = false;
        Move[] compBranches = null;
        int count = 0;
        for (int i = 1; i < branches.length; i++) if (branches[i] != null) count++; else compressed = true;
        if (compressed && count > 0) {
            compBranches = new Move[count + 1];
            count = 0;
            for (int i = 0; i < branches.length; i++) if (i < 1 || branches[i] != null) compBranches[count++] = branches[i];
            branches = compBranches;
        }
        return compressed && count > 0;
    }

    public void remove(int i) {
        if (branches == null) throw new ArrayIndexOutOfBoundsException("no moves to remove");
        if (i == -1) throw new ArrayIndexOutOfBoundsException("move does not exist in continuation list");
        if (branches[i] != null) {
            if (branches[i].isExecuted()) departureMove.getHistory().goTo(departureMove);
            branches[i].dispose();
            branches[i] = null;
            compressVariations();
        }
    }

    public void remove(Move m) {
        remove(getIndex(m));
    }

    public void removeAll() {
        if (branches != null) for (int i = 0; i < branches.length; i++) {
            if (branches[i].isExecuted()) departureMove.getHistory().goTo(departureMove);
            branches[i].dispose();
            branches[i] = null;
        }
        branches = null;
    }

    public void removeAllVariations() {
        if (branches != null && sizeOfVariations() > 0) {
            for (int i = 1; i < branches.length; i++) {
                if (branches[i] != null) {
                    if (branches[i].isExecuted()) departureMove.getHistory().goTo(departureMove);
                    branches[i].dispose();
                    branches[i] = null;
                }
            }
            compressVariations();
        }
    }

    /** reclaims all resources and recursively deletes all branch moves.
    */
    public void dispose() {
        departureMove = null;
        removeAll();
        branches = null;
    }

    public int promote(Move move, int num) {
        if (num < 0) throw new IllegalArgumentException("cannot perform negative promotion");
        Move tmp = null;
        int newIndex = 0;
        int oldIndex = getIndex(move);
        if (oldIndex == -1) throw new NullPointerException("Move is not a current variation"); else move = branches[oldIndex];
        if (num != 0) newIndex = oldIndex - num;
        if (newIndex < 0) throw new ArrayIndexOutOfBoundsException("Move cannot be promoted beyond the main line");
        if (newIndex == 0 && branches[0] == null) {
            branches[oldIndex] = null;
            branches[0] = move;
            compressVariations();
        } else {
            branches[oldIndex] = null;
            for (int i = newIndex; i <= oldIndex; i++) {
                tmp = branches[i];
                branches[i] = move;
                move = tmp;
            }
            compressVariations();
        }
        return newIndex;
    }

    public int demote(Move move, int num) {
        if (num < 0) throw new IllegalArgumentException("cannot perform negative demotion");
        Move tmp = null;
        int newIndex = 0;
        int oldIndex = getIndex(move);
        if (oldIndex == -1) throw new NullPointerException("Move is not a current variation"); else move = branches[oldIndex];
        if (num != 0) newIndex = oldIndex + num; else newIndex = branches.length - 1;
        if (newIndex >= branches.length) throw new ArrayIndexOutOfBoundsException("Move cannot be demoted beyond the end of the list");
        for (int i = oldIndex; i < newIndex; i++) {
            branches[i] = branches[i + 1];
        }
        branches[newIndex] = move;
        return newIndex;
    }

    /** for debugging
    */
    public String dump() {
        StringBuffer sb = new StringBuffer();
        sb.append("branches(").append(branches.length).append("): \n");
        for (int i = 0; i < branches.length; i++) {
            sb.append("   branches[").append(i).append("]:");
            if (branches[i] == null) sb.append("null"); else sb.append(branches[i].toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
