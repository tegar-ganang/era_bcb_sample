package org.apache.batik.swing.svg;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Graphics;
import org.apache.batik.swing.gvt.Overlay;

/**
 * One line Class Desc
 *
 * Complete Class Desc
 *
 * @author <a href="mailto:deweese@apache.org">deweese</a>
 * @version $Id: SVGUpdateOverlay.java,v 1.1 2005/11/21 09:51:34 dev Exp $
 */
public class SVGUpdateOverlay implements Overlay {

    List rects = new LinkedList();

    int size, updateCount;

    int[] counts;

    public SVGUpdateOverlay(int size, int numUpdates) {
        this.size = size;
        counts = new int[numUpdates];
    }

    public void addRect(Rectangle r) {
        rects.add(r);
        if (rects.size() > size) rects.remove(0);
        updateCount++;
    }

    public void endUpdate() {
        int i = 0;
        int total = 0;
        for (; i < counts.length - 1; i++) {
            counts[i] = counts[i + 1];
        }
        counts[i] = updateCount;
        updateCount = 0;
        int num = rects.size();
        for (i = counts.length - 1; i >= 0; i--) {
            if (counts[i] > num) {
                counts[i] = num;
            }
            num -= counts[i];
        }
        counts[0] += num;
    }

    public void paint(Graphics g) {
        Iterator i = rects.iterator();
        int count = 0;
        int idx = 0;
        int group = 0;
        while ((group < counts.length - 1) && (idx == counts[group])) group++;
        int cmax = counts.length - 1;
        while (i.hasNext()) {
            Rectangle r = (Rectangle) i.next();
            Color c;
            c = new Color(1f, (cmax - group) / (float) cmax, 0, (count + 1f) / rects.size());
            g.setColor(c);
            g.drawRect(r.x, r.y, r.width, r.height);
            count++;
            idx++;
            while ((group < counts.length - 1) && (idx == counts[group])) {
                group++;
                idx = 0;
            }
        }
    }
}
