package de.beas.explicanto.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import net.sf.hibernate.ObjectNotFoundException;
import org.apache.log4j.Logger;
import de.beas.explicanto.RolesListValidator;
import de.beas.explicanto.XPCConst;
import de.beas.explicanto.components.CourseTransformator;
import de.beas.explicanto.screenplay.JAXBConverter;
import de.beas.explicanto.screenplay.jaxb.Screenplay;
import de.beas.explicanto.screenplay.reports.ReportsManager;
import de.beas.explicanto.server.exceptions.AuthenticationException;
import de.beas.explicanto.server.exceptions.EntitlementEx;
import de.beas.explicanto.server.exceptions.ObjNotFoundException;
import de.beas.explicanto.server.exceptions.ParamException;
import de.beas.explicanto.server.exceptions.PathToRootAboveException;
import de.beas.explicanto.template.Document;
import de.beas.explicanto.template.Element;
import de.beas.explicanto.template.Property;
import de.beas.explicanto.template.TemplateConstants;
import de.beas.explicanto.types.NewJAXBConverter;
import de.beas.explicanto.types.UidElem;
import de.beas.explicanto.types.WSAuthentication;
import de.beas.explicanto.types.WSCourse;
import de.beas.explicanto.types.WSCourseName;
import de.beas.explicanto.types.WSCustomer;
import de.beas.explicanto.types.WSLesson;
import de.beas.explicanto.types.WSPage;
import de.beas.explicanto.types.WSPageBlob;
import de.beas.explicanto.types.WSPageHead;
import de.beas.explicanto.types.WSPageType;
import de.beas.explicanto.types.WSProject;
import de.beas.explicanto.types.WSResponse;
import de.beas.explicanto.types.WSRoleElem;
import de.beas.explicanto.types.WSRoleType;
import de.beas.explicanto.types.WSScreenPlayObject;
import de.beas.explicanto.types.WSScreenplay;
import de.beas.explicanto.types.WSScreenplayHeader;
import de.beas.explicanto.types.WSTypeBase;
import de.beas.explicanto.types.WSUnit;
import de.beas.explicanto.types.WSUnitItem;
import de.beas.explicanto.types.WSUnitPattern;
import de.beas.explicanto.types.WSUser;

/**
 * @author AlexanderS Singleton class
 */
public class TreeManager {

    public static final int POS_LAST = Integer.MAX_VALUE;

    public static final String IRRELEVANT_STR = "";

    public static final boolean IRRELEVANT_BOOL = false;

    public static final boolean MASTERCOURSE = true;

    public static final boolean COURSE = false;

    public static final boolean STORE_UNLOCK = true;

    public static final boolean UNLOCK = false;

    private static final String APPLET_EXPORT_FORMAT = "java";

    private static final int AUDIO_TYPE = 0;

    private static final int IMAGE_TYPE = 1;

    private static final int SCREENPLAY_TYPE = 2;

    private static final int SCENES_TYPE = 3;

    public Explicanto explicanto;

    private ExplicantoServerProperties properties;

    private Properties defaultNodeNames;

    private Persistency persistency;

    private CourseExtractor courseGen;

    private CourseTransformator crsTrans;

    private MediaUsgThread mediaUsgThread = null;

    private StatusProp statusPropagator;

    private NotificationManager notify;

    public ErrorHandler errHandler;

    private Logger logger;

    public TreeManager(Explicanto ex, ExplicantoServerProperties props, Persistency pers, MediaUsgThread mut, NotificationManager nm) throws Exception {
        logger = Logger.getLogger(this.getClass());
        errHandler = new ErrorHandler(logger);
        explicanto = ex;
        properties = props;
        persistency = pers;
        mediaUsgThread = mut;
        notify = nm;
        defaultNodeNames = new Properties();
        defaultNodeNames.load(this.getClass().getClassLoader().getResourceAsStream("defaultNodeNames.properties"));
        statusPropagator = new StatusProp(persistency, getProps().roleCount, getProps().numOfStati);
        courseGen = new CourseExtractor(this, persistency);
        File courseTransContext = new File(Explicanto.courseTransContextDir);
        crsTrans = new CourseTransformator(courseTransContext);
        logger.info("startup: CourseTransformator (version '" + crsTrans.getVersion() + "') initialized");
        logger.info("startup: CourseTransformator context dir is '" + courseTransContext.toString() + "'");
        logger.info("startup: TreeManager initialized");
    }

