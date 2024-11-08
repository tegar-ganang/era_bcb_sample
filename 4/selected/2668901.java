package net.sourceforge.olduvai.lrac.drawer.queries;

import java.util.Date;
import java.util.List;
import net.sourceforge.olduvai.lrac.TimeRangeSampleIntervalRelation;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.Strip;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.AbstractQuery;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.DetailQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelItemInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceInterface;

/**
 * Immutable DetailQuery implementation. 
 * 
 * @author peter
 *
 */
public class DetailQuery extends AbstractQuery implements DetailQueryInterface {

    private final SourceInterface source;

    private final Date beginDate;

    private final Date endDate;

    private final Strip strip;

    private final TimeRangeSampleIntervalRelation interval;

    /**
	 * Constructor specifies all fields of the object
	 *  
	 * @param source SourceInterface for which we plan to collect data
	 * @param strip Strip where the data is destined (provides list of input channels)
	 * @param beginDate Begin date of first data value
	 * @param endDate End date of last data value
	 * @param interval Aggregation level requested in samples / hour provided by interval object. {@link TimeRangeSampleIntervalRelation}
	 * @param queryNumber unique incremented number for this query
	 */
    public DetailQuery(SourceInterface source, Strip strip, Date beginDate, Date endDate, TimeRangeSampleIntervalRelation interval, long queryNumber) {
        super(queryNumber);
        this.source = source;
        this.strip = strip;
        this.beginDate = beginDate;
        this.endDate = endDate;
        this.interval = interval;
    }

    public SourceInterface getSource() {
        return source;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public List<InputChannelItemInterface> getChannelList() {
        return strip.getChannels();
    }

    public TimeRangeSampleIntervalRelation getInterval() {
        return interval;
    }

    public Strip getStrip() {
        return strip;
    }
}
