package com.adpython.dao.impl;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.jdo.Extent;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import org.springframework.orm.jdo.support.JdoDaoSupport;
import com.adpython.dao.ArticleDao;
import com.adpython.dao.PMF;
import com.adpython.domain.Article;

public class ArticleDaoImpl extends JdoDaoSupport implements ArticleDao {

    public Article get(Long id) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            Extent r = pm.getExtent(Article.class, false);
            String filter = "id==" + id;
            Query query = pm.newQuery(r, filter);
            List<Article> list = (List<Article>) query.execute();
            if (!list.isEmpty()) return list.get(0); else return null;
        } finally {
            pm.close();
        }
    }

    public void remove(Article article) {
        if (article != null) this.removeById(article.getId());
    }

    public void removeById(Long id) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            Extent r = pm.getExtent(Article.class, false);
            String filter = "id==" + id;
            Query query = pm.newQuery(r, filter);
            List<Article> list = (List<Article>) query.execute();
            if (!list.isEmpty()) {
                Article delArticle = list.get(0);
                pm.deletePersistent(delArticle);
            }
            tx.commit();
        } finally {
            if (tx.isActive()) tx.rollback();
            pm.close();
        }
    }

    public void save(Article article) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            pm.makePersistent(article);
            tx.commit();
        } finally {
            if (tx.isActive()) tx.rollback();
            pm.close();
        }
    }

    public void update(Article article) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Article old = pm.getObjectById(Article.class, article.getId());
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            old.setChannelId(article.getChannelId());
            old.setContent(article.getContent());
            old.setCreateTime(old.getCreateTime());
            old.setCreateUserId(article.getCreateUserId());
            old.setIfShow(article.getIfShow());
            old.setIsDeleted(article.getIsDeleted());
            old.setRank(article.getRank());
            old.setTitle(article.getTitle());
            old.setUpdateTime(new Date());
            tx.commit();
        } finally {
            if (tx.isActive()) tx.rollback();
            pm.close();
        }
    }

    public List<Article> queryAll() {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            Query query = pm.newQuery(Article.class);
            query.setOrdering("rank desc");
            return (List<Article>) pm.detachCopyAll((Collection<Article>) query.execute());
        } finally {
            pm.close();
        }
    }

    public List<Article> queryByChannel(Long channelId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            Query query = pm.newQuery(Article.class);
            query.setFilter("channelId==" + channelId + "");
            List<Article> list = (List<Article>) pm.detachCopyAll((Collection<Article>) query.execute());
            return list;
        } finally {
            pm.close();
        }
    }
}
