package com.creawor.hz_market.t_channel;

import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import com.creawor.hz_market.servlet.LoadMapInfoAjax;
import com.creawor.imei.base.AbsQueryMap;

public class t_channel_QueryMap extends AbsQueryMap {

    private static final Logger logger = Logger.getLogger(LoadMapInfoAjax.class);

    public t_channel_QueryMap() throws HibernateException {
        this.initSession();
    }

    public Iterator findAll() throws HibernateException {
        this.totalrow = ((Integer) this.session.iterate("select count(*) from t_channel").next()).intValue();
        String querystr = "from t_channel";
        Query query = this.session.createQuery(querystr);
        this.setQueryPage(query);
        return query.iterate();
    }

    public List findAllList() throws HibernateException {
        this.totalrow = ((Integer) this.session.iterate("select count(*) from t_channel").next()).intValue();
        String querystr = "from t_channel";
        Query query = this.session.createQuery(querystr);
        this.setQueryPage(query);
        return query.list();
    }

    public List findAllByCounty(String county) throws HibernateException {
        this.totalrow = ((Integer) this.session.iterate("select count(*) from t_channel where county ='" + county + "'").next()).intValue();
        String querystr = "from t_channel  as t where t.county ='" + county + "'";
        Query query = this.session.createQuery(querystr);
        this.setQueryPage(query);
        return query.list();
    }

    public t_channel_Form getByID(String ID) throws HibernateException {
        t_channel_Form vo = null;
        logger.debug("\nt_channel_QueryMap getByID:" + ID);
        this.session.clear();
        try {
            vo = new t_channel_Form();
            t_channel po = (t_channel) this.session.load(t_channel.class, new Integer(ID));
            try {
                vo.setId(String.valueOf(po.getId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            vo.setService_hall_code(po.getService_hall_code());
            vo.setService_hall_name(po.getService_hall_name());
            vo.setChannel_type(po.getChannel_type());
            vo.setStar_level(po.getStar_level());
            vo.setAddress(po.getAddress());
            vo.setCompany(po.getCompany());
            vo.setContact_man(po.getContact_man());
            vo.setContact_tel(po.getContact_tel());
            vo.setContact_mobile(po.getContact_mobile());
            vo.setVillage_xz(po.getVillage_xz());
            vo.setVillage_zr(po.getVillage_zr());
            vo.setParent(po.getParent());
            vo.setJindu(po.getJindu());
            vo.setMain_type(po.getMain_type());
            vo.setIscomplete(po.getIscomplete());
            try {
                vo.setRent(String.valueOf((po.getRent())));
            } catch (Exception e) {
                e.printStackTrace();
            }
            vo.setTown(po.getTown());
            vo.setTowncode(po.getTowncode());
            vo.setCounty(po.getCounty());
            vo.setCounty_code(po.getCounty_code());
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
            try {
                vo.setZoom(String.valueOf(po.getZoom()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (HibernateException e) {
            logger.debug("\nERROR in getByID @t_channel:" + e);
        }
        return vo;
    }
}
