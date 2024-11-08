package com.luzan.app.map.service.handler;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.hibernate.criterion.Expression;
import com.luzan.common.httprpc.HttpRpcException;
import com.luzan.common.httprpc.annotation.HttpAction;
import com.luzan.common.httprpc.annotation.HttpAuthentication;
import com.luzan.common.pool.PoolFactory;
import com.luzan.bean.Pagination;
import com.luzan.bean.User;
import com.luzan.app.map.bean.MapOriginal;
import com.luzan.app.map.processor.TaskState;
import com.luzan.app.map.pool.MapOverrideStrategy;
import com.luzan.db.dao.GenericDAO;
import com.luzan.db.dao.DAOFactory;
import com.luzan.db.dao.DAOException;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.rmi.RemoteException;

/**
 * GMapHandler
 *
 * @author Alexander Bondar
 */
public class GMapHandler {

    private static final Logger logger = Logger.getLogger(GMapHandler.class);

    private static HttpRpcException ERROR_INTERNAL = new HttpRpcException(100, "Server error: 100", HttpRpcException.LEVEL_CRITICAL_ERROR);

    private static HttpRpcException ERROR_INPROC_MODIFICATION = new HttpRpcException(101, "Processed item can't be modified: 100", HttpRpcException.LEVEL_FLOW_ERROR);

    @HttpAction(name = "map.saveOrUpdate", method = { HttpAction.Method.post }, responseType = "text/plain")
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE })
    public String saveOrUpdate(FileItem file, User user, MapOriginal map) throws HttpRpcException {
        File tmpFile;
        GenericDAO<MapOriginal> mapDao = DAOFactory.createDAO(MapOriginal.class);
        try {
            assert (file != null);
            String jobid = null;
            if (file.getContentType().startsWith("image/")) {
                tmpFile = File.createTempFile("gmap", "img");
                OutputStream out = new FileOutputStream(tmpFile);
                IOUtils.copy(file.getInputStream(), out);
                out.flush();
                out.close();
                map.setState(MapOriginal.MapState.UPLOAD);
                map.setUser(user);
                map.setMapPath(tmpFile.getPath());
                map.setThumbnailUrl("/map/inproc.gif");
                map.setMimeType(file.getContentType());
                mapDao.saveOrUpdate(map);
                jobid = PoolFactory.getClientPool().put(map, TaskState.STATE_MO_FINISH, MapOverrideStrategy.class);
            }
            return jobid;
        } catch (IOException e) {
            logger.error(e);
            throw ERROR_INTERNAL;
        } catch (DAOException e) {
            logger.error(e);
            throw ERROR_INTERNAL;
        }
    }

    @HttpAction(name = "map.select", method = { HttpAction.Method.post })
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE })
    public List<MapOriginal> select(User user, Pagination pagination) throws HttpRpcException {
        GenericDAO<MapOriginal> dao = DAOFactory.createDAO(MapOriginal.class);
        List<MapOriginal> _rez;
        try {
            _rez = dao.findByCriteria(Expression.and(Expression.ne("state", MapOriginal.MapState.DELETE), Expression.eq("user", user)), pagination.getFrom(), pagination.getCount());
        } catch (DAOException e) {
            logger.error(e);
            throw ERROR_INTERNAL;
        }
        return _rez;
    }

    @HttpAction(name = "map.delete", method = { HttpAction.Method.post })
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE })
    public List<MapOriginal> delete(User user, Pagination pagination, MapOriginal map) throws HttpRpcException {
        GenericDAO<MapOriginal> dao = DAOFactory.createDAO(MapOriginal.class);
        List<MapOriginal> _rez;
        try {
            map = dao.findById(map.getId());
            if (map.getSubState() == MapOriginal.MapSubState.INPROC) throw ERROR_INPROC_MODIFICATION;
            map.setState(MapOriginal.MapState.DELETE);
            PoolFactory.getClientPool().put(map, TaskState.STATE_MO_FINISH, MapOverrideStrategy.class);
            dao.update(map);
            _rez = dao.findByCriteria(Expression.and(Expression.ne("state", MapOriginal.MapState.DELETE), Expression.eq("user", user)), pagination.getFrom(), pagination.getCount());
        } catch (DAOException e) {
            logger.error(e);
            throw ERROR_INTERNAL;
        } catch (RemoteException e) {
            logger.error(e);
            throw ERROR_INTERNAL;
        }
        return _rez;
    }

    @HttpAction(name = "map.calibrate", method = { HttpAction.Method.post })
    @HttpAuthentication(method = { HttpAuthentication.Method.WSSE })
    public List<MapOriginal> calibrate(User user, Pagination pagination, MapOriginal map) throws HttpRpcException {
        GenericDAO<MapOriginal> dao = DAOFactory.createDAO(MapOriginal.class);
        List<MapOriginal> _rez;
        try {
            map = dao.findById(map.getId());
            if (map.getSubState() == MapOriginal.MapSubState.INPROC) throw ERROR_INPROC_MODIFICATION;
            _rez = dao.findByCriteria(Expression.and(Expression.ne("state", MapOriginal.MapState.DELETE), Expression.eq("user", user)), pagination.getFrom(), pagination.getCount());
        } catch (DAOException e) {
            logger.error(e);
            throw ERROR_INTERNAL;
        }
        return _rez;
    }
}
