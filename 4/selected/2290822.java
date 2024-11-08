package com.luzan.app.map.service.handler;

import com.luzan.app.map.bean.MapOriginal;
import com.luzan.app.map.bean.gmap.GSize;
import com.luzan.app.map.bean.publik.PublicMapPost;
import com.luzan.app.map.bean.user.UserMapOriginal;
import com.luzan.app.map.pool.GeneralCompleteStrategy;
import com.luzan.app.map.service.bean.*;
import com.luzan.app.map.utils.Configuration;
import com.luzan.app.map.manager.MapManager;
import com.luzan.bean.Language;
import com.luzan.bean.Pagination;
import com.luzan.bean.User;
import com.luzan.common.httprpc.HttpRpcException;
import com.luzan.common.httprpc.annotation.HttpAction;
import com.luzan.common.httprpc.annotation.HttpAuthentication;
import com.luzan.common.httprpc.annotation.HttpParameter;
import com.luzan.common.pool.PoolClientInterface;
import com.luzan.common.pool.PoolFactory;
import com.luzan.common.pool.StatesStack;
import com.luzan.db.ReadOnlyTransaction;
import com.luzan.db.TransactionManager;
import com.luzan.db.dao.DAOFactory;
import com.luzan.db.dao.GenericDAO;
import com.luzan.db.dao.ObjectNotFoundException;
import com.luzan.db.dao.UserMapOriginalDAO;
import com.luzan.db.hibernate.dao.UserMapOriginalHibernateDAO;
import com.sun.xfile.XFile;
import com.sun.xfile.XFileInputStream;
import com.sun.xfile.XFileOutputStream;
import com.sun.media.jai.codec.FileCacheSeekableStream;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Expression;
import javax.media.jai.RenderedOp;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.awt.image.renderable.ParameterBlock;

/**
 * UserMapHandler
 *
 * @author Alexander Bondar
 */
public class UserMapHandler {

    private static final Logger logger = Logger.getLogger(UserMapHandler.class);

    private static URI emptyTileURI;

    private static XFile mapStorage;

    public UserMapHandler() {
        try {
            emptyTileURI = this.getClass().getClassLoader().getResource("transparent.png").toURI();
            mapStorage = new XFile(Configuration.getInstance().getPrivateMapStorage().toString());
        } catch (Throwable e) {
            logger.error(e);
            emptyTileURI = null;
        }
    }

