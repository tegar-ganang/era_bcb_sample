package com.liusoft.dlog4j.dao;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import com.liusoft.dlog4j.ObjectNotFoundException;
import com.liusoft.dlog4j.SessionUserObject;
import com.liusoft.dlog4j.base._PhotoBase;
import com.liusoft.dlog4j.base._ReplyBean;
import com.liusoft.dlog4j.beans.AlbumBean;
import com.liusoft.dlog4j.beans.DiaryBean;
import com.liusoft.dlog4j.beans.PhotoBean;
import com.liusoft.dlog4j.beans.PhotoOutlineBean;
import com.liusoft.dlog4j.beans.PhotoReplyBean;
import com.liusoft.dlog4j.beans.SiteBean;
import com.liusoft.dlog4j.beans.TagBean;
import com.liusoft.dlog4j.search.SearchDataProvider;
import com.liusoft.dlog4j.util.DLOG4JUtils;
import com.liusoft.dlog4j.util.DateUtils;
import com.liusoft.dlog4j.util.StringUtils;

/**
 * ������Ƭ����ݿ���ʽӿ�
 * @author Winter Lau
 */
public class PhotoDAO extends DAO implements SearchDataProvider {

    /**
	 * ��ȡ����ר����־��Site��days���ڵ������ŵ���Ƭ
	 * @param days
	 * @param count
	 * @return
	 */
    public static List listHotPhotos(int days, int count) {
        Calendar cal = Calendar.getInstance();
        DateUtils.resetTime(cal);
        cal.add(Calendar.DATE, -days);
        return executeNamedQuery("LIST_HOT_PHOTOS", 0, count, new Object[] { PhotoOutlineBean.I_STATUS_NORMAL, cal.getTime(), AlbumBean.I_TYPE_PUBLIC });
    }

    /**
	 * ����ָ����վ����Ƭ�����û��ָ����վ�򷵻�������Ƭ��
	 * @param site
	 * @return
	 */
    public static int getPhotoCount(int site) {
        String hql = "SELECT COUNT(*) FROM PhotoBean AS d WHERE d.status=?";
        if (site > 0) {
            hql += " AND d.site.id=?";
            return executeStatAsInt(hql, PhotoBean.STATUS_NORMAL, site);
        }
        return executeStatAsInt(hql, PhotoBean.STATUS_NORMAL);
    }

    /**
	 * �õ�ָ����Ƭ����һƪ(������ʾ��Ƭҳ)
	 * @param site
	 * @param user
	 * @param album_id
	 * @param photo_id
	 * @return
	 */
    public static PhotoOutlineBean getPrevPhoto(SiteBean site, SessionUserObject user, int album_id, int photo_id) {
        if (site == null) return null;
        boolean is_owner = site.isOwner(user);
        StringBuffer hql = new StringBuffer("FROM PhotoOutlineBean AS p WHERE p.status=:photo_status AND p.site.id=:site AND p.id<:photo");
        if (!is_owner) {
            hql.append(" AND p.album.type=:album_type");
        }
        if (album_id > 0) {
            hql.append(" AND p.album.id=:album");
        }
        hql.append(" ORDER BY p.id DESC");
        Session ssn = getSession();
        try {
            Query q = ssn.createQuery(hql.toString());
            q.setInteger("photo_status", PhotoBean.STATUS_NORMAL);
            q.setInteger("site", site.getId());
            q.setInteger("photo", photo_id);
            if (album_id > 0) q.setInteger("album", album_id);
            if (!is_owner) q.setInteger("album_type", AlbumBean.TYPE_PUBLIC);
            q.setMaxResults(1);
            return (PhotoOutlineBean) q.uniqueResult();
        } finally {
            hql = null;
        }
    }

