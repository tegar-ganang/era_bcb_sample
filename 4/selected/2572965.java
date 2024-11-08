package net.sourceforge.olduvai.lrac.dataservice.queries;

import java.util.TreeSet;

public abstract class AbstractAggQuery extends AbstractQuery {

    TreeSet<String> sourceNames = new TreeSet<String>();

    TreeSet<String> channelNames = new TreeSet<String>();

    public AbstractAggQuery() {
        super();
    }

    public TreeSet<String> getSourceNames() {
        return sourceNames;
    }

    public TreeSet<String> getChannelNames() {
        return channelNames;
    }
}
