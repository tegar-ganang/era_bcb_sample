package joelib2.math;

import org.apache.log4j.Category;

/**
 * An object for storing, manipulating and applying coordinate transformations.
 *
 *The most basic way to setup a transformation is to use the member methodName {@link #setup(double [])},
 *which requires you to supply a specific set of 4 coordinates from the initial reference frame
 *in the final reference frame.  The methodName {@link #setup(double [], double [], int)}
 *will set up a transformation given an arbitrary set of coordinates in the final and initial
 *reference frame.  A rotation/translation or a translation/rotation can also be supplied,
 *see the various member functions begining with Setup.
 *
 * <p>
 * Applying the transformation
 *The {@link #transform(double[], int)} member methodName applies the objects transform
 *to a set of coordinates.
 *
 *<p>
 * Utilities:<br>
 *The {@link #invert()} member methodName will reverses the current transformation such that it changes
 *what was originally the final reference frame into what was originally the initial reference frame.
 *A rotation/translation or translation/rotation cooresponding to the objects transformation
 *can be extracted, see the various member functions begining with Get.  The
 * {@link #add(JOECoordTrans)}
 *and {@link #adding(JOECoordTrans)} are also defined for combining transformation.
 *
 *<p>
 * Note:<br>
 * This object can only handle transformations that can be described
 *with Euler angles (i.e., No inversions).  Several member methodName will take rotation
 *matricies as input, however, it is assumed that these matricies only apply simple
 *rotations that can be described with Euler angles.
 *
 * @.author     wegnerj
 * @.author     original author: OpenEye Scientific Software
 * @.license GPL
 * @.cvsversion    $Revision: 1.2 $, $Date: 2005/02/17 16:48:35 $
 */
