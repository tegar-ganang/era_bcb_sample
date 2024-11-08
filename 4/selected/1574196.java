package com.creawor.hz_market.t_village_signal;

import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import com.creawor.hz_market.servlet.LoadMapInfoAjax;
import com.creawor.imei.base.AbsQueryMap;

public class t_village_signal_QueryMap extends AbsQueryMap {

    private static final Logger logger = Logger.getLogger(LoadMapInfoAjax.class);

    public t_village_signal_QueryMap() throws HibernateException {
        this.initSession();
    }

    public Iterator findAll() throws HibernateException {
        this.totalrow = ((Integer) this.session.iterate("select count(*) from t_village_signal").next()).intValue();
        String querystr = "from t_village_signal";
        Query query = this.session.createQuery(querystr);
        this.setQueryPage(query);
        return query.iterate();
    }

    public List findAllList() throws HibernateException {
        this.totalrow = ((Integer) this.session.iterate("select count(*) from t_village_signal").next()).intValue();
        String querystr = "from t_village_signal";
        Query query = this.session.createQuery(querystr);
        this.setQueryPage(query);
        return query.list();
    }

    public List findAllByCounty(String county) throws HibernateException {
        this.totalrow = ((Integer) this.session.iterate("select count(*) from t_village_signal where county ='" + county + "'").next()).intValue();
        String querystr = "from t_village_signal  as t where t.county ='" + county + "'";
        Query query = this.session.createQuery(querystr);
        this.setQueryPage(query);
        return query.list();
    }

    public t_village_signal_Form getByID(String ID) throws HibernateException {
        t_village_signal_Form vo = null;
        logger.debug("\nt_village_signal_QueryMap getByID:" + ID);
        this.session.clear();
        try {
            vo = new t_village_signal_Form();
            t_village_signal po = (t_village_signal) this.session.load(t_village_signal.class, new Integer(ID));
            try {
                vo.setId(String.valueOf(po.getId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            vo.setVillage_code(po.getVillage_code());
            vo.setName(po.getName());
            vo.setSignal(po.getSignal());
            vo.setTown(po.getTown());
            vo.setVillage_xz(po.getVillage_xz());
            vo.setVillage_zr(po.getVillage_zr());
            vo.setCounty(po.getCounty());
            vo.setChannel_code(po.getChannel_code());
            vo.setIs_covered(po.getIs_covered());
            vo.setVillage(po.getVillage());
            vo.setSound_quality(po.getSound_quality());
            try {
                vo.setCover_num(String.valueOf(po.getCover_num()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            vo.setRival_info(po.getRival_info());
            vo.setCovered_name(String.valueOf(po.getCovered_name()));
            try {
                vo.setX(String.valueOf((po.getX())));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                vo.setY(String.valueOf((po.getY())));
            } catch (Exception e) {
                e.printStackTrace();
            }
            vo.setIscreate_net(po.getIscreate_net());
            vo.setIseveryone(po.getIseveryone());
            try {
                vo.setInsert_day(com.creawor.km.util.DateUtil.getStr(po.getInsert_day(), "yyyy-MM-dd"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (HibernateException e) {
            logger.debug("\nERROR in getByID @t_village_signal:" + e);
        }
        return vo;
    }
}
