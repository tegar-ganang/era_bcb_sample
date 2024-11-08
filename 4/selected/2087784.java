package com.creawor.hz_market.t_channel;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.LockMode;
import com.creawor.imei.base.AbsEditMap;

public class t_channel_EditMap extends AbsEditMap {

    public void add(t_channel_Form vo) throws HibernateException {
        t_channel po = new t_channel();
        try {
            po.setId(java.lang.Integer.parseInt(vo.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        po.setVillage_xz(vo.getVillage_xz());
        po.setVillage_zr(vo.getVillage_zr());
        po.setParent(vo.getParent());
        try {
            po.setService_hall_code(vo.getService_hall_code());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setService_hall_name(vo.getService_hall_name());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setChannel_type(vo.getChannel_type());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setStar_level(vo.getStar_level());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setAddress(vo.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCompany(vo.getCompany());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setContact_man(vo.getContact_man());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setMain_type(vo.getMain_type());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setIscomplete(vo.getIscomplete());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setContact_tel(vo.getContact_tel());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setContact_mobile(vo.getContact_mobile());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setRent(java.lang.Double.parseDouble(vo.getRent()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setTown(vo.getTown());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setTowncode(vo.getTowncode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCounty(vo.getCounty());
            if ("����".equals(vo.getCounty())) {
                po.setCounty("�ع�");
            } else {
                po.setCounty(vo.getCounty());
            }
            if ("�й�˾".equalsIgnoreCase(vo.getCounty())) {
                po.setParent(vo.getCounty());
            } else {
                po.setParent(vo.getCounty() + "�ֹ�˾");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCounty_code(vo.getCounty_code());
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
            po.setZoom(java.lang.Integer.parseInt(vo.getZoom()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setJindu(vo.getJindu());
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

    public void delete(t_channel_Form vo) throws HibernateException {
        String id = vo.getId();
        delete(id);
    }

    public void delete(String id) throws HibernateException {
        Session session = this.beginTransaction();
        try {
            t_channel po = (t_channel) session.load(t_channel.class, new Integer(id));
            session.delete(po);
            this.endTransaction(true);
        } catch (HibernateException e) {
            e.printStackTrace();
            this.endTransaction(false);
        }
    }

    public void update(t_channel_Form vo) throws HibernateException {
        Session session = this.beginTransaction();
        String id = vo.getId();
        t_channel po = (t_channel) session.load(t_channel.class, new Integer(id));
        try {
            po.setId(java.lang.Integer.parseInt(vo.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setService_hall_code(vo.getService_hall_code());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setService_hall_name(vo.getService_hall_name());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setChannel_type(vo.getChannel_type());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setStar_level(vo.getStar_level());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setAddress(vo.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCompany(vo.getCompany());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setContact_man(vo.getContact_man());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setContact_tel(vo.getContact_tel());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setContact_mobile(vo.getContact_mobile());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setRent(java.lang.Double.parseDouble(vo.getRent()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setTown(vo.getTown());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setTowncode(vo.getTowncode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCounty(vo.getCounty());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setMain_type(vo.getMain_type());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setIscomplete(vo.getIscomplete());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setCounty_code(vo.getCounty_code());
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
            po.setZoom(java.lang.Integer.parseInt(vo.getZoom()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            po.setJindu(vo.getJindu());
        } catch (Exception e) {
            e.printStackTrace();
        }
        po.setVillage_xz(vo.getVillage_xz());
        po.setVillage_zr(vo.getVillage_zr());
        po.setParent(vo.getParent());
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