    /**
	 * �õ�ָ����Ƭ����һƪ(������ʾ��Ƭҳ)
	 * @param site
	 * @param user
	 * @param album_id
	 * @param photo_id
	 * @return
	 */
    public static PhotoOutlineBean getNextPhoto(SiteBean site, SessionUserObject user, int album_id, int photo_id) {
        if (site == null) return null;
        StringBuffer hql = new StringBuffer("FROM PhotoOutlineBean AS p WHERE p.status=:photo_status AND p.site.id=:site AND p.id>:photo");
        if (user == null || !site.isOwner(user)) {
            hql.append(" AND p.album.type=:album_type");
        }
        if (album_id > 0) {
            hql.append(" AND p.album.id=:album");
        }
        hql.append(" ORDER BY p.id ASC");
        Session ssn = getSession();
        try {
            Query q = ssn.createQuery(hql.toString());
            q.setInteger("photo_status", PhotoBean.STATUS_NORMAL);
            q.setInteger("site", site.getId());
            q.setInteger("photo", photo_id);
            if (album_id > 0) q.setInteger("album", album_id);
            if (!site.isOwner(user)) q.setInteger("album_type", AlbumBean.TYPE_PUBLIC);
            q.setMaxResults(1);
            return (PhotoOutlineBean) q.uniqueResult();
        } finally {
            hql = null;
        }
    }

    /**
	 * ������Ƭ���Ķ���
	 * @param photo_id
	 * @param incCount
	 * @return
	 */
    public static void incViewCount(int photo_id, int incCount) {
        commitNamedUpdate("INC_PHOTO_VIEW_COUNT", incCount, photo_id);
    }

    /**
	 * ������Ƭ
	 * @param photo_id
	 * @return
	 */
    public static PhotoBean getPhotoByID(int photo_id) {
        if (photo_id <= 0) return null;
        return (PhotoBean) getBean(PhotoBean.class, photo_id);
    }

    /**
	 * ������Ƭ
	 * @param photo_id
	 * @return
	 */
    public static PhotoOutlineBean getPhotoOutlineByID(int photo_id) {
        if (photo_id <= 0) return null;
        return (PhotoOutlineBean) getBean(PhotoOutlineBean.class, photo_id);
    }

    /**
	 * �г���Ƭ�е���Ч�·�
	 * @param site_id
	 * @return
	 */
    public static List listMonths(int site_id) {
        return findNamedAll("PHOTO_MONTHS", site_id);
    }

    /**
	 * ��������г���Ƭ
	 * @param site
	 * @param user
	 * @param album_id
	 * @param month_stamp �·ݴ�,����200506��ʾ��2005��6�·ݵ���Ƭ
	 * @param fromIdx
	 * @param count
	 * @return
	 * @see com.liusoft.dlog4j.velocity.DLOG_VelocityTool#list_photos(SiteBean, int, int) 
	 */
    public static List listPhotos(SiteBean site, SessionUserObject user, int album_id, int month_stamp, int date, int fromIdx, int count) {
        StringBuffer hql = new StringBuffer("FROM PhotoOutlineBean AS p WHERE p.site.id=:site");
        if (album_id > 0) hql.append(" AND (p.album.id=:album OR p.album.parent.id=:album)");
        if (month_stamp > 190000 && month_stamp < 209912) {
            hql.append(" AND p.year=:year AND p.month=:month");
        }
        if (user == null || site.getOwner().getId() != user.getId()) {
            hql.append(" AND p.status<>:hidden_status AND p.album.type=:owner_album");
        }
        if (date > 0) {
            hql.append(" AND p.date=:date");
        }
        hql.append(" ORDER BY p.id DESC");
        Session ssn = getSession();
        try {
            Query q = ssn.createQuery(hql.toString());
            q.setInteger("site", site.getId());
            if (album_id > 0) q.setInteger("album", album_id);
            if (month_stamp > 190000 && month_stamp < 209912) {
                q.setInteger("year", month_stamp / 100);
                q.setInteger("month", month_stamp % 100);
            }
            if (user == null || site.getOwner().getId() != user.getId()) {
                q.setInteger("hidden_status", PhotoBean.STATUS_PRIVATE);
                q.setInteger("owner_album", AlbumBean.TYPE_PUBLIC);
            }
            if (date > 0) {
                q.setInteger("date", date);
            }
            q.setFirstResult(fromIdx);
            q.setMaxResults(count);
            return q.list();
        } finally {
            hql = null;
        }
    }

