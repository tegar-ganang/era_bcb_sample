package com.liusoft.dlog4j.dao;

import java.util.Calendar;
import java.util.Date;
import org.hibernate.Query;
import org.hibernate.Session;
import com.liusoft.dlog4j.beans.SiteBean;
import com.liusoft.dlog4j.beans.SiteStatBean;

/**
 * ����ͳ��
 * @author Winter Lau
 */
public class VisitStatDAO extends DAO {

    /**
	 * ���շ����˴�
	 * @param site
	 * @return
	 */
    public static long getUVToday(SiteBean site) {
        if (site == null) return -1;
        Calendar cal = Calendar.getInstance();
        int statDate = cal.get(Calendar.YEAR) * 10000 + cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DATE);
        return toLong(executeNamedStat("VISIT_STAT", site.getId(), statDate), 1);
    }

    /**
	 * ���ܷ����˴�
	 * @param site
	 * @return
	 */
    public static long getUVThisWeek(SiteBean site) {
        if (site == null) return -1;
        Calendar cal = Calendar.getInstance();
        int week = cal.get(Calendar.DAY_OF_WEEK);
        if (week > 1) cal.add(Calendar.DATE, 1 - week);
        int statDate = cal.get(Calendar.YEAR) * 10000 + cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DATE);
        return toLong(executeNamedStat("VISIT_STAT", site.getId(), statDate), 1);
    }

    /**
	 * ���·����˴�
	 * @param site
	 * @return
	 */
    public static long getUVThisMonth(SiteBean site) {
        if (site == null) return -1;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DATE, 1);
        int statDate = cal.get(Calendar.YEAR) * 10000 + cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DATE);
        return toLong(executeNamedStat("VISIT_STAT", site.getId(), statDate), 1);
    }

    /**
	 * ��������˴�
	 * @param site
	 * @return
	 */
    public static long getUVThisYear(SiteBean site) {
        if (site == null) return -1;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, 1);
        cal.set(Calendar.DATE, 1);
        int statDate = cal.get(Calendar.YEAR) * 10000 + cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DATE);
        return toLong(executeNamedStat("VISIT_STAT", site.getId(), statDate), 1);
    }

    /**
	 * ���з����˴�
	 * @param site
	 * @return
	 */
    public static long getUVTotal(SiteBean site) {
        if (site == null) return -1;
        return toLong(executeNamedStat("VISIT_STAT_2", site.getId()), 1);
    }

    private static long toLong(Number v, int defaultValue) {
        return (v != null) ? v.longValue() : defaultValue;
    }

    /**
	 * дͳ����ݵ���ݿ�
	 * @param ssb
	 * @throws Exception 
	 */
    public static void writeStatData(int siteid, int uvCount, int source) throws Exception {
        Calendar cal = Calendar.getInstance();
        int statDate = cal.get(Calendar.YEAR) * 10000 + cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DATE);
        Session ssn = getSession();
        try {
            beginTransaction();
            Query update_q = ssn.getNamedQuery((siteid > 0) ? "UPDATE_SITE_STAT_1" : "UPDATE_SITE_STAT_2");
            update_q.setInteger(0, uvCount);
            update_q.setInteger(1, statDate);
            update_q.setInteger(2, source);
            if (siteid > 0) update_q.setInteger(3, siteid);
            if (update_q.executeUpdate() < 1) {
                SiteStatBean ssb = new SiteStatBean();
                ssb.setSiteId(siteid);
                ssb.setUvCount(uvCount);
                ssb.setUpdateTime(new Date());
                ssb.setSource(source);
                ssb.setStatDate(statDate);
                ssn.save(ssb);
            }
            commit();
        } catch (Exception e) {
            rollback();
            throw e;
        }
    }
}
