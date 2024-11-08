package org.polepos.circuits.sepang;

import org.polepos.framework.*;

/**
 * @author Herkules
 */
public class Sepang extends Circuit {

    @Override
    public String description() {
        return "writes, reads and then deletes an object tree";
    }

    @Override
    protected void addLaps() {
        add(new Lap("write"));
        add(new Lap("read"));
        add(new Lap("read_hot", true, true));
        add(new Lap("delete"));
    }

    @Override
    public Class requiredDriver() {
        return SepangDriver.class;
    }
}
