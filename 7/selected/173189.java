package org.pqt.mr2rib.ribtranslator;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;
import org.pqt.mr2rib.GlobalOptions;
import org.pqt.mr2rib.PrivateOptions;
import org.pqt.mr2rib.mrutil.Group;
import org.pqt.mr2rib.mrutil.Vertex;

/**This translates vertices in mental ray format into a series of parameters
 * in a hashtable, suitable for a RIB primitive
 *
 * @author Peter Quint  */
public class VertexTranslator extends Translator {

    /** The conversted state of the vertices - a parameter list in RIB form, as
     * it is at the start of any motion*/
    public Hashtable parameters = new Hashtable();

    /** The converted state of the vertices as they are at the end of any motion,
     * this will be null if the object does not change shape*/
    public Hashtable parametersAfterMotion = null;

    /**True if these vertices include reference point, Pref information*/
    public boolean hasPref = false;

    /** Creates a new instance of VertexTranslator. This takes the given set of
     *  and creates RIB style parameters for "P", "st", "N". If there are additional
     *  texture vectors these are put into a userst parameter, and if there are user
     *  vectors defined these go in a upoints parameter, and derivatives go into
     *  a derivs parameter. The parameters are named using in line type declarations.
     *  For the texture, user, and derivative values to be converted each vertex must have
     *  the same number of each parameter. E.g. if the first vertex has three texture
     *  vectors associated with it then all the other vertices must have three texture
     *  vectors or the texture vectors will all be ignored.
     * @param vertindices the integer indices into the vertex array for the current object
     * @param g the group which contains the vertex definitions and associated coordinates
     */
    public VertexTranslator(Group g, int[] vertindices) throws RIBException {
        Vector points = new Vector(), movedPoints = new Vector(), normals = new Vector(), s = new Vector(), t = new Vector(), texture = new Vector(), deriv = new Vector(), user = new Vector();
        float[] tmp, mtmp;
        boolean hasMotion = false;
        Util.progressDetail("Converting vertices");
        int[] dminmax = getMinMaxD(g.vertices);
        int[] uminmax = getMinMaxU(g.vertices);
        int[] tminmax = getMinMaxT(g.vertices);
        int[] translator = new int[g.vertices.length];
        Arrays.fill(translator, -1);
        boolean doDerivs = (dminmax[0] == dminmax[1]) && (dminmax[0] > 0);
        boolean doUser = (uminmax[0] == uminmax[1]) && (uminmax[0] > 0);
        boolean doTexture = (tminmax[0] == tminmax[1]) && (tminmax[0] > 0);
        Vertex vv;
        for (int i = 0; i < vertindices.length; i++) {
            if (translator[vertindices[i]] >= 0) vertindices[i] = translator[vertindices[i]]; else {
                vv = g.vertices[vertindices[i]];
                tmp = Util.getTriple(vv.v, g.numberList);
                mtmp = tmp;
                if ((vv.m != null) && (vv.m.length > 0)) {
                    hasMotion = true;
                    mtmp = Util.sum(tmp, Util.getTotal(vv.m, g.numberList, Integer.MAX_VALUE));
                }
                translator[vertindices[i]] = points.size() / 3;
                vertindices[i] = translator[vertindices[i]];
                Util.add(points, tmp);
                Util.add(movedPoints, mtmp);
                if (vv.n >= 0) Util.add(normals, Util.getTriple(vv.n, g.numberList));
                if (doDerivs) {
                    for (int j = 0; j < vv.d.length; j++) Util.add(deriv, Util.getTriple(vv.d[j], g.numberList));
                }
                if (doUser) {
                    for (int j = 0; j < vv.u.length; j++) Util.add(user, Util.getTriple(vv.u[j], g.numberList));
                }
                if (doTexture) {
                    tmp = Util.getDouble(vv.t[0][0], g.numberList);
                    s.add(new Float(tmp[0]));
                    t.add(new Float(tmp[1]));
                    for (int j = 1; j < vv.t.length; j++) {
                        tmp = Util.getTriple(vv.t[j][0], g.numberList);
                        Util.add(texture, tmp);
                    }
                }
            }
        }
        parameters.put("P", Util.extractFloat(points));
        if (normals.size() > 0) parameters.put("N", Util.extractFloat(normals));
        if (s.size() > 0) {
            parameters.put("s", Util.extractFloat(t));
            parameters.put("t", Util.extractFloat(s));
        }
        if (texture.size() > 0) {
            int repeat = tminmax[0] - 1;
            int limit = repeat;
            if (limit > PrivateOptions.maxExtraTextureCoords) limit = PrivateOptions.maxExtraTextureCoords;
            for (int i = 0; i < limit; i++) {
                if (areZero(texture, repeat * 3, i * 3 + 2) || hasPref) {
                    parameters.put("varying float s" + Integer.toString(i + 1), extractMembers(texture, repeat * 3, i * 3 + 1, 1));
                    parameters.put("varying float t" + Integer.toString(i + 1), extractMembers(texture, repeat * 3, i * 3, 1));
                } else {
                    parameters.put(PrivateOptions.PREFDECL, extractMembers(texture, repeat * 3, i * 3, 3));
                    hasPref = true;
                }
            }
        }
        if (hasMotion) {
            parametersAfterMotion = (Hashtable) parameters.clone();
            parametersAfterMotion.put("P", Util.extractFloat(movedPoints));
        } else parametersAfterMotion = null;
    }

