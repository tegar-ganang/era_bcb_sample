package com.wwg.cms.service.impl;

import com.css.framework.container.Container;
import com.css.framework.dao.GeneralDao;
import com.wwg.cms.bo.Document;
import com.wwg.cms.bo.entity.DocumentEntity;
import com.wwg.cms.service.TemplateDataService;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.tool.instrument.cglib.InstrumentTask;
import org.springframework.orm.hibernate3.HibernateCallback;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author 王文磊
 * @version $Id: TemplateDataServiceImpl.java 16 2010-08-30 07:15:17Z sanshi2 $
 * @date 2010-8-18 15:09:23
 */
public class TemplateDataServiceImpl implements TemplateDataService {

    private GeneralDao generalDao;

    public void setGeneralDao(GeneralDao generalDao) {
        this.generalDao = generalDao;
    }

    public GeneralDao getGeneralDao() {
        if (generalDao == null) {
            synchronized (this) {
                generalDao = (GeneralDao) Container.getBean("generalDao", GeneralDao.class);
            }
        }
        return generalDao;
    }

    public List getDocumentByChannel(Long channelId) {
        String hql = "from DocumentEntity as doc";
        return (List) getGeneralDao().doInSession(new HibernateCallback() {

            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                return session.createCriteria(DocumentEntity.class).setFetchMode("content", FetchMode.JOIN).list();
            }
        });
    }

    public List getChannelByParent(Long channelId) {
        return null;
    }

    public List getPlacardList() {
        return null;
    }

    public Document getDocumentById(Long docId) {
        return (Document) getGeneralDao().fetch(docId, DocumentEntity.class);
    }
}
