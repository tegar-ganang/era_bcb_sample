package engine.graphics.synthesis.texture;

import engine.base.Vector4;
import engine.graphics.synthesis.texture.CacheTileManager.TileCacheEntry;
import engine.parameters.FloatParam;

public final class FilterWarp extends Channel {

    FloatParam strength;

    public String getName() {
        return "Warp";
    }

    public String getHelpText() {
        return "Warps (moves) the coordinates of the input image based on\n" + "the grayscale derivative of the second input.";
    }

    public FilterWarp() {
        super(2);
        strength = CreateLocalFloatParam("Strength", 1.0f, 0.0f, 8.0f);
        strength.setDefaultIncrement(0.25f);
    }

    public OutputType getOutputType() {
        return OutputType.RGBA;
    }

    public OutputType getChannelInputType(int idx) {
        if (idx == 0) return OutputType.RGBA; else if (idx == 1) return OutputType.SCALAR; else System.err.println("Invalid channel access in " + this);
        return OutputType.SCALAR;
    }

    protected void cache_function(Vector4 out, TileCacheEntry[] caches, int localX, int localY, float u, float v) {
        float du = caches[1].sample_du(localX, localY).XYZto1f() * strength.get();
        float dv = caches[1].sample_dv(localX, localY).XYZto1f() * strength.get();
        out.set(inputChannels[0].valueRGBA(u + du, v + dv));
    }

    protected float _value1f(float u, float v) {
        Vector4 val = valueRGBA(u, v);
        return (val.x + val.y + val.z) * (1.0f / 3.0f);
    }

    protected Vector4 _valueRGBA(float u, float v) {
        float du = inputChannels[1].du1f(u, v).XYZto1f() * strength.get();
        float dv = inputChannels[1].dv1f(u, v).XYZto1f() * strength.get();
        Vector4 c = inputChannels[0].valueRGBA(u + du, v + dv);
        return c;
    }
}
