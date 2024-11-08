package net.sf.pulse.data;

import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

/**
 * Data source.
 * 
 * Defines one physical data source. May contain several channels.
 */
public abstract class Source<E> {

    private final URL sourceDef;

    private final Set<String> channelNames = new TreeSet<String>();

    public Source(URL sourceDef) {
        if (sourceDef == null) {
            throw new IllegalArgumentException("sourceDef can't be null");
        }
        this.sourceDef = sourceDef;
    }

    /**
	 * @return Channel names known by this time. This set persists and may be reinitialized with {@link #reset()}.
	 */
    public Set<String> getChannelNames() {
        return channelNames;
    }

    /**
	 * Clear {@link #getChannelNames()} channel names.
	 */
    public void reset() {
    }

    /**
	 * Blocking read operation.
	 * 
	 * @return data sample from the source.
	 */
    public abstract DataSample<E> read();
}
