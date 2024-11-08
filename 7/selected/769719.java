package sgpdec.tsengine.microstructure;

import java.util.Arrays;
import java.util.BitSet;
import sgpdec.techie.BytePuffer;
import sgpdec.tsengine.Transformation;

/** 
 * Represents a digraph component of a transformation. A transformation digraph 
 * may have several components (subgraphs with no common vertex). A component consists of an inner cycle (might be with length 1)
 * and trees with their root elements in the cycle.
 */
public class TDGComponent {

    /** Records what points are stored in this component. */
    private BitSet contain_element = new BitSet();

    /** One element of the cycle. */
    private CycleMappedPoint cycle;

    private byte cycle_size;

    private byte node_counter = 0;

    /**
     * The constructor creates the component.
     * @param t the transformation from which the transdigraph is made of
     * @param cycle_elements Elements must be in cyclic notation order!
     */
    public TDGComponent(Transformation t, byte[] cycle_elements) {
        cycle_size = (byte) cycle_elements.length;
        cycle = new CycleMappedPoint(cycle_elements[0]);
        if (cycle_size == 1) {
            cycle.setPrevInCycle(cycle);
            cycle.setImage(cycle);
        } else {
            CycleMappedPoint prevtmp = cycle;
            CycleMappedPoint ncmp = null;
            for (int i = 1; i < cycle_size; i++) {
                ncmp = new CycleMappedPoint(cycle_elements[i], prevtmp);
                prevtmp.setImage(ncmp);
                prevtmp = ncmp;
            }
            cycle.setPrevInCycle(ncmp);
            ncmp.setImage(cycle);
        }
        CycleMappedPoint cmp = cycle;
        do {
            buildTree(cmp, t);
            cmp = cmp.getPrevInCycle();
        } while (cmp != cycle);
    }

    public boolean containsPoint(int p) {
        return contain_element.get(p);
    }

    /** TODO!!! to use statesets instead of this!!! */
    public BitSet getPointRegisterBitSet() {
        return contain_element;
    }

    public int size() {
        return contain_element.cardinality();
    }

    public CycleMappedPoint getCycle() {
        return cycle;
    }

    public int cycleSize() {
        return cycle_size;
    }

    public int[] getTags() {
        int[] tags = new int[cycle_size];
        CycleMappedPoint cmp = cycle;
        for (int i = 0; i < tags.length; i++) {
            tags[i] = cmp.getTag();
            cmp = (CycleMappedPoint) cmp.getImage();
        }
        return tags;
    }