    /** Gets the minimum and maximum size of the derivatives array in this
     *array of vertices*/
    private int[] getMinMaxD(Vertex[] v) {
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (int i = 0; i < v.length; i++) {
            if (v[i].d != null) {
                if (v[i].d.length > max) max = v[i].d.length;
                if (v[i].d.length < min) min = v[i].d.length;
            } else min = 0;
        }
        return new int[] { min, max };
    }

    /** Gets the minimum and maximum size of the user array in this
     *array of vertices*/
    private int[] getMinMaxU(Vertex[] v) {
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (int i = 0; i < v.length; i++) {
            if (v[i].u != null) {
                if (v[i].u.length > max) max = v[i].u.length;
                if (v[i].u.length < min) min = v[i].u.length;
            } else min = 0;
        }
        return new int[] { min, max };
    }

    /** Gets the minimum and maximum size of the texture array in this
     *array of vertices*/
    private int[] getMinMaxT(Vertex[] v) {
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (int i = 0; i < v.length; i++) {
            if (v[i].t != null) {
                if (v[i].t.length > max) max = v[i].t.length;
                if (v[i].t.length < min) min = v[i].t.length;
            } else min = 0;
        }
        return new int[] { min, max };
    }

    /** Find out how many motions steps are specified in these vertices*/
    public static int getMotionSteps(Vertex[] vertices) {
        int max = 0;
        for (int i = 0; i < vertices.length; i++) {
            if ((vertices[i].m != null) && (vertices[i].m.length > max)) max = vertices[i].m.length;
        }
        return max;
    }

    void clear() {
        parameters = null;
        parametersAfterMotion = null;
    }

    public void writeNormal(PrettyPrint out, RenderContext rc) {
    }

    public void writeCamera(PrettyPrint out) {
    }

    /**Convert an array of hpoints (4 value vectors) into an array of three
     * value vectors
     * @param points the hpoints
     * @return the points as three value vectors
     */
    public float[] convertPwtoP(float[] points) {
        float[] newPoints = new float[3 * points.length / 4];
        float div;
        for (int i = 0; i < points.length; i += 4) {
            div = points[i + 3];
            if (div == 0) div = 1;
            for (int j = 0; j < 3; j++) newPoints[(3 * i / 4) + j] = points[i + j] / div;
        }
        return newPoints;
    }

    /**Extract a set of float numbers from a vector occuring at set intervals
     *@param v the vector
     *@param repeat how often the numbers occur
     *@param offset the initial offset for the first number
     *@param width the number of consecutive values to extract*/
    public float[] extractMembers(Vector v, int repeat, int offset, int width) {
        Vector result = new Vector();
        for (int i = offset; i < v.size(); i += repeat) for (int j = 0; j < width; j++) result.add((Float) v.get(i + j));
        return Util.extractFloat(result);
    }

    /**Check whether a set of numbers at a given offset are zero*/
    public boolean areZero(Vector v, int repeat, int offset) {
        for (int i = offset; i < v.size(); i += repeat) if (((Float) v.get(i)).floatValue() != 0) return false;
        return true;
    }

    /**Swap alternate members of an array*/
    public float[] swapMembers(float[] array) {
        float tmp;
        for (int i = 0; i < array.length; i += 2) {
            tmp = array[i];
            array[i] = array[i + 1];
            array[i + 1] = tmp;
        }
        return array;
    }
}