    /**
	 * �г�ĳ���ಾ����Ƭ
	 * @param album
	 * @param fromIdx
	 * @param count
	 * @return
	 */
    public static List listPhotos(AlbumBean album, int fromIdx, int count) {
        Query q = getSession().getNamedQuery("PHOTOS_OF_ALBUM");
        q.setInteger("album", album.getId());
        if (fromIdx > 0) q.setFirstResult(fromIdx);
        if (count > 0) q.setMaxResults(count);
        return q.list();
    }

    /**
	 * �г�����ͼƬ�����ڹ��?
	 * @param fromIdx
	 * @param count
	 * @return
	 */
    public static List listPhotos(int fromIdx, int count) {
        String hql = "FROM PhotoOutlineBean AS p ORDER BY p.id DESC";
        return executeQuery(hql, fromIdx, count, null);
    }

    /**
	 * ��ȡ��Ƭ�����ڹ��?
	 * @return
	 */
    public static int photoCount() {
        String hql = "SELECT COUNT(*) FROM PhotoOutlineBean AS p";
        return executeStatAsInt(hql, null);
    }

    /**
	 * ��������г���Ƭ
	 * @param site
	 * @param user
	 * @param album_id
	 * @param month_stamp �·ݴ�,����200506��ʾ��2005��6�·ݵ���Ƭ
	 * @param fromIdx
	 * @param count
	 * @return
	 * @see com.liusoft.dlog4j.velocity.DLOG_VelocityTool#list_photos(SiteBean, int, int) 
	 */
    public static List listPhotos(int album_id, int month_stamp, int date, int fromIdx, int count) {
        StringBuffer hql = new StringBuffer("FROM PhotoOutlineBean AS p WHERE 1=1");
        if (album_id > 0) hql.append(" AND (p.album.id=:album OR p.album.parent.id=:album)");
        if (month_stamp > 190000 && month_stamp < 209912) {
            hql.append(" AND p.year=:year AND p.month=:month");
        }
        hql.append(" AND p.status<>:hidden_status AND p.album.type=:owner_album");
        if (date > 0) {
            hql.append(" AND p.date=:date");
        }
        hql.append(" AND p.site.status=:site_status ORDER BY p.id DESC");
        Session ssn = getSession();
        try {
            Query q = ssn.createQuery(hql.toString());
            if (album_id > 0) q.setInteger("album", album_id);
            if (month_stamp > 190000 && month_stamp < 209912) {
                q.setInteger("year", month_stamp / 100);
                q.setInteger("month", month_stamp % 100);
            }
            q.setInteger("hidden_status", PhotoBean.STATUS_PRIVATE);
            q.setInteger("owner_album", AlbumBean.TYPE_PUBLIC);
            if (date > 0) {
                q.setInteger("date", date);
            }
            q.setInteger("site_status", SiteBean.STATUS_NORMAL);
            q.setFirstResult(fromIdx);
            q.setMaxResults(count);
            return q.list();
        } finally {
            hql = null;
        }
    }

    /**
	 * ��������г���Ƭ
	 * @param site
	 * @param user
	 * @param album_id
	 * @param month_stamp �·ݴ�,����200506��ʾ��2005��6�·ݵ���Ƭ
	 * @return
	 * @see com.liusoft.dlog4j.velocity.DLOG_VelocityTool#list_photos(SiteBean, int, int) 
	 */
    public static int getPhotoCount(SiteBean site, SessionUserObject user, int album_id, int month_stamp, int date) {
        boolean is_owner = site.isOwner(user);
        StringBuffer hql = new StringBuffer("SELECT COUNT(*) FROM PhotoBean AS p WHERE p.site.id=:site");
        if (album_id > 0) hql.append(" AND p.album.id=:album");
        if (month_stamp > 190000 && month_stamp < 209912) {
            hql.append(" AND p.year=:year AND p.month=:month");
        }
        if (!is_owner) {
            hql.append(" AND p.status=:normal_status AND p.album.type=:public_album");
        }
        if (date > 0) {
            hql.append(" AND p.date=:date");
        }
        Session ssn = getSession();
        try {
            Query q = ssn.createQuery(hql.toString());
            q.setInteger("site", site.getId());
            if (album_id > 0) q.setInteger("album", album_id);
            if (month_stamp > 190000 && month_stamp < 209912) {
                q.setInteger("year", month_stamp / 100);
                q.setInteger("month", month_stamp % 100);
            }
            if (!is_owner) {
                q.setInteger("normal_status", PhotoBean.STATUS_NORMAL);
                q.setInteger("public_album", AlbumBean.TYPE_PUBLIC);
            }
            if (date > 0) {
                q.setInteger("date", date);
            }
            return ((Number) q.uniqueResult()).intValue();
        } finally {
            hql = null;
        }
    }

