package com.hilaver.dzmis.dao;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Transaction;
import com.hilaver.dzmis.procurement.ProductPackageItem;
import com.hilaver.dzmis.product.ProductOrderItem;

public class ProductPackageDAO extends BaseHibernateDAO {

    public List<ProductOrderItem> getProductOrderItemFrom(Integer id) throws Exception {
        String hql = "from " + ProductOrderItem.class.getName() + " where productIdentification.productPackage.id = ? order by productIdentification.id, productIdentification.reference, colorNumber";
        try {
            Query queryObject = getSession().createQuery(hql);
            queryObject.setParameter(0, id);
            List<ProductOrderItem> rtnList = queryObject.list();
            return rtnList;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void deleteProductPackageItemFrom(Integer id) throws Exception {
        Transaction tx = null;
        String hql = "delete from " + ProductPackageItem.class.getName() + " where productPackage.id = ?";
        try {
            tx = getSession().beginTransaction();
            Query queryObject = getSession().createQuery(hql);
            queryObject.setParameter(0, id);
            queryObject.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List<ProductPackageItem> getProductPackageItemFrom(Integer id) throws Exception {
        Transaction tx = null;
        String hql = "from " + ProductPackageItem.class.getName() + " where productPackage.id = ? order by id";
        try {
            Query queryObject = getSession().createQuery(hql);
            queryObject.setParameter(0, id);
            return queryObject.list();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List<ProductPackageItem> getProductPackageItemFrom(Integer parentId, String orderNumber, Integer packageIndex) throws Exception {
        String hql = "from " + ProductPackageItem.class.getName() + " where productPackage.id = ? and orderNumber = ? and packageNumber = ?";
        try {
            Query queryObject = getSession().createQuery(hql);
            queryObject.setParameter(0, parentId);
            queryObject.setParameter(1, orderNumber);
            queryObject.setParameter(2, packageIndex);
            List<ProductPackageItem> rtnList = queryObject.list();
            return rtnList;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List<ProductPackageItem> getProductPackageItemFrom(Integer parentId, String orderNumber, String reference, String yarnName) throws Exception {
        String hql = "from " + ProductPackageItem.class.getName() + " where productPackage.id = ? and orderNumber = ? and reference = ? and yarnName = ? order by id, packageNumber";
        try {
            Query queryObject = getSession().createQuery(hql);
            queryObject.setParameter(0, parentId);
            queryObject.setParameter(1, orderNumber);
            queryObject.setParameter(2, reference);
            queryObject.setParameter(3, yarnName);
            List<ProductPackageItem> rtnList = queryObject.list();
            return rtnList;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
