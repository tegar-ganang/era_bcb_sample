package com.cameocontrol.cameo.control;

import java.util.Hashtable;
import java.util.Enumeration;
import com.cameocontrol.cameo.dataStructure.Position;
import com.cameocontrol.cameo.file.adt.MagicChannelData;
import com.cameocontrol.cameo.file.adt.MagicSheetData;

public abstract class BasicMagicSheet implements ConsoleMagicSheet {

    protected Hashtable<Integer, Position> _chans;

    public BasicMagicSheet() {
        _chans = new Hashtable<Integer, Position>();
    }

    public void AddChannel(int num, double x, double y) {
        _chans.put(new Integer(num), new Position(x, y));
    }

    public Position getChannel(int num) {
        if (_chans.containsKey(new Integer(num))) return _chans.get(new Integer(num)); else return new Position();
    }

    public Enumeration<Position> getChannels() {
        return _chans.elements();
    }

    public MagicSheetData distill() {
        MagicSheetData msd = new MagicSheetData();
        for (Integer chan : _chans.keySet()) {
            Position p = _chans.get(chan);
            msd.put(new Integer(chan), new MagicChannelData(p.getX(), p.getY()));
        }
        return msd;
    }

    public void extractFrom(MagicSheetData data) {
    }
}
