package hdr_plugin.tonemapping;

import ij.ImagePlus;

/**
 *
 * @author Alexander Heidrich <alexander.heidrich@hki-jena.de>
 */
public class SimpleToneMapper {

    private ImagePlus imp;

    public SimpleToneMapper(ImagePlus imp) {
        this.imp = imp;
    }

    public double[] map() {
        float[] pixels = (float[]) imp.getChannelProcessor().getPixels();
        double out[] = new double[pixels.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = pixels[i] / (pixels[i] + 1);
        }
        return out;
    }
}
