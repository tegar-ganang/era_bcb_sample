package net.sourceforge.olduvai.lrac.darkstardataservice;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import net.sourceforge.olduvai.lrac.TimeRangeSampleIntervalRelation;
import net.sourceforge.olduvai.lrac.drawer.queries.DetailQuery;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.Strip;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.AbstractQuery;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelItemInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceInterface;

/**
 * This query is constructed from multiple instances of individual detail queries 
 * which are requesting data for the same group of input channels. (Strip).  
 * 
 * When the data request is sent to the server, all enqueued detail queries are 
 * sent to the server.   
 * 
 * @author peter
 *
 */
public class ChartDetailBulkQuery extends AbstractQuery {

    List<SourceInterface> sources = new LinkedList<SourceInterface>();

    private final Date beginDate;

    private final Date endDate;

    private final Strip strip;

    private final TimeRangeSampleIntervalRelation interval;

    /**
	 * Constructs a new ChartDetailBulkQuery.  Once beginDate, endDate,
	 * strip and interval 
	 * are established, all queries that are added to this bulk queries
	 * must be identical for these variables. Queries added that do not 
	 * adhere to these requirements will cause a runtime exception.  
	 *   
	 * @param beginDate
	 * @param endDate
	 * @param s
	 * @param interval
	 * @param queryID
	 */
    public ChartDetailBulkQuery(Date beginDate, Date endDate, Strip s, TimeRangeSampleIntervalRelation interval, long queryID) {
        super(queryID);
        this.beginDate = beginDate;
        this.endDate = endDate;
        this.strip = s;
        this.interval = interval;
    }

    /**
	 * Adds a detail query to the bulk query.  
	 * 
	 * @param q
	 */
    public void addDetailQuery(DetailQuery q) {
        if (q.getBeginDate().equals(beginDate) && q.getEndDate().equals(endDate) && q.getStrip() == strip && q.getInterval().equals(interval)) {
        } else {
            throw new RuntimeException("Detail query added that does not match with bulk query parameters");
        }
        sources.add(q.getSource());
    }

    public List<SourceInterface> getSourceList() {
        return sources;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public TimeRangeSampleIntervalRelation getInterval() {
        return interval;
    }

    public List<InputChannelItemInterface> getChannelList() {
        return strip.getChannels();
    }

    public SourceInterface getSourceByName(String sourceName) {
        for (SourceInterface s : sources) {
            if (s.getName().equals(sourceName)) return s;
        }
        throw new RuntimeException("Database returned source not in the query.");
    }
}
