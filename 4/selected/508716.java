package psd.layer;

import java.io.IOException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import psd.base.PsdInputStream;

public class PsdSWTLayer extends PsdLayer {

    ImageData imageData;

    public PsdSWTLayer(PsdInputStream stream) throws IOException {
        super(stream);
    }

    public PsdSWTLayer(int width, int height, int channels) {
        super(width, height, channels);
    }

    public void readImage(PsdInputStream input, boolean needReadPlaneInfo, short[] lineLengths) throws IOException {
        byte[] r = null, g = null, b = null, a = null;
        for (int j = 0; j < numberOfChannels; j++) {
            int id = channelsInfo.get(j).getId();
            switch(id) {
                case 0:
                    r = readPlane(input, getWidth(), getHeight(), lineLengths, needReadPlaneInfo, j);
                    break;
                case 1:
                    g = readPlane(input, getWidth(), getHeight(), lineLengths, needReadPlaneInfo, j);
                    break;
                case 2:
                    b = readPlane(input, getWidth(), getHeight(), lineLengths, needReadPlaneInfo, j);
                    break;
                case -1:
                    a = readPlane(input, getWidth(), getHeight(), lineLengths, needReadPlaneInfo, j);
                    if (this.opacity != -1) {
                        double opacity = (this.opacity & 0xff) / 256d;
                        for (int i = 0; i < a.length; i++) {
                            a[i] = (byte) ((a[i] & 0xff) * opacity);
                        }
                    }
                    break;
                default:
                    input.skipBytes(getChannelInfoById(id).getDataLength());
            }
        }
        int n = getWidth() * getHeight();
        if (r == null) r = fillBytes(n, 0);
        if (g == null) g = fillBytes(n, 0);
        if (b == null) b = fillBytes(n, 0);
        if (a == null) a = fillBytes(n, 255);
        imageData = makeSWTImage(getWidth(), getHeight(), r, g, b, a);
    }

    protected ImageData makeSWTImage(int w, int h, byte[] r, byte[] g, byte[] b, byte[] a) {
        if (w == 0 || h == 0) {
            return null;
        }
        int s = w * h;
        if (r.length != s || g.length != s || b.length != s || a.length != s) {
            return null;
        }
        byte[] data = new byte[w * h * 4];
        ImageData iData = new ImageData(w, h, 32, new PaletteData(0xFF00, 0xFF0000, 0xFF000000));
        iData.data = data;
        for (int i = 0, j = 0; i < data.length; i += 4, j++) {
            data[i] = b[j];
            data[i + 1] = g[j];
            data[i + 2] = r[j];
            data[i + 3] = a[j];
        }
        return iData;
    }

    public ImageData getImageData() {
        return imageData;
    }
}
