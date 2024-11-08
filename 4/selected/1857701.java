package net.sourceforge.olduvai.lrac.drawer.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sourceforge.olduvai.lrac.TimeRangeSampleIntervalRelation;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.Strip;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.AbstractQuery;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.SwatchQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelItemInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceGroupInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceInterface;

/**
 * Implementation of the SwatchQueryInterface.  
 * 
 * @author peter
 *
 */
public class SwatchQuery extends AbstractQuery implements SwatchQueryInterface {

    final Date startDate;

    final Date endDate;

    final List<SourceGroupInterface> groupList;

    final List<SourceInterface> sourceList;

    final Collection<Strip> strips;

    final TimeRangeSampleIntervalRelation interval;

    /**
	 * Creates a new swatch query.  
	 * 
	 * @param startDate Timestamp for the start of the query
	 * @param endDate Timestamp for the end of the query
	 * @param sourceList  Individual sources that should be selected, this is a means to include "exceptions" where an entire group of sources is not desirable
	 * @param groupList Groups of sources that should be selected, these should not be redundant with the individual sources
	 * @param stripList The list of strips for which swatches are requested (means of obtaining input channel names) 
	 * @param timeRangeSampleIntervalRelation The desired data aggregation level in samples / hour.  
	 */
    public SwatchQuery(Date startDate, Date endDate, List<SourceInterface> sourceList, List<SourceGroupInterface> groupList, Collection<Strip> stripList, TimeRangeSampleIntervalRelation timeRangeSampleIntervalRelation, long queryNumber) {
        super(queryNumber);
        this.startDate = startDate;
        this.endDate = endDate;
        this.groupList = groupList;
        this.sourceList = sourceList;
        this.strips = stripList;
        this.interval = timeRangeSampleIntervalRelation;
    }

    public List<InputChannelItemInterface> getInputChannelItemList() {
        List<InputChannelItemInterface> inputChannelItemList = new ArrayList<InputChannelItemInterface>(strips.size());
        for (Iterator<Strip> it = strips.iterator(); it.hasNext(); ) {
            final Strip s = it.next();
            final InputChannelItemInterface c = s.getChannel(0);
            if (c != null) inputChannelItemList.add(c);
        }
        return inputChannelItemList;
    }

    /**
	 * TODO: currently unimplemented
	 */
    public List<SourceInterface> getFilterList() {
        return new ArrayList<SourceInterface>();
    }

    public List<SourceGroupInterface> getSourceGroupList() {
        return groupList;
    }

    public List<SourceInterface> getSourceList() {
        return sourceList;
    }

    public Date getEndDate() {
        return endDate;
    }

    public TimeRangeSampleIntervalRelation getInterval() {
        return interval;
    }

    public Date getBeginDate() {
        return startDate;
    }

    public List<SourceInterface> getMergedSourceList(Map<String, SourceInterface> sourceNameMap) {
        final List<SourceInterface> results = new ArrayList<SourceInterface>(sourceList);
        for (final SourceGroupInterface group : groupList) {
            final List<String> groupMembers = group.getSourceList();
            for (String member : groupMembers) {
                final SourceInterface source = sourceNameMap.get(member);
                if (source != null) results.add(source);
            }
        }
        return results;
    }
}
