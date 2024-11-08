package neuron;

import java.util.ArrayList;
import java.util.List;

public class Segment {

    /**
	 * Coordinates of points in this segment
	 * 
	 */
    private List<Coord> points;

    /**
	 * Previous segment (null for initial segment)
	 */
    private Segment previousSegment;

    /**
	 * Last coordinate before this segment (i.e. branching point)
	 */
    private Coord startCoordinate;

    /**
	 * Branches continuing from this segment. All null for terminal segment.
	 */
    private Segment[] branch;

    @Override
    public String toString() {
        return "Segment " + points.size() + " coords, " + getBranches().size() + " branches.";
    }

    public Segment() {
        points = new ArrayList<Coord>();
        branch = new Segment[3];
    }

    /**
	 * Set parent (i.e. previous) segment.
	 * 
	 * @param s
	 */
    public void setParent(Segment s) {
        if (s == null || s.getPoints().isEmpty()) {
            previousSegment = null;
            return;
        }
        previousSegment = s;
        startCoordinate = s.lastCoordinate();
    }

    public void addPoint(Coord c) {
        if (c == null) return;
        points.add(c);
    }

    /**
	 * Add branch.
	 * @param s
	 */
    public void addBranch(Segment s) {
        if (s == null) return;
        s.setParent(this);
        if (branch[0] == null) branch[0] = s; else if (branch[1] == null) branch[1] = s; else if (branch[2] == null) branch[2] = s; else throw new RuntimeException("Trying to add branch to a segment with already three branches!");
    }

    /**
	 * Last coordinate in this segment. 
	 * @return
	 */
    public Coord lastCoordinate() {
        if (points.size() == 0) return null;
        return points.get(points.size() - 1);
    }

    /**
	 * first coordinate
	 */
    public Coord firstCoordinate() {
        return points.get(0);
    }

    /**
	 * Collect all segments
	 */
    public void collectSegments(List<Segment> lst) {
        if (points.size() > 0) lst.add(this);
        for (Segment s : branch) if (s != null) s.collectSegments(lst);
    }

    /**
	 * Is terminal?
	 * @return
	 */
    public boolean isTerminalSegment() {
        return (branch[0] == null);
    }

    /**
	 * Is intermediate?
	 * @return
	 */
    public boolean isIntermediateSegment() {
        return (branch[0] != null);
    }

    /**
	 * Is branch? (== intermediate)
	 */
    public boolean isBranch() {
        return isIntermediateSegment();
    }

    /**
	 * Segment length.
	 */
    public double length() {
        if (points.size() == 0) return Double.NaN;
        Coord prev = startCoordinate;
        if (prev == null) prev = points.get(0);
        double len = 0;
        for (int i = 0; i < points.size(); i++) {
            Coord cur = points.get(i);
            len += prev.distanceTo(cur);
            prev = cur;
        }
        return len;
    }

    /**
	 * Get branch. 
	 * @param index
	 * @return
	 */
    public Segment getBranch(int index) {
        return branch[index];
    }

    public void deleteBranch(int index) {
        branch[index] = null;
        for (int i = index; i < 2; i++) {
            branch[i] = branch[i + 1];
        }
    }

    /**
	 * Get all branches of this segment.
	 * @return
	 */
    public List<Segment> getBranches() {
        List<Segment> lst = new ArrayList<Segment>();
        for (int i = 0; i < branch.length; i++) {
            if (branch[i] != null) lst.add(branch[i]);
        }
        return lst;
    }

    public List<Segment> getTerminalSegments() {
        List<Segment> lst = new ArrayList<Segment>();
        if (isTerminalSegment()) {
            lst.add(this);
            return lst;
        }
        for (Segment s : branch) {
            if (s != null) lst.addAll(s.getTerminalSegments());
        }
        return lst;
    }

    public List<Coord> getPoints() {
        return points;
    }

    public Coord getStartCoordinate() {
        return startCoordinate;
    }

    public List<Segment> maxLengthPath() {
        List<Segment> list = new ArrayList<Segment>();
        list.add(this);
        if (isTerminalSegment()) return list;
        double aLen = 0, bLen = 0, cLen = 0;
        if (branch[0] != null) aLen = branch[0].segMaxLen();
        if (branch[1] != null) bLen = branch[1].segMaxLen();
        if (branch[2] != null) cLen = branch[2].segMaxLen();
        if (aLen > bLen && aLen > cLen) {
            list.addAll(branch[0].maxLengthPath());
        } else if (bLen > aLen && bLen > cLen) {
            list.addAll(branch[1].maxLengthPath());
        } else {
            list.addAll(branch[2].maxLengthPath());
        }
        return list;
    }

    /**
	 * Try to determine apical base path. Works like maxLengthPath but with
	 * tests to determine where apical base stops.  
	 * @return
	 */
    public List<Segment> apicalBasePath(double maxPathLen) {
        List<Segment> list = new ArrayList<Segment>();
        list.add(this);
        if (isTerminalSegment()) return list;
        double aLen = 0, bLen = 0, cLen = 0;
        if (branch[0] != null) aLen = branch[0].segMaxLen();
        if (branch[1] != null) bLen = branch[1].segMaxLen();
        if (branch[2] != null) cLen = branch[2].segMaxLen();
        double p, q;
        p = Math.max(aLen, Math.max(bLen, cLen));
        q = aLen;
        if (bLen != 0 && bLen < q) q = bLen;
        if (cLen != 0 && cLen < q) q = cLen;
        System.out.println("a " + aLen + ", b " + bLen + ", ratio " + p / q + ", max" + maxPathLen);
        if (p < maxPathLen * 0.5 && p / q < 5) {
            return list;
        }
        if (aLen > bLen && aLen > cLen) {
            list.addAll(branch[0].apicalBasePath(maxPathLen));
        } else if (bLen > aLen && bLen > cLen) {
            list.addAll(branch[1].apicalBasePath(maxPathLen));
        } else {
            list.addAll(branch[2].apicalBasePath(maxPathLen));
        }
        return list;
    }

    public double segMaxLen() {
        if (isTerminalSegment()) return length();
        double aLen = 0, bLen = 0, cLen = 0;
        if (branch[0] != null) aLen = branch[0].segMaxLen();
        if (branch[1] != null) bLen = branch[1].segMaxLen();
        if (branch[2] != null) cLen = branch[2].segMaxLen();
        return length() + Math.max(aLen, Math.max(bLen, cLen));
    }

    /**
	 * Compute length of path
	 * @param list
	 * @return
	 */
    public static double pathLength(List<Segment> list) {
        double len = 0;
        for (Segment s : list) len += s.length();
        return len;
    }
}
