package engine.graphics.synthesis.texture;

import engine.base.Vector4;
import engine.graphics.synthesis.texture.CacheTileManager.TileCacheEntry;
import engine.parameters.ColorGradientParam;

public final class FilterColorize extends Channel {

    ColorGradientParam colorGradientParam;

    public String getHelpText() {
        return "Interprets the input image as grayscale (computing (R+G+B)/1.0f)\n" + "and maps the value in [0,1] to the specified color gradient value.\n";
    }

    public String getName() {
        return "Colorize";
    }

    public FilterColorize() {
        super(1);
        colorGradientParam = CreateLocalColorGradientParam("Gray to Color Mapping");
    }

    public OutputType getChannelInputType(int idx) {
        if (idx == 0) return OutputType.SCALAR; else System.err.println("Invalid channel access in " + this);
        return OutputType.SCALAR;
    }

    public OutputType getOutputType() {
        return OutputType.RGBA;
    }

    private final void _function(Vector4 out, Vector4 in) {
        out.set(colorGradientParam.get().getColor(in.XYZto1f()));
    }

    protected void cache_function(Vector4 out, TileCacheEntry[] caches, int localX, int localY, float u, float v) {
        _function(out, caches[0].sample(localX, localY));
    }

    protected float _value1f(float u, float v) {
        Vector4 val = valueRGBA(u, v);
        return (val.x + val.y + val.z) * (1.0f / 3.0f);
    }

    protected Vector4 _valueRGBA(float u, float v) {
        Vector4 ret = new Vector4();
        _function(ret, inputChannels[0].valueRGBA(u, v));
        return ret;
    }
}
