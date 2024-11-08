package org.jogre.carTricks.client;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import javax.swing.ImageIcon;
import org.jogre.client.awt.GameImages;
import java.lang.Math;
import org.jogre.client.awt.JogreComponent;

public class CarTricksPositionComponent extends JogreComponent {

    private static final int OUTSIDE_SPACING = 15;

    private static final int V_SPACING = 5;

    private CarTricksClientModel model;

    private CarTricksGraphics CT_graphics;

    private int start_x, start_y;

    private int displayCode;

    public int dragging_index;

    private int drag_off_x, drag_off_y;

    private int curr_drag_x, curr_drag_y;

    private int hcenter, vcenter;

    /**
	 * Constructor which creates the position component
	 *
	 * @param model					The game model
	 * @param displayCode			Code that indicates which array to display.
	 *									@see model.getCarArray()
	 */
    public CarTricksPositionComponent(CarTricksClientModel model, int displayCode) {
        this.model = model;
        this.CT_graphics = CarTricksGraphics.getInstance();
        this.displayCode = displayCode;
        dragging_index = -1;
        drag_off_x = 0;
        drag_off_y = 0;
        curr_drag_x = 0;
        curr_drag_y = 0;
        hcenter = (CT_graphics.largeCarWidth / 2);
        vcenter = (CT_graphics.largeCarHeight / 2);
        int w = 2 * OUTSIDE_SPACING + CT_graphics.largeCarWidth;
        int h = 2 * OUTSIDE_SPACING + 6 * CT_graphics.largeCarHeight + 5 * V_SPACING;
        start_x = OUTSIDE_SPACING + hcenter;
        start_y = OUTSIDE_SPACING + vcenter;
        Dimension dim = new Dimension(w, h);
        setPreferredSize(dim);
        setMinimumSize(dim);
    }

    /**
	 * Update the graphics depending on the model
	 *
	 * @param	g			The graphics area to draw on
	 */
    public void paintComponent(Graphics g) {
        int[] car_array = model.getCarArray(displayCode);
        int drag_color = 0;
        if (dragging_index >= 0) {
            drag_color = car_array[dragging_index];
            car_array[dragging_index] = -1;
        }
        CT_graphics.paintLargeCarArray(g, start_x, start_y, car_array, 0, (CT_graphics.largeCarHeight + V_SPACING));
        if (dragging_index >= 0) {
            CT_graphics.paintLargeCar(g, curr_drag_x, curr_drag_y, drag_color);
            car_array[dragging_index] = drag_color;
        }
    }

    /**
	 * Return the index (0..(n-1)) of the bid whose rectangle (x,y) lies inside of
	 * (where n = the number of cars in the race)
	 *
	 * @param (x,y)			The location of the point to be tested
	 * @returns				0..(n-1) => index of the bid at that point.
	 *						-1 => (x,y) not within a bid
	 */
    public int getBidIndex(int x, int y) {
        if ((x < OUTSIDE_SPACING) || (x > (OUTSIDE_SPACING + CT_graphics.largeCarWidth))) {
            return (-1);
        }
        y -= OUTSIDE_SPACING;
        int index = y / (CT_graphics.largeCarHeight + V_SPACING);
        if ((y - (index * (CT_graphics.largeCarHeight + V_SPACING))) > CT_graphics.largeCarHeight) {
            return (-1);
        }
        if (index >= model.numCarsInRace()) {
            return (-1);
        }
        return (index);
    }

    /**
	 * Try to start dragging at point (x,y) in the component.
	 *
	 * @param	(x,y)		Location user clicked
	 * @return				true => Drag is ok to start
	 * 						false => Drag is not ok to start
	 */
    public boolean startDragAt(int x, int y) {
        dragging_index = getBidIndex(x, y);
        if (dragging_index < 0) {
            return (false);
        }
        drag_off_x = (x - OUTSIDE_SPACING - hcenter);
        drag_off_y = (y - (OUTSIDE_SPACING + (dragging_index * (CT_graphics.largeCarHeight + V_SPACING))) - vcenter);
        setDragPoint(x, y);
        return (true);
    }

    /**
	 * Set the drag point to the given location.  This will
	 * pin the drag point to the visible rectangle on the screen.
	 *
	 * @param	(x,y)		Location to set drag point to
	 */
    public void setDragPoint(int x, int y) {
        x = (x - drag_off_x);
        y = (y - drag_off_y);
        if (x < hcenter) x = hcenter;
        if (x > (getVisibleRect().width - hcenter)) x = (getVisibleRect().width - hcenter);
        if (y < vcenter) y = vcenter;
        if (y > (getVisibleRect().height - vcenter)) y = (getVisibleRect().height - vcenter);
        curr_drag_x = x;
        curr_drag_y = y;
    }

    /**
	 * End dragging at point (x,y).  This will return the index of the
	 * space that the dragging ended at.  The drag model is that the
	 * dragged car will be dropped between two other cars and so the
	 * index of where the car should be dropped depends on whether
	 * the car is being dragged up or down.
	 *
	 * @param	(x,y)		Location user ended drag at
	 * @return				index (0..(n-1)) of car that drag ended in.
	 *						-1 => ended outside of any range
	 */
    public int endDragAt(int x, int y) {
        int drop_index = (curr_drag_y - OUTSIDE_SPACING - vcenter + (CT_graphics.largeCarHeight + V_SPACING)) / (CT_graphics.largeCarHeight + V_SPACING);
        if (dragging_index < drop_index) {
            drop_index -= 1;
        }
        if ((drop_index < 0) || (drop_index >= model.numCarsInRace())) {
            drop_index = -1;
        }
        dragging_index = -1;
        return (drop_index);
    }

    /**
	 * Rearrange the cars in the array.  This will move the car at
	 * <from_index> to the space at <to_index> and slide all the
	 * other cars to fill in the gap.
	 *
	 * @param	from_index		The car that is moving from
	 * @param	to_index		The place the car is moving to
	 */
    public void rearrangeCars(int from_index, int to_index) {
        int[] car_array = model.getCarArray(displayCode);
        int i;
        int moving_car = car_array[from_index];
        if (from_index > to_index) {
            for (i = from_index; i > to_index; i--) {
                car_array[i] = car_array[i - 1];
            }
        } else {
            for (i = from_index; i < to_index; i++) {
                car_array[i] = car_array[i + 1];
            }
        }
        car_array[to_index] = moving_car;
    }
}
