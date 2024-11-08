package com.creawor.hz_market.t_village_signal;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.LockMode;
import com.creawor.imei.base.AbsEditMap;

public class t_village_signal_EditMap extends AbsEditMap {

    public void add(t_village_signal_Form vo) throws HibernateException {
        t_village_signal po = new t_village_signal();
        try {
            po.setId(java.lang.Integer.parseInt(vo.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setVillage_code(vo.getVillage_code());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setName(vo.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setSignal(vo.getSignal());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setTown(vo.getTown());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setVillage_xz(vo.getVillage_xz());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setVillage_zr(vo.getVillage_zr());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCounty(vo.getCounty());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setVillage(vo.getVillage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setSound_quality(vo.getSignal());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCover_num(java.lang.Integer.parseInt(vo.getCover_num()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setRival_info(vo.getRival_info());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCovered_name(vo.getCovered_name());
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
        po.setChannel_code(vo.getChannel_code());
        po.setIs_covered(vo.getIs_covered());
        po.setIscreate_net(vo.getIscreate_net());
        po.setIseveryone(vo.getIseveryone());
        try {
            po.setInsert_day(com.creawor.km.util.DateUtil.getDate(vo.getInsert_day(), "yyyy-MM-dd"));
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

    public void delete(t_village_signal_Form vo) throws HibernateException {
        String id = vo.getId();
        delete(id);
    }

    public void delete(String id) throws HibernateException {
        Session session = this.beginTransaction();
        try {
            t_village_signal po = (t_village_signal) session.load(t_village_signal.class, new Integer(id));
            session.delete(po);
            this.endTransaction(true);
        } catch (HibernateException e) {
            e.printStackTrace();
            this.endTransaction(false);
        }
    }

    public void update(t_village_signal_Form vo) throws HibernateException {
        Session session = this.beginTransaction();
        String id = vo.getId();
        t_village_signal po = (t_village_signal) session.load(t_village_signal.class, new Integer(id));
        try {
            po.setId(java.lang.Integer.parseInt(vo.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setVillage_code(vo.getVillage_code());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setName(vo.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setSignal(vo.getSignal());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setTown(vo.getTown());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setVillage_xz(vo.getVillage_xz());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setVillage_zr(vo.getVillage_zr());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCounty(vo.getCounty());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setVillage(vo.getVillage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setSound_quality(vo.getSignal());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCover_num(java.lang.Integer.parseInt(vo.getCover_num()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setRival_info(vo.getRival_info());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCovered_name(vo.getCovered_name());
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
        po.setChannel_code(vo.getChannel_code());
        po.setIs_covered(vo.getIs_covered());
        po.setIscreate_net(vo.getIscreate_net());
        po.setIseveryone(vo.getIseveryone());
        try {
            po.setInsert_day(com.creawor.km.util.DateUtil.getDate(vo.getInsert_day(), "yyyy-MM-dd"));
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
