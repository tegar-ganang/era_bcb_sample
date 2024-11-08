package fireteam.orb.server;

import fireteam.orb.server.stub.*;
import fireteam.orb.server.processors.*;
import fireteam.orb.util.FileClassLoader;
import fireteam.orb.util.ObjUtil;
import fireteam.orb.util.ObjUtil;
import java.util.*;
import java.io.*;
import java.math.BigDecimal;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Any;

/**
 * Класс для работы с системой
 * User: tolik1
 * Date: 25.01.2008
 * Time: 13:17:29
*/
public class FTDSessionImpl extends FTDSessionPOA {

    private class REQINFO {

        Class<?> cl;

        Method method;
    }

    private static final Map m_hashMap = Collections.synchronizedMap(new HashMap<String, REQINFO>());

    private static FileClassLoader m_fileLoader = FileClassLoader.getInstance();

    private static final Map m_robotMap = Collections.synchronizedMap(new HashMap<String, FTDRobot>());

    /**
	 * Класс для работы с пулом соединений, инициализированный уже под данную сессию
	 */
    private oraDB m_oraDB;

    private String m_sInitSql;

    private sessionData m_sessionData;

    private static final BigDecimal ROOT = BigDecimal.valueOf(-1);

    private static final ResourceBundle m_Resources = ResourceBundle.getBundle(FTDSessionImpl.class.getName());

    private static final Map m_typesMap;

    static {
        m_typesMap = Collections.synchronizedMap(new HashMap<String, Class<?>>());
        m_typesMap.put("DIR", FTTreeItemImpl.class);
    }

    public void registerTypeClass(Class cl, String sType) {
        if (cl.isAssignableFrom(FTDObjectImpl.class)) m_typesMap.put(sType, cl);
    }

    private static Constructor<?> getConstructorByObj(Class cl) throws NoSuchMethodException {
        return cl.getConstructor(new Class[] { oraDB.class, FTDObject.class, ORB.class });
    }

    /**
	 * Разбор данных переданных от
	 * @param oid		-
	 */
    private void parseOID(byte[] oid) {
        try {
            int iHash = Integer.valueOf(new String(oid));
            m_sessionData = sessionData.getFromMap(iHash);
            m_sInitSql = m_Resources.getString("INIT").replaceAll(":A", String.valueOf(m_sessionData.m_iClientID));
            m_oraDB = new oraDB(m_sInitSql, m_sessionData);
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
        }
    }

    /**
	 * Конструктор создает объект сессии, уникальный по данным oid
	 * @param oid
	 */
    public FTDSessionImpl(byte[] oid) {
        parseOID(oid);
    }

    /**
	 * Функция возвращает корневой элемент тип его корень дерева
	 * @return - базовый элемент
	 */
    @Override
    public FTDObject root() {
        try {
            return getObject(ROOT.toString());
        } catch (StandardException e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
        }
        return null;
    }

    @Override
    public void registerRobot(String Operation, FTDRobot robot) throws StandardException {
        m_robotMap.put(Operation, robot);
    }

    @Override
    public void unregisterRobot(String Operation, FTDRobot robot) throws StandardException {
        FTDRobot test = (FTDRobot) m_robotMap.get(Operation);
        if (!test.equals(robot)) return;
        m_robotMap.remove(Operation);
    }

    @Override
    public FTDObject getObject(String ID) throws StandardException {
        FTDObject root = FTDObjectImpl.getObjectByID(m_oraDB, _orb(), ID);
        Class<?> cl = (Class<?>) m_typesMap.get(root.Type);
        if (cl != null) {
            try {
                return ((FTDObjectImpl) getConstructorByObj(cl).newInstance(m_oraDB, root, _orb())).getObject();
            } catch (Exception e) {
                Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
                throw (ObjUtil.throwStandardException(e));
            }
        } else return root;
    }

    @Override
    public FTDObject[] getChildrenByType(String sParentID, String sTypeName) throws StandardException {
        FTTreeItemImpl tit = new FTTreeItemImpl(m_oraDB, sParentID, _orb());
        return tit.getChildrenByType(sTypeName);
    }

