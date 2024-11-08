package org.polepos.circuits.melbourne;

import org.polepos.framework.*;

/**
 * @author Herkules
 */
public class Melbourne extends Circuit {

    @Override
    public String description() {
        return "writes, reads and deletes unstructured flat objects of one kind in bulk mode";
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
        return MelbourneDriver.class;
    }
}
