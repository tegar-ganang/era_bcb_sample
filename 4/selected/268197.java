package ch.laoe.clip;

import java.util.ArrayList;
import ch.laoe.operation.AOToolkit;
import ch.laoe.operation.AOperation;

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


Class:			ASelection
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	a selection defines a continuous set of
               samples inside a channel.

History:
Date:			Description:									Autor:
25.07.00		erster Entwurf									oli4
03.08.00		neuer Stil        							oli4
19.12.00		float audio samples							oli4
16.05.01		add start/endOperation						oli4
11.01.02		introduce intensity-points					oli4

***********************************************************/
public class AChannelSelection extends ASelection {

    /**
	* constructor
	*/
    public AChannelSelection(AChannel ch) {
        super(ch);
        plotter = new ASelectionPlotter(this);
        initIntensityPoints();
    }

    /**
	* null-constructor
	*/
    public AChannelSelection() {
        super(null);
        plotter = new ASelectionPlotter(this);
        initIntensityPoints();
        length = 0;
    }

    /**
	*	easy constructor
	*/
    public AChannelSelection(AChannel ch, int offset, int length) {
        this(ch);
        setOffset(offset);
        setLength(length);
    }

    /**
	* copy-constructor
	*/
    public AChannelSelection(AChannelSelection s) {
        this((AChannel) s.model);
        this.name = s.name;
        this.offset = s.offset;
        this.length = s.length;
        this.plotter = s.plotter;
        intensityPoints = new ArrayList<Point>();
        for (int i = 0; i < s.getIntensityPoints().size(); i++) {
            intensityPoints.add(new Point((Point) s.getIntensityPoints().get(i)));
        }
        intensityUsed = s.intensityUsed;
        intensityScale = s.intensityScale;
    }

    public void copy(AChannelSelection s) {
        if (s == this) {
            return;
        }
        this.offset = s.offset;
        this.length = s.length;
        intensityPoints.clear();
        for (int i = 0; i < s.getIntensityPoints().size(); i++) {
            intensityPoints.add(new Point((Point) s.getIntensityPoints().get(i)));
        }
        intensityUsed = s.intensityUsed;
        intensityScale = s.intensityScale;
    }

    public AChannel getChannel() {
        return (AChannel) model;
    }

    public void setChannel(AChannel ch) {
        model = ch;
    }

    private static int nameCounter;

    /**
	*	set the default name of the layer
	*/
    public void setDefaultName() {
        setDefaultName("channelSelection", nameCounter++);
    }

    private ASelectionPlotter plotter;

    public ASelectionPlotter getPlotter() {
        return plotter;
    }

    private int offset;

    /**
	*	set offset
	*/
    public void setOffset(int o) {
        if (o < 0) offset = 0; else offset = o;
    }

    /**
	*	get offset
	*/
    public int getOffset() {
        return offset;
    }

    private int length;

    /**
	*	set length
	*/
    public void setLength(int l) {
        if (l < 0) length = 0; else length = l;
    }

    /**
	*	get length
	*/
    public int getLength() {
        return length;
    }

    /**
	*	returns true if x is inside the selectioned indexes
	*/
    public boolean isSelected(int x) {
        return (x >= offset) && (x < (offset + length));
    }

    /**
	*	returns true if anything is selected
	*/
    public boolean isSelected() {
        return length > 0;
    }

    private ArrayList<Point> intensityPoints;

    private boolean intensityUsed = false;

    /**
	 *	returns the arraylist of all intensity-points of this selection
	 */
    public ArrayList<Point> getIntensityPoints() {
        intensityChanged = true;
        return intensityPoints;
    }

    /**
	 *	returns the index of the next left intensity-point from x, where x is the normalized
	 *	horizontal position, in the range of 0 to 1.
	 */
    public int searchLeftIntensityPointIndex(double x) {
        for (int i = 0; i < intensityPoints.size(); i++) {
            if (x < ((AChannelSelection.Point) intensityPoints.get(i)).x) {
                return i;
            }
        }
        return 0;
    }

