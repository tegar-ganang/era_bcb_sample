package com.cameocontrol.cameo.dataStructure.jgraph;

import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import com.cameocontrol.cameo.control.CameoCue;
import com.cameocontrol.cameo.control.ConsoleCue;
import com.cameocontrol.cameo.output.LiveCue;

public class GraphCell extends DefaultGraphCell {

    protected int _channelNumber;

    protected GraphCell(int cn, LiveCue c) {
        _channelNumber = cn;
        this.setUserObject(new CueChannelWraper(cn, c));
        GraphConstants.setOpaque(this.getAttributes(), true);
        GraphConstants.setEditable(this.getAttributes(), false);
        GraphConstants.setSizeable(this.getAttributes(), false);
        GraphConstants.setMoveable(this.getAttributes(), false);
    }

    public int getChannelNumber() {
        return _channelNumber;
    }

    protected class CueChannelWraper {

        private LiveCue _cue;

        private int _channelNumber;

        CueChannelWraper(int cn, LiveCue c) {
            super();
            _cue = c;
            _channelNumber = cn;
        }

        public String toString() {
            if (_cue.getLevel(_channelNumber) >= 0) return Integer.toString(_channelNumber + 1) + "\n" + _cue.getLevel(_channelNumber);
            return Integer.toString(_channelNumber + 1) + "\n";
        }
    }
}
