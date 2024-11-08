package org.chernovia.sims.wondrous;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import org.chernovia.lib.graphics.ColorSpectrum;
import org.chernovia.lib.music.midi.JMIDI;

public class WonderBox extends Canvas {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private ColorSpectrum spec;

    private BufferedImage buff_img;

    private int last_pitch = 0, curr_rad = 0, curr_inst = 0, curr_lim, MAX_RAD, MID_X, MID_Y;

    public WonderBox(int w, int h, int limit) {
        setBackground(Color.BLACK);
        setSize(w, h);
        resetSpectrum(limit);
        buff_img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        MID_X = w / 2;
        MID_Y = h / 2;
        MAX_RAD = (int) ((Math.sqrt((w * w) + (h * h))) / 2);
        setVisible(true);
    }

    private void resetSpectrum(int limit) {
        spec = new ColorSpectrum(limit, 255, 255, 255);
        curr_lim = limit;
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(buff_img, 0, 0, this);
    }

    public void wonderize(JWondrousMachine jwm) {
        wonderize(jwm, true, true);
    }

    public void wonderize(JWondrousMachine jwm, boolean play, boolean draw) {
        if (play) {
            int l = jwm.getLimit();
            if (l != curr_lim) {
                resetSpectrum(l);
            }
            int i = jwm.getInstrument();
            if (i != curr_inst) {
                JMIDI.setChannel(jwm.getChannel(), i);
                curr_inst = i;
            }
            int p = jwm.getCurrentPitch();
            if (p != last_pitch) {
                JMIDI.getChannel(jwm.getChannel()).noteOff(last_pitch);
                JMIDI.getChannel(jwm.getChannel()).noteOn(p, jwm.getVolume());
                last_pitch = p;
            }
        }
        if (draw) {
            Graphics g = buff_img.getGraphics();
            g.setColor(new Color(spec.getColor(jwm.getCurrentWondrousness())));
            g.drawOval(MID_X - curr_rad, MID_Y - curr_rad, curr_rad * 2, curr_rad * 2);
            repaint();
            if (++curr_rad > MAX_RAD) {
                curr_rad = 0;
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }
}
