package engine.graphics.synthesis.texture;

import engine.base.Vector3;
import engine.base.Vector4;
import engine.graphics.synthesis.texture.CacheTileManager.TileCacheEntry;
import engine.parameters.FloatParam;

public final class FilterNormalMap extends Channel {

    FloatParam strength = CreateLocalFloatParam("Strength", 1.0f, 0.0f, Float.MAX_VALUE).setDefaultIncrement(0.125f);

    public String getName() {
        return "Normal Map";
    }

    public FilterNormalMap() {
        super(1);
    }

    public OutputType getOutputType() {
        return OutputType.RGBA;
    }

    public OutputType getChannelInputType(int idx) {
        if (idx == 0) return OutputType.SCALAR; else System.err.println("Invalid channel access in " + this);
        return OutputType.SCALAR;
    }

    private final Vector4 _function(float du, float dv) {
        Vector3 n = new Vector3(du * strength.get(), dv * strength.get(), 1.0f);
        n.normalize();
        Vector4 c = new Vector4(n.x * 0.5f + 0.5f, n.y * 0.5f + 0.5f, n.z * 0.5f + 0.5f, 1.0f);
        return c;
    }

    protected void cache_function(Vector4 out, TileCacheEntry[] caches, int localX, int localY, float u, float v) {
        float du = caches[0].sample_du(localX, localY).XYZto1f();
        float dv = caches[0].sample_dv(localX, localY).XYZto1f();
        out.set(_function(du, dv));
    }

    protected float _value1f(float u, float v) {
        Vector4 val = valueRGBA(u, v);
        return (val.x + val.y + val.z) * (1.0f / 3.0f);
    }

    protected Vector4 _valueRGBA(float u, float v) {
        return _function(inputChannels[0].du1f(u, v).XYZto1f(), inputChannels[0].dv1f(u, v).XYZto1f());
    }
}
