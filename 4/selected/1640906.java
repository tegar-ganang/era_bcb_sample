package com.cameocontrol.cameo.output;

import java.util.TreeSet;
import java.util.Vector;
import com.cameocontrol.cameo.control.CameoCue;
import com.cameocontrol.cameo.control.ConsoleCue;
import com.cameocontrol.cameo.control.ConsoleFade;
import com.cameocontrol.cameo.file.adt.PatchData;

public abstract class BasicPatch implements Patch {

    protected int _totalChannels;

    protected int _totalDimmers;

    protected Vector<TreeSet<Integer>> _patch;

    protected BasicPatch() {
        _totalChannels = 512;
        _totalDimmers = 512;
        _patch = new Vector<TreeSet<Integer>>();
        for (int x = 0; x < _totalChannels; x++) _patch.add(new TreeSet<Integer>());
    }

    public void setTotalChannels(int c) {
        Vector<TreeSet<Integer>> newPatch = new Vector<TreeSet<Integer>>();
        for (int x = 0; x < c; x++) if (x < _totalChannels) newPatch.add(x, _patch.get(x)); else newPatch.add(x, new TreeSet<Integer>());
        _totalChannels = c;
        _patch = newPatch;
    }

    public void setTotalDimmers(int d) {
        for (int x = d; x < _totalDimmers; x++) unpatchDim(x);
        _totalDimmers = d;
    }

    public int getTotalChannels() {
        return _totalChannels;
    }

    public int getTotalDimmers() {
        return _totalDimmers;
    }

    public void unpatchChan(int c) {
        _patch.get(c).clear();
    }

    public void unpatchDim(int d) {
        for (TreeSet<Integer> hs : _patch) if (hs.contains(new Integer(d))) hs.remove(new Integer(d));
    }

    public void patch(int c, int d) {
        unpatchDim(d);
        _patch.get(c).add(new Integer(d));
    }

    public TreeSet<Integer> getDimsFor(int c) {
        return _patch.get(c);
    }

    public int getChannelFor(int d) {
        for (int x = 0; x < _totalChannels; x++) if (_patch.get(x).contains(new Integer(d))) return x;
        return -1;
    }

    public boolean isDimPatched(int d) {
        if (d < _totalDimmers && d >= 0) for (int x = 0; x < _totalChannels; x++) if (_patch.get(x).contains(new Integer(d))) return true;
        return false;
    }

    public boolean isChanPatched(int c) {
        if (c < _totalChannels && c >= 0) return (!_patch.get(c).isEmpty()); else return false;
    }

    public void setPatch1to1() {
        for (int x = 0; x < _totalChannels && x < _totalDimmers; x++) {
            TreeSet<Integer> hs = new TreeSet<Integer>();
            hs.add(new Integer(x));
            _patch.set(x, hs);
        }
    }

    public TreeSet<Integer> getUnpachedDims() {
        TreeSet<Integer> dims = new TreeSet<Integer>();
        for (int x = 0; x < _totalDimmers; x++) dims.add(new Integer(x));
        for (TreeSet<Integer> chan : _patch) dims.removeAll(chan);
        return dims;
    }

    public void unpatchAll() {
        _patch = new Vector<TreeSet<Integer>>();
        for (int x = 0; x < _totalChannels; x++) _patch.add(new TreeSet<Integer>());
    }

    public ConsoleCue translateLevelsToChan(short[] d) {
        ConsoleCue cue = new CameoCue();
        for (int x = 0; x < _totalDimmers && x < d.length; x++) {
            int chan = getChannelFor(x);
            if (chan > -1 && d[x] > 0) cue.setLevel(chan, d[x]);
        }
        return cue;
    }

    public short[] translateLevelsToDim(ConsoleCue c) {
        short[] dims = new short[_totalDimmers];
        for (int x = 0; x < dims.length; x++) dims[x] = 0;
        for (int x = 0; x < _patch.size(); x++) {
            if (c.getLevel(x) > 0) {
                TreeSet<Integer> dimmers = _patch.get(x);
                for (Integer aDim : dimmers) dims[aDim.intValue()] = c.getLevel(x);
            }
        }
        return dims;
    }

    public PatchData distill() {
        PatchData pd = new PatchData();
        for (int x = 0; x < getTotalChannels(); x++) {
            TreeSet<Integer> dimmers = getDimsFor(x);
            for (Integer dim : dimmers) pd.patch(x, dim);
        }
        return pd;
    }

    public void extractFrom(PatchData data) {
        if (data.contains(ID_ATTRIB)) assert (data.get(ID_ATTRIB).equals(getIDValue()));
        _totalChannels = data.getTotalChannels();
        _totalDimmers = data.getTotalDimmers();
        _patch = new Vector<TreeSet<Integer>>();
        for (int x = 0; x < _totalChannels; x++) {
            TreeSet<Integer> dims = new TreeSet<Integer>();
            if (data.containsKey(new Integer(x))) {
                for (Integer dim : data.get(new Integer(x))) {
                    dims.add(dim);
                }
            }
            _patch.add(dims);
        }
    }
}
