package ch.laoe.audio;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/***********************************************************

This file is part of LAoE.

LAoE is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation; either version 2 of the License,
or (at your option) any later version.

LAoE is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LAoE; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


Class:			AudioPlotter
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	.

History:
Date:			Description:									Autor:
02.12.00		new stream-technique							oli4
28.07.01		graphics dirty region management for
            play pointer         						oli4

***********************************************************/
public class AudioPlotter {

    /**
	* constructor
	*/
    protected AudioPlotter(Audio a) {
        this.audio = a;
    }

    protected Audio audio;

    /**
	* returns the audio, which this plotter represents.
	*/
    public Audio getAudio() {
        return audio;
    }

    protected int xPlayPointer;

    /**
	*	paints the play pointers
	*/
    public void paintPlayPointer(Graphics2D g2d, Rectangle rect, Color color) {
        xPlayPointer = audio.getClip().getSelectedLayer().getChannel(0).getPlotter().sampleToGraphX(audio.getPlayPointer());
        int y0 = (int) rect.getX();
        int y1 = (int) (rect.getX() + rect.getHeight());
        g2d.setClip(rect);
        g2d.setStroke(new BasicStroke());
        g2d.setColor(color);
        g2d.drawLine(xPlayPointer, y0, xPlayPointer, y1);
    }

    /**
	*	returns the x coordinate of the play-pointer
	*/
    public int getXPlayPointer() {
        return xPlayPointer;
    }

    protected int xLoopStartPointer;

    protected int xLoopEndPointer;

    /**
	*	paints the loop pointers
	*/
    public void paintLoopPointer(Graphics2D g2d, Rectangle rect, Color color) {
        int y0 = (int) rect.getX();
        int y1 = (int) (rect.getX() + rect.getHeight());
        int h = 5;
        g2d.setClip(rect);
        g2d.setStroke(new BasicStroke());
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2d.setColor(color);
        xLoopStartPointer = audio.getClip().getSelectedLayer().getChannel(0).getPlotter().sampleToGraphX(audio.getLoopStartPointer());
        g2d.drawLine(xLoopStartPointer, y0, xLoopStartPointer, y1);
        xLoopEndPointer = audio.getClip().getSelectedLayer().getChannel(0).getPlotter().sampleToGraphX(audio.getLoopEndPointer());
        g2d.drawLine(xLoopEndPointer, y0, xLoopEndPointer, y1);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2d.fillRect(xLoopStartPointer, y1 - h, xLoopEndPointer - xLoopStartPointer, h);
    }

    /**
	*	returns the x coordinate of the loop start pointer
	*/
    public int getXLoopStartPointer() {
        return xLoopStartPointer;
    }

    /**
	*	returns the x coordinate of the loop end pointer
	*/
    public int getXLoopEndPointer() {
        return xLoopEndPointer;
    }
}
