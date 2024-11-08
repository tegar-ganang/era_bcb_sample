package com.creawor.hz_market.t_town_base;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.LockMode;
import com.creawor.imei.base.AbsEditMap;

public class t_town_base_EditMap extends AbsEditMap {

    public void add(t_town_base_Form vo) throws HibernateException {
        t_town_base po = new t_town_base();
        try {
            po.setId(java.lang.Integer.parseInt(vo.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCode(vo.getCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setName(vo.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCounty(vo.getCounty());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setInsert_day(com.creawor.km.util.DateUtil.getDate(vo.getInsert_day(), "yyyy-MM-dd"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setPopulation(java.lang.Integer.parseInt(vo.getPopulation()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setXzc_num((vo.getXzc_num()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setYryc_num((vo.getYryc_num()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setShort_net_num((vo.getShort_net_num()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setChannel_num((vo.getChannel_num()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setManager_num((vo.getManager_num()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setD_manager_num((vo.getD_manager_num()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setL_manager_num((vo.getL_manager_num()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setSec(vo.getSec());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setAlcalde(vo.getAlcalde());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setX(java.lang.Double.parseDouble(vo.getX()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setY(java.lang.Double.parseDouble(vo.getY()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Session session = this.beginTransaction();
        try {
            session.save(po);
            this.endTransaction(true);
        } catch (Exception e) {
            this.endTransaction(false);
            e.printStackTrace();
            throw new HibernateException(e);
        }
    }

    public void delete(t_town_base_Form vo) throws HibernateException {
        String id = vo.getId();
        delete(id);
    }

    public void delete(String id) throws HibernateException {
        Session session = this.beginTransaction();
        try {
            t_town_base po = (t_town_base) session.load(t_town_base.class, new Integer(id));
            session.delete(po);
            this.endTransaction(true);
        } catch (HibernateException e) {
            e.printStackTrace();
            this.endTransaction(false);
        }
    }

    public void update(t_town_base_Form vo) throws HibernateException {
        Session session = this.beginTransaction();
        String id = vo.getId();
        t_town_base po = (t_town_base) session.load(t_town_base.class, new Integer(id));
        try {
            po.setId(java.lang.Integer.parseInt(vo.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCode(vo.getCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setName(vo.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCounty(vo.getCounty());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setInsert_day(com.creawor.km.util.DateUtil.getDate(vo.getInsert_day(), "yyyy-MM-dd"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setPopulation(java.lang.Integer.parseInt(vo.getPopulation()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setSec(vo.getSec());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setAlcalde(vo.getAlcalde());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setX(java.lang.Double.parseDouble(vo.getX()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setY(java.lang.Double.parseDouble(vo.getY()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            session.update(po);
            this.endTransaction(true);
        } catch (Exception e) {
            this.endTransaction(false);
            e.printStackTrace();
            throw new HibernateException(e);
        }
    }
}
