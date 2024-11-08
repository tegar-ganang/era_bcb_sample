package uk.org.toot.audio.eq;

import java.util.ArrayList;
import uk.org.toot.audio.eq.EQ;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.filter.*;

/**
 * The abstract class for serial and parallel EQ.
 */
public abstract class AbstractEQ extends EQ {

    protected ArrayList<Filter> filters = new ArrayList<Filter>();

    /**
     * @supplierCardinality 1
     * @link aggregation 
     */
    protected EQ.Specification specification;

    protected int sampleRate = -1;

    private boolean wasBypassed;

    protected boolean relative;

    public AbstractEQ(EQ.Specification spec, boolean relative) {
        specification = spec;
        this.relative = relative;
        wasBypassed = !specification.isBypassed();
        createEQ(spec);
    }

    public void open() {
        for (Filter filter : filters) {
            filter.open();
        }
    }

    public void close() {
        for (Filter filter : filters) {
            filter.close();
        }
    }

    protected void clear() {
        for (Filter filter : filters) {
            filter.clear();
        }
    }

    protected void createEQ(EQ.Specification spec) {
        for (FilterSpecification fspec : spec.getFilterSpecifications()) {
            filters.add(createFilter(fspec));
            if (fspec.is4thOrder()) {
                filters.add(createFilter(fspec));
            }
        }
    }

    protected Filter createFilter(FilterSpecification fspec) {
        return new BiQuadFilter(fspec, relative);
    }

    public int getSize() {
        return filters.size();
    }

    public EQ.Specification getSpecification() {
        return specification;
    }

    public int processAudio(AudioBuffer buffer) {
        boolean bypassed = specification.isBypassed();
        if (bypassed) {
            if (!wasBypassed) {
                clear();
                wasBypassed = true;
            }
            return AUDIO_OK;
        }
        int newRate = (int) buffer.getSampleRate();
        if (sampleRate != newRate) {
            sampleRate = newRate;
            updateDesigns();
        }
        int nc = buffer.getChannelCount();
        int ns = buffer.getSampleCount();
        for (int c = 0; c < nc; c++) {
            filter(buffer.getChannel(c), ns, c);
        }
        wasBypassed = bypassed;
        return AUDIO_OK;
    }

    protected abstract int filter(float[] buffer, int length, int chan);

    protected void updateDesigns() {
        for (Filter f : filters) {
            f.setSampleRate(sampleRate);
        }
    }
}