    @HttpAction(name = "map.calibrate", method = { HttpAction.Method.post }, responseType = "text/plain", parameters = { @HttpParameter(name = "user"), @HttpParameter(name = "guid"), @HttpParameter(name = "uploadFile"), @HttpParameter(name = "mapUrl"), @HttpParameter(name = "mapSource"), @HttpParameter(name = "south"), @HttpParameter(name = "west"), @HttpParameter(name = "north"), @HttpParameter(name = "east") })
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE })
    public String calibrate(User user, String guid, Collection<FileItem> uploadFile, String mapUrl, String mapSource, String south, String west, String north, String east) throws HttpRpcException {
        GenericDAO<UserMapOriginal> dao = DAOFactory.createDAO(UserMapOriginal.class);
        try {
            TransactionManager.beginTransaction();
        } catch (Throwable e) {
            logger.error(e);
            return "FAIL";
        }
        try {
            final UserMapOriginal mapOriginal = dao.findUniqueByCriteria(Expression.eq("guid", guid));
            if (mapOriginal == null) throw new HttpRpcException(ErrorConstant.ERROR_NOT_FOUND, "map");
            if (UserMapOriginal.SubState.INPROC.equals(mapOriginal.getSubstate())) throw new HttpRpcException(ErrorConstant.ERROR_ILLEGAL_OBJECT_STATE, "map");
            if (UserMapOriginal.State.COMBINE.equals(mapOriginal.getState())) throw new HttpRpcException(ErrorConstant.ERROR_ILLEGAL_OBJECT_STATE, "map");
            if (!"download".equals(mapSource) && !"upload".equals(mapSource) && !"current".equals(mapSource)) throw new HttpRpcException(ErrorConstant.ERROR_INVALID_OBJECT, "mapSource");
            try {
                mapOriginal.setSWLat(Double.parseDouble(south));
            } catch (Throwable t) {
                throw new HttpRpcException(ErrorConstant.ERROR_INVALID_OBJECT, "south");
            }
            try {
                mapOriginal.setSWLon(Double.parseDouble(west));
            } catch (Throwable t) {
                throw new HttpRpcException(ErrorConstant.ERROR_INVALID_OBJECT, "west");
            }
            try {
                mapOriginal.setNELat(Double.parseDouble(north));
            } catch (Throwable t) {
                throw new HttpRpcException(ErrorConstant.ERROR_INVALID_OBJECT, "north");
            }
            try {
                mapOriginal.setNELon(Double.parseDouble(east));
            } catch (Throwable t) {
                throw new HttpRpcException(ErrorConstant.ERROR_INVALID_OBJECT, "east");
            }
            mapOriginal.setState(UserMapOriginal.State.CALIBRATE);
            mapOriginal.setSubstate(UserMapOriginal.SubState.INPROC);
            final XFile mapStorage = new XFile(new XFile(Configuration.getInstance().getPrivateMapStorage().toString()), mapOriginal.getGuid());
            mapStorage.mkdir();
            if ("download".equals(mapSource)) {
                final XFile tmpFile;
                final URI uri = new URI(mapUrl);
                String query = (StringUtils.isEmpty(uri.getQuery())) ? "?BBOX=" : "&BBOX=";
                query += west + "," + south + "," + east + "," + north;
                URLConnection con = (new URL(mapUrl + query)).openConnection();
                if (con == null || con.getContentLength() == 0) throw new HttpRpcException(ErrorConstant.ERROR_INVALID_RESOURCE, "mapUrl");
                if (!con.getContentType().startsWith("image/")) throw new HttpRpcException(ErrorConstant.ERROR_INVALID_OBJECT_TYPE, "mapUrl");
                tmpFile = new XFile(mapStorage, mapOriginal.getGuid());
                XFileOutputStream out = new XFileOutputStream(tmpFile);
                IOUtils.copy(con.getInputStream(), out);
                out.flush();
                out.close();
            } else if ("upload".equals(mapSource)) {
                final XFile tmpFile;
                final FileItem file = uploadFile.iterator().next();
                if (file == null || file.getSize() == 0) throw new HttpRpcException(ErrorConstant.ERROR_INVALID_RESOURCE, "uploadFile");
                if (!file.getContentType().startsWith("image/")) throw new HttpRpcException(ErrorConstant.ERROR_INVALID_OBJECT_TYPE, "uploadFile");
                tmpFile = new XFile(mapStorage, mapOriginal.getGuid());
                XFileOutputStream out = new XFileOutputStream(tmpFile);
                IOUtils.copy(file.getInputStream(), out);
                out.flush();
                out.close();
            } else if ("current".equals(mapSource)) {
            }
            dao.update(mapOriginal);
            TransactionManager.commitTransaction();
            try {
                PoolClientInterface pool = PoolFactory.getInstance().getClientPool();
                if (pool == null) throw ErrorConstant.EXCEPTION_INTERNAL;
                pool.put(mapOriginal, new StatesStack(new byte[] { 0x00, 0x18 }), GeneralCompleteStrategy.class);
            } catch (Throwable t) {
                logger.error(t);
            }
            return "SUCCESS";
        } catch (HttpRpcException e) {
            TransactionManager.rollbackTransaction();
            logger.error(e);
            return "FAIL";
        } catch (Throwable e) {
            logger.error(e);
            TransactionManager.rollbackTransaction();
            return "FAIL";
        }
    }

    @HttpAction(name = "map.update", method = { HttpAction.Method.post })
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE })
    public String update(User user, MapRequest pw) throws HttpRpcException {
        GenericDAO<UserMapOriginal> dao = DAOFactory.createDAO(UserMapOriginal.class);
        try {
            TransactionManager.beginTransaction();
        } catch (Throwable e) {
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        }
        try {
            UserMapOriginal mapOriginal = null;
            try {
                mapOriginal = dao.findUniqueByCriteria(Expression.eq("guid", pw.getGuid()));
            } catch (ObjectNotFoundException e) {
            }
            if (mapOriginal == null) throw new HttpRpcException(ErrorConstant.ERROR_NOT_FOUND, "map");
            if (UserMapOriginal.SubState.INPROC.equals(mapOriginal.getSubstate())) throw new HttpRpcException(ErrorConstant.ERROR_ILLEGAL_OBJECT_STATE, "map");
            if (pw.getName().length() > 64) throw new HttpRpcException(ErrorConstant.ERROR_INVALID_OBJECT_LENGTH, "name");
            mapOriginal.setName(pw.getName());
            mapOriginal.setDescription(pw.getDescription());
            mapOriginal.setModified(Calendar.getInstance().getTime());
            final String region = MapManager.verifyRegion(pw.getRegion());
            if (!StringUtils.isEmpty(region)) {
                mapOriginal.setRegion(region);
                mapOriginal.setState(UserMapOriginal.State.UPLOAD);
                mapOriginal.setSubstate(UserMapOriginal.SubState.COMPLETE);
            }
            dao.update(mapOriginal);
            TransactionManager.commitTransaction();
        } catch (HttpRpcException e) {
            TransactionManager.rollbackTransaction();
            logger.error(e);
            throw e;
        } catch (Throwable e) {
            TransactionManager.rollbackTransaction();
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        }
        return "OK";
    }

    @HttpAction(name = "map.setSubstate", method = { HttpAction.Method.post })
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE })
    public String setSubstate(User user, MapRequest pw) throws HttpRpcException {
        GenericDAO<UserMapOriginal> dao = DAOFactory.createDAO(UserMapOriginal.class);
        try {
            TransactionManager.beginTransaction();
        } catch (Throwable e) {
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        }
        try {
            UserMapOriginal mapOriginal = null;
            try {
                mapOriginal = dao.findUniqueByCriteria(Expression.eq("guid", pw.getGuid()));
            } catch (ObjectNotFoundException e) {
            }
            if (mapOriginal == null) throw new HttpRpcException(ErrorConstant.ERROR_NOT_FOUND, "map");
            if (UserMapOriginal.SubState.INPROC.equals(mapOriginal.getSubstate())) throw new HttpRpcException(ErrorConstant.ERROR_ILLEGAL_OBJECT_STATE, "map");
            mapOriginal.setSubstate(UserMapOriginal.SubState.valueOf(pw.getSubstate()));
            dao.update(mapOriginal);
            TransactionManager.commitTransaction();
        } catch (HttpRpcException e) {
            TransactionManager.rollbackTransaction();
            logger.error(e);
            throw e;
        } catch (Throwable e) {
            TransactionManager.rollbackTransaction();
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        }
        return "OK";
    }

    @HttpAction(name = "map.select", method = { HttpAction.Method.post })
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE })
    public Collection<? extends MapOriginal> select(User user, Pagination pagination, MapRequest mapArea) throws HttpRpcException {
        final UserMapOriginalDAO dao = new UserMapOriginalHibernateDAO(UserMapOriginal.class);
        Collection<? extends MapOriginal> _rez;
        try {
            ReadOnlyTransaction.beginTransaction();
        } catch (Throwable e) {
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        }
        try {
            _rez = dao.findMaps(user, pagination, mapArea);
        } catch (Throwable e) {
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        } finally {
            ReadOnlyTransaction.closeTransaction();
        }
        return _rez;
    }

    @HttpAction(name = "map.count", method = { HttpAction.Method.post })
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE })
    public CountResponse count(User user, MapRequest mapArea) throws HttpRpcException {
        final UserMapOriginalDAO dao = new UserMapOriginalHibernateDAO(UserMapOriginal.class);
        CountResponse _rez = new CountResponse();
        try {
            _rez.setCount(dao.countMaps(user, mapArea));
        } catch (Throwable e) {
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        }
        return _rez;
    }

    @HttpAction(name = "map.remove", method = { HttpAction.Method.post })
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE })
    public Boolean remove(User user, MergeRequest mergeRequest) throws HttpRpcException {
        GenericDAO<UserMapOriginal> dao = DAOFactory.createDAO(UserMapOriginal.class);
        try {
            TransactionManager.beginTransaction();
        } catch (Throwable e) {
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        }
        try {
            if (mergeRequest.getItems().size() <= 0) throw new HttpRpcException(ErrorConstant.ERROR_NOTHING_TO_PROCESS, "items");
            for (MapItemRequest r : mergeRequest.getItems()) {
                UserMapOriginal map = null;
                try {
                    map = dao.findUniqueByCriteria(Expression.eq("guid", r.getGuid()));
                } catch (ObjectNotFoundException e) {
                }
                if (map == null) throw new HttpRpcException(ErrorConstant.ERROR_NOT_FOUND, "map");
                if (map.getSubstate() == UserMapOriginal.SubState.INPROC) throw new HttpRpcException(ErrorConstant.ERROR_ILLEGAL_OBJECT_STATE, "map");
                map.setState(UserMapOriginal.State.DELETE);
                map.setSubstate(UserMapOriginal.SubState.COMPLETE);
                dao.update(map);
            }
            TransactionManager.commitTransaction();
        } catch (HttpRpcException e) {
            TransactionManager.rollbackTransaction();
            logger.error(e);
            throw e;
        } catch (Throwable e) {
            TransactionManager.rollbackTransaction();
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        }
        return true;
    }

    @HttpAction(name = "map.publish", method = { HttpAction.Method.post })
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE })
    public Boolean publish(User user, PublishRequest pubReq) throws HttpRpcException {
        GenericDAO<PublicMapPost> postDao = DAOFactory.createDAO(PublicMapPost.class);
        GenericDAO<Language> langDao = DAOFactory.createDAO(Language.class);
        GenericDAO<UserMapOriginal> dao = DAOFactory.createDAO(UserMapOriginal.class);
        try {
            TransactionManager.beginTransaction();
        } catch (Throwable e) {
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        }
        try {
            Language lang = langDao.findUniqueByCriteria(Expression.eq("code", pubReq.getLanguage()));
            if (lang == null) throw new HttpRpcException(ErrorConstant.ERROR_NOT_FOUND, "language");
            if (pubReq.getItems().size() <= 0) throw new HttpRpcException(ErrorConstant.ERROR_NOTHING_TO_PROCESS, "items");
            double map_maxLat = -190;
            double map_maxLon = -190;
            double map_minLat = 190;
            double map_minLon = 190;
            final UserMapOriginal mapC;
            ArrayList<UserMapOriginal> mapList = new ArrayList<UserMapOriginal>();
            if (!StringUtils.isEmpty(pubReq.getFrontMap())) {
                UserMapOriginal m = dao.findUniqueByCriteria(Expression.eq("guid", pubReq.getFrontMap()));
                if (m == null) throw new HttpRpcException(ErrorConstant.ERROR_NOT_FOUND, "map");
                if (!UserMapOriginal.State.COMBINE.equals(m.getState())) throw new HttpRpcException(ErrorConstant.ERROR_ILLEGAL_OBJECT_STATE, "map");
                if (!UserMapOriginal.SubState.COMPLETE.equals(m.getSubstate())) throw new HttpRpcException(ErrorConstant.ERROR_ILLEGAL_OBJECT_STATE, "map");
                mapC = m;
                mapList.addAll(m.getSubmaps());
                map_minLat = mapC.getSWLat();
                map_minLon = mapC.getSWLon();
                map_maxLat = mapC.getNELat();
                map_maxLon = mapC.getNELon();
            } else {
                mapC = new UserMapOriginal();
                mapC.setState(UserMapOriginal.State.COMBINE);
                mapC.setSubstate(UserMapOriginal.SubState.COMPLETE);
                mapC.setUser(user);
                mapC.setName(pubReq.getSubject());
                mapC.setDescription(pubReq.getMessage());
            }
            for (MapItemRequest r : pubReq.getItems()) {
                if (!"map".equals(r.getItemType())) throw new HttpRpcException(ErrorConstant.ERROR_INVALID_OBJECT_TYPE, "map");
                UserMapOriginal m = null;
                try {
                    m = dao.findUniqueByCriteria(Expression.eq("guid", r.getGuid()));
                } catch (ObjectNotFoundException e) {
                }
                if (m == null) throw new HttpRpcException(ErrorConstant.ERROR_NOT_FOUND, "map");
                if (!UserMapOriginal.State.CALIBRATE.equals(m.getState())) throw new HttpRpcException(ErrorConstant.ERROR_ILLEGAL_OBJECT_STATE, "map");
                if (!UserMapOriginal.SubState.COMPLETE.equals(m.getSubstate())) throw new HttpRpcException(ErrorConstant.ERROR_ILLEGAL_OBJECT_STATE, "map");
                map_maxLat = Math.max(map_maxLat, m.getNELat());
                map_maxLon = Math.max(map_maxLon, m.getNELon());
                map_minLat = Math.min(map_minLat, m.getSWLat());
                map_minLon = Math.min(map_minLon, m.getSWLon());
                mapList.add(m);
            }
            mapC.setSWLat(map_minLat);
            mapC.setSWLon(map_minLon);
            mapC.setNELat(map_maxLat);
            mapC.setNELon(map_maxLon);
            mapC.setSubmaps(mapList);
            dao.save(mapC);
            PublicMapPost post = new PublicMapPost();
            post.setSWLat(map_minLat);
            post.setSWLon(map_minLon);
            post.setNELat(map_maxLat);
            post.setNELon(map_maxLon);
            post.setLanguage(pubReq.getLanguage());
            post.setLogin(user.getLogin());
            post.setName(pubReq.getSubject());
            post.setDescription(pubReq.getMessage());
            post.setFrontMap(mapC.getGuid());
            post.setBackMap(pubReq.getBackMap());
            post.setState(PublicMapPost.State.HIDDEN);
            post.setSubstate(PublicMapPost.SubState.INPROC);
            postDao.save(post);
            pubReq.setPostGuid(post.getGuid());
            pubReq.setFrontMap(mapC.getGuid());
            TransactionManager.commitTransaction();
            try {
                PoolClientInterface pool = PoolFactory.getInstance().getClientPool();
                if (pool == null) throw ErrorConstant.EXCEPTION_INTERNAL;
                pool.put(pubReq, new StatesStack((byte) 0x01), new StatesStack(new byte[] { 0x01, 0x24, 0x21, 0x12 }), GeneralCompleteStrategy.class);
            } catch (Throwable t) {
                logger.error(t);
            }
        } catch (HttpRpcException e) {
            TransactionManager.rollbackTransaction();
            logger.error(e);
            throw e;
        } catch (Throwable e) {
            TransactionManager.rollbackTransaction();
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        }
        return Boolean.TRUE;
    }

    @HttpAction(name = "map.selectOne", method = { HttpAction.Method.post }, parameters = { @HttpParameter(name = "guid") })
    @HttpAuthentication(method = { HttpAuthentication.Method.NONE })
    public UserMapOriginal selectOne(String guid) throws HttpRpcException {
        GenericDAO<UserMapOriginal> postDao = DAOFactory.createDAO(UserMapOriginal.class);
        try {
            ReadOnlyTransaction.beginTransaction();
        } catch (Throwable e) {
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        }
        try {
            if (StringUtils.isEmpty(guid)) throw new HttpRpcException(ErrorConstant.ERROR_INVALID_OBJECT, "guid");
            return postDao.findUniqueByCriteria(Expression.eq("guid", guid));
        } catch (HttpRpcException e) {
            logger.error(e);
            throw e;
        } catch (Throwable e) {
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        } finally {
            ReadOnlyTransaction.closeTransaction();
        }
    }

    @HttpAction(name = "map.thmb", method = { HttpAction.Method.get, HttpAction.Method.post }, responseType = "image/png", parameters = { @HttpParameter(name = "guid"), @HttpParameter(name = "size") })
    @HttpAuthentication(method = { HttpAuthentication.Method.NONE })
    public InputStream getThumbnail(String guid, String size) throws HttpRpcException {
        if (StringUtils.isEmpty(guid)) return null;
        final XFile dir = new XFile(mapStorage, guid);
        if (!dir.isDirectory()) return null;
        XFile thmbFileS = new XFile(dir, "thumbnail-s");
        XFile thmbFileM = new XFile(dir, "thumbnail-m");
        XFile thmbFileL = new XFile(dir, "thumbnail-l");
        try {
            if ("s".equalsIgnoreCase(size)) return new XFileInputStream(thmbFileS); else if ("m".equalsIgnoreCase(size)) return new XFileInputStream(thmbFileM); else if ("l".equalsIgnoreCase(size)) return new XFileInputStream(thmbFileL); else return new XFileInputStream(thmbFileM);
        } catch (Throwable e) {
            logger.error(e);
            return null;
        }
    }

    @HttpAction(name = "map.img", cached = true, method = { HttpAction.Method.get, HttpAction.Method.post }, responseType = "image/jpeg", parameters = { @HttpParameter(name = "guid") })
    @HttpAuthentication(method = { HttpAuthentication.Method.NONE })
    public InputStream getImage(String guid) throws HttpRpcException {
        if (StringUtils.isEmpty(guid)) return null;
        final XFile dir = new XFile(mapStorage, guid);
        if (!dir.isDirectory()) return null;
        XFile thmbFileS = new XFile(dir, guid);
        try {
            return new XFileInputStream(thmbFileS);
        } catch (Throwable e) {
            logger.error(e);
            return null;
        }
    }

    @HttpAction(name = "map.show", method = { HttpAction.Method.get, HttpAction.Method.post }, responseType = "image/png")
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE, HttpAuthentication.Method.NONE })
    public InputStream show(User user, TileIndex ti) throws HttpRpcException {
        try {
            final String guid = ti.getGuid();
            if (StringUtils.isEmpty(guid)) throw new IllegalArgumentException("invalid map guid");
            final XFile tileFile = new XFile(new XFile(mapStorage, guid), ti.getX() + "_" + ti.getY() + "_" + ti.getZ());
            if (!tileFile.exists()) return new FileInputStream(new File(emptyTileURI));
            return new XFileInputStream(tileFile);
        } catch (Throwable e) {
            logger.error(e);
            try {
                return new FileInputStream(new File(emptyTileURI));
            } catch (Throwable t) {
                logger.error(e);
                return null;
            }
        }
    }

    @HttpAction(name = "map.corner", method = { HttpAction.Method.get, HttpAction.Method.post }, parameters = { @HttpParameter(name = "guid"), @HttpParameter(name = "name") }, responseType = "image/png")
    @HttpAuthentication(method = { HttpAuthentication.Method.NONE })
    public InputStream corner(String guid, String name) throws HttpRpcException {
        try {
            if (StringUtils.isEmpty(guid)) throw new IllegalArgumentException("invalid map guid");
            final XFile tileFile = new XFile(new XFile(mapStorage, guid), name + ".jpg");
            if (!tileFile.exists()) return new FileInputStream(new File(emptyTileURI));
            return new XFileInputStream(tileFile);
        } catch (Throwable e) {
            logger.error(e);
            try {
                return new FileInputStream(new File(emptyTileURI));
            } catch (Throwable t) {
                logger.error(e);
                return null;
            }
        }
    }

    @HttpAction(name = "map.size", method = { HttpAction.Method.get, HttpAction.Method.post }, parameters = { @HttpParameter(name = "guid") })
    @HttpAuthentication(method = { HttpAuthentication.Method.NONE })
    public GSize size(String guid) throws HttpRpcException {
        XFileInputStream in = null;
        FileCacheSeekableStream min = null;
        RenderedOp imSrc = null;
        try {
            final XFile file = new XFile(new XFile(mapStorage, guid), guid);
            if (!file.exists() || !file.isFile()) throw new HttpRpcException(ErrorConstant.ERROR_NOT_FOUND, "map");
            in = new XFileInputStream(file);
            min = new FileCacheSeekableStream(in);
            imSrc = new RenderedOp("stream", (new ParameterBlock()).add(min), null);
            return new GSize(imSrc.getWidth(), imSrc.getHeight());
        } catch (Throwable e) {
            logger.error(e);
            throw ErrorConstant.EXCEPTION_INTERNAL;
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(min);
            if (imSrc != null) imSrc.dispose();
        }
    }
}