    /**
	 * Top level method (accessible by webservice). This is the only method
	 * accessible without authentication checking. Remove all information from
	 * the users except id and name;
	 */
    public WSResponse loadLoginUsers() throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            logger.debug("loadLoginUsers");
            List erg = persistency.loadUsersNoSecrets();
            if (erg.size() == 0) {
                resp.error(1000, "loadLoginUsers: no users in database", erg);
                logger.warn("loadLoginUsers: no users in database");
            } else {
                WSUser.hideAllInfos(erg);
                resp.success(erg);
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "loadLoginUsers: ");
        }
        return resp;
    }

    public WSResponse loadLanguages() throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            logger.debug("loadLanguages");
            List erg = persistency.loadLanguages();
            if (erg.size() == 0) {
                resp.error(1000, "loadLanguages: no languages in database", erg);
                logger.warn("loadLoginUsers: no languages in database");
            } else {
                resp.success(erg);
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "loadLanguages: ");
        }
        return resp;
    }

    /**
	 * Similar to verifyUserRoles but change role string and display message in
	 * logger.debug
	 */
    private void correctUserListRoles(List ulist) {
        for (Iterator iter = ulist.iterator(); iter.hasNext(); ) correctUserRoles((WSUser) iter.next());
    }

    private void correctUserRoles(WSUser usr) {
        String[] res = RolesListValidator.validateRoles(usr.getAssignedRoles(), getProps().roleCount);
        if (res[RolesListValidator.RESULT_CODE].compareTo("OK") != 0) {
            logger.error("error while parsing roles of user \"" + usr.getUsername() + "\" (uid=" + usr.getUid() + "):\n" + res[RolesListValidator.RESULT_CODE]);
            usr.setAssignedRoles(res[RolesListValidator.ROLE_STRING]);
            logger.error("    will use roles description\"" + usr.getAssignedRoles() + "\"");
        }
    }

    public WSResponse loadConnectedUsers(WSAuthentication auth) throws RemoteException, Exception {
        logger.debug("getConnectedUsers: " + usr(auth));
        WSResponse resp = new WSResponse();
        List users, connectedUsers = new ArrayList();
        try {
            checkAuthentication(auth);
            users = persistency.loadUsersNoSecrets();
            for (Iterator iter = users.iterator(); iter.hasNext(); ) {
                WSUser usr = (WSUser) iter.next();
                if (!usr.noWSgetAuthentication().equals("")) connectedUsers.add(usr.hideSecretInfos());
            }
            resp.success(connectedUsers);
        } catch (Exception e) {
            errHandler.handleError(e, resp, "getConnectedUsers: ");
        }
        return resp;
    }

    public WSResponse updateUserRoles(WSAuthentication auth, List users) throws RemoteException, Exception {
        logger.debug("updateUserRoles: " + usr(auth));
        WSResponse resp = new WSResponse();
        try {
            checkAuthentication(auth);
            persistency.updateUserRoles(users);
            resp.success();
        } catch (Exception e) {
            errHandler.handleError(e, resp, "updateUserRoles: ");
        }
        return resp;
    }

    private static DateFormat formatter = null;

    /**
	 * Top level method (accessible by webservice)
	 */
    public WSResponse login(WSUser usr, String address) throws RemoteException, Exception {
        if (formatter == null) {
            formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
        }
        String usrName;
        WSUser dbUsr = null;
        WSResponse resp = new WSResponse();
        try {
            if (usr != null) logger.debug("login user '" + usr.getUsername() + "' " + uid(usr) + " " + (usr.getAdmin() != 0 ? " as admin" : "") + " from address: " + address); else logger.debug("login user '<null>' " + uid(usr));
            checkParam("usr", usr);
            synchronized (this) {
                dbUsr = persistency.loadUserWithSecrets(usr.getUid());
                if (dbUsr != null) {
                    if (Explicanto.TESTMODE || dbUsr.getPassword().equals(usr.getPassword())) {
                        if (usr.getAdmin() != 0 && dbUsr.getAdmin() == 0) {
                            resp.error(XPCConst.E_USR_IS_NO_ADMIN, "login: user is no admin", usr.getUsername(), null);
                            logger.debug("login denied. user '" + usr.getUsername() + "' " + uid(usr) + " is no admin");
                        } else {
                            String authId = dbUsr.noWSgetAuthentication();
                            String encAddr = XPCUtil.encodeAddress(address);
                            logger.debug("Authid: " + authId);
                            logger.debug("encoded address: " + encAddr);
                            if (!authId.equals("")) {
                                logger.debug("authId not empty, checking multiple logins");
                                if (authId.lastIndexOf("_") > 0) {
                                    String origAddr = authId.substring(authId.indexOf("_") + 1);
                                    if (origAddr.indexOf("_admin") > 0) origAddr = origAddr.substring(0, origAddr.indexOf("_admin"));
                                    logger.debug("original address: " + origAddr);
                                    if (!origAddr.equals(encAddr)) {
                                        logger.debug("new address is different from the original address, checking login times");
                                        String lastLogon = dbUsr.getLastLogon();
                                        logger.debug("lastLogon: " + lastLogon);
                                        if (lastLogon != null && lastLogon.length() > 0) {
                                            logger.debug("compare login data");
                                            Date lastDate = new Date();
                                            try {
                                                lastDate = formatter.parse(lastLogon);
                                                logger.debug("parsed first solution");
                                            } catch (Exception e) {
                                                logger.debug("failed first parsing, trying second solution");
                                                logger.debug(e);
                                            }
                                            logger.debug("last logon data: " + lastDate);
                                            long elapsed = XPCUtil.getCurrentGMTTimestamp().getTime() - lastDate.getTime();
                                            logger.debug("elapsed: " + elapsed);
                                            if (elapsed < 60 * 60 * 1000) {
                                                logger.debug("elapsed is too small, access denied!");
                                                resp.error(XPCConst.E_DUPLICATE_LOGIN, "login: user already logged from another workstation");
                                                logger.warn("login: user '" + usr.getUsername() + "' " + uid(usr) + " already logged in from another workstation");
                                                return resp;
                                            }
                                            logger.debug("allow override!");
                                        }
                                    }
                                }
                                logger.debug("generate login warning");
                                resp.setResultObj2((!dbUsr.getLastLogon().equals("") ? dbUsr.getLastLogon() : "<unbekannt>"));
                                logger.warn("login: user '" + usr.getUsername() + "' " + uid(usr) + " already logged in");
                            }
                            WSAuthentication auth = new WSAuthentication(dbUsr.getUid(), XPCUtil.generateRandomString());
                            dbUsr.noWSsetAuthentication(auth.getAuthentification() + "_" + encAddr + (usr.getAdmin() != 0 ? "_admin" : ""));
                            dbUsr.setLastLogon(formatter.format(XPCUtil.getCurrentGMTTimestamp()));
                            logger.debug("set login mark: " + auth.getAuthentification() + ", " + encAddr + ", " + dbUsr.getLastLogon());
                            persistency.updateElem(dbUsr);
                            List lockedNodes = persistency.getAllLocked(dbUsr);
                            for (Iterator it = lockedNodes.iterator(); it.hasNext(); ) {
                                List lockedPath = persistency.getPathToRoot((WSTypeBase) it.next());
                                pathUnlockInMemory(lockedPath);
                                persistency.storePathToRoot(lockedPath);
                            }
                            logger.debug("login: unlocked " + lockedNodes.size() + " elements for user '" + usr.getUsername() + "' " + uid(usr));
                            resp.success(auth);
                        }
                    } else {
                        resp.error(XPCConst.E_WRONG_PASSWD, "login: password incorrect");
                        logger.debug("login: password incorrect");
                    }
                }
            }
        } catch (Exception e) {
            logger.debug(e);
            errHandler.handleError(e, resp, "login: ");
        }
        return resp;
    }

    /**
	 * Top level method (accessible by webservice)
	 */
    public WSResponse logoff(WSAuthentication auth) throws RemoteException, Exception {
        WSUser dbUsr = null;
        WSResponse resp = new WSResponse();
        try {
            logger.debug("logoff: " + usr(auth));
            checkAuthentication(auth);
            try {
                synchronized (this) {
                    dbUsr = persistency.loadUserWithSecrets(auth.getUserID());
                    dbUsr.noWSsetAuthentication("");
                    dbUsr.setLastLogon("");
                    persistency.updateElem(dbUsr);
                }
                resp = loadLoginUsers();
            } catch (ObjNotFoundException onf) {
                resp.error(XPCConst.E_NO_SUCH_USR, "logoff: no such user", "user ID=" + auth.getUserID(), null);
                logger.error("logoff: no such user ID = " + auth.getUserID() + " : " + resp.toString());
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "logoff: ");
        }
        return resp;
    }

    public WSResponse coursePreview(WSAuthentication auth, long prID, long cnID, long cID, String fmt, boolean zip, boolean climpImport) throws RemoteException, Exception {
        return preview(auth, prID, cnID, cID, -1, fmt, zip, climpImport);
    }

    public WSResponse unitPreview(WSAuthentication auth, long prID, long cnID, long cID, long uID, String fmt) throws RemoteException, Exception {
        return preview(auth, prID, cnID, cID, uID, fmt, false, false);
    }

    /**
	 * @return preview url/response from climp and a CR separated list of
	 *         potential replacements key/value
	 */
    private WSResponse preview(WSAuthentication auth, long prID, long cnID, long cID, long uID, String fmt, boolean zip, boolean climpImport) throws RemoteException, Exception {
        boolean javaFormat = APPLET_EXPORT_FORMAT.equals(fmt);
        if (javaFormat) fmt = "html";
        WSResponse resp = new WSResponse();
        boolean fmtChanged = false;
        climpImport = uID == -1 && zip && climpImport;
        String resultURL, replacedFiles = "";
        String genDir, genSubDir;
        String tgtDir, tgtSubDir;
        try {
            logger.debug("export: " + usr(auth) + " prID=" + prID + " cnID=" + cnID + " cID=" + cID + (uID == -1 ? "" : " uID=" + uID) + " fmt='" + fmt + "' zip=" + zip);
            checkParam("format", fmt);
            checkAuthentication(auth);
            if (climpImport) {
                Boolean climpEnabled = explicanto.settingsMgr.getValBool(CLIMPImporter.C_ENABLED, false);
                if (!climpEnabled.booleanValue()) {
                    logger.error("export: tried a CLIMP import although setting 'climp_enabled' doesn't evaluate to true");
                    resp.error(XPCConst.E_CLIMP_NOT_ENABLED, "CLIMP import is not enabled");
                    return resp;
                } else if (!CLIMPImporter.climpAvaliable()) {
                    logger.error("export: tried a CLIMP import, " + "but climp is unavailable. ");
                    resp.error(XPCConst.E_CLIMP_NOT_AVAILABLE, "CLIMP library not available");
                    return resp;
                }
            }
            WSCourse crs = (WSCourse) persistency.loadTree(cID, WSCourse.class, WSUnit.class);
            resp = hasLessonAndUnit(crs);
            if (!resp.isSuccess()) {
                logger.debug("export: " + resp.toString());
                return resp;
            }
            List path2Root = persistency.getPathToRoot(uID == -1 ? cID : uID, uID == -1 ? WSCourse.class : WSUnit.class);
            genDir = courseGen.createTargetDir(crsTrans.getCourseSrcDir().toString(), crs);
            logger.debug("export: gen dir created: \"" + genDir + "\"");
            genSubDir = genDir.substring(genDir.lastIndexOf("/") + 1);
            if (climpImport) tgtDir = courseGen.createTargetDir(crsTrans.getCourseSrcDir().toString(), crs); else tgtDir = courseGen.createTargetDir(getProps().getPath("course_target_location"), crs);
            logger.debug("export: trans dir created: \"" + tgtDir + "\"");
            tgtSubDir = tgtDir.substring(tgtDir.lastIndexOf("/") + 1);
            if (!XPCUtil.isInStrArray(crsTrans.getSupportedFormats(), fmt)) {
                logger.warn("export: export format '" + fmt + "' not supported, using default format '" + crsTrans.getDefaultFormat() + "'");
                fmt = crsTrans.getDefaultFormat();
                fmtChanged = true;
            }
            resp = courseGen.generateCourses(auth, genDir, cID, fmt);
            if (resp.isSuccess()) {
                replacedFiles = (String) resp.getResultObj2();
                if (uID == -1) {
                    logger.debug("export: calling transformCourse('" + fmt + "'," + (zip ? "zip" : "nozip") + ",'" + tgtDir + "','" + genSubDir + "'");
                    resultURL = crsTrans.transformCourse(fmt, zip, new File(tgtDir), genSubDir);
                } else {
                    String unitID = "U_" + XPCUtil.d5(XPCUtil.getUnitPosInCourse((WSCourse) resp.getResultObj(), uID));
                    logger.debug("export: calling transformUnitById('" + unitID + "', '" + fmt + "', '" + tgtDir + "','" + genSubDir + "'");
                    resultURL = crsTrans.transformUnitById(unitID, fmt, new File(tgtDir), genSubDir);
                }
                logger.info("export: resulting URL part is: '" + getProps().getProperty("preview_directory") + "/" + tgtSubDir + "/" + resultURL + "\"");
                if (resultURL != null) {
                    String remarks = (!fmtChanged ? "" : XPCConst.FORMATCHANGE + "/" + fmt + "\n") + replacedFiles;
                    if (climpImport) {
                        WSResponse clResp = new CLIMPImporter(this, explicanto.settingsMgr).climpImport(tgtDir + "/" + resultURL);
                        if (!clResp.isSuccess()) return clResp;
                        resp.success("CLIMP:\n" + clResp.getResultObj(), remarks);
                    } else {
                        resp.success(getProps().getProperty("preview_directory") + "/" + tgtSubDir + "/" + resultURL, remarks);
                        if (javaFormat && zip) {
                            String zipDir = tgtDir + "/coursepackage_html.zip";
                            String tempDir = explicanto.getExplicantoHomeDirFromDB() + "/ziptmp/";
                            Unzipper.unzip(zipDir, tempDir);
                            copy(XPCUtil.readClasspathInputStream("jrex.jar"), tempDir + "compound-html/jrex.jar");
                            copy(XPCUtil.readClasspathInputStream("rexapplet3.jar"), tempDir + "compound-html/rexapplet3.jar");
                            copy(new FileInputStream(new File(tempDir + "/compound-html/courseobject_index.html")), tempDir + "compound-html/courseobject_orig_index.html");
                            copy(XPCUtil.readClasspathInputStream("newindex.html"), tempDir + "compound-html/courseobject_index.html");
                            Zipper.zip(zipDir, tempDir);
                        }
                        if (javaFormat && !zip) {
                            copy(XPCUtil.readClasspathInputStream("jrex.jar"), tgtDir + "/html/compound-html/jrex.jar");
                            copy(XPCUtil.readClasspathInputStream("rexapplet3.jar"), tgtDir + "/html/compound-html/rexapplet3.jar");
                            copy(new FileInputStream(new File(tgtDir + "/html/compound-html/courseobject_index.html")), tgtDir + "/html/compound-html/courseobject_orig_index.html");
                            copy(XPCUtil.readClasspathInputStream("newindex.html"), tgtDir + "/html/compound-html/courseobject_index.html");
                        }
                    }
                } else throw new Exception("transformCourse() returns null");
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "unit/course preview: ");
        }
        return resp;
    }

    private void copy(InputStream fis, String outFile) throws Exception {
        FileOutputStream fos = new FileOutputStream(new File(outFile));
        byte[] data = new byte[100000];
        int read = fis.read(data);
        while (read > 0) {
            fos.write(data, 0, read);
            read = fis.read(data);
        }
        fos.close();
        fis.close();
    }

    private WSResponse hasLessonAndUnit(WSCourse c) {
        boolean hasLesson = false;
        if (c.getChildren() != null) for (Iterator iter = c.getChildren().iterator(); iter.hasNext(); ) {
            hasLesson = true;
            WSLesson les = (WSLesson) iter.next();
            if (les.getChildren() != null) for (Iterator it = les.getChildren().iterator(); it.hasNext(); ) return new WSResponse().success();
        }
        if (hasLesson) return new WSResponse().error(XPCConst.E_NO_UNIT_IN_COURSE, "cannot export course id=" + c.getUid() + ": no units"); else return new WSResponse().error(XPCConst.E_NO_LESSON_IN_COURSE, "cannot export course id=" + c.getUid() + ": no lessons");
    }

    private WSResponse lockElementNoLog(WSAuthentication auth, Class nodeClass, long uid) {
        return lockElement(auth, nodeClass, uid, false);
    }

    public WSResponse getRightsForElement(WSAuthentication auth, long uid, boolean log) {
        WSResponse resp = new WSResponse();
        try {
            if (log) logger.debug("check rights for element " + usr(auth) + " all classes uid=" + uid);
            checkAuthentication(auth);
            synchronized (this) {
                checkRightsForElement(auth, uid);
                resp.success();
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "checkRightsForElement: ");
        }
        return resp;
    }

    public void checkRightsForElement(WSAuthentication auth, long uid) throws Exception {
        Object obj = null;
        boolean found = false;
        Class nodeClass = null;
        List path2Root = null;
        if (!found) {
            try {
                obj = persistency.loadElem(WSCustomer.class, uid);
                checkForAdminRole(auth);
                return;
            } catch (ObjectNotFoundException e) {
            }
        }
        if (!found) {
            try {
                obj = persistency.loadElem(WSProject.class, uid);
                found = true;
                nodeClass = obj.getClass();
            } catch (ObjectNotFoundException e) {
            }
        }
        if (!found) {
            try {
                obj = persistency.loadElem(WSCourse.class, uid);
                found = true;
                nodeClass = obj.getClass();
            } catch (ObjectNotFoundException e) {
            }
        }
        if (!found) {
            try {
                obj = persistency.loadElem(WSLesson.class, uid);
                found = true;
                nodeClass = obj.getClass();
            } catch (ObjectNotFoundException e) {
            }
        }
        if (!found) {
            try {
                obj = persistency.loadElem(WSUnit.class, uid);
                found = true;
                nodeClass = obj.getClass();
            } catch (ObjectNotFoundException e) {
            }
        }
        if (!found) {
            obj = persistency.loadElem(WSPageBlob.class, uid);
            found = true;
            path2Root = persistency.getPathToRoot(uid, obj.getClass());
            path2Root.remove(0);
            path2Root.remove(0);
            checkEntitlement(auth, path2Root);
            return;
        }
        path2Root = persistency.getPathToRoot(uid, nodeClass);
        checkEntitlement(auth, path2Root);
    }

    public WSResponse getRightsForElement(WSAuthentication auth, Class nodeClass, long uid, boolean log) {
        WSResponse resp = new WSResponse();
        try {
            if (log) logger.debug("check rights for element " + usr(auth) + " " + clz(nodeClass) + " uid=" + uid);
            checkParam("nodeClass", nodeClass);
            checkAuthentication(auth);
            synchronized (this) {
                checkRightsForElement(auth, nodeClass, uid);
            }
            resp.success();
        } catch (Exception e) {
            errHandler.handleError(e, resp, "checkRightsForElement: ");
        }
        return resp;
    }

    public void checkRightsForElement(WSAuthentication auth, Class nodeClass, long uid) throws Exception {
        if (nodeClass == WSCustomer.class) checkForAdminRole(auth);
        List path2Root = persistency.getPathToRoot(uid, nodeClass);
        checkEntitlement(auth, path2Root);
    }

    /**
	 * Top level method (accessible by webservice)
	 */
    public WSResponse lockElement(WSAuthentication auth, Class nodeClass, long uid, boolean log) {
        WSTypeBase retObj = null;
        WSResponse lockResp, resp = new WSResponse();
        try {
            if (log) logger.debug("lockElement: " + usr(auth) + " " + clz(nodeClass) + " uid=" + uid);
            checkParam("nodeClass", nodeClass);
            checkAuthentication(auth);
            if (nodeClass == WSCustomer.class) checkForAdminRole(auth);
            synchronized (this) {
                List path2Root = persistency.getPathToRoot(uid, nodeClass);
                checkEntitlement(auth, path2Root);
                if (nodeClass == WSUnit.class) {
                    WSUnit temp = (WSUnit) path2Root.get(0);
                    path2Root.clear();
                    path2Root.add(temp);
                }
                lockResp = isUnlocked(path2Root, auth);
                if (lockResp.isSuccess()) {
                    pathLockInMemory(auth, path2Root);
                    persistency.storePathToRoot(path2Root);
                    resp.success();
                    if (nodeClass == WSUnit.class) retObj = persistency.loadTree(((WSTypeBase) path2Root.get(0)).getUid(), WSUnit.class, WSPageHead.class); else retObj = persistency.loadTree(((WSTypeBase) path2Root.get(0)).getUid(), nodeClass, XPCUtil.childClass(nodeClass));
                    if (retObj instanceof WSCustomer) prepareCustomerForSending((WSCustomer) retObj);
                    resp.setResultObj(retObj);
                } else resp = lockResp;
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "lockElement: ");
        }
        return resp;
    }

    /**
	 * Diese Methode liefert einen E_NO_PATH_TO_ROOT_ABOVE-Fehler anstelle eines
	 * E_NO_PATH_TO_ROOT-Fehlers. Dies ergibt korrektere Fehlermeldungen, wenn
	 * zum Einf�gen ein �bergeordnetes Element gesperrt wird.
	 */
    private WSResponse lockAsParentElement(WSAuthentication auth, Class nodeClass, long uid) {
        WSResponse resp = lockElementNoLog(auth, nodeClass, uid);
        if (resp.getErrorCode() == XPCConst.E_NO_PATH_TO_ROOT && XPCUtil.clazzName(nodeClass).equals((String) resp.getResultObj())) {
            WSResponse resp2 = new WSResponse();
            errHandler.handleError(new PathToRootAboveException(new ObjNotFoundException(uid, nodeClass), uid, nodeClass), resp2, "lockAsParentElement: ");
            return resp2;
        } else return resp;
    }

    /**
	 * The entitlement to unlock an object must be checked (with lockedByAuth())
	 * before calling this method.
	 * 
	 * @throws Exception
	 */
    private void unlockElement(WSAuthentication auth, Class nodeClass, long uid) throws Exception {
        try {
            checkParam("nodeClass", nodeClass);
            checkAuthentication(auth);
            List path2Root = persistency.getPathToRoot(uid, nodeClass);
            WSTypeBase nd = (WSTypeBase) path2Root.get(0);
            if (nodeClass == WSUnit.class) {
                path2Root.clear();
                path2Root.add(nd);
            }
            if (nd.getLockUserID() == auth.getUserID() || nd.getLockUserID() == WSTypeBase.NOLOCKUSERID) {
                pathUnlockInMemory(path2Root);
                persistency.storePathToRoot(path2Root);
            } else logger.error("unlockElement requested with foreign lock: " + auth.getUserID() + ", lock in db is " + nd.getLockUserID());
        } catch (Exception e) {
            throw new Exception("ERROR in unlockElement", e);
        }
    }

    /**
	 * @param path2Root
	 * @return If an element of the path is locked by someone else (not by auth)
	 *         this element is returned in the response.
	 */
    private WSResponse isUnlocked(List path2Root, WSAuthentication auth) {
        WSResponse resp = new WSResponse();
        String node2Lock = XPCUtil.clazzName(path2Root.get(0)) + " " + uid((WSTypeBase) path2Root.get(0));
        for (int i = path2Root.size() - 1; i >= 0; i--) {
            WSTypeBase nd = (WSTypeBase) path2Root.get(i);
            if (nd.getLockUserID() != WSTypeBase.NOLOCKUSERID && nd.getLockUserID() != auth.getUserID()) {
                try {
                    WSUser lckr = persistency.loadUserNoSecrets(nd.getLockUserID());
                    String msg = usr(auth) + " cannot lock " + node2Lock + ". " + XPCUtil.clazzName(nd) + " " + uid(nd) + " locked by \"" + lckr.getUsername() + "\" (" + nd.getLockUserID() + ") since " + nd.getLockTime();
                    resp.error(XPCConst.E_ELEM_LOCKED_BY_USERNAME, msg, nd.obfuscate(), lckr);
                    logger.debug("  lockElement: " + msg);
                } catch (Exception e) {
                    String msg = usr(auth) + " cannot lock " + node2Lock + ". " + XPCUtil.clazzName(nd) + " " + uid(nd) + " locked by " + nd.getLockUserID() + " since " + nd.getLockTime();
                    resp.error(XPCConst.E_ELEM_LOCKED_BY_USERID, msg, nd.obfuscate(), new Long(nd.getLockUserID()));
                    logger.debug("  lockElement: " + msg);
                }
                return resp;
            }
        }
        WSTypeBase elem = (WSTypeBase) path2Root.get(0);
        if (elem.getLockCount() != WSTypeBase.NOLOCKCOUNT) {
            String msg = usr(auth) + " cannot lock " + node2Lock + ". An element of its subtree is locked.";
            resp.error(XPCConst.E_ELEM_LOCKED_BELOW, msg, elem.obfuscate(), null);
            logger.error("  lockElement: " + msg);
            return resp;
        }
        resp.success();
        return resp;
    }

    /**
	 * Sets lockID/lockTime of lowest node. if this node isn't already locked by
	 * auth, increment lockCount for each node from parent to root by 1. this
	 * behavior allows multiple locks with a single unlock.
	 */
    private void pathLockInMemory(WSAuthentication auth, List path2Root) {
        WSTypeBase elem = (WSTypeBase) path2Root.get(0);
        elem.setLockTime(XPCUtil.getCurrentGMTTimestamp().toString());
        if (elem.getLockUserID() == WSTypeBase.NOLOCKUSERID) {
            elem.setLockUserID(auth.getUserID());
            for (int i = 1; i < path2Root.size(); i++) {
                WSTypeBase nd = (WSTypeBase) path2Root.get(i);
                nd.setLockCount(nd.getLockCount() + 1);
            }
        } else if (elem.getLockUserID() != auth.getUserID()) {
            logger.error("locking error: tried to lock " + cls(elem) + " " + uid(elem) + " but is locked by " + elem.getLockUserID());
        }
    }

    /**
	 * Resets lockID/lockTime of lowest node and decrement lockCount for each
	 * node from parent to root by 1.
	 */
    private void pathUnlockInMemory(List path2Root) {
        if (path2Root != null && path2Root.size() > 0) {
            ((WSTypeBase) path2Root.get(0)).setLockUserID(WSTypeBase.NOLOCKUSERID);
            ((WSTypeBase) path2Root.get(0)).setLockTime(WSTypeBase.NOLOCKTIME);
            for (int i = 1; i < path2Root.size(); i++) {
                WSTypeBase nd = (WSTypeBase) path2Root.get(i);
                nd.setLockCount(Math.max(nd.getLockCount() - 1, 0));
            }
        }
    }

    /**
	 * Top level method (accessible by webservice)
	 */
    public WSResponse loadCustomer(WSAuthentication auth) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        WSCustomer cust;
        try {
            logger.debug("loadCustomer: " + usr(auth));
            checkAuthentication(auth);
            synchronized (this) {
                cust = (WSCustomer) persistency.loadTree(0, WSCustomer.class, WSCourse.class);
            }
            prepareCustomerForSending(cust);
            resp.success(cust);
        } catch (Exception e) {
            errHandler.handleError(e, resp, "loadCustomer: ");
        }
        return resp;
    }

    public WSTypeBase loadTreeForNode(WSAuthentication auth, WSTypeBase node) throws RemoteException, Exception {
        WSTypeBase parent = null;
        logger.debug("load tree: " + usr(auth));
        checkAuthentication(auth);
        synchronized (this) {
            if (node instanceof WSProject) parent = persistency.loadTree(node.getUid(), WSProject.class, WSUnit.class); else if (node instanceof WSCourseName) parent = persistency.loadTree(node.getUid(), WSCourseName.class, WSUnit.class); else if (node instanceof WSCourse) parent = persistency.loadTree(node.getUid(), WSCourse.class, WSUnit.class); else if (node instanceof WSLesson) parent = persistency.loadTree(node.getUid(), WSLesson.class, WSUnit.class); else if (node instanceof WSUnit) parent = persistency.loadTree(node.getUid(), WSUnit.class, WSUnit.class);
        }
        return parent;
    }

    /**
	 * Additional things before customer is sent out
	 */
    private void prepareCustomerForSending(WSCustomer c) throws Exception {
        c.removeUnsupportedSkins();
        correctUserListRoles(c.users);
        c.setRoletypes(XPCUtil.getRoleTypesFromProps(getProps()));
        Boolean climp = explicanto.settingsMgr.getValBool(CLIMPImporter.C_ENABLED, false);
        c.setClimpExportEnabled(climp.booleanValue());
    }

    /**
	 * Top level method (accessible by webservice)
	 */
    public WSResponse loadLessons(WSAuthentication auth, long courseUID) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            logger.debug("loadLessons: " + usr(auth) + " courseUID=" + courseUID);
            checkAuthentication(auth);
            synchronized (this) {
                persistency.getPathToRoot(courseUID, WSCourse.class);
                resp.success(persistency.loadLessonsToPage(courseUID));
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "loadLessons: ");
        }
        return resp;
    }

    /**
	 * Top level method (accessible by webservice)
	 */
    public WSResponse loadPage(WSAuthentication auth, long pageUID) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            logger.debug("loadPage: " + usr(auth) + " pageUID=" + pageUID);
            checkAuthentication(auth);
            WSPage erg = persistency.loadPageToComponent(pageUID);
            resp.success(erg);
        } catch (Exception e) {
            errHandler.handleError(e, resp, "loadPage: ");
        }
        return resp;
    }

    public WSResponse loadRolePath(WSAuthentication auth, long uid, Class clazz) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            logger.debug("reloadRolePath: " + usr(auth) + " " + clz(clazz));
            checkParam("clazz", clazz);
            if (clazz != WSCustomer.class && clazz != WSProject.class && clazz != WSCourseName.class && clazz != WSCourse.class && clazz != WSLesson.class && clazz != WSUnit.class) throw new ParamException("cannot reloadRolePath for class " + XPCUtil.claZZName(clazz));
            checkAuthentication(auth);
            List erg = new ArrayList();
            synchronized (this) {
                if (clazz != WSCustomer.class) for (Iterator iter = persistency.getPathToProject(uid, clazz).iterator(); iter.hasNext(); ) erg.add(XPCUtil.getRoles((WSTypeBase) iter.next()));
                erg.add(persistency.loadUsersNoSecrets());
            }
            resp.success(erg);
        } catch (Exception e) {
            errHandler.handleError(e, resp, "loadRolePath: ");
        }
        return resp;
    }

    public WSResponse storeUnlockElement(WSAuthentication auth, WSTypeBase elem, boolean store) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            logger.debug((store ? "storeUnlockElement: " : "unlockElement: ") + usr(auth) + " " + cls(elem) + " " + uid(elem));
            checkParam("elem", elem);
            checkAuthentication(auth);
            WSResponse lockedResp = lockedByAuth(auth, elem);
            if (!lockedResp.isSuccess()) return lockedResp;
            notifyRoles(auth, elem);
            synchronized (this) {
                if (store) {
                    WSResponse statusChangeCheck = checkStatusChange(elem, auth.getUserID());
                    if (!statusChangeCheck.isSuccess()) {
                        unlockElement(auth, elem.getClass(), elem.getUid());
                        return statusChangeCheck;
                    } else {
                        if (elem instanceof WSUnit) {
                            persistency.storeReplaceTree(elem);
                            resp.setResultObj2(((WSUnit) elem).saveSpace());
                            mediaUsgThread.recalcMediaUsage();
                        } else {
                            if (elem instanceof WSCourse || elem instanceof WSProject) mediaUsgThread.recalcMediaUsage();
                            if (elem instanceof WSCustomer) {
                                persistency.updateUserRoles(((WSCustomer) elem).users);
                                WSCustomer dbCust = persistency.loadCustomer();
                                ((WSCustomer) elem).setExportFormats(dbCust.getExportFormats());
                                List unsuppSkins = WSCustomer.getUnsupportedSkins(dbCust.getSkins(), dbCust.getExportFormats());
                                ((WSCustomer) elem).removeUnsupportedSkins();
                                ((WSCustomer) elem).getSkins().addAll(unsuppSkins);
                            }
                            persistency.storeElemRearrangeChildren(elem);
                            if (elem instanceof WSCustomer) resp.setResultObj2(((WSCustomer) elem).saveSpace());
                        }
                        statusPropagator.propagateStatusDown(elem);
                        String statusProp = statusPropagator.propagateStatusUpwards(elem);
                        resp.setResultObj(statusProp);
                    }
                }
                unlockElement(auth, elem.getClass(), elem.getUid());
            }
            resp.success();
        } catch (Exception e) {
            errHandler.handleError(e, resp, store ? "storeUnlockElement: " : "unlockElement: ");
        }
        return resp;
    }

    public WSResponse storeElement(WSAuthentication auth, WSTypeBase elem) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            logger.debug("storeElement: " + usr(auth) + " " + cls(elem) + " " + uid(elem));
            checkParam("elem", elem);
            checkAuthentication(auth);
            if (!(elem instanceof WSPage)) notifyRoles(auth, elem); else {
                logger.debug("WSPage in storeElement, notifyRoles bypassed!");
            }
            WSUnit unit = null;
            if (!(elem instanceof WSPage)) {
                WSResponse lockedResp = lockedByAuth(auth, elem);
                if (!lockedResp.isSuccess()) return lockedResp;
            } else {
                WSPageBlob pgBlob = NewJAXBConverter.convertToPageBlob((WSPage) elem);
                persistency.loadServerInternalAttributes(pgBlob);
                long unitItemUID = pgBlob.noWSgetParentUID();
                logger.debug("elem.parentUid : " + unitItemUID);
                WSUnitItem item = (WSUnitItem) persistency.loadElem(WSUnitItem.class, unitItemUID);
                long unitUID = item.noWSgetParentUID();
                logger.debug("unititem parent uid : " + unitUID);
                unit = (WSUnit) persistency.loadElem(WSUnit.class, unitUID);
                WSResponse lockedResp = lockedByAuth(auth, unit);
                if (!lockedResp.isSuccess()) return lockedResp;
            }
            logger.debug("not locked");
            synchronized (this) {
                WSResponse statusChangeCheck;
                if (!(elem instanceof WSPage)) statusChangeCheck = checkStatusChange(elem, auth.getUserID()); else {
                    statusChangeCheck = new WSResponse().success();
                }
                if (!statusChangeCheck.isSuccess()) {
                    logger.debug("statuschange failed");
                    unlockElement(auth, elem.getClass(), elem.getUid());
                    return statusChangeCheck;
                } else {
                    logger.debug("statuschange succedeed");
                    if (elem instanceof WSUnit) {
                        logger.debug("elem instanceof WSUnit");
                        persistency.storeReplaceTree(elem);
                        resp.setResultObj2(((WSUnit) elem).saveSpace());
                        mediaUsgThread.recalcMediaUsage();
                    } else {
                        logger.debug("elem not WSUnit");
                        if (elem instanceof WSCourse || elem instanceof WSProject) mediaUsgThread.recalcMediaUsage();
                        if (elem instanceof WSCustomer) {
                            persistency.updateUserRoles(((WSCustomer) elem).users);
                            WSCustomer dbCust = persistency.loadCustomer();
                            ((WSCustomer) elem).setExportFormats(dbCust.getExportFormats());
                            List unsuppSkins = WSCustomer.getUnsupportedSkins(dbCust.getSkins(), dbCust.getExportFormats());
                            ((WSCustomer) elem).removeUnsupportedSkins();
                            ((WSCustomer) elem).getSkins().addAll(unsuppSkins);
                        }
                        logger.debug("storeElement");
                        if (elem instanceof WSPage) {
                            logger.debug("calling persistency.storeReplacePage");
                            persistency.storeReplacePage((WSPage) elem);
                        } else persistency.storeElemRearrangeChildren(elem);
                        if (elem instanceof WSCustomer) resp.setResultObj2(((WSCustomer) elem).saveSpace());
                    }
                    if (!(elem instanceof WSPage)) {
                        statusPropagator.propagateStatusDown(elem);
                        String statusProp = statusPropagator.propagateStatusUpwards(elem);
                        resp.setResultObj(statusProp);
                    }
                }
            }
            resp.success();
        } catch (Exception e) {
            errHandler.handleError(e, resp, "storeElement: ");
        }
        return resp;
    }

    /**
	 * If any role a user is assigned to is configured as an extended role that
	 * user may change the stati of all other roles. Otherwise a user may change
	 * only stati of roles he is assigned to.
	 */
    private WSResponse checkStatusChange(WSTypeBase elem, long userID) throws Exception {
        int[] dbStatus = statusPropagator.statusField((WSTypeBase) persistency.loadElem(elem.getClass(), elem.getUid()));
        int[] elemStatus = statusPropagator.statusField(elem);
        List path2Root = persistency.getPathToRoot(elem);
        boolean[] roleSingleResp = new boolean[elemStatus.length];
        for (int i = 0; i < elemStatus.length; i++) {
            roleSingleResp[i] = 0 == isInRoleInSingleResp(userID, i + 1, path2Root);
            if (roleSingleResp[i] && XPCUtil.isExtendedRole(i + 1, getProps())) return new WSResponse().success();
        }
        for (int i = 0; i < elemStatus.length; i++) {
            if (elemStatus[i] != dbStatus[i] && !roleSingleResp[i]) return new WSResponse().error(XPCConst.E_CANT_CHANGE_FOREIGN_ROLE, "can't change foreign role " + (i + 1), (i + 1) + "");
        }
        return new WSResponse().success();
    }

    /**
	 * Returns OK if elem is locked for auth or isn't locked at all.
	 */
    private WSResponse lockedByAuth(WSAuthentication auth, WSTypeBase elem) throws Exception {
        if (auth == null) throw new AuthenticationException("authentification missing", "<null>");
        WSResponse resp = new WSResponse();
        WSTypeBase dbElem = (WSTypeBase) persistency.loadElem(elem.getClass(), elem.getUid());
        if (dbElem.getLockUserID() == auth.getUserID() || dbElem.getLockUserID() == WSTypeBase.NOLOCKUSERID) resp.success(); else {
            resp.error(XPCConst.E_ELEM_NOT_LOCKED_FOR_YOU, "element " + XPCUtil.clazzName(elem) + " (uid=" + elem.getUid() + ") isn't locked for userid " + auth.getUserID() + " but for userid " + dbElem.getLockUserID());
            resp.setResultObj(elem.obfuscate());
            logger.debug("lockedByAuth: " + resp.toString());
        }
        return resp;
    }

    /**
	 * @param origID
	 *            if != -1 the new element is a copy of the original element
	 *            with uid==origID
	 */
    public WSResponse addElementUnlock(WSAuthentication auth, WSTypeBase parent, boolean createMasterCrs, String unittype, int pos, long origID) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        WSTypeBase newElem = null;
        try {
            logger.debug("addElementInternal: " + usr(auth) + " parent " + cls(parent) + " " + uid(parent) + " createMastercourse=" + createMasterCrs + " unittype=" + unittype + " pos=" + pos);
            checkParam("parent", parent);
            checkParam("unittype", unittype);
            checkAuthentication(auth);
            synchronized (this) {
                if (origID == -1) resp = createNewElement(parent, createMasterCrs, unittype); else {
                    persistency.getPathToRoot(origID, XPCUtil.childClass(parent));
                    resp = copyElement(parent, origID);
                }
                if (!resp.isSuccess()) return resp;
                newElem = (WSTypeBase) resp.getResultObj();
                WSResponse lockResp = lockElementNoLog(auth, parent.getClass(), parent.getUid());
                if (lockResp.getErrorCode() != 0) {
                    resp = lockResp;
                    logger.error("addElementInternal: " + resp.toString());
                } else {
                    newElem.noWSsetParentUID(parent.getUid());
                    persistency.insertElemAt(newElem, pos);
                    persistency.storeReplaceTree(newElem);
                    mediaUsgThread.recalcMediaUsage();
                    String statusProp = statusPropagator.propagateStatusUpwards(newElem);
                    resp.setResultObj2(statusProp);
                    unlockElement(auth, parent.getClass(), parent.getUid());
                    pruneTree(newElem);
                    resp.success(newElem);
                }
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "addElementUnlock: ");
        }
        return resp;
    }

    /**
	 * Cut result tree according to stage loading of trees:
	 * customer,project,cname,course down to: course, lesson, unit down to: page
	 */
    private void pruneTree(WSTypeBase node) {
        if (node instanceof WSProject) {
            for (Iterator i1 = node.getChildren().iterator(); i1.hasNext(); ) for (Iterator i2 = ((WSCourseName) i1.next()).getChildren().iterator(); i2.hasNext(); ) ((WSCourse) i2.next()).setChildren(null);
        } else if (node instanceof WSCourseName) {
            for (Iterator i1 = node.getChildren().iterator(); i1.hasNext(); ) ((WSCourse) i1.next()).setChildren(null);
        } else if (node instanceof WSCourse) {
            ((WSCourse) node).setChildren(null);
        } else if (node instanceof WSLesson) {
            for (Iterator i1 = node.getChildren().iterator(); i1.hasNext(); ) for (Iterator i2 = ((WSUnit) i1.next()).getChildren().iterator(); i2.hasNext(); ) for (Iterator i3 = ((WSUnitItem) i2.next()).getChildren().iterator(); i3.hasNext(); ) ((WSPage) i3.next()).setChildren(null);
        } else if (node instanceof WSUnit) {
            for (Iterator i1 = node.getChildren().iterator(); i1.hasNext(); ) for (Iterator i2 = ((WSUnitItem) i1.next()).getChildren().iterator(); i2.hasNext(); ) ((WSPage) i2.next()).setChildren(null);
        } else logger.error("error in pruneResultTree(): type of node is " + node.getClass().toString());
    }

    /**
	 * Create a new element including all attributes according to the given
	 * parameters.
	 */
    private WSResponse createNewElement(WSTypeBase parent, boolean createMastercourse, String unittype) throws Exception {
        WSTypeBase newElem = null;
        WSResponse resp = new WSResponse();
        if (parent instanceof WSCustomer) {
            newElem = new WSProject();
            ((WSProject) newElem).setProjectName(defaultNodeNames.getProperty("default_project_name"));
        } else if (parent instanceof WSProject) {
            newElem = new WSCourseName();
            WSCourse mc = new WSCourse();
            mc.setCourseTitle(defaultNodeNames.getProperty("default_course_name"));
            mc.setLanguageSuffix(defaultNodeNames.getProperty("default_lang_suffix"));
            mc.setMasterCourse(true);
            mc.noWSsetParentUID(newElem.getUid());
            mc.setUid(0);
            newElem.getChildren().add(mc);
        } else if (parent instanceof WSCourseName) {
            if (!createMastercourse) {
                newElem = persistency.cloneMastercourse((WSCourseName) parent);
            } else {
                WSCourse mc = persistency.getMasterCourse(((WSCourseName) parent).getUid());
                if (mc != null) {
                    resp.error(XPCConst.E_MCOURSE_ALREADY_EXISTS, "Could not create mastercourse. A mastercourse (uid=" + mc.getUid() + ") already exists.", mc.obfuscate(), null);
                    logger.error("createNewElement: " + resp.toString());
                    return resp;
                } else {
                    newElem = new WSCourse();
                    ((WSCourse) newElem).setCourseTitle(defaultNodeNames.getProperty("default_course_name"));
                    ((WSCourse) newElem).setLanguageSuffix(defaultNodeNames.getProperty("default_lang_suffix"));
                    ((WSCourse) newElem).setMasterCourse(true);
                }
            }
        } else if (parent instanceof WSCourse) {
            newElem = new WSLesson();
            ((WSLesson) newElem).setLessonTitle(defaultNodeNames.getProperty("default_lesson_name"));
        } else if (parent instanceof WSLesson) {
            newElem = new WSUnit();
            resp = initNewUnit((WSUnit) newElem, unittype, (WSLesson) parent);
            if (!resp.isSuccess()) {
                logger.debug("createNewElement: " + resp.toString());
                return resp;
            }
        } else {
            resp.error(2000, "Invalid parent for add operation: " + parent.getClass().getName().replaceFirst(parent.getClass().getPackage().getName() + ".", ""));
            logger.error("createNewElement: " + resp.toString());
            return resp;
        }
        if (newElem == null) {
            resp.error(2001, "Could not add element for parent " + parent.getClass().getName().replaceFirst(parent.getClass().getPackage().getName() + ".", "") + ". (Allocation of new element failed)", null, null);
            logger.error("createNewElement: " + resp.toString());
            return resp;
        }
        resp.success(newElem);
        return resp;
    }

    /**
	 * Copy the given element down to component level. Used for paste
	 * operations. Important: Pages are created at the client. Make sure that
	 * they have a proper uid != 0. Usually this is done via storeUnitUnlock and
	 * copying the uids of the result tree.
	 */
    private WSResponse copyElement(WSTypeBase parent, long origID) throws Exception {
        WSTypeBase newElem = null;
        WSResponse resp = new WSResponse();
        try {
            if (parent instanceof WSCustomer) {
                newElem = ((WSProject) persistency.loadTree(origID, WSProject.class, WSPageBlob.class)).copyTree();
            } else if (parent instanceof WSProject) {
                newElem = ((WSCourseName) persistency.loadTree(origID, WSCourseName.class, WSPageBlob.class)).copyTree();
            } else if (parent instanceof WSCourseName) {
                newElem = ((WSCourse) persistency.loadTree(origID, WSCourse.class, WSPageBlob.class)).copyTree();
                ((WSCourse) newElem).setMasterCourse(false);
            } else if (parent instanceof WSCourse) {
                newElem = ((WSLesson) persistency.loadTree(origID, WSLesson.class, WSPageBlob.class)).copyTree();
            } else if (parent instanceof WSLesson) {
                newElem = ((WSUnit) persistency.loadTree(origID, WSUnit.class, WSPageBlob.class)).copyTree();
            } else {
                resp.error(2000, "Invalid parent for add operation: " + parent.getClass().getName().replaceFirst(parent.getClass().getPackage().getName() + ".", ""));
                logger.error("copyElement: " + resp.toString());
                return resp;
            }
            if (newElem == null) {
                resp.error(2001, "Could not add element for parent " + parent.getClass().getName().replaceFirst(parent.getClass().getPackage().getName() + ".", "") + ". (Allocation of new element failed)", null, null);
                logger.error("copyElement: " + resp.toString());
                return resp;
            }
            resp.success(newElem);
        } catch (ObjNotFoundException e) {
            resp.error(2002, "Source element not found for add operation: parent=" + parent.getClass().getName().replaceFirst(parent.getClass().getPackage().getName() + ".", "") + ", source uid=" + origID, null, null);
            logger.error("copyElement: " + resp.toString());
            return resp;
        }
        return resp;
    }

    public WSResponse removeElemUnlock(WSAuthentication auth, WSTypeBase elem, boolean keepUnits) {
        WSResponse resp = new WSResponse(), lockResp;
        try {
            logger.debug("removeElemUnlock: " + usr(auth) + " " + cls(elem) + " " + uid(elem));
            checkParam("elem", elem);
            checkAuthentication(auth);
            synchronized (this) {
                List path2Root = persistency.getPathToRoot(elem.getUid(), elem.getClass());
                WSTypeBase dbElem = (WSTypeBase) persistency.loadElem(elem.getClass(), elem.getUid());
                if (elem instanceof WSUnit) {
                    lockResp = lockAsParentElement(auth, elem.getClass(), elem.getUid());
                    if (!lockResp.isSuccess()) return lockResp;
                }
                Class parentClass = XPCUtil.parentClass(dbElem.getClass());
                lockResp = lockAsParentElement(auth, parentClass, dbElem.noWSgetParentUID());
                if (!lockResp.isSuccess()) resp = lockResp; else {
                    WSTypeBase sibling = persistency.loadAnySibling(dbElem);
                    persistency.deleteTree(dbElem);
                    mediaUsgThread.recalcMediaUsage();
                    String statusProp = statusPropagator.propagateStatusUpwards(sibling);
                    resp.setResultObj(statusProp);
                    unlockElement(auth, parentClass, dbElem.noWSgetParentUID());
                    resp.success();
                }
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "removeElemUnlock: ");
        }
        return resp;
    }

    private WSResponse initNewUnit(WSUnit unit, String unittype, WSLesson lesson) throws Exception {
        logger.debug("initNewUnit: unittype=" + unittype);
        WSResponse resp = new WSResponse();
        boolean found = false;
        unit.setUnitType(unittype);
        unit.setUnitTitle(defaultNodeNames.getProperty("default_unit_name"));
        for (Iterator iter = persistency.loadCustomer().getUnitPatterns().iterator(); iter.hasNext(); ) {
            WSUnitPattern up = (WSUnitPattern) iter.next();
            if (up.getUnitType().compareTo(unittype) == 0) {
                found = true;
                for (Iterator pgtIt = up.getChildren().iterator(); pgtIt.hasNext(); ) unit.getChildren().add(new WSUnitItem(unit.getUid(), 0, ((WSPageType) pgtIt.next()).getPageType(), 0));
            }
        }
        if (!found) {
            resp.error(XPCConst.E_NO_SUCH_UNITTYPE, "Could not create unit. Unit type \"" + unittype + "\" not found.", unittype, null);
            logger.error("initNewUnit: " + resp.toString());
        } else if (unit.getChildren().size() == 0) {
            resp.error(XPCConst.E_NO_PAGE_IN_UNITTYPE, "Could not create unit. Unit type \"" + unittype + "\" contains no pages", unittype, null);
            logger.error("initNewUnit: " + resp.toString());
        } else resp.success();
        return resp;
    }

    public Persistency getPersistency() {
        return persistency;
    }

    public void checkAuthentication(WSAuthentication auth) throws Exception {
        if (auth == null || auth.getAuthentification() == null) throw new AuthenticationException("authentification missing", "<null>");
        if (Explicanto.TESTMODE) return;
        WSUser usr = persistency.loadUserWithSecrets(auth.getUserID());
        String authId = usr.noWSgetAuthentication();
        authId = authId.replaceAll("_admin", "");
        authId = authId.substring(0, authId.lastIndexOf("_"));
        if (!authId.equals(auth.getAuthentification())) throw new AuthenticationException("authentification invalid", usr.getUsername());
    }

    public void checkForAdminRole(WSAuthentication auth) throws Exception {
        if (Explicanto.TESTMODE) return;
        WSUser usr = persistency.loadUserWithSecrets(auth.getUserID());
        if (!usr.noWSgetAuthentication().endsWith("_admin")) throw new AuthenticationException("admin privileges required", usr.getUsername());
    }

    public void checkEntitlement(WSAuthentication auth, List pathToRoot) throws Exception {
        WSUser usr = persistency.loadUserWithSecrets(auth.getUserID());
        if (!usr.noWSgetAuthentication().endsWith("_admin")) {
            int entitlement = Integer.MAX_VALUE;
            for (int role = 1; role <= getProps().roleCount; role++) entitlement = Math.min(entitlement, isInRoleInSingleResp(auth.getUserID(), role, pathToRoot));
            if (entitlement != 0) throw new EntitlementEx(entitlement);
        }
    }

    private int isInRoleInSingleResp(long userID, int role, List pathToRoot) throws Exception {
        boolean bon = isBelowOwnerNode((WSTypeBase) pathToRoot.get(0), role);
        boolean ras = hasRoleAssFromMaxProject(pathToRoot, role, userID);
        boolean ssp = hasRoleSubsetProperty(pathToRoot, 1, role, userID);
        if (!bon) return EntitlementEx.NBON; else if (bon && !ras) return EntitlementEx.NRAS; else if (bon && ras && !ssp) return EntitlementEx.NSSP; else return EntitlementEx.OK;
    }

    private boolean isDirectInRole(WSTypeBase elem, int role, long uid) throws Exception {
        if (elem instanceof WSCustomer) {
            WSUser usr = persistency.loadUserWithSecrets(uid);
            correctUserRoles(usr);
            String[] roles = usr.getAssignedRoles().split(",");
            for (int i = 0; i < roles.length; i++) if (!roles[i].equals("") && Integer.parseInt(roles[i]) == role) return true;
        } else for (Iterator it = XPCUtil.getRoles(elem).iterator(); it.hasNext(); ) {
            WSRoleElem re = (WSRoleElem) it.next();
            if (re.getRoleID() == role && re.getUserID() == uid) return true;
        }
        return false;
    }

    private boolean hasRoleSubsetProperty(List pathToRoot, int pos, int role, long uid) throws Exception {
        if (pos >= pathToRoot.size()) return true;
        WSTypeBase elem = (WSTypeBase) pathToRoot.get(pos);
        if (hasUsersInRole(elem, role)) return isDirectInRole(elem, role, uid) && hasRoleSubsetProperty(pathToRoot, pos + 1, role, uid); else return hasRoleSubsetProperty(pathToRoot, pos + 1, role, uid);
    }

    private boolean hasUsersInRole(WSTypeBase elem, int role) throws Exception {
        if (elem instanceof WSCustomer) {
            for (Iterator iter = persistency.loadUsersNoSecrets().iterator(); iter.hasNext(); ) {
                WSUser usr = (WSUser) iter.next();
                String[] res = RolesListValidator.validateRoles(usr.getAssignedRoles(), getProps().roleCount);
                if (!res[RolesListValidator.RESULT_CODE].equals("OK")) logger.error("*** error while parsing roles of user \"" + usr.getUsername() + "\" (uid=" + usr.getUid() + "):\n" + res[RolesListValidator.RESULT_CODE] + "\n    using roles\"" + res[RolesListValidator.ROLE_STRING] + "\"");
                usr.setAssignedRoles(res[RolesListValidator.ROLE_STRING]);
                String[] roles = usr.getAssignedRoles().split(",");
                for (int i = 0; i < roles.length; i++) if (!roles[i].equals("") && Integer.parseInt(roles[i]) == role) return true;
            }
        } else for (Iterator iter = XPCUtil.getRoles(elem).iterator(); iter.hasNext(); ) if (((WSRoleElem) iter.next()).getRoleID() == role) return true;
        return false;
    }

    /**
	 * Wahr, wenn Benutzer uid die Rolle role im Element pathToProject(0)
	 * innehat, egal ob direkt oder vererbt. Wenn vererbt, dann jedoch nicht vom
	 * Customer.
	 * 
	 * @throws Exception
	 */
    private boolean hasRoleAssFromMaxProject(List pathToRoot, int role, long uid) throws Exception {
        for (Iterator iter = pathToRoot.iterator(); iter.hasNext(); ) {
            WSTypeBase element = (WSTypeBase) iter.next();
            if (!(element instanceof WSCustomer) && isDirectInRole(element, role, uid)) return true;
        }
        return false;
    }

    /**
	 * Ist elem unterhalb oder auf der Ebene, ab der Rolle role nur noch ein
	 * Benutzer zugewiesen ist?
	 */
    private boolean isBelowOwnerNode(WSTypeBase elem, int role) {
        for (Iterator iter = XPCUtil.getRoleTypesFromProps(getProps()).iterator(); iter.hasNext(); ) {
            WSRoleType rty = (WSRoleType) iter.next();
            if (rty.getRoleID() == role) return rty.noWSisBelowOwnerNode(elem);
        }
        return false;
    }

    public ExplicantoServerProperties getProps() {
        return properties;
    }

    public void checkParam(String name, Object o) throws ParamException {
        if (o == null) throw new ParamException("param " + "'" + name + "' is null");
    }

    public String usr(WSAuthentication a) {
        return "usrID=" + (a != null ? ("" + a.getUserID()) : "<null>");
    }

    public String clz(Class c) {
        return "cls=" + XPCUtil.claZZName(c);
    }

    public String cls(Object o) {
        return "cls=" + XPCUtil.clazzName(o);
    }

    public String uid(UidElem e) {
        return "uid=" + (e != null ? ("" + e.getUid()) : "<null>");
    }

    /**
	 * Import into an existing project
	 */
    public WSResponse importCSDEProject(WSAuthentication auth, WSProject existing, WSProject neu) {
        WSResponse resp = new WSResponse();
        synchronized (this) {
            try {
                checkAuthentication(auth);
                checkParam("existing", existing);
                checkParam("neu", neu);
                WSResponse r;
                r = lockedByAuth(auth, existing);
                if (!r.isSuccess()) return r;
                r = lockElement(auth, WSProject.class, existing.getUid(), true);
                if (!r.isSuccess()) return r;
                existing = (WSProject) persistency.loadTree(existing.getUid(), WSProject.class, WSPage.class);
                int pos = 0;
                for (Iterator iter = existing.getChildren().iterator(); iter.hasNext(); ) pos = Math.max(pos, ((WSCourseName) iter.next()).noWSgetPosition());
                for (Iterator iter = neu.getChildren().iterator(); iter.hasNext(); ) {
                    WSCourseName c = (WSCourseName) iter.next();
                    c.noWSsetParentUID(existing.getUid());
                    c.noWSsetPosition(++pos);
                    existing.getChildren().add(c);
                }
                persistency.storeReplaceTree(existing);
                unlockElement(auth, WSProject.class, existing.getUid());
                resp.success();
            } catch (Exception e) {
                e.printStackTrace();
                errHandler.handleError(e, resp, "importCSDEProject: ");
                return resp;
            }
        }
        return resp;
    }

    public WSResponse notifyRoles(WSAuthentication auth, WSTypeBase elem) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            logger.debug("notifyRoles");
            checkParam("elem", elem);
            synchronized (this) {
                WSResponse statusChangeCheck = checkStatusChange(elem, auth.getUserID());
                if (!statusChangeCheck.isSuccess()) {
                    return statusChangeCheck;
                } else {
                    WSTypeBase oldElem;
                    if (elem instanceof WSCourse) {
                        oldElem = (WSTypeBase) persistency.loadElem(WSCourse.class, elem.getUid());
                        checkNotification(XPCUtil.getRoles(oldElem), XPCUtil.getRoles(elem), 1, ((WSCourse) elem).getCourseTitle());
                    } else if (elem instanceof WSProject) {
                        oldElem = (WSTypeBase) persistency.loadElem(WSProject.class, elem.getUid());
                        checkNotification(XPCUtil.getRoles(oldElem), XPCUtil.getRoles(elem), 0, ((WSProject) elem).getProjectName());
                    } else return resp;
                    resp.success();
                }
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "notifyRoles: ");
        }
        return resp;
    }

    private void checkNotification(List old, List neu, int type, String tag) {
        List sendNeu = new ArrayList();
        List sendDel = new ArrayList();
        for (Iterator i = neu.iterator(); i.hasNext(); ) {
            WSRoleElem nre = (WSRoleElem) i.next();
            boolean bFound = false;
            for (Iterator j = old.iterator(); j.hasNext(); ) {
                WSRoleElem ore = (WSRoleElem) j.next();
                if (ore.getRoleID() == nre.getRoleID() && ore.getUserID() == nre.getUserID()) {
                    bFound = true;
                    break;
                }
            }
            if (!bFound) sendNeu.add(nre);
        }
        for (Iterator i = old.iterator(); i.hasNext(); ) {
            WSRoleElem ore = (WSRoleElem) i.next();
            boolean bFound = false;
            for (Iterator j = neu.iterator(); j.hasNext(); ) {
                WSRoleElem nre = (WSRoleElem) j.next();
                if (ore.getRoleID() == nre.getRoleID() && ore.getUserID() == nre.getUserID()) {
                    bFound = true;
                    break;
                }
            }
            if (!bFound) sendDel.add(ore);
        }
        if (sendNeu.size() > 0) notify.notifyRoleAddUser(sendNeu, type, tag);
        if (sendDel.size() > 0) notify.notifyRoleRemoveUser(sendDel, type, tag);
    }

    public void updateFromTemplate(WSAuthentication auth, Document doc) {
        try {
            WSResponse cr = loadCustomer(auth);
            if (!cr.isSuccess()) {
                logger.error("could not load the customer for database update after template import");
                return;
            }
            WSCustomer cust = (WSCustomer) cr.getResultObj();
            List patterns = cust.getUnitPatterns();
            List units = doc.getElementsByObjectType("unit");
            for (Iterator i = units.iterator(); i.hasNext(); ) {
                String id = ((String) i.next());
                Element unitElem = doc.getElement(id);
                WSUnitPattern wsUP = null;
                for (Iterator j = patterns.iterator(); j.hasNext(); ) {
                    wsUP = (WSUnitPattern) j.next();
                    if (wsUP.getUnitType().equalsIgnoreCase(getElementProperty(unitElem, TemplateConstants.UNITTYPE))) {
                        break;
                    }
                    wsUP = null;
                }
                if (wsUP != null) {
                    updateUnitPattern(cust, doc, wsUP, unitElem);
                } else {
                    addUnitPattern(cust, doc, unitElem);
                }
            }
            persistency.updateCustomer(cust);
        } catch (RemoteException e) {
            logger.error(e);
            e.printStackTrace();
        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    private void addUnitPattern(WSCustomer cust, Document doc, Element unitElem) throws Exception {
        WSUnitPattern up = new WSUnitPattern();
        up.setUnitType(getElementProperty(unitElem, TemplateConstants.UNITTYPE));
        up.noWSsetParentUID(cust.getUid());
        List items = doc.getElementsByObjectType("unititem");
        for (Iterator i = items.iterator(); i.hasNext(); ) {
            String elemId = ((String) i.next());
            Element item = doc.getElement(elemId);
            ArrayList parents = item.getParents();
            if (parents != null) for (int j = 0; j < parents.size(); ++j) {
                if (((String) parents.get(j)).equalsIgnoreCase(unitElem.getId())) {
                    WSPageType pt = new WSPageType();
                    pt.setPageType(getElementProperty(item, TemplateConstants.TITLE));
                    up.getChildren().add(pt);
                }
            } else {
                WSPageType pt = new WSPageType();
                pt.setPageType(getElementProperty(item, TemplateConstants.TITLE));
                up.getChildren().add(pt);
            }
        }
        up.setUid(persistency.insertElem(up));
        List l = cust.getUnitPatterns();
        l.add(up);
        cust.setUnitPatterns(l);
    }

    private void updateUnitPattern(WSCustomer cust, Document doc, WSUnitPattern wsUP, Element unitElem) throws Exception {
        List items = doc.getElementsByObjectType("unititem");
        for (Iterator i = items.iterator(); i.hasNext(); ) {
            String elemId = ((String) i.next());
            Element item = doc.getElement(elemId);
            String title = getElementProperty(item, TemplateConstants.TITLE);
            boolean isAvailable = false;
            ArrayList parents = item.getParents();
            if (parents != null) for (int j = 0; j < parents.size(); ++j) {
                if (((String) parents.get(j)).equalsIgnoreCase(unitElem.getId())) {
                    isAvailable = true;
                    break;
                }
            } else {
                isAvailable = true;
            }
            if (isAvailable) {
                List wsPages = wsUP.getChildren();
                for (Iterator j = wsPages.iterator(); j.hasNext(); ) {
                    WSPageType pt = (WSPageType) j.next();
                    if (pt.getPageType().equalsIgnoreCase(title)) {
                        title = null;
                        break;
                    }
                }
                if (title != null) {
                    WSPageType pt = new WSPageType();
                    pt.setPageType(getElementProperty(item, TemplateConstants.TITLE));
                    wsUP.getChildren().add(pt);
                }
            }
        }
        persistency.updateElem(wsUP);
    }

    private String getElementProperty(Element e, String name) {
        Property p = (Property) e.getProperties().get(name);
        if (p != null) return p.getValue();
        return "";
    }

    /**
	 * Retrieves a list with all the screenplays and their status.
	 * <p>
	 * The list is missing the screenplay content.
	 * 
	 * @param auth
	 *            The authentication on the database
	 * @return Returns a List with WSScreenplay elements without the content
	 *         field
	 * 
	 */
    public WSResponse getScreenplayList(WSAuthentication auth) throws RemoteException, Exception {
        List templates;
        WSResponse resp = new WSResponse();
        try {
            checkAuthentication(auth);
            synchronized (this) {
                templates = persistency.loadElemList(WSScreenplayHeader.class);
            }
            List result = new ArrayList(templates.size());
            for (Iterator iter = templates.iterator(); iter.hasNext(); ) {
                WSScreenplayHeader th = (WSScreenplayHeader) iter.next();
                WSScreenplay t = new WSScreenplay();
                t.setUid(th.getUid());
                t.setTitle(th.getTitle());
                t.setContent(null);
                t.setLockUserID(th.getLockUserID());
                t.setLockCount(th.getLockCount());
                t.setLockTime(th.getLockTime());
                result.add(t);
            }
            resp.success(result);
        } catch (Exception e) {
            errHandler.handleError(e, resp, "getScreenplayList: ");
            e.printStackTrace();
            return resp;
        }
        return resp;
    }

    /**
	 * Loads a screenplay, optionally locking it
	 * 
	 * @param auth
	 * @param uid
	 * @param lock
	 * @return
	 * @throws RemoteException
	 * @throws Exception
	 */
    public WSResponse loadScreenplay(WSAuthentication auth, long uid, boolean lock) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            checkAuthentication(auth);
            synchronized (this) {
                WSScreenplay sp = (WSScreenplay) persistency.loadElem(WSScreenplay.class, uid);
                if (lock) {
                    if (sp.getLockUserID() == auth.getUserID() || sp.getLockUserID() == WSTypeBase.NOLOCKUSERID) {
                        WSScreenplayHeader sph = new WSScreenplayHeader();
                        sph.setUid(sp.getUid());
                        sph.setTitle(sp.getTitle());
                        setLockInformation(auth, sph, true);
                        persistency.updateElem(sph);
                        sp.setLockUserID(sph.getLockUserID());
                        sp.setLockTime(sph.getLockTime());
                        sp.setLockCount(sph.getLockCount());
                        resp.success(JAXBConverter.convertScreenplay(sp));
                    } else {
                        resp.error(XPCConst.E_ELEM_NOT_LOCKED_FOR_YOU, "element " + XPCUtil.clazzName(sp) + " (uid=" + sp.getUid() + ") isn't locked for userid " + auth.getUserID() + " but for userid " + sp.getLockUserID());
                        resp.setResultObj(sp.obfuscate());
                        logger.debug("lockedByAuth: " + resp.toString());
                    }
                } else {
                    resp.success(JAXBConverter.convertScreenplay(sp));
                }
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "loadScreenplay: ");
            return resp;
        }
        return resp;
    }

    /**
	 * Saves the changes to a screenplay optionally unlocking it
	 * 
	 * @param auth
	 * @param tpl
	 * @param update
	 * @param unlock
	 * @return
	 * @throws RemoteException
	 * @throws Exception
	 */
    public WSResponse storeScreenplay(WSAuthentication auth, WSScreenPlayObject tpl, boolean update, boolean unlock) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            WSScreenplay sc = JAXBConverter.convertScreenplay(tpl);
            checkParam("screenplay", sc);
            checkAuthentication(auth);
            synchronized (this) {
                if (update) {
                    WSTypeBase base = sc;
                    if (sc.getContent() == null) {
                        WSScreenplayHeader xx = new WSScreenplayHeader();
                        xx.setUid(sc.getUid());
                        xx.setTitle(sc.getTitle());
                        base = xx;
                    }
                    setLockInformation(auth, base, !unlock);
                    persistency.updateElem(base);
                } else {
                    setLockInformation(auth, sc, !unlock);
                    dumpScreenplay(sc);
                    sc.setUid(persistency.insertElem(sc));
                }
            }
            resp.success(new Long(sc.getUid()));
        } catch (Exception e) {
            errHandler.handleError(e, resp, (update ? "updateScreenplay: " : "addScreenplay: "));
            return resp;
        }
        return resp;
    }

    /**
	 * Simply locks/unlocks a WSTypeBase element (just set the field content!)
	 * 
	 * @param auth
	 * @param base
	 * @param lock
	 */
    private void setLockInformation(WSAuthentication auth, WSTypeBase base, boolean lock) {
        if (lock) {
            base.setLockTime(XPCUtil.getCurrentGMTTimestamp().toString());
            base.setLockUserID(auth.getUserID());
            base.setLockCount(1);
        } else {
            base.setLockUserID(WSTypeBase.NOLOCKUSERID);
            base.setLockCount(WSTypeBase.NOLOCKCOUNT);
            base.setLockTime(WSTypeBase.NOLOCKTIME);
        }
    }

    /**
	 * Removes a screenplay from the database
	 * 
	 * @param auth
	 * @param uid
	 * @return
	 * @throws RemoteException
	 * @throws Exception
	 */
    public WSResponse deleteScreenplay(WSAuthentication auth, long uid) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            checkAuthentication(auth);
            synchronized (this) {
                WSScreenplayHeader sp = (WSScreenplayHeader) persistency.loadElem(WSScreenplayHeader.class, uid);
                if (sp.getLockUserID() == auth.getUserID() || sp.getLockUserID() == WSTypeBase.NOLOCKUSERID) {
                    persistency.deleteElem(WSScreenplayHeader.class, uid);
                    resp.success();
                } else {
                    resp.error(XPCConst.E_ELEM_NOT_LOCKED_FOR_YOU, "element " + XPCUtil.clazzName(sp) + " (uid=" + sp.getUid() + ") isn't locked for userid " + auth.getUserID() + " but for userid " + sp.getLockUserID());
                    resp.setResultObj(sp.obfuscate());
                    logger.debug("lockedByAuth: " + resp.toString());
                }
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "deleteScreenplay: ");
            return resp;
        }
        return resp;
    }

    /**
	 * Simply unlocks a screenplay after it cecks the rights
	 * 
	 * @param auth
	 * @param uid
	 * @return
	 * @throws RemoteException
	 * @throws Exception
	 */
    public WSResponse unlockScreenplay(WSAuthentication auth, long uid) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            checkAuthentication(auth);
            synchronized (this) {
                WSScreenplayHeader sp = (WSScreenplayHeader) persistency.loadElem(WSScreenplayHeader.class, uid);
                if (sp.getLockUserID() == auth.getUserID() || sp.getLockUserID() == WSTypeBase.NOLOCKUSERID) {
                    setLockInformation(auth, sp, false);
                    persistency.updateElem(sp);
                    resp.success();
                } else {
                    resp.error(XPCConst.E_ELEM_NOT_LOCKED_FOR_YOU, "element " + XPCUtil.clazzName(sp) + " (uid=" + sp.getUid() + ") isn't locked for userid " + auth.getUserID() + " but for userid " + sp.getLockUserID());
                    resp.setResultObj(sp.obfuscate());
                    logger.debug("lockedByAuth: " + resp.toString());
                }
            }
        } catch (Exception e) {
            errHandler.handleError(e, resp, "unlockScreenplay: ");
            return resp;
        }
        return resp;
    }

    private void dumpScreenplay(WSScreenplay sp) {
        logger.debug("Screenplay dump");
        logger.debug("    uid   : " + sp.getUid());
        logger.debug("  title   : " + sp.getTitle());
        logger.debug(" content  : " + sp.getContent());
        logger.debug(" lock user: " + sp.getLockUserID());
        logger.debug(" lock cnt : " + sp.getLockCount());
        logger.debug(" lock time: " + sp.getLockTime());
    }

    public WSResponse genRaport(WSAuthentication auth, WSScreenPlayObject scp, int type, String scpModelName, long characterUid, String characterName, Hashtable params) throws RemoteException, Exception {
        WSResponse resp = new WSResponse();
        try {
            Screenplay sc = JAXBConverter.convertScreenplayNoWS(scp);
            checkParam("screenplay", sc);
            checkAuthentication(auth);
            byte[] result = null;
            synchronized (this) {
                ReportsManager rMgr = new ReportsManager();
                if (type == AUDIO_TYPE) result = rMgr.createAudioReport(sc, scpModelName, characterUid, characterName); else if (type == IMAGE_TYPE) result = rMgr.createImagesReport(sc, scpModelName); else if (type == SCREENPLAY_TYPE) result = rMgr.createScreenplayReport(sc, scpModelName, params); else if (type == SCENES_TYPE) result = rMgr.createScenesReport(sc, params);
            }
            resp.success(result);
        } catch (Exception e) {
            errHandler.handleError(e, resp, "generate report: ");
            e.printStackTrace();
            return resp;
        }
        return resp;
    }
}
