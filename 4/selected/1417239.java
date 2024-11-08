package net.sourceforge.olduvai.lrac.dataservice.queries;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.olduvai.lrac.dataservice.records.SourceGroup;
import net.sourceforge.olduvai.lrac.drawer.strips.Strip;
import net.sourceforge.olduvai.lrac.drawer.strips.StripChannel;

/**
 * A query used for gathering cell swatch color information
 * 
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 *
 */
public class CellSwatchQuery extends AbstractQuery {

    static final String BASESWATCHQUERY = "&query=swatchquery";

    String queryString;

    /**
	 * Specify beginning and end dates for this query
	 * 
	 * @param startDate
	 * @param endDate
	 */
    public CellSwatchQuery(Date startDate, Date endDate, Iterator<SourceGroup> groupList, Iterator<Strip> stripIter, int alarmRequestType, int aggType) {
        this.aggType = aggType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.alarmRequestType = alarmRequestType;
        String baseQuery = getCoreQuery() + BASESWATCHQUERY;
        StringBuffer strbuf = new StringBuffer(baseQuery + "&beginTime=" + df.format(startDate) + "&endTime=" + df.format(endDate));
        while (groupList.hasNext()) {
            SourceGroup g = groupList.next();
            strbuf.append("&group=" + g.getType() + "|" + g.getShortname());
        }
        while (stripIter.hasNext()) {
            Strip strip = stripIter.next();
            try {
                StripChannel channel = strip.getFirstChannel();
                if (channel == null) {
                    System.err.println("No first channel for strip: " + strip);
                    continue;
                }
                if (channel.getType() == StripChannel.STAT) {
                    strbuf.append("&sChannel=" + channel.getChannelID());
                } else if (channel.getType() == StripChannel.PREFIX) {
                    List<StripChannel> channelList = strip.getChannelGroupList(channel);
                    Iterator<StripChannel> channelListIt = channelList.iterator();
                    while (channelListIt.hasNext()) {
                        final StripChannel c = channelListIt.next();
                        strbuf.append("&sChannel=" + c.getChannelID());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        queryString = strbuf.toString() + getAggregregate(aggType) + getOpenAlarmString();
    }

    @Override
    public String getQueryString() {
        return queryString;
    }
}
