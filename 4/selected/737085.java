package org.polepos.circuits.barcelona;

import org.polepos.framework.*;

public class Barcelona extends Circuit {

    @Override
    public String description() {
        return "writes, reads, queries and deletes objects with a 5 level inheritance structure";
    }

    @Override
    protected void addLaps() {
        add(new Lap("write"));
        add(new Lap("read"));
        add(new Lap("query"));
        add(new Lap("delete"));
    }

    @Override
    public Class requiredDriver() {
        return BarcelonaDriver.class;
    }
}