    @Override
    public FTDObject[] getChildren(String sParentID) throws StandardException {
        FTDObject root = FTDObjectImpl.getObjectByID(m_oraDB, _orb(), sParentID);
        Class<?> cl = (Class<?>) m_typesMap.get(root.Type);
        if (cl == null) cl = FTDObjectImpl.class;
        try {
            return ((FTDObjectImpl) getConstructorByObj(cl).newInstance(m_oraDB, root, _orb())).getChildren();
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        }
    }

    @Override
    public FTDObject addObject(String parentID, FTDObject obj) throws StandardException {
        FTDObjectImpl rootImpl;
        FTDObject root = FTDObjectImpl.getObjectByID(m_oraDB, _orb(), parentID);
        Class<?> cl = (Class<?>) m_typesMap.get(root.Type);
        if (cl == null) cl = FTDObjectImpl.class;
        try {
            rootImpl = ((FTDObjectImpl) getConstructorByObj(cl).newInstance(m_oraDB, root, _orb()));
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        }
        return rootImpl.add(obj);
    }

    @Override
    public void deleteObject(String ID) throws StandardException {
        FTDObjectImpl rootImpl;
        FTDObject root = FTDObjectImpl.getObjectByID(m_oraDB, _orb(), ID);
        Class<?> cl = (Class<?>) m_typesMap.get(root.Type);
        if (cl == null) cl = FTDObjectImpl.class;
        try {
            rootImpl = ((FTDObjectImpl) getConstructorByObj(cl).newInstance(m_oraDB, root, _orb()));
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        }
        rootImpl.delete();
    }

    @Override
    public void renameObject(String ID, String newObjectName) throws StandardException {
        FTDObjectImpl rootImpl;
        FTDObject root = FTDObjectImpl.getObjectByID(m_oraDB, _orb(), ID);
        Class<?> cl = (Class<?>) m_typesMap.get(root.Type);
        if (cl == null) cl = FTDObjectImpl.class;
        try {
            rootImpl = ((FTDObjectImpl) getConstructorByObj(cl).newInstance(m_oraDB, root, _orb()));
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        }
        rootImpl.rename(newObjectName);
    }

    @Override
    public void moveObject(String ID, String newParentID) throws StandardException {
        FTDObjectImpl rootImpl;
        FTDObject root = FTDObjectImpl.getObjectByID(m_oraDB, _orb(), ID);
        Class<?> cl = (Class<?>) m_typesMap.get(root.Type);
        if (cl == null) cl = FTDObjectImpl.class;
        try {
            rootImpl = ((FTDObjectImpl) getConstructorByObj(cl).newInstance(m_oraDB, root, _orb()));
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        }
        rootImpl.move(newParentID);
    }

    @Override
    public void changeObject(FTDObject obj) throws StandardException {
        FTDObjectImpl rootImpl;
        FTDObject root = FTDObjectImpl.getObjectByID(m_oraDB, _orb(), obj.ID);
        Class<?> cl = (Class<?>) m_typesMap.get(root.Type);
        if (cl == null) cl = FTDObjectImpl.class;
        try {
            rootImpl = ((FTDObjectImpl) getConstructorByObj(cl).newInstance(m_oraDB, root, _orb()));
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        }
        rootImpl.change(obj);
    }

    @Override
    public FTDObject[] getContent(String sParentID, String sType) throws StandardException {
        FTDObject root = FTDObjectImpl.getObjectByID(m_oraDB, _orb(), sParentID);
        Class<?> cl = (Class<?>) m_typesMap.get(root.Type);
        if (cl == null) cl = FTDObjectImpl.class;
        try {
            return ((FTDObjectImpl) getConstructorByObj(cl).newInstance(m_oraDB, root, _orb())).getContent(sType);
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        }
    }

    @Override
    public UserRights[] getObjectPermission(String ID) throws StandardException {
        return FTDObjectImpl.getRightsUsers(m_oraDB, ID);
    }

    @Override
    public void setObjectPermission(String ID, UserRights[] newPermissions, boolean isRecurcive, boolean isReplace) throws StandardException {
        FTDObjectImpl.setRightsUsers(m_oraDB, ID, newPermissions, isRecurcive, isReplace);
    }

