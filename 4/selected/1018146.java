package engine.graphics.synthesis.texture;

import engine.base.FMath;
import engine.base.Vector3;
import engine.base.Vector4;
import engine.graphics.synthesis.texture.CacheTileManager.TileCacheEntry;
import engine.parameters.FloatParam;

public final class FilterEmboss extends Channel {

    FloatParam strength;

    FloatParam angle;

    public String getName() {
        return "Emboss";
    }

    public String getHelpText() {
        return "A simple emboss effect that adds or subtracts white based on\n" + "the grayscale derivative of the second input.";
    }

    public FilterEmboss() {
        super(2);
        strength = CreateLocalFloatParam("Strength", 8.0f, 0.0f, Float.MAX_VALUE);
        angle = CreateLocalFloatParam("Angle", 45.0f, 0.0f, 360.0f);
        ;
        angle.setDefaultIncrement(22.5f);
    }

    public OutputType getOutputType() {
        return OutputType.RGBA;
    }

    public OutputType getChannelInputType(int idx) {
        if (idx == 0) return OutputType.RGBA; else if (idx == 1) return OutputType.SCALAR; else System.err.println("Invalid channel access in " + this);
        return OutputType.SCALAR;
    }

    private final Vector4 _function(Vector4 in0, float du, float dv) {
        Vector4 c = new Vector4(in0);
        Vector3 n = new Vector3(du * strength.get(), dv * strength.get(), 0.0f);
        float a = FMath.deg2rad(angle.get());
        Vector3 dir = new Vector3(FMath.cos(a), FMath.sin(a), 0);
        float addValue = n.dot(dir);
        c.x = Math.max(0.0f, Math.min(1.0f, c.x + addValue));
        c.y = Math.max(0.0f, Math.min(1.0f, c.y + addValue));
        c.z = Math.max(0.0f, Math.min(1.0f, c.z + addValue));
        return c;
    }

    protected void cache_function(Vector4 out, TileCacheEntry[] caches, int localX, int localY, float u, float v) {
        float du = caches[1].sample_du(localX, localY).XYZto1f();
        float dv = caches[1].sample_dv(localX, localY).XYZto1f();
        out.set(_function(caches[0].sample(localX, localY), du, dv));
    }

    protected Vector4 _valueRGBA(float u, float v) {
        float du = inputChannels[1].du1f(u, v).XYZto1f();
        float dv = inputChannels[1].dv1f(u, v).XYZto1f();
        return _function(inputChannels[0].valueRGBA(u, v), du, dv);
    }
}
