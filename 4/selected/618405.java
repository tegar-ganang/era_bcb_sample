package ch.laoe.clip;

import ch.laoe.operation.AOperation;
import ch.laoe.ui.GEditableArea;

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
27.10.2010	first draft     								oli4

***********************************************************/
public class AChannel2DSelection {

    /**
	* constructor
	*/
    public AChannel2DSelection(AChannel ch, GEditableArea area) {
        this.channel = ch;
        this.area = area;
    }

    private AChannel channel;

    private GEditableArea area;

    public GEditableArea getArea() {
        return area;
    }

    public AChannel getChannel() {
        return channel;
    }

    public boolean isSelected() {
        return area.isSomethingSelected();
    }

    public boolean isSelected(double x, double y) {
        return area.isSelected(x, y);
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