    /**
	 * ɾ��ĳ����Ƭ
	 * @param photo
	 * @throws SQLException 
	 */
    public static void delete(_PhotoBase photo) throws SQLException {
        if (photo == null) return;
        Session ssn = getSession();
        try {
            beginTransaction();
            int photo_size = DLOG4JUtils.sizeInKbytes(photo.getPhotoInfo().getSize());
            photo.getSite().getCapacity().incPhotoUsed(photo_size);
            photo.getAlbum().incPhotoCount(-1);
            photo.getUser().getCount().incPhotoCount(-1);
            AlbumBean parent = photo.getAlbum().getParent();
            int deep = 0;
            do {
                if (parent == null) break;
                deep++;
                parent.incPhotoCount(-1);
                parent = parent.getParent();
            } while (deep < 10);
            TagDAO.deleteTagByRefId(photo.getId(), TagBean.TYPE_PHOTO);
            List rpls = photo.getReplies();
            for (int i = 0; rpls != null && i < rpls.size(); i++) {
                PhotoReplyBean prb = (PhotoReplyBean) rpls.get(i);
                if (prb.getUser() != null) {
                    prb.getUser().getCount().incPhotoReplyCount(-1);
                }
            }
            executeUpdate("UPDATE AlbumBean AS a SET a.cover = NULL WHERE a.cover.id=?", photo.getId());
            ssn.delete(photo);
            commit();
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * ������Ƭ��Ϣ
	 * @param new_album_id
	 * @param photo
	 * @param newKeyword
	 * @param cover �Ƿ�����Ϊ����
	 * @throws ObjectNotFoundException 
	 * @throws IllegalAccessException 
	 */
    public static void update(int new_album_id, PhotoBean photo, String newKeyword, boolean cover) throws ObjectNotFoundException, IllegalAccessException {
        if (photo == null || new_album_id < 1) return;
        try {
            beginTransaction();
            if (photo.getAlbum().getId() != new_album_id) {
                if (photo.getAlbum().getCover() != null && photo.getAlbum().getCover().getId() == photo.getId()) photo.getAlbum().setCover(null);
                AlbumBean new_album = AlbumDAO.getAlbumByID(new_album_id);
                if (new_album == null) throw new ObjectNotFoundException(String.valueOf(new_album));
                if (new_album.getSite().getId() != photo.getSite().getId()) throw new IllegalAccessException(new_album.getName());
                AlbumBean parent = new_album;
                int deep = 0;
                do {
                    if (parent == null) break;
                    deep++;
                    parent.incPhotoCount(1);
                    parent = parent.getParent();
                } while (deep < 10);
                parent = photo.getAlbum();
                deep = 0;
                do {
                    if (parent == null) break;
                    deep++;
                    parent.incPhotoCount(-1);
                    parent = parent.getParent();
                } while (deep < 10);
                photo.setAlbum(new_album);
            }
            if (cover) {
                photo.getAlbum().setCover(photo);
            }
            if (photo.getAlbum().getType() == AlbumBean.TYPE_PUBLIC && photo.getStatus() != PhotoBean.STATUS_PRIVATE) {
                if (!StringUtils.equals(photo.getKeyword(), newKeyword)) {
                    TagDAO.deleteTagByRefId(photo.getId(), DiaryBean.TYPE_PHOTO);
                    photo.setKeyword(newKeyword);
                    List tags = photo.getKeywords();
                    if (tags != null && tags.size() > 0) {
                        int tag_count = 0;
                        for (int i = 0; i < tags.size(); i++) {
                            if (tag_count >= MAX_TAG_COUNT) break;
                            String tag_name = (String) tags.get(i);
                            if (tag_name.getBytes().length > MAX_TAG_LENGTH) continue;
                            TagBean tag = new TagBean();
                            tag.setSite(photo.getSite());
                            tag.setRefId(photo.getId());
                            tag.setRefType(DiaryBean.TYPE_PHOTO);
                            tag.setName((String) tags.get(i));
                            photo.getTags().add(tag);
                            tag_count++;
                        }
                    }
                }
            } else {
                TagDAO.deleteTagByRefId(photo.getId(), DiaryBean.TYPE_PHOTO);
            }
            commit();
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * д��Ƭ��Ϣ����ݿ�
	 * @param album
	 * @param photo
	 * @param cover
	 * @throws IllegalAccessException 
	 * @throws ObjectNotFoundException 
	 */
    public static void create(AlbumBean album, PhotoBean photo, boolean cover) throws IllegalAccessException, ObjectNotFoundException {
        if (photo == null || album == null) throw new IllegalArgumentException();
        if (album.getSite().getId() != photo.getSite().getId()) throw new IllegalAccessException(album.getName());
        photo.setAlbum(album);
        Calendar cal = Calendar.getInstance();
        photo.setYear(cal.get(Calendar.YEAR));
        photo.setMonth((cal.get(Calendar.MONTH) + 1));
        photo.setDate(cal.get(Calendar.DATE));
        photo.setCreateTime(cal.getTime());
        Session ssn = getSession();
        try {
            beginTransaction();
            int photo_site = DLOG4JUtils.sizeInKbytes(photo.getPhotoInfo().getSize());
            photo.getSite().getCapacity().incPhotoUsed(photo_site);
            album.setPhotoCount(album.getPhotoCount() + 1);
            if (cover) album.setCover(photo);
            AlbumBean parent = album.getParent();
            int deep = 0;
            do {
                if (parent == null) break;
                deep++;
                parent.incPhotoCount(1);
                parent = parent.getParent();
            } while (deep < 10);
            photo.getUser().getCount().incPhotoCount(1);
            ssn.save(photo);
            if (album.getType() == AlbumBean.TYPE_PUBLIC && photo.getStatus() != PhotoBean.STATUS_PRIVATE) {
                List tags = photo.getKeywords();
                if (tags != null && tags.size() > 0) {
                    int tag_count = 0;
                    for (int i = 0; i < tags.size(); i++) {
                        if (tag_count >= MAX_TAG_COUNT) break;
                        String tag_name = (String) tags.get(i);
                        if (tag_name.getBytes().length > MAX_TAG_LENGTH) continue;
                        TagBean tag = new TagBean();
                        tag.setSite(photo.getSite());
                        tag.setRefId(photo.getId());
                        tag.setRefType(DiaryBean.TYPE_PHOTO);
                        tag.setName(tag_name);
                        ssn.save(tag);
                        tag_count++;
                    }
                }
            }
            commit();
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * ��ȡĳ����վ��ǰ������Ƭ�Ĵ�С�ܺ�
	 * @param sid
	 * @param album_id
	 * @return
	 */
    public static int getTotalPhotoSize(int sid, int album_id) {
        if (sid < 1) return -1;
        StringBuffer hql = new StringBuffer("SELECT SUM(p.photoInfo.size) FROM PhotoBean AS p WHERE p.site.id=?");
        if (album_id > 0) hql.append(" AND p.album.id=?");
        Session ssn = getSession();
        Query q = ssn.createQuery(hql.toString());
        q.setInteger(0, sid);
        if (album_id > 0) q.setInteger(1, album_id);
        try {
            Number size = (Number) q.uniqueResult();
            return (size != null) ? size.intValue() : 0;
        } finally {
            hql = null;
        }
    }

    /**
	 * ��ȡĳ����վ��ǰ������Ƭ����
	 * @param sid
	 * @param album_id
	 * @return
	 */
    public static int getTotalPhotoCount(int sid, int album_id, int month) {
        if (sid < 1) return -1;
        StringBuffer hql = new StringBuffer("SELECT COUNT(*) FROM PhotoBean AS p WHERE p.site.id=:site");
        if (album_id > 0) hql.append(" AND p.album.id=:album");
        if (month >= 190001 && month <= 209912) {
            hql.append(" AND p.year = :year AND p.month = :month");
        }
        Session ssn = getSession();
        Query q = ssn.createQuery(hql.toString());
        q.setInteger("site", sid);
        if (album_id > 0) q.setInteger("album", album_id);
        if (month >= 190001 && month <= 209912) {
            q.setInteger("year", month / 100);
            q.setInteger("month", month % 100);
        }
        try {
            Number size = (Number) q.uniqueResult();
            return (size != null) ? size.intValue() : 0;
        } finally {
            hql = null;
        }
    }

    /**
	 * ͳ��ָ���·�ÿ�����Ƭ��
	 * @param site
	 * @param loginUser
	 * @param month
	 * @return
	 */
    public static int[] statCalendarPhotoCount(SiteBean site, SessionUserObject user, Calendar month) {
        Calendar firstDate = (Calendar) month.clone();
        firstDate.set(Calendar.DATE, 1);
        DateUtils.resetTime(firstDate);
        Calendar nextMonthFirstDate = (Calendar) firstDate.clone();
        nextMonthFirstDate.add(Calendar.MONTH, 1);
        Calendar tempCal = (Calendar) nextMonthFirstDate.clone();
        tempCal.add(Calendar.DATE, -1);
        int dateCount = tempCal.get(Calendar.DATE);
        int[] logCounts = new int[dateCount + 1];
        boolean is_owner = site.isOwner(user);
        StringBuffer hql = new StringBuffer("SELECT j.createTime FROM PhotoBean AS j WHERE j.createTime>=:beginTime AND j.createTime<:endTime AND j.site.id=:site");
        if (!is_owner) {
            hql.append(" AND j.status=:status AND j.album.type=:album_type");
        }
        try {
            Session ssn = getSession();
            Query q = ssn.createQuery(hql.toString()).setCacheable(true);
            q.setTimestamp("beginTime", firstDate.getTime());
            q.setTimestamp("endTime", nextMonthFirstDate.getTime());
            q.setInteger("site", site.getId());
            if (!is_owner) {
                q.setInteger("status", PhotoBean.STATUS_NORMAL);
                q.setInteger("album_type", AlbumBean.TYPE_PUBLIC);
            }
            int total = 0;
            Iterator logs = q.list().iterator();
            while (logs.hasNext()) {
                tempCal.setTime((Date) logs.next());
                int date = tempCal.get(Calendar.DATE);
                logCounts[date]++;
                total++;
            }
            logCounts[0] = total;
            return logCounts;
        } finally {
            hql = null;
            firstDate = null;
            nextMonthFirstDate = null;
            tempCal = null;
        }
    }

    /**
	 * @see com.liusoft.dlog4j.search.SearchDataProvider#fetchAfter(Date)
	 */
    public List fetchAfter(Date beginTime) throws Exception {
        return findNamedAll("LIST_PHOTO_AFTER", new Object[] { beginTime, PhotoBean.I_STATUS_NORMAL, AlbumBean.I_TYPE_PRIVATE });
    }

    /**
	 * ����ָ��վ�����Ƭ��������
	 * @param site
	 * @return
	 */
    public static int getPhotoReplyCount(int site) {
        String hql = "SELECT COUNT(*) FROM PhotoReplyBean AS d WHERE d.status=?";
        if (site > 0) {
            hql += " AND d.site.id=?";
            return executeStatAsInt(hql, PhotoReplyBean.STATUS_NORMAL, site);
        }
        return executeStatAsInt(hql, PhotoReplyBean.STATUS_NORMAL);
    }

    /**
	 * ��ȡ�����������(p_replies.vm)
	 * @param site
	 * @param user
	 * @param fromIdx
	 * @param count
	 * @return
	 */
    public static int getPhotoReplyCount(SiteBean site, SessionUserObject user) {
        boolean is_owner = site.isOwner(user);
        StringBuffer hql = new StringBuffer("SELECT COUNT(*) FROM PhotoReplyBean AS r WHERE r.status=? AND r.site.id=?");
        if (!is_owner) {
            hql.append(" AND r.photo.album.type=? AND r.photo.status=?");
            return executeStatAsInt(hql.toString(), PhotoReplyBean.STATUS_NORMAL, site.getId(), AlbumBean.TYPE_PUBLIC, PhotoBean.STATUS_NORMAL);
        }
        return executeStatAsInt(hql.toString(), PhotoReplyBean.STATUS_NORMAL, site.getId());
    }

    /**
	 * �г�������Ƭ����
	 * @param site
	 * @param user
	 * @param fromIdx
	 * @param count
	 * @return
	 */
    public static List listPhotoReplies(SiteBean site, int fromIdx, int count, SessionUserObject user) {
        boolean is_owner = site.isOwner(user);
        StringBuffer hql = new StringBuffer("FROM PhotoReplyBean AS r WHERE r.status=:status AND r.site.id=:site AND r.photo.status=:photo_status");
        if (!is_owner) {
            hql.append(" AND r.photo.album.type=:album_type");
            hql.append(" AND (r.ownerOnly = 0 OR r.user.id=:userid)");
        }
        hql.append(" ORDER BY r.id DESC");
        Session ssn = getSession();
        Query q = ssn.createQuery(hql.toString());
        q.setInteger("status", PhotoReplyBean.STATUS_NORMAL);
        q.setInteger("photo_status", PhotoBean.STATUS_NORMAL);
        q.setInteger("site", site.getId());
        if (!is_owner) {
            q.setInteger("album_type", AlbumBean.TYPE_PUBLIC);
            q.setInteger("userid", (user != null) ? user.getId() : -1);
        }
        if (fromIdx > 0) q.setFirstResult(fromIdx);
        if (count > 0) q.setMaxResults(count);
        return q.list();
    }

    /**
	 * ��ҳ�г�ĳ����Ƭ������
	 * @param log_id
	 * @param fromIdx
	 * @param count
	 * @return
	 */
    public static List listPhotoReplies(int photo_id, int fromIdx, int count) {
        return executeNamedQuery("REPLIES_OF_PHOTO", fromIdx, count, photo_id);
    }

    /**
	 * ������Ƭ����,�Զ����¶�Ӧ��Ƭ��������
	 * �������������������������Զ�����
	 * @param reply
	 */
    public static void createPhotoReply(PhotoReplyBean reply) {
        try {
            Session ssn = getSession();
            int max_reply_count = ConfigDAO.getMaxReplyCount(reply.getSite().getId());
            beginTransaction();
            reply.getPhoto().incReplyCount(1);
            if (reply.getPhoto().getReplyCount() >= max_reply_count) reply.getPhoto().setLock(1);
            reply.getPhoto().setLastReplyTime(new Date());
            if (reply.getUser() != null) reply.getUser().getCount().incPhotoReplyCount(1);
            ssn.save(reply);
            commit();
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * ɾ����Ƭ����,�Զ����ٶ�Ӧ��Ƭ��������
	 * @param reply
	 */
    public static void deletePhotoReply(PhotoReplyBean reply) {
        Session ssn = getSession();
        try {
            beginTransaction();
            reply.getPhoto().incReplyCount(-1);
            if (reply.getUser() != null) reply.getUser().getCount().incPhotoReplyCount(-1);
            ssn.delete(reply);
            commit();
        } catch (HibernateException e) {
            rollback();
        }
    }

    /**
	 * ��ȡĳ��ʱ����Ժ�������������(SearchEnginePlugIn::buildReplyIndex)
	 * @param date
	 * @return
	 * @throws Exception
	 */
    public static List listPhotoRepliesAfter(Date date) {
        return executeNamedQuery("LIST_PHOTO_REPLIES_AFTER", -1, -1, new Object[] { date, _ReplyBean.I_STATUS_NORMAL, PhotoBean.I_STATUS_NORMAL, AlbumBean.I_TYPE_PRIVATE });
    }
}
