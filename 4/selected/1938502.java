package net.sourceforge.olduvai.lrac.dataservice.queries;

import java.util.Iterator;
import java.util.List;
import net.sourceforge.olduvai.lrac.drawer.strips.Strip;
import net.sourceforge.olduvai.lrac.drawer.strips.StripChannel;

public class ChartDataAggQuery extends AbstractAggQuery {

    boolean alarmQuery = false;

    boolean statQuery = false;

    boolean getAvg = true;

    public ChartDataAggQuery(int aggType) {
        super();
        this.aggType = aggType;
    }

    /**
	 * Integrate a query into the aggregate query
	 * @param cdq
	 */
    public void addDataQuery(ChartDataQuery cdq) {
        sourceNames.add(cdq.sourceName);
        if (cdq.getAggType() < getAggType()) aggType = cdq.getAggType();
        if (!cdq.isGetAvg()) setGetAvg(false);
        if (startDate == null) startDate = cdq.startDate; else if (cdq.startDate != startDate) System.err.println("Illegal ChartDataAgg add, begin dates don't match.");
        if (endDate == null) endDate = cdq.endDate; else if (cdq.endDate != endDate) System.err.println("Illegal ChartDataAgg add, end dates don't match.");
        this.alarmRequestType = cdq.alarmRequestType;
        Strip strip = cdq.strip;
        if (strip.getStripTitle().equals(Strip.ALARMSTRIPTITLE)) {
            alarmQuery = true;
            return;
        }
        statQuery = true;
        Iterator<StripChannel> it = strip.getChannelList().iterator();
        while (it.hasNext()) {
            StripChannel channel = it.next();
            if (channel.getType() == StripChannel.STAT) channelNames.add("&sChannel=" + channel.getChannelID()); else if (channel.getType() == StripChannel.PREFIX) {
                List<StripChannel> channelList = strip.getChannelGroupList(channel);
                Iterator<StripChannel> channelListIt = channelList.iterator();
                while (channelListIt.hasNext()) {
                    final StripChannel c = channelListIt.next();
                    channelNames.add("&sChannel=" + c.getChannelID());
                }
            } else System.err.println("ChartDataAggQuery: invalid channel type: " + channel);
        }
    }

    @Override
    public String getQueryString() {
        final String baseQuery = getCoreQuery() + ChartDataQuery.getBASECHARTDATAQUERY();
        StringBuffer buf = new StringBuffer(baseQuery);
        buf.append("&beginTime=" + df.format(getStartDate()));
        buf.append("&endTime=" + df.format(getEndDate()));
        Iterator<String> sourceList = getSourceNames().iterator();
        while (sourceList.hasNext()) {
            buf.append("&source=" + sourceList.next());
        }
        Iterator<String> channelList = getChannelNames().iterator();
        while (channelList.hasNext()) {
            buf.append(channelList.next());
        }
        return buf.toString() + getAggregregate(aggType) + getOpenAlarmString() + getAvgString();
    }

    public String getAlarmQueryString() {
        final String baseQuery = getCoreQuery() + ChartDataQuery.getBASEALARMDATAQUERY();
        StringBuffer buf = new StringBuffer(baseQuery);
        buf.append("&beginTime=" + df.format(getStartDate()));
        buf.append("&endTime=" + df.format(getEndDate()));
        Iterator<String> sourceList = getSourceNames().iterator();
        while (sourceList.hasNext()) {
            buf.append("&source=" + sourceList.next());
        }
        return buf.toString();
    }

    public boolean isAlarmQuery() {
        return alarmQuery;
    }

    public boolean isStatQuery() {
        return statQuery;
    }

    public boolean isGetAvg() {
        return getAvg;
    }

    /**
	 * Whether to retrieve rolling averages (if available)
	 * 
	 * @param getAvg
	 */
    public void setGetAvg(boolean getAvg) {
        this.getAvg = getAvg;
    }

    private String getAvgString() {
        if (getAvg) return "";
        return "&getAvg=No";
    }
}