    @Override
    public String[] getObjectVersions(String ID) throws StandardException {
        return new String[0];
    }

    @Override
    public String addObjectVersion(FTDObject obj, String newVersion) throws StandardException {
        return null;
    }

    @Override
    public SIGN[] getObjectSigns(String ID, String Version) throws StandardException {
        return FTDObjectImpl.getObjectSigns(m_oraDB, _orb(), ID, Version);
    }

    @Override
    public boolean isObjectSigned(String ID, String Version) throws StandardException {
        return false;
    }

    public boolean isObjectSigned(String ID) throws StandardException {
        return false;
    }

    @Override
    public Group[] getGroups() throws StandardException {
        ArrayList<Group> arGrp = new ArrayList<Group>();
        Connection con = null;
        try {
            con = m_oraDB.getConnection();
            String sSql = m_Resources.getString("DEP.LIST");
            PreparedStatement ps = con.prepareStatement(sSql);
            ResultSet rSet = ps.executeQuery();
            while (rSet.next()) {
                Group grp = new Group(rSet.getString("IDEPID"), rSet.getString("CDEPNAME"));
                arGrp.add(grp);
            }
            rSet.close();
            ps.close();
            return arGrp.toArray(new Group[arGrp.size()]);
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        } finally {
            if (con != null) try {
                con.close();
            } catch (Exception e1) {
            }
        }
    }

    @Override
    public User[] getUsers() throws StandardException {
        ArrayList<User> arUsr = new ArrayList<User>();
        Connection con = null;
        try {
            con = m_oraDB.getConnection();
            String sSql = m_Resources.getString("USR.LIST");
            PreparedStatement ps = con.prepareStatement(sSql);
            ResultSet rSet = ps.executeQuery();
            while (rSet.next()) {
                User usr = new User(rSet.getString("IUSRID"), rSet.getString("CLOGNAME"), rSet.getString("CUSRNAME"), rSet.getString("CUSRDUTY"), rSet.getShort("IUSRSTATUS"));
                if (usr.usrDuty == null) usr.usrDuty = "?";
                arUsr.add(usr);
            }
            rSet.close();
            ps.close();
            return arUsr.toArray(new User[arUsr.size()]);
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        } finally {
            if (con != null) try {
                con.close();
            } catch (Exception e1) {
            }
        }
    }

    @Override
    public User[] getGroupUsers(Group grp) throws StandardException {
        ArrayList<User> arUsr = new ArrayList<User>();
        Connection con = null;
        try {
            con = m_oraDB.getConnection();
            String sSql = m_Resources.getString("USR.DEP.LIST");
            PreparedStatement ps = null;
            if (!grp.ID.equals("-1")) {
                ps = con.prepareStatement(sSql);
                ps.setString(1, grp.ID);
            } else {
                ps = con.prepareStatement(m_Resources.getString("USR.NDEP.LIST"));
            }
            ResultSet rSet = ps.executeQuery();
            while (rSet.next()) {
                User usr = new User(rSet.getString("IUSRID"), rSet.getString("CLOGNAME"), rSet.getString("CUSRNAME"), rSet.getString("CUSRDUTY"), rSet.getShort("IUSRSTATUS"));
                usr.usrDuty = usr.usrDuty == null ? "нет" : usr.usrDuty;
                arUsr.add(usr);
            }
            rSet.close();
            ps.close();
            return arUsr.toArray(new User[arUsr.size()]);
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        } finally {
            if (con != null) try {
                con.close();
            } catch (Exception e1) {
            }
        }
    }

