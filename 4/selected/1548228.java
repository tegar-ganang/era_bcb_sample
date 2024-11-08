package org.polepos.circuits.montreal;

import org.polepos.framework.*;

public class Montreal extends CircuitBase {

    @Override
    public String description() {
        return "writes and reads 1000 ArrayLists";
    }

    @Override
    protected void addLaps() {
        add(new Lap("write"));
        add(new Lap("read"));
    }

    @Override
    public Class requiredDriver() {
        return MontrealDriver.class;
    }
}
