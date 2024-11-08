package org.sw.wavepane;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.logging.Logger;
import org.sw.asp.JSndInfo;
import org.sw.asp.reader.JSndWaveReader;
import org.sw.utils.tools;

/**
 *
 * @author Rui Dong
 */
public class WaveDataSource implements WavePaneDataSource {

    JSndWaveReader sr;

    JSndInfo info;

    static Logger logger = Logger.getLogger(WaveDataSource.class.getName());

    float[] v;

    /** Creates a new instance of WaveData */
    public WaveDataSource(String filename) {
        sr = new JSndWaveReader();
        try {
            sr.open(filename);
            info = sr.getInfo();
            int len = info.getFrames() * info.getChannels();
            v = sr.read_items_float(len / 2);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public double getSeconds() {
        return sr.getInfo().getSections();
    }

    public Points getPoints(Rectangle rect, double time, double time_start, int amp) {
        int pct = (int) (sr.getInfo().getSamplerate() * time);
        Points p = new Points(pct <= rect.width);
        double[] value = new double[pct];
        int pos = (int) (time_start * info.getSamplerate());
        for (int i = pos; i < pct + pos; i++) {
            value[i] = 1f - ((i < v.length) ? (v[i]) : 0f);
        }
        if (p.isSparsity()) {
            for (int i = 0; i < pct; i++) p.add(new Point(i, (int) (amp * value[i])));
        } else {
            int len = pct / rect.width;
            for (int i = 0; i < rect.width; i++) {
                Point.Double pd = tools.getMMPoint(value, i * len, len);
                p.add(new Point((int) (pd.x * amp), (int) (pd.y * amp)));
            }
        }
        return p;
    }
}
