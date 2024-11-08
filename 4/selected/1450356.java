package info.port4.bbsp2.raster;

import java.awt.Rectangle;
import info.port4.bbsp2.*;
import info.port4.bbsp2.rendering.*;
import info.port4.bbsp2.util.ArrayStore;

/**
 * <code>IntRasterLayer</code> is RasterLayer by 32bit pixelmap.
 * Pixels commonly used as packed 1byte RGB and alpha channel.
 *
 * @author <a href="mailto:harumanx@geocities.co.jp">MIYABE Tatsuhiko</a>
 * @version $Id: IntRasterLayer.java,v 1.7 2002/06/23 11:48:20 harumanx Exp $
 */
public class IntRasterLayer extends RasterLayer {

    private int[] pixels;

    public IntRasterLayer() {
        setRenderer(getRenderer(Renderer.ROLE_UNIFIER));
    }

    public IntRasterLayer(int w, int h) {
        this(0, 0, w, h);
    }

    public IntRasterLayer(int x, int y, int w, int h) {
        setLocation(x, y);
        setSize(w, h);
    }

    public void dispose() {
        ArrayStore.release(this.pixels);
    }

    public void setSize(int w, int h) {
        super.setSize(w, h);
        if (this.pixels != null) {
            ArrayStore.release(this.pixels);
        }
        this.pixels = (int[]) ArrayStore.get(int.class, w * h);
        for (int i = 0; i < this.pixels.length; ++i) {
            this.pixels[i] = 0;
        }
    }

    protected Object getPixels(int channelIndex) {
        if (channelIndex == 0) {
            return pixels;
        } else {
            throw new ArrayIndexOutOfBoundsException(channelIndex);
        }
    }

    public int getChannelDepth(int channelIndex) {
        if (channelIndex == 0) {
            return 32;
        } else {
            throw new ArrayIndexOutOfBoundsException(channelIndex);
        }
    }

    public int getChannelsCount() {
        return 1;
    }

    public Layer createCompatibleLayer(Rectangle bounds) {
        return new IntRasterLayer(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
