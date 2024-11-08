package name.huliqing.qblog.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import name.huliqing.qblog.entity.PhotoEn;

/**
 *
 * @author huliqing
 */
public abstract class PhotoDa extends BaseDao<PhotoEn, Long> {

    public PhotoDa() {
        super(PhotoEn.class);
    }

    /**
     * 直接删除
     * @param photoId
     * @return
     */
    @Override
    protected boolean delete(Long photoId) {
        String q = "delete from PhotoEn obj where obj.photoId=:photoId";
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            Query query = em.createQuery(q);
            query.setParameter("photoId", photoId);
            tx.begin();
            int result = query.executeUpdate();
            tx.commit();
            return (result >= 1);
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * 删除某一个相册下的所有图片
     * @param folderId 相册ID
     * @return
     */
    protected boolean deleteByFolderId(Long folderId) {
        if (folderId == null) throw new NullPointerException("folderId could not be null!");
        String q = "delete from PhotoEn obj where obj.folder=:folderId";
        EntityManager em = getEM();
        try {
            Query query = em.createQuery(q);
            query.setParameter("folderId", folderId);
            int result = query.executeUpdate();
            return (result >= 0);
        } finally {
            em.close();
        }
    }

    /**
     * 注：该方法不会搜索出所有字段，bytes字段不会取出,bytesMin也不会被取出
     * @param search
     * @param sortField
     * @param asc
     * @param start
     * @param size
     * @return
     */
    @Override
    protected List<PhotoEn> findByObject(PhotoEn search, String sortField, Boolean asc, Integer start, Integer size) {
        StringBuilder sb = new StringBuilder("select obj.photoId " + ",obj.name" + ",obj.contentType" + ",obj.suffix" + ",obj.fileSize" + ",obj.pack" + ",obj.createDate" + ",obj.folder" + ",obj.des" + " from PhotoEn obj ");
        EntityManager em = getEM();
        try {
            Query qd = QueryMake2.makeQuery(em, search, sb, sortField, asc, start, size);
            List<Object[]> result = (List<Object[]>) qd.getResultList();
            List<PhotoEn> pes = new ArrayList<PhotoEn>(result.size());
            for (Object[] r : result) {
                PhotoEn pe = new PhotoEn();
                pe.setPhotoId((Long) r[0]);
                pe.setName((String) r[1]);
                pe.setContentType((String) r[2]);
                pe.setSuffix((String) r[3]);
                pe.setFileSize((Long) r[4]);
                pe.setPack((Boolean) r[5]);
                pe.setCreateDate((Date) r[6]);
                pe.setFolder((Long) r[7]);
                pe.setDes((String) r[8]);
                pes.add(pe);
            }
            return pes;
        } finally {
            em.close();
        }
    }
}