    public boolean isIsomorphicTo(TDGComponent tdgc) {
        if ((size() != tdgc.size()) || (cycleSize() != tdgc.cycleSize())) {
            return false;
        }
        int[] tags = getTags();
        int[] otags = tdgc.getTags();
        if (cycleSize() == 1) {
            if (Arrays.equals(tags, otags)) {
                if (isIsomorphicCycle(cycle, tdgc.cycle, 0)) {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            int offset = 0;
            while (offset < tags.length) {
                if (Arrays.equals(tags, otags)) {
                    if (isIsomorphicCycle(cycle, tdgc.cycle, offset)) {
                        return true;
                    } else {
                        rotate(otags);
                        offset++;
                    }
                } else {
                    rotate(otags);
                    offset++;
                }
            }
        }
        return false;
    }

    private boolean isIsomorphicCycle(CycleMappedPoint cmp1, CycleMappedPoint cmp2, int offset) {
        while (offset != 0) {
            cmp2 = (CycleMappedPoint) cmp2.getImage();
            offset--;
        }
        for (int i = 0; i < cycle_size; i++) {
            if (!cmp1.isIsomorphicTo(cmp2)) {
                return false;
            }
            cmp2 = (CycleMappedPoint) cmp2.getImage();
            cmp1 = (CycleMappedPoint) cmp1.getImage();
        }
        return true;
    }

    private void rotate(int[] a) {
        int tmp = a[0];
        for (int i = 0; i < a.length - 1; i++) {
            a[i] = a[i + 1];
        }
        a[a.length - 1] = tmp;
    }

    /**
     * Creates the tree part for a cycle point. The root of the tree is the cyclepoint,
     * the childrens of a node are the points which map to the value of the node. Also
     * fills the @see contain_element bitset.
     */
    private void buildTree(MappedPoint mp, Transformation t) {
        byte val = mp.getValue();
        contain_element.set(val);
        BytePuffer.reset();
        if (mp instanceof CycleMappedPoint) {
            byte preval = ((CycleMappedPoint) mp).getPrevInCycle().getValue();
            for (byte i = 1; i <= t.size(); i++) {
                if ((val == t.mapOnePoint(i)) && (i != preval)) {
                    BytePuffer.add(i);
                    contain_element.set(i);
                }
            }
        } else {
            for (byte i = 1; i <= t.size(); i++) {
                if (val == t.mapOnePoint(i)) {
                    BytePuffer.add(i);
                    contain_element.set(i);
                }
            }
        }
        if (!BytePuffer.isEmpty()) {
            byte[] children = BytePuffer.dump();
            MappedPoint[] preimage = new MappedPoint[children.length];
            for (int i = 0; i < children.length; i++) {
                preimage[i] = new MappedPoint(children[i], mp);
            }
            mp.setPreImage(preimage);
            for (int i = 0; i < preimage.length; i++) {
                buildTree(preimage[i], t);
            }
            byte height = 1;
            byte size = 1;
            for (int i = 0; i < preimage.length; i++) {
                size += preimage[i].size();
                if (height < preimage[i].getHeight()) {
                    height = preimage[i].getHeight();
                }
            }
            height++;
            mp.setSize(size);
            mp.setHeight(height);
        }
    }

    /** for optimizing the size of the picture */
    int lx, hx, ly, hy;

    /**
     * Returns a LaTeX code fragment for drawing the component using the GasTeX package. 
     */
    public String toGastex(boolean draw_nodes) {
        hx = hy = 0;
        lx = ly = Integer.MAX_VALUE;
        return toGastex(draw_nodes, 7, hx - lx, hy - ly, 50 - lx, 50 - ly);
    }

    String toGastex(boolean dn, int r, int xsize, int ysize, int ax, int ay) {
        int x = 0;
        int y = 0;
        node_counter = 0;
        int[] cycle_nodenames = new int[cycle_size];
        StringBuffer sb = new StringBuffer();
        sb.append("\\begin{center}\n\\begin{picture}(" + xsize + "," + ysize + ")(0,0)\n");
        double cycle_slice = (2 * Math.PI) / cycle_size;
        double act_direction = 0.0;
        if (cycle_size == 1) {
            cycle_slice = Math.PI;
        }
        CycleMappedPoint act_cnode = cycle;
        for (int i = 0; i < cycle_size; i++) {
            cycle_nodenames[i] = node_counter;
            x = ax + (int) (r * Math.cos(act_direction));
            y = ay + (int) (r * Math.sin(act_direction));
            if (hx < x) {
                hx = x;
            }
            if (lx > x) {
                lx = x;
            }
            if (hy < y) {
                hy = y;
            }
            if (ly > y) {
                ly = y;
            }
            if (dn) {
                sb.append("\\node[Nadjust=wh](" + (node_counter++) + ")(" + x + "," + y + "){" + act_cnode.getValue() + "}\n");
            } else {
                sb.append("\\node[Nadjust=wh](" + (node_counter++) + ")(" + x + "," + y + "){}\n");
            }
            act_direction += cycle_slice;
            act_cnode = act_cnode.getPrevInCycle();
        }
        if (cycle_size == 1) {
            sb.append("\\drawloop[loopangle=180](" + (node_counter - 1) + "){}\n");
        } else {
            for (int i = 1; i < cycle_size; i++) {
                sb.append("\\drawedge[curvedepth=5](" + cycle_nodenames[i] + "," + cycle_nodenames[i - 1] + "){}\n");
            }
            sb.append("\\drawedge[curvedepth=5](" + cycle_nodenames[0] + "," + cycle_nodenames[cycle_size - 1] + "){}\n");
        }
        act_direction = 0.0;
        act_cnode = cycle;
        for (int i = 0; i < cycle_size; i++) {
            x = ax + (int) (r * Math.cos(act_direction));
            y = ay + (int) (r * Math.sin(act_direction));
            gastexDrawTree(dn, cycle_nodenames[i], act_cnode, act_direction - (cycle_slice / 2.0), act_direction + (cycle_slice / 2.0), x, y, r + 3, sb);
            act_direction += cycle_slice;
            act_cnode = act_cnode.getPrevInCycle();
        }
        sb.append("\\end{picture}\n \\end{center}\n");
        return sb.toString();
    }

    void gastexDrawTree(boolean dn, int parentnodeid, MappedPoint mp, double sangle, double eangle, int ox, int oy, int r, StringBuffer sb) {
        MappedPoint[] children = mp.getChildren();
        if (children.length == 0) {
            return;
        }
        if (!(mp instanceof CycleMappedPoint) && ((sangle - eangle) == Math.PI)) {
            eangle = (eangle + sangle) / 2.0 + ((eangle + sangle) / 4.0);
            sangle = (eangle + sangle) / 2.0 - ((eangle + sangle) / 4.0);
        }
        double anglestep = (eangle - sangle) / (children.length - 1);
        double used_angle = sangle;
        if (children.length == 1) {
            used_angle = (eangle + sangle) / 2.0;
        }
        int x = 0;
        int y = 0;
        for (int i = 0; i < children.length; i++) {
            x = ox + (int) (r * Math.cos(used_angle));
            y = oy + (int) (r * Math.sin(used_angle));
            if (hx < x) {
                hx = x;
            }
            if (lx > x) {
                lx = x;
            }
            if (hy < y) {
                hy = y;
            }
            if (ly > y) {
                ly = y;
            }
            if (dn) {
                sb.append("\\node[Nadjust=wh](" + (node_counter++) + ")(" + x + "," + y + "){" + children[i].getValue() + "}\n");
            } else {
                sb.append("\\node[Nadjust=wh](" + (node_counter++) + ")(" + x + "," + y + "){}\n");
            }
            sb.append("\\drawedge(" + (node_counter - 1) + "," + parentnodeid + "){}\n");
            gastexDrawTree(dn, (node_counter - 1), children[i], sangle, eangle, x, y, r, sb);
            used_angle += anglestep;
        }
    }
}