    /**
	 *	returns the index of the nearest intensity-point from x, where x is the normalized
	 *	horizontal position, in the range of 0 to 1.
	 */
    public int searchNearestIntensityPointIndex(double x) {
        double d = Double.MAX_VALUE;
        int nearestIndex = 0;
        for (int i = 0; i < intensityPoints.size(); i++) {
            double newD = Math.abs(x - ((AChannelSelection.Point) intensityPoints.get(i)).x);
            if (newD < d) {
                d = newD;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    /**
	 *	add a new intensity-point at the right place in the arraylist
	 *	if x corresponds to an existing point, then just change its y-value
	 */
    public void addIntensityPoint(double x, float y) {
        for (int i = 0; i < intensityPoints.size(); i++) {
            if (x == ((AChannelSelection.Point) intensityPoints.get(i)).x) {
                modifyIntensityPoint(i, x, y);
                return;
            }
        }
        if ((x > 0) && (x < 1)) {
            int i = searchLeftIntensityPointIndex(x);
            if ((i > 0) && (i < (intensityPoints.size()))) {
                intensityPoints.add(i, new AChannelSelection.Point(x, y));
                intensityChanged = true;
                intensityUsed = true;
            }
        }
    }

    /**
	 *	modify an existing intensity-point
	 */
    public void modifyIntensityPoint(int index, double x, float y) {
        Point p = (Point) intensityPoints.get(index);
        p.y = y;
        if ((index > 0) && (index < (intensityPoints.size() - 1))) {
            double xLeft = ((Point) intensityPoints.get(index - 1)).x;
            double xRight = ((Point) intensityPoints.get(index + 1)).x;
            if (x < xLeft) p.x = xLeft + .001f; else if (x > xRight) p.x = xRight - .001f; else p.x = x;
        }
        intensityChanged = true;
        intensityUsed = true;
    }

    /**
	 *	remove a intensity-point from the arraylist
	 */
    public void removeIntensityPoint(double x) {
        int i = searchNearestIntensityPointIndex(x);
        if ((i > 0) && (i < (intensityPoints.size() - 1))) {
            intensityPoints.remove(i);
            intensityChanged = true;
            intensityUsed = true;
        }
    }

    /**
	 *	clear the intensity-points to default value (all 1)
	 */
    public void clearIntensity() {
        intensityPoints.clear();
        intensityPoints.add(new AChannelSelection.Point(0.f, 1.f));
        intensityPoints.add(new AChannelSelection.Point(1.f, 1.f));
        intensityChanged = true;
        intensityUsed = false;
    }

    private int activeIntensityPointIndex = -1;

    public void setActiveIntensityPoint(double x) {
        if ((x > 0) && (x < 1)) {
            activeIntensityPointIndex = searchNearestIntensityPointIndex(x);
        } else {
            activeIntensityPointIndex = -1;
        }
    }

    public int getActiveIntensityPointIndex() {
        return activeIntensityPointIndex;
    }

    public static final int LINEAR_INTENSITY_SCALE = 1;

    public static final int SQUARE_INTENSITY_SCALE = 2;

    public static final int CUBIC_INTENSITY_SCALE = 3;

    public static final int SQUARE_ROOT_INTENSITY_SCALE = -1;

    private static int intensityScale = LINEAR_INTENSITY_SCALE;

    /**
	 * scale
	 */
    public static void setIntensityScale(int s) {
        intensityScale = s;
    }

    private MMArray px, py;

    private boolean intensityChanged;

    /**
	 *	returns the (interpolated) intensity at the given channel-index x
	 */
    public float getIntensity(int x) {
        if (!intensityUsed) {
            return 1.f;
        }
        if (intensityChanged) {
            if (px == null) {
                px = new MMArray(intensityPoints.size(), 0);
            }
            px.setLength(intensityPoints.size());
            if (py == null) {
                py = new MMArray(intensityPoints.size(), 0);
            }
            py.setLength(intensityPoints.size());
            for (int i = 0; i < intensityPoints.size(); i++) {
                px.set(i, (float) ((Point) intensityPoints.get(i)).x);
                py.set(i, (float) ((Point) intensityPoints.get(i)).y);
            }
            intensityChanged = false;
        }
        float i;
        switch(intensityScale) {
            case LINEAR_INTENSITY_SCALE:
                i = AOToolkit.interpolate1(px, py, (((float) x) - getOffset()) / getLength());
                break;
            case SQUARE_INTENSITY_SCALE:
                i = AOToolkit.interpolate2(px, py, (((float) x) - getOffset()) / getLength());
                break;
            case CUBIC_INTENSITY_SCALE:
                i = AOToolkit.interpolate3(px, py, (((float) x) - getOffset()) / getLength());
                break;
            case SQUARE_ROOT_INTENSITY_SCALE:
                i = AOToolkit.interpolate1(px, py, (((float) x) - getOffset()) / getLength());
                i = (float) Math.sqrt(i);
                break;
            default:
                i = 1;
        }
        if (i > 1) {
            return 1;
        }
        if (i < 0) {
            return 0;
        }
        return i;
    }

    /**
	 *	mixes directly original and modified sample in function of the
	 *	current intensity-value.
	 */
    public float mixIntensity(int index, float original, float modified) {
        float intensity = getIntensity(index);
        return modified * intensity + original * (1.f - intensity);
    }

    private void initIntensityPoints() {
        if (intensityPoints == null) {
            intensityPoints = new ArrayList<Point>();
            clearIntensity();
        }
    }

    /**
	 *	this class represents one point of intensity of this selection
	 */
    public static class Point {

        public double x, y;

        public Point(Point p) {
            x = p.x;
            y = p.y;
        }

        public Point(double x, double y) {
            setPoint(x, y);
        }

        public void setPoint(double x, double y) {
            if (x > 1) this.x = 1; else if (x < 0) this.x = 0; else this.x = x;
            if (y > 1) this.y = 1; else if (y < 0) this.y = 0; else this.y = y;
        }
    }

    /**
	*	operate this channel
	*/
    public void operateChannel(AOperation o) {
        o.startOperation();
        if (isSelected()) {
            o.operate(this);
        }
        o.endOperation();
        System.gc();
    }
}
