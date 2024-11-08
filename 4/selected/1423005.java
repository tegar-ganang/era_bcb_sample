package engine.graphics.synthesis.texture;

import engine.base.FMath;
import engine.base.Vector3;
import engine.base.Vector4;
import engine.parameters.Matrix3x3Param;

/**
 * A Pattern is a function that lives on [0,1)x[0,1)=>[0,1]. It is a channel with 0 input parameters
 * and thus works as a generator. Usually it is scala valued but may also be RGB.
 * @author Holger Dammertz
 *
 */
public class Pattern extends Channel {

    Matrix3x3Param transformation = CreateLocalMatrix3x3Param("Transformation");

    public String getName() {
        return "Pattern";
    }

    public OutputType getChannelInputType(int idx) {
        System.err.println("Invalid channel access in " + this);
        return OutputType.SCALAR;
    }

    protected final Vector3 transform(float u, float v) {
        Vector3 p = transformation.getMatrix().mult(new Vector3(u, v, 1.0f));
        p.x = p.x - FMath.ffloor(p.x);
        p.y = p.y - FMath.ffloor(p.y);
        return p;
    }

    @Override
    public Vector4 valueRGBA(float u, float v) {
        Vector3 p = transform(u, v);
        Vector4 val = _valueRGBA(p.x, p.y);
        return val;
    }
}