public class CoordinateTransformation implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private static Category logger = Category.getInstance(CoordinateTransformation.class.getName());

    /**
     *  Description of the Field
     */
    protected double[] _euler = new double[3];

    /**
     *  Description of the Field
     */
    protected double[] _rmat = new double[9];

    /**
     *  Description of the Field
     */
    protected double[] _trans = new double[3];

    /**
     *  Constructor for the JOECoordTrans object
     */
    public CoordinateTransformation() {
        _trans[0] = 0.0;
        _trans[1] = 0.0;
        _trans[2] = 0.0;
        _euler[0] = 0.0;
        _euler[1] = 0.0;
        _euler[2] = 0.0;
        _rmat[0] = 1.0;
        _rmat[1] = 0.0;
        _rmat[2] = 0.0;
        _rmat[3] = 0.0;
        _rmat[4] = 1.0;
        _rmat[5] = 0.0;
        _rmat[6] = 0.0;
        _rmat[7] = 0.0;
        _rmat[8] = 1.0;
    }

    /**
     *  Combine two transformations. Obviously the communative property does not
     *  apply hence, ct1+ct2 is \b not the same as ct2+ct1.
     *
     * @param  ct2  Second transformation
     * @return      A transformation equivilant to applying (*this) and then ct2.
     */
    public final CoordinateTransformation add(final CoordinateTransformation ct2) {
        double[] xyz = new double[12];
        int i;
        for (i = 0; i < 12; i++) {
            xyz[i] = 0.0f;
        }
        xyz[(3 * 1) + 0] = 1.0f;
        xyz[(3 * 2) + 1] = 1.0f;
        xyz[(3 * 3) + 2] = 1.0f;
        CoordinateTransformation ct = new CoordinateTransformation();
        transform(xyz, 4);
        ct2.transform(xyz, 4);
        ct.setup(xyz);
        return ct;
    }

    /**
     *  Changes this transform to the orginal transform followed by an additional
     *  transform.
     *
     * @param  ct  The additional coordinate transformation.
     * @return     A transform equivilant to the original transform followed by
     *      the transform given in ct. Note that the communative property does not
     *      apply so this is \b not the same as applying ct then the original
     *      transform.
     */
    public CoordinateTransformation adding(final CoordinateTransformation ct) {
        double[] xyz = new double[12];
        int i;
        for (i = 0; i < 12; i++) {
            xyz[i] = 0.0f;
        }
        xyz[(3 * 1) + 0] = 1.0f;
        xyz[(3 * 2) + 1] = 1.0f;
        xyz[(3 * 3) + 2] = 1.0f;
        transform(xyz, 4);
        ct.transform(xyz, 4);
        setup(xyz);
        return this;
    }

    /**
     *  Clears the object as if it were just constructed.
     */
    public void clear() {
        _trans[0] = 0.0f;
        _trans[1] = 0.0f;
        _trans[2] = 0.0f;
        _euler[0] = 0.0f;
        _euler[1] = 0.0f;
        _euler[2] = 0.0f;
        _rmat[0] = 1.0f;
        _rmat[1] = 0.0f;
        _rmat[2] = 0.0f;
        _rmat[3] = 0.0f;
        _rmat[4] = 1.0f;
        _rmat[5] = 0.0f;
        _rmat[6] = 0.0f;
        _rmat[7] = 0.0f;
        _rmat[8] = 1.0f;
    }

    /**
     *  Returns a \b rotation and \b translation (applied in that order)
     *  corresponding to this objects transformation.
     *
     * @param  euler  A length 3 array that will be returned with euler angles of
     *      the rotation. The angles are applied in the following order around the
     *      following axis. euler[0] rotation about the z-axis, euler[1] rotation
     *      about the x-axis and euler[2] rotation about the z-axis.
     * @param  trans  A length 3 array that will be returned with the x,y and z
     *      components of the translation.
     */
    public final void getEulerTranslation(double[] euler, double[] trans) {
        int i;
        for (i = 0; i < 3; i++) {
            euler[i] = _euler[i];
            trans[i] = _trans[i];
        }
    }

    /**
     *  Returns a \b rotation and \b translation (applied in that order)
     *  coresponding to this objects transformation.
     *
     * @param  rmat   A length 9 array that will be returned with the elements of
     *      a rotation matrix. rmat[3*i+j] is the value of the element in the i'th
     *      row and j'th column.
     * @param  trans  A length 3 array that will be returned with the x,y and z
     *      components of the translation.
     */
    public final void getRmatrixTranslation(double[] rmat, double[] trans) {
        int i;
        for (i = 0; i < 3; i++) {
            trans[i] = _trans[i];
        }
        for (i = 0; i < 9; i++) {
            rmat[i] = _rmat[i];
        }
    }

    /**
     *  Returns a \b rotation and \b translation (applied in that order)
     *  coresponding to this objects transformation.
     *
     * @param  rmatrix  A Matrix3x3 that will be returned with the rotation.
     * @param  tvec     A Vector that will be returned with the translation.
     */
    public void getRmatrixTranslation(BasicMatrix3D rmatrix, BasicVector3D tvec) {
        double[] rmat = new double[9];
        double[] trans = new double[3];
        getRmatrixTranslation(rmat, trans);
        int irow;
        int icolumn;
        for (irow = 0; irow < 3; irow++) {
            for (icolumn = 0; icolumn < 3; icolumn++) {
                rmatrix.set(irow, icolumn, rmat[(3 * irow) + icolumn]);
            }
        }
        tvec.set(trans);
    }

    /**
     *  Returns a \b translation and \b rotation (applied in that order)
     *  coresponding to this objects transformation.
     *
     * @param  trans  A length 3 array that will be returned with the x,y and z
     *      components of the translation.
     * @param  euler  A length 3 array that will be returned with euler angles of
     *      the rotation. The angles are applied in the following order around the
     *      following axis. euler[0] rotation about the z-axis, euler[1] rotation
     *      about the x-axis and euler[2] rotation about the z-axis.
     */
    public final void getTranslationEuler(double[] trans, double[] euler) {
        euler[0] = _euler[0];
        euler[1] = _euler[1];
        euler[2] = _euler[2];
        trans[0] = 0.0f;
        trans[1] = 0.0f;
        trans[2] = 0.0f;
        transform(trans, 1);
        applyEulerInvert(euler, trans, 1);
    }

    /**
     *  Returns a \b translation and \b rotation (applied in that order)
     *  coresponding to this objects transformation.
     *
     * @param  trans  A length 3 array that will be returned with the x,y and z
     *      components of the translation.
     * @param  rmat   A length 9 array that will be returned with the elements of
     *      a rotation matrix. rmat[3*i+j] is the value of the element in the i'th
     *      row and j'th column.
     */
    public final void getTranslationRmatrix(double[] trans, double[] rmat) {
        double[] euler = new double[3];
        getTranslationEuler(trans, euler);
        eulerToRmatrix(euler, rmat);
    }

    /**
     *  Returns a \b translation and \b rotation (applied in that order)
     *  coresponding to this objects transformation.
     *
     * @param  tvec     A Vector that will be returned with the translation.
     * @param  rmatrix  A Matrix3x3 that will be returned with the rotation.
     */
    public void getTranslationRmatrix(BasicVector3D tvec, BasicMatrix3D rmatrix) {
        double[] rmat = new double[9];
        double[] trans = new double[3];
        getTranslationRmatrix(trans, rmat);
        int irow;
        int icolumn;
        for (irow = 0; irow < 3; irow++) {
            for (icolumn = 0; icolumn < 3; icolumn++) {
                rmatrix.set(irow, icolumn, rmat[(3 * irow) + icolumn]);
            }
        }
        tvec.set(trans);
    }

    /**
     *  Inverts this objects transformation. (i.e., instead of converting from
     *  reference frame 1 to 2 it now converts from 2 to 1.)
     */
    public void invert() {
        double[] xyz = new double[12];
        int i;
        for (i = 0; i < 12; i++) {
            xyz[i] = 0.0f;
        }
        xyz[(3 * 1) + 0] = 1.0f;
        xyz[(3 * 2) + 1] = 1.0f;
        xyz[(3 * 3) + 2] = 1.0f;
        for (i = 0; i < 4; i++) {
            xyz[(3 * i) + 0] -= _trans[0];
            xyz[(3 * i) + 1] -= _trans[1];
            xyz[(3 * i) + 2] -= _trans[2];
        }
        applyEulerInvert(_euler, xyz, 4);
        setup(xyz);
    }

    /**
     *  Assignment.
     *
     * @param  cp  Description of the Parameter
     * @return     Description of the Return Value
     */
    public CoordinateTransformation set(final CoordinateTransformation cp) {
        _trans[0] = cp._trans[0];
        _trans[1] = cp._trans[1];
        _trans[2] = cp._trans[2];
        _euler[0] = cp._euler[0];
        _euler[1] = cp._euler[1];
        _euler[2] = cp._euler[2];
        _rmat[0] = cp._rmat[0];
        _rmat[1] = cp._rmat[1];
        _rmat[2] = cp._rmat[2];
        _rmat[3] = cp._rmat[3];
        _rmat[4] = cp._rmat[4];
        _rmat[5] = cp._rmat[5];
        _rmat[6] = cp._rmat[6];
        _rmat[7] = cp._rmat[7];
        _rmat[8] = cp._rmat[8];
        return this;
    }

    /**
     *  Core function to setup the coordinate transformation.
     *
     * @param  in_xyz  A length 12 array containing 4 coordinates
     * (0,0,0), (1,0,0), (0,1,0) and (0,0,1) from the initial
     * reference frame transformed into the final reference frame
     * @return         <tt>true</tt> if o.k.
     */
    public boolean setup(double[] in_xyz) {
        double[] xyz = new double[12];
        double[] y = new double[4];
        double[] z = new double[4];
        int i;
        System.arraycopy(in_xyz, 0, xyz, 0, 12);
        System.arraycopy(in_xyz, 3 * 1, y, 0, 3);
        System.arraycopy(in_xyz, 3 * 2, z, 0, 3);
        _trans[0] = xyz[0];
        _trans[1] = xyz[1];
        _trans[2] = xyz[2];
        for (i = 0; i < 4; i++) {
            xyz[(3 * i) + 0] -= _trans[0];
            xyz[(3 * i) + 1] -= _trans[1];
            xyz[(3 * i) + 2] -= _trans[2];
        }
        double sn;
        double cs;
        double mag;
        mag = Math.sqrt((z[0] * z[0]) + (z[1] * z[1]));
        if (mag > 0.000001) {
            cs = z[1] / mag;
            sn = z[0] / mag;
            _euler[2] = (double) angle(sn, cs);
        } else {
            cs = 1.0f;
            sn = 0.0f;
            _euler[2] = 0.0f;
        }
        double xx;
        double yy;
        for (i = 0; i < 4; i++) {
            xx = xyz[(3 * i) + 0];
            yy = xyz[(3 * i) + 1];
            xyz[(3 * i) + 0] = (cs * xx) - (sn * yy);
            xyz[(3 * i) + 1] = (cs * yy) + (sn * xx);
        }
        cs = z[2];
        sn = z[1];
        _euler[1] = (double) angle(sn, cs);
        double zz;
        for (i = 0; i < 4; i++) {
            yy = xyz[(3 * i) + 1];
            zz = xyz[(3 * i) + 2];
            xyz[(3 * i) + 1] = (cs * yy) - (sn * zz);
            xyz[(3 * i) + 2] = (cs * zz) + (sn * yy);
        }
        cs = y[1];
        sn = y[0];
        _euler[0] = (double) angle(sn, cs);
        eulerToRmatrix(_euler, _rmat);
        for (i = 0; i < 4; i++) {
            xx = xyz[(3 * i) + 0];
            yy = xyz[(3 * i) + 1];
            xyz[(3 * i) + 0] = (cs * xx) - (sn * yy);
            xyz[(3 * i) + 1] = (cs * yy) + (sn * xx);
        }
        boolean error = false;
        double tol = 0.0001f;
        if (Math.abs(xyz[0]) > tol) {
            error = true;
        }
        if (Math.abs(xyz[1]) > tol) {
            error = true;
        }
        if (Math.abs(xyz[2]) > tol) {
            error = true;
        }
        if (Math.abs(xyz[(3 * 1) + 0] - 1.0) > tol) {
            error = true;
        }
        if (Math.abs(xyz[(3 * 2) + 1] - 1.0) > tol) {
            error = true;
        }
        if (Math.abs(xyz[(3 * 3) + 2] - 1.0) > tol) {
            error = true;
        }
        if (error) {
            logger.error("WARNING! JOECoordTrans.setup(double []) probable invalid input");
            return false;
        }
        return true;
    }

    /**
     *  Sets up a coordinate transformation from an arbitrary set of coordinates
     *  in the initial and final reference frame. Note that this procedure \b will
     *  deal with the case of N = 0,1 or 2. In the case of N=0 the identity
     *  transform is returned. In the case of N=1 the appropriate translation,
     *  without and translation is returned. In the case of N=2 there are multiple
     *  degenerate transformations, and a correct, but arbitrary, transformation
     *  is returned.<br>
     *  init_xyz and final_xyz must be identical sets of coordinates except for
     *  the frame of reference.
     *
     * @param  init_xyz    An array with the coordinates in the initial reference
     *      frame.
     * @param  final_xyz   Description of the Parameter
     * @param  n           Description of the Parameter
     */
    public void setup(double[] init_xyz, double[] final_xyz, int n) {
        clear();
        double[] xyz1 = new double[12];
        double[] xyz2 = new double[12];
        int i;
        int j;
        if (n != 0) {
            for (i = 0; i < 3; i++) {
                xyz1[i] = init_xyz[i];
                xyz2[i] = final_xyz[i];
            }
        } else {
            return;
        }
        if (n > 1) {
            double dist;
            double maxdist;
            int id = 1;
            maxdist = 0.0;
            for (i = 1; i < n; i++) {
                for (dist = 0.0, j = 0; j < 3; j++) {
                    dist += ((xyz1[j] - init_xyz[(3 * i) + j]) * (xyz1[j] - init_xyz[(3 * i) + j]));
                }
                dist = Math.sqrt(dist);
                if (dist > maxdist) {
                    maxdist = dist;
                    id = i;
                }
            }
            for (i = 0; i < 3; i++) {
                xyz1[3 + i] = init_xyz[(3 * id) + i];
                xyz2[3 + i] = final_xyz[(3 * id) + i];
            }
        } else {
            double[] euler = new double[3];
            double[] trans = new double[3];
            for (i = 0; i < 3; i++) {
                euler[i] = 0.0f;
                trans[i] = xyz2[i] - xyz1[i];
            }
            setupEulerTranslation(euler, trans);
            return;
        }
        if (n > 2) {
            double mag;
            double maxcross;
            double[] xx = new double[3];
            double[] yy = new double[3];
            double[] cr = new double[3];
            int ic = 1;
            for (j = 0; j < 3; j++) {
                xx[j] = xyz1[3 + j] - xyz1[j];
            }
            maxcross = 0.0f;
            for (i = 1; i < n; i++) {
                for (j = 0; j < 3; j++) {
                    yy[j] = init_xyz[(3 * i) + j] - xyz1[j];
                }
                cr[0] = (xx[1] * yy[2]) - (xx[2] * yy[1]);
                cr[1] = (-xx[0] * yy[2]) + (xx[2] * yy[0]);
                cr[2] = (xx[0] * yy[1]) - (xx[1] * yy[0]);
                mag = Math.sqrt((cr[0] * cr[0]) + (cr[1] * cr[1]) + (cr[2] * cr[2]));
                if (mag > maxcross) {
                    maxcross = mag;
                    ic = i;
                }
            }
            for (i = 0; i < 3; i++) {
                xyz1[6 + i] = init_xyz[(3 * ic) + i];
                xyz2[6 + i] = final_xyz[(3 * ic) + i];
            }
        } else {
            double[] xx = new double[3];
            double[] yy = new double[3];
            xx[0] = xyz1[3 + 0] - xyz1[0];
            xx[1] = xyz1[3 + 1] - xyz1[1];
            xx[2] = xyz1[3 + 2] - xyz1[2];
            yy[0] = xx[2];
            yy[1] = xx[0];
            yy[2] = xx[1];
            xyz1[6 + 0] = yy[0] + xyz1[0];
            xyz1[6 + 1] = yy[1] + xyz1[1];
            xyz1[6 + 2] = yy[2] + xyz1[2];
            xx[0] = xyz2[3 + 0] - xyz2[0];
            xx[1] = xyz2[3 + 1] - xyz2[1];
            xx[2] = xyz2[3 + 2] - xyz2[2];
            yy[0] = xx[2];
            yy[1] = xx[0];
            yy[2] = xx[1];
            xyz2[6 + 0] = yy[0] + xyz2[0];
            xyz2[6 + 1] = yy[1] + xyz2[1];
            xyz2[6 + 2] = yy[2] + xyz2[2];
        }
        double mag;
        double dot;
        double[] xx1 = new double[3];
        double[] yy1 = new double[3];
        double[] zz1 = new double[3];
        mag = 0.0f;
        for (i = 0; i < 3; i++) {
            xx1[i] = xyz1[3 + i] - xyz1[i];
        }
        for (i = 0; i < 3; i++) {
            mag += (xx1[i] * xx1[i]);
        }
        mag = Math.sqrt(mag);
        for (i = 0; i < 3; i++) {
            xx1[i] /= mag;
            xyz1[3 + i] = xx1[i] + xyz1[i];
        }
        dot = 0.0f;
        for (i = 0; i < 3; i++) {
            yy1[i] = xyz1[6 + i] - xyz1[i];
        }
        for (i = 0; i < 3; i++) {
            dot += (xx1[i] * yy1[i]);
        }
        for (i = 0; i < 3; i++) {
            yy1[i] -= (xx1[i] * dot);
        }
        mag = 0.0f;
        for (i = 0; i < 3; i++) {
            mag += (yy1[i] * yy1[i]);
        }
        mag = Math.sqrt(mag);
        for (i = 0; i < 3; i++) {
            yy1[i] /= mag;
            xyz1[6 + i] = yy1[i] + xyz1[i];
        }
        zz1[0] = (xx1[1] * yy1[2]) - (xx1[2] * yy1[1]);
        zz1[1] = (-xx1[0] * yy1[2]) + (xx1[2] * yy1[0]);
        zz1[2] = (xx1[0] * yy1[1]) - (xx1[1] * yy1[0]);
        for (i = 0; i < 3; i++) {
            xyz1[9 + i] = zz1[i] + xyz1[i];
        }
        double[] xx2 = new double[3];
        double[] yy2 = new double[3];
        double[] zz2 = new double[3];
        mag = 0.0f;
        for (i = 0; i < 3; i++) {
            xx2[i] = xyz2[3 + i] - xyz2[i];
        }
        for (i = 0; i < 3; i++) {
            mag += (xx2[i] * xx2[i]);
        }
        mag = Math.sqrt(mag);
        for (i = 0; i < 3; i++) {
            xx2[i] /= mag;
            xyz2[3 + i] = xx2[i] + xyz2[i];
        }
        dot = 0.0f;
        for (i = 0; i < 3; i++) {
            yy2[i] = xyz2[6 + i] - xyz2[i];
        }
        for (i = 0; i < 3; i++) {
            dot += (xx2[i] * yy2[i]);
        }
        for (i = 0; i < 3; i++) {
            yy2[i] -= (xx2[i] * dot);
        }
        mag = 0.0f;
        for (i = 0; i < 3; i++) {
            mag += (yy2[i] * yy2[i]);
        }
        mag = Math.sqrt(mag);
        for (i = 0; i < 3; i++) {
            yy2[i] /= mag;
            xyz2[6 + i] = yy2[i] + xyz2[i];
        }
        zz2[0] = (xx2[1] * yy2[2]) - (xx2[2] * yy2[1]);
        zz2[1] = (-xx2[0] * yy2[2]) + (xx2[2] * yy2[0]);
        zz2[2] = (xx2[0] * yy2[1]) - (xx2[1] * yy2[0]);
        for (i = 0; i < 3; i++) {
            xyz2[9 + i] = zz2[i] + xyz2[i];
        }
        CoordinateTransformation cti = new CoordinateTransformation();
        CoordinateTransformation ctf = new CoordinateTransformation();
        cti.setup(xyz1);
        ctf.setup(xyz2);
        cti.invert();
        this.set(cti.add(ctf));
    }

    /**
     *  Sets up this objects transformation based on a specified \b rotation and
     *  \b translation (applied in that order).<br>
     *  This member methodName is distinct from <tt> setupTranslationEuler</tt> .
     *  The transformation setup in this methodName applies the rotation before the
     *  translation.
     *
     * @param  euler  A length 3 array with euler angles. euler[0] is a rotation
     *      about the z-axis, euler[1] is a rotation about the x-axis and euler[2]
     *      is a rotation about the z-axis. The Euler rotations are applied in the
     *      order listed. \b IMPORTANT: Angles are in \b radians.
     * @param  trans  A length 3 array with the x,y and z translations.
     * @see  joelib2.math.CoordinateTransformation#setupTranslationEuler(double [], double[])
     */
    public void setupEulerTranslation(double[] euler, double[] trans) {
        double[] xyz = new double[12];
        int i;
        for (i = 0; i < 12; i++) {
            xyz[i] = 0.0f;
        }
        xyz[(3 * 1) + 0] = 1.0f;
        xyz[(3 * 2) + 1] = 1.0f;
        xyz[(3 * 3) + 2] = 1.0f;
        applyEuler(euler, xyz, 4);
        applyTranslation(trans, xyz, 4);
        setup(xyz);
    }

    /**
     *  Sets up this objects transformation based on a specified \b rotation and
     *  \b translation (applied in that order). This member methodName is distinct
     *  from <tt>setupTranslationRmatrix()</tt> . This transformation setup in
     *  this methodName applies the rotation before the translation.
     *
     * @param  rmat   A length 9 array representing the rotation matrix.
     *      rmat[3*i+j] is the value of the element in the i'th row and j'th
     *      column.
     * @param  trans  A length 3 array with the x,y and z translations.
     * @see  joelib2.math.CoordinateTransformation#setupTranslationRmatrix(double[], double[])
     */
    public void setupRmatrixTranslation(double[] rmat, double[] trans) {
        double[] xyz = new double[12];
        int i;
        for (i = 0; i < 12; i++) {
            xyz[i] = 0.0f;
        }
        xyz[(3 * 1) + 0] = 1.0f;
        xyz[(3 * 2) + 1] = 1.0f;
        xyz[(3 * 3) + 2] = 1.0f;
        applyRmatrix(rmat, xyz, 4);
        applyTranslation(trans, xyz, 4);
        setup(xyz);
    }

    /**
     *  Sets up this objects transformation based on a specified \b rotation and
     *  \b translation (applied in that order). This member methodName is distinct
     *  from <tt>setupTranslationRmatrix()</tt> . This transformation setup in
     *  this methodName applies the rotation before the translation.
     *
     * @param  rmatrix  A Matrix3x3 rotation matrix.
     * @param  tvec     A Vector holding the translation
     * @see  joelib2.math.CoordinateTransformation#setupTranslationRmatrix(XYZVector, Matrix3x3)
     */
    public void setupRmatrixTranslation(BasicMatrix3D rmatrix, BasicVector3D tvec) {
        double[] rmat = new double[9];
        double[] trans = new double[3];
        int irow;
        int icolumn;
        for (irow = 0; irow < 3; irow++) {
            for (icolumn = 0; icolumn < 3; icolumn++) {
                rmat[(3 * irow) + icolumn] = rmatrix.get(irow, icolumn);
            }
        }
        tvec.get(trans);
        setupRmatrixTranslation(rmat, trans);
    }

    /**
     *  Sets up this objects transformation based on a specified \b translation
     *  and \b rotation (applied in that order). This member methodName is distinct
     *  from <tt>setupEulerTranslation()</tt> . The transformation setup in this
     *  methodName applies the translation before the rotation.
     *
     * @param  trans  A length 3 array with the x,y and z translations.
     * @param  euler  length 3 array with euler angles. euler[0] is a rotation
     *      about the z-axis, euler[1] is a rotation about the x-axis and euler[2]
     *      is a rotation about the z-axis. The Euler rotations are applied in the
     *      order listed. \b IMPORTANT: Angles are in \b radians.
     * @see joelib2.math.CoordinateTransformation#setupEulerTranslation(double [], double[])
     */
    public void setupTranslationEuler(double[] trans, double[] euler) {
        double[] xyz = new double[12];
        int i;
        for (i = 0; i < 12; i++) {
            xyz[i] = 0.0f;
        }
        xyz[(3 * 1) + 0] = 1.0f;
        xyz[(3 * 2) + 1] = 1.0f;
        xyz[(3 * 3) + 2] = 1.0f;
        applyTranslation(trans, xyz, 4);
        applyEuler(euler, xyz, 4);
        setup(xyz);
    }

    /**
     *  Sets up this objects transformation based on a specified \b translation
     *  and \b rotation (applied in that order). This member methodName is distinct
     *  from <tt>setupRmatrixTranslation()</tt> . This transformation setup in
     *  this methodName applies the translation before the rotation.
     *
     * @param  trans    Description of the Parameter
     * @param  rmat     Description of the Parameter
     * @see  joelib2.math.CoordinateTransformation#setupRmatrixTranslation(double[], double[])
     */
    public void setupTranslationRmatrix(double[] trans, double[] rmat) {
        double[] xyz = new double[12];
        int i;
        for (i = 0; i < 12; i++) {
            xyz[i] = 0.0f;
        }
        xyz[(3 * 1) + 0] = 1.0;
        xyz[(3 * 2) + 1] = 1.0;
        xyz[(3 * 3) + 2] = 1.0;
        applyTranslation(trans, xyz, 4);
        applyRmatrix(rmat, xyz, 4);
        setup(xyz);
    }

    /**
     *  Sets up this objects transformation based on a specified \b translation
     *  and \b rotation (applied in that order). This member methodName is distinct
     *  from <tt>setupRmatrixTranslation()</tt> . This transformation setup in
     *  this methodName applies the translation before the rotation.
     *
     * @param  tvec     Description of the Parameter
     * @param  rmatrix  Description of the Parameter
     * @see  joelib2.math.CoordinateTransformation#setupRmatrixTranslation(Matrix3x3, XYZVector)
     */
    public void setupTranslationRmatrix(BasicVector3D tvec, BasicMatrix3D rmatrix) {
        double[] rmat = new double[9];
        double[] trans = new double[3];
        int irow;
        int icolumn;
        for (irow = 0; irow < 3; irow++) {
            for (icolumn = 0; icolumn < 3; icolumn++) {
                rmat[(3 * irow) + icolumn] = rmatrix.get(irow, icolumn);
            }
        }
        tvec.get(trans);
        setupTranslationRmatrix(trans, rmat);
    }

    /**
     *  Applies this objects transformation to a set of coordinates
     *
     * @param  xyz  Array of coordinates to be transformed
     * @param  n    Number of coordinates in xyz array
     */
    public final void transform(double[] xyz, int n) {
        int i;
        double x;
        double y;
        double z;
        for (i = 0; i < n; i++) {
            x = xyz[(3 * i) + 0];
            y = xyz[(3 * i) + 1];
            z = xyz[(3 * i) + 2];
            xyz[(3 * i) + 0] = (_rmat[0] * x) + (_rmat[1] * y) + (_rmat[2] * z) + _trans[0];
            xyz[(3 * i) + 1] = (_rmat[3] * x) + (_rmat[4] * y) + (_rmat[5] * z) + _trans[1];
            xyz[(3 * i) + 2] = (_rmat[6] * x) + (_rmat[7] * y) + (_rmat[8] * z) + _trans[2];
        }
    }

    /**
     *  Get's an angle given it's Math.sine and Math.coMath.sine.
     *
     * @param  cs  Math.coMath.sine of the angle
     * @param  sn  Math.sine of the angle
     * @return     Angle in radians
     */
    protected double angle(double sn, double cs) {
        double angle = 0.0;
        if (Math.abs(cs) < Math.abs(sn)) {
            angle = Math.acos(cs);
            if ((sn < 0.0f) && (angle < Math.PI)) {
                angle = (2.0 * Math.PI) - angle;
            } else if ((sn > 0.0f) && (angle > Math.PI)) {
                angle = (2.0 * Math.PI) - angle;
            }
        } else {
            angle = Math.asin(sn);
            if (angle < Math.PI) {
                if ((cs < 0.0f) && (angle < (0.5 * Math.PI))) {
                    angle = Math.PI - angle;
                } else if ((cs > 0.0f) && (angle > (0.5 * Math.PI))) {
                    angle = Math.PI - angle;
                }
            } else {
                if ((cs < 0.0f) && (angle > (1.5 * Math.PI))) {
                    angle = (3.0 * Math.PI) - angle;
                }
                if ((cs > 0.0f) && (angle < (1.5 * Math.PI))) {
                    angle = (3.0 * Math.PI) - angle;
                }
            }
        }
        return angle;
    }

    /**
     *  Applies an euler rotation (no translation) to a set of coordinates.
     *
     * @param  euler  A length 3 array with the euler angles. euler[0] is a
     *      rotation about the z-axis, euler[1] is a rotation about the x-axis and
     *      euler[2] is a rotation about the z-axis. The rotations are applied in
     *      the order listed. \b IMPORTANT: Angles are in \b radians.
     * @param  xyz    Array of coordinates to be transformed
     * @param  n      Description of the Parameter
     */
    protected final void applyEuler(double[] euler, double[] xyz, int n) {
        double cs0 = Math.cos(euler[0]);
        double cs1 = Math.cos(euler[1]);
        double cs2 = Math.cos(euler[2]);
        double sn0 = Math.sin(euler[0]);
        double sn1 = Math.sin(euler[1]);
        double sn2 = Math.sin(euler[2]);
        int i;
        double xx;
        double yy;
        for (i = 0; i < n; i++) {
            xx = xyz[(3 * i) + 0];
            yy = xyz[(3 * i) + 1];
            xyz[(3 * i) + 0] = (cs0 * xx) + (sn0 * yy);
            xyz[(3 * i) + 1] = (cs0 * yy) - (sn0 * xx);
        }
        double zz;
        for (i = 0; i < n; i++) {
            yy = xyz[(3 * i) + 1];
            zz = xyz[(3 * i) + 2];
            xyz[(3 * i) + 1] = (cs1 * yy) + (sn1 * zz);
            xyz[(3 * i) + 2] = (cs1 * zz) - (sn1 * yy);
        }
        for (i = 0; i < n; i++) {
            xx = xyz[(3 * i) + 0];
            yy = xyz[(3 * i) + 1];
            xyz[(3 * i) + 0] = (cs2 * xx) + (sn2 * yy);
            xyz[(3 * i) + 1] = (cs2 * yy) - (sn2 * xx);
        }
    }

    /**
     *  Applies an euler rotation (no translation) in reverse to a set of
     *  coordinates.
     *
     * @param  euler  A length 3 array with the euler angles. euler[0] is a
     *      rotation about the z-axis, euler[1] is a rotation about the x-axis and
     *      euler[2] is a rotation about the z-axis. In general the rotations are
     *      applied in the order listed, however, in this procedure they are
     *      applied in reverse order. \b IMPORTANT: Angles are in \b radians.
     *      \param xyz Array of coordinates to be transformed \param n Number of
     *      coordinates in xyz array
     * @param  xyz    Description of the Parameter
     * @param  n      Description of the Parameter
     */
    protected final void applyEulerInvert(double[] euler, double[] xyz, int n) {
        double cs0 = Math.cos(euler[0]);
        double cs1 = Math.cos(euler[1]);
        double cs2 = Math.cos(euler[2]);
        double sn0 = Math.sin(euler[0]);
        double sn1 = Math.sin(euler[1]);
        double sn2 = Math.sin(euler[2]);
        int i;
        double xx;
        double yy;
        for (i = 0; i < n; i++) {
            xx = xyz[(3 * i) + 0];
            yy = xyz[(3 * i) + 1];
            xyz[(3 * i) + 0] = (cs2 * xx) - (sn2 * yy);
            xyz[(3 * i) + 1] = (cs2 * yy) + (sn2 * xx);
        }
        double zz;
        for (i = 0; i < n; i++) {
            yy = xyz[(3 * i) + 1];
            zz = xyz[(3 * i) + 2];
            xyz[(3 * i) + 1] = (cs1 * yy) - (sn1 * zz);
            xyz[(3 * i) + 2] = (cs1 * zz) + (sn1 * yy);
        }
        for (i = 0; i < n; i++) {
            xx = xyz[(3 * i) + 0];
            yy = xyz[(3 * i) + 1];
            xyz[(3 * i) + 0] = (cs0 * xx) - (sn0 * yy);
            xyz[(3 * i) + 1] = (cs0 * yy) + (sn0 * xx);
        }
    }

    /**
     *  Applies a rotation (no translation) to a set of coordinates.
     *
     * @param  rmat  A length 9 array representing the rotation matrix.
     *      rmat[3*i+j] is the value of the element in the i'th row and j'th
     *      column.
     * @param  xyz   Array of coordinates to be transformed
     * @param  n     Number of coordinates in xyz array
     */
    protected final void applyRmatrix(double[] rmat, double[] xyz, int n) {
        int i;
        double x;
        double y;
        double z;
        for (i = 0; i < n; i++) {
            x = (rmat[0] * xyz[(3 * i) + 0]) + (rmat[1] * xyz[(3 * i) + 1]) + (rmat[2] * xyz[(3 * i) + 2]);
            y = (rmat[3] * xyz[(3 * i) + 0]) + (rmat[4] * xyz[(3 * i) + 1]) + (rmat[5] * xyz[(3 * i) + 2]);
            z = (rmat[6] * xyz[(3 * i) + 0]) + (rmat[7] * xyz[(3 * i) + 1]) + (rmat[8] * xyz[(3 * i) + 2]);
            xyz[(3 * i) + 0] = x;
            xyz[(3 * i) + 1] = y;
            xyz[(3 * i) + 2] = z;
        }
    }

    /**
     *  Applies a translation (no rotation) to a set of coordates.
     *
     * @param  trans  A length 3 array holding the x,y and z translation
     * @param  xyz    Array of coordinates to be transformed
     * @param  n      Number of coordinates in xyz array
     */
    protected final void applyTranslation(double[] trans, double[] xyz, int n) {
        int i;
        for (i = 0; i < n; i++) {
            xyz[(3 * i) + 0] += trans[0];
            xyz[(3 * i) + 1] += trans[1];
            xyz[(3 * i) + 2] += trans[2];
        }
    }

    /**
     *  Sets up a rotation matrix from three euler angles
     *
     * @param  euler  A length 3 array with euler angles. euler[0] is a rotation
     *      about the z-axis, euler[1] is a rotation about the x-axis and euler[2]
     *      is a rotation about the z-axis. The Euler rotations are applied in the
     *      order listed. \b IMPORTANT: Angles are in \b radians.
     * @param  rmat   A length 9 array representing the rotation matrix.
     *      rmat[3*i+j] is the value of the element in the i'th row and j'th
     *      column.
     */
    protected final void eulerToRmatrix(double[] euler, double[] rmat) {
        double[] xyz = new double[9];
        int i;
        int j;
        for (i = 0; i < 9; i++) {
            xyz[i] = 0.0f;
        }
        xyz[(3 * 0) + 0] = 1.0f;
        xyz[(3 * 1) + 1] = 1.0f;
        xyz[(3 * 2) + 2] = 1.0f;
        applyEuler(euler, xyz, 3);
        for (i = 0; i < 3; i++) {
            for (j = 0; j < 3; j++) {
                rmat[(3 * i) + j] = xyz[(3 * j) + i];
            }
        }
    }
}
