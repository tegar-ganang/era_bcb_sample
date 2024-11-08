package info.port4.bbsp2.raster;

import info.port4.bbsp2.*;
import info.port4.bbsp2.models.color.*;
import info.port4.bbsp2.rendering.*;
import info.port4.bbsp2.util.ArrayStore;
import java.awt.Color;
import java.awt.Rectangle;

/**
 * <code>ByteRasterLayer</code> is RasterLayer by 8bit pixelmap.
 * Pixels commonly used as indexed color with <code>ColorPaletteModel</code>.
 *
 * @see info.port4.bbsp2.color.ColorPaletteModel
 * @author <a href="mailto:harumanx@geocities.co.jp">MIYABE Tatsuhiko</a>
 * @version $Id: ByteRasterLayer.java,v 1.11 2002/06/23 11:48:20 harumanx Exp $
 */
public class ByteRasterLayer extends RasterLayer {

    private byte[] pixels;

    private ColorPaletteModel cpm = new DefaultColorPaletteModel();

    public class ByteRasterContent extends RasterContent {

        public ColorPaletteModel getColorPaletteModel() {
            return ByteRasterLayer.this.getColorPaletteModel();
        }
    }

    protected final RasterContent content = new ByteRasterContent();

    public ByteRasterLayer() {
        setRenderer(getRenderer(Renderer.ROLE_UNIFIER));
    }

    public ByteRasterLayer(int w, int h) {
        this(0, 0, w, h);
    }

    public ByteRasterLayer(int x, int y, int w, int h) {
        setLocation(x, y);
        setSize(w, h);
    }

    public void dispose() {
        ArrayStore.release(this.pixels);
    }

    public void setColorPaletteModel(ColorPaletteModel cpm) {
        this.cpm = cpm;
        cpm.addColorPaletteListener(new ColorPaletteListener() {

            public void colorIndexChanged(ColorPaletteEvent e) {
            }

            public void colorValueChanged(ColorPaletteEvent e) {
                ByteRasterLayer.this.repaint();
            }
        });
    }

    public ColorPaletteModel getColorPaletteModel() {
        return this.cpm;
    }

    public void setSize(int w, int h) {
        super.setSize(w, h);
        if (this.pixels != null) {
            ArrayStore.release(this.pixels);
        }
        this.pixels = (byte[]) ArrayStore.get(byte.class, w * h);
        for (int i = 0; i < this.pixels.length; ++i) {
            this.pixels[i] = 0;
        }
    }

    protected Object getPixels(int channelIndex) {
        if (channelIndex == 0) {
            return pixels;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public int getChannelDepth(int channelIndex) {
        if (channelIndex == 0) {
            int l = cpm.getArray().length;
            for (int i = 0; ; i++) {
                if ((l >>= 1) == 0) {
                    return i;
                }
            }
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public int getChannelsCount() {
        return 1;
    }

    public Layer createCompatibleLayer(Rectangle bounds) {
        return new ByteRasterLayer(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    protected Content getContent() {
        return this.content;
    }
}