    private synchronized boolean reloadClasses() {
        m_fileLoader = FileClassLoader.getInstance();
        File dir = new File(m_Resources.getString("PROC_DIR"));
        File[] files = dir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.indexOf(".class") >= 0;
            }
        });
        if (files != null) for (File fd : files) m_fileLoader.loadFile(fd);
        m_hashMap.clear();
        System.gc();
        return true;
    }

    /**
	 * Функция загружает класс и метод для текущего запроса из БД
	 * @param sReqTypeID	- Название (Идентификатор запроса)
	 * @return данные о классе и методе для выполнения запроса
	 * @throws StandardException
	 */
    private synchronized REQINFO loadReqInfo(String sReqTypeID) throws StandardException {
        REQINFO ret = new REQINFO();
        Connection con = null;
        try {
            String sSql = m_Resources.getString("REQINFO");
            con = m_oraDB.getConnection();
            PreparedStatement stmt = con.prepareStatement(sSql);
            stmt.setString(1, sReqTypeID);
            ResultSet rSet = stmt.executeQuery();
            if (rSet.next()) {
                String sClassName = rSet.getString(1);
                String sMethod = rSet.getString(2);
                try {
                    ret.cl = m_fileLoader.loadClass(sClassName, true);
                    ret.method = ret.cl.getMethod(sMethod, Connection.class, Any.class, Any.class);
                } catch (Exception ce) {
                    Logger.getLogger("fireteam").log(Level.SEVERE, null, ce);
                }
            }
            stmt.close();
            rSet.close();
            return ret;
        } catch (SQLException e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (new StandardException("REQINFO: " + e.getSQLState() + " - " + e.getMessage()));
        } finally {
            try {
                if (con != null) con.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * Функция используется для получения информации, не представленной
	 * обычными функциями класса
	 * @param param
	 * @return
	 */
    @Override
    public Any request(String sRequestID, Any param) throws StandardException {
        FTDRobot robot = (FTDRobot) m_robotMap.get(sRequestID);
        if (robot != null) return robot.request(param);
        REQINFO info = (REQINFO) m_hashMap.get(sRequestID);
        if (info == null) {
            info = loadReqInfo(sRequestID);
            if (info.method == null) throw (new StandardException("processRequest FAILED: Could't load class "));
            m_hashMap.put(sRequestID, info);
        }
        if (info.method != null) {
            Connection con = null;
            try {
                con = m_oraDB.getConnection();
                Any ret = (Any) info.method.invoke(null, con, param, _orb().create_any());
                return ret;
            } catch (Exception e) {
                Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
                throw (ObjUtil.throwStandardException(e));
            } finally {
                if (con != null) try {
                    con.close();
                } catch (SQLException e) {
                }
            }
        }
        throw (new StandardException("Function " + sRequestID + " not found"));
    }

    /**
	 * Функция перезагружает все классы обработчиков в директории processors
	 * @throws fireteam.orb.server.stub.StandardException
	 */
    @Override
    public void reloadProcessors() throws StandardException {
        reloadClasses();
    }

    /**
	 * Функция больше не используется за ненадобностью
	 * @deprecated
	 */
    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
	 * Функция возвращает файл в формате javaFX, для отображения формы
	 * все файлы даного типа начинаются с директории forms
	 * @param Resource	- название файла
	 * @return данные из файла FormsDir/Resource в пожатом виде
	 * @throws fireteam.orb.server.stub.StandardException
	 */
    @Override
    public byte[] getFxResource(String Resource) throws StandardException {
        try {
            String sFormDir = java.util.ResourceBundle.getBundle(getClass().getName()).getString("FormsDir");
            FileInputStream fi = new FileInputStream(sFormDir + File.separator + Resource);
            byte fileData[] = new byte[(int) fi.getChannel().size()];
            fi.close();
            fileData = ObjUtil.compress(fileData);
            return fileData;
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        }
    }

    @Override
    public String[] getTypePermissions(String sType) throws StandardException {
        ArrayList<String> arPrv = new ArrayList<String>();
        Connection con = null;
        try {
            con = m_oraDB.getConnection();
            String sSql = m_Resources.getString("TYPE.PRV");
            PreparedStatement ps = con.prepareStatement(sSql);
            ps.setString(1, sType);
            ResultSet rSet = ps.executeQuery();
            while (rSet.next()) {
                arPrv.add(rSet.getString("CPRVNAME"));
            }
            rSet.close();
            ps.close();
            return arPrv.toArray(new String[arPrv.size()]);
        } catch (Exception e) {
            Logger.getLogger("fireteam").log(Level.SEVERE, null, e);
            throw (ObjUtil.throwStandardException(e));
        } finally {
            if (con != null) try {
                con.close();
            } catch (Exception e1) {
            }
        }
    }
}
