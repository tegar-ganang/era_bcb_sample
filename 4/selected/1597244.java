package com.manydesigns.portofino.base;

import com.manydesigns.portofino.base.workflow.MDWfState;
import java.util.Set;

/**
 *
 * @author Paolo Predonzani - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo      - angelo.lupo@manydesigns.com
 */
public class MDActorAttribute {

    public static final String copyright = "Copyright (c) 2005-2009, ManyDesigns srl";

    private final int id;

    private final MDAttribute attr;

    private final MDWfState state;

    private final boolean canWrite;

    /** Creates a new instance of MDActorWfTransition */
    public MDActorAttribute(int id, MDAttribute attr, MDWfState state, boolean canWrite) {
        this.id = id;
        this.attr = attr;
        this.state = state;
        this.canWrite = canWrite;
    }

    public void fillAttributes(Set readSet, Set writeSet, MDWfState state) throws Exception {
        if (getState() == state) {
            readSet.add(attr);
            if (canWrite) writeSet.add(attr);
        }
    }

    public MDAttribute getAttribute() {
        return attr;
    }

    public MDWfState getState() {
        return state;
    }

    public int getId() {
        return id;
    }

    public boolean isCanWrite() {
        return canWrite;
    }
}
