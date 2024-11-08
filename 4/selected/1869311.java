package com.dotmarketing.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.persister.AbstractEntityPersister;
import org.apache.commons.beanutils.BeanUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicUserFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DotConnect;
import com.dotmarketing.db.DotHibernate;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.ejb.GroupManagerUtil;
import com.liferay.portal.ejb.LayoutManagerUtil;
import com.liferay.portal.ejb.RoleManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Image;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.PortletPreferences;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;

/**
 * @author Jason Tesser
 * @version 1.6
 *
 */
public class ImportExportUtil {

    /**
	 * The path where tmp files are stored. This gets wiped alot
	 */
    private String backupTempFilePath = "WEB-INF/backup/temp";

    private ArrayList<String> classesWithIdentity = new ArrayList<String>();

    private Map<String, String> sequences;

    private Map<String, String> tableIDColumns;

    private Map<String, String> tableNames;

    private String dbType = DbConnectionFactory.getDBType();

    private static String assetRealPath = null;

    private static String assetPath = "/assets";

    private File companyXML;

    private File userXML;

    private File roleXML;

    private File groupXML;

    private File layoutXML;

    public ImportExportUtil() {
        try {
            assetRealPath = Config.getStringProperty("ASSET_REAL_PATH");
        } catch (Exception e) {
        }
        try {
            assetPath = Config.getStringProperty("ASSET_PATH");
        } catch (Exception e) {
        }
        classesWithIdentity.add("Inode");
        classesWithIdentity.add("Rating");
        classesWithIdentity.add("Indexation");
        classesWithIdentity.add("Language");
        classesWithIdentity.add("Permission");
        classesWithIdentity.add("UserPreference");
        classesWithIdentity.add("WebForm");
        classesWithIdentity.add("UsersToDelete");
        tableNames = new HashMap<String, String>();
        tableNames.put("Inode", "inode");
        tableNames.put("Rating", "content_rating");
        tableNames.put("Indexation", "indexation");
        tableNames.put("Language", "language");
        tableNames.put("Permission", "permission");
        tableNames.put("UserPreference", "user_preferences");
        tableNames.put("WebForm", "web_form");
        tableNames.put("UsersToDelete", "users_to_delete");
        if (dbType.equals(DbConnectionFactory.POSTGRESQL) || dbType.equals(DbConnectionFactory.ORACLE)) {
            sequences = new HashMap<String, String>();
            sequences.put("inode", "inode_seq");
            sequences.put("content_rating", "content_rating_sequence");
            sequences.put("indexation", "indexation_seq");
            sequences.put("language", "language_seq");
            sequences.put("permission", "permission_seq");
            sequences.put("user_preferences", "user_preferences_seq");
            sequences.put("web_form", "web_form_seq");
            sequences.put("users_to_delete", "user_to_delete_seq");
            tableIDColumns = new HashMap<String, String>();
            tableIDColumns.put("inode", "inode");
            tableIDColumns.put("content_rating", "id");
            tableIDColumns.put("indexation", "indexation_id");
            tableIDColumns.put("language", "id");
            tableIDColumns.put("permission", "id");
            tableIDColumns.put("user_preferences", "id");
            tableIDColumns.put("web_form", "web_form_id");
            tableIDColumns.put("users_to_delete", "id");
        }
    }

    /**
	 * Takes a zip file from the temp directory to restore dotCMS data. Currently it will blow away all current data
	 * This method cannot currently be run in a transaction. For performance reasons with db drivers and connections it closes the 
	 * session every so often. 
	 * @param out A print writer for output
	 * @throws IOException
	 */
    public void doImport(PrintWriter out) throws IOException {
        File f = new File(Config.CONTEXT.getRealPath(getBackupTempFilePath()));
        String[] _tempFiles = f.list();
        out.println("<pre>Found " + _tempFiles.length + " files to import");
        Logger.info(this, "Found " + _tempFiles.length + " files to import");
        deleteDotCMS();
        List<File> treeXMLs = new ArrayList<File>();
        File assetDir = null;
        boolean hasAssetDir = false;
        for (int i = 0; i < _tempFiles.length; i++) {
            try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
            File _importFile = new File(Config.CONTEXT.getRealPath(getBackupTempFilePath() + "/" + _tempFiles[i]));
            if (_importFile.isDirectory()) {
                if (_importFile.getName().equals("asset")) {
                    hasAssetDir = true;
                    assetDir = new File(_importFile.getPath());
                }
            } else if (_importFile.getName().contains("com.dotmarketing.beans.Tree")) {
                treeXMLs.add(new File(_importFile.getPath()));
            } else if (_importFile.getName().endsWith("User.xml")) {
                userXML = new File(_importFile.getPath());
            } else if (_importFile.getName().endsWith("Role.xml")) {
                roleXML = new File(_importFile.getPath());
            } else if (_importFile.getName().endsWith("Layout.xml")) {
                layoutXML = new File(_importFile.getPath());
            } else if (_importFile.getName().endsWith("Group.xml")) {
                groupXML = new File(_importFile.getPath());
            } else if (_importFile.getName().endsWith("Company.xml")) {
                companyXML = new File(_importFile.getPath());
            } else if (_importFile.getName().endsWith(".xml")) {
                try {
                    doXMLFileImport(_importFile, out);
                } catch (Exception e) {
                    Logger.error(this, "Unable to load " + _importFile.getName() + " : " + e.getMessage(), e);
                }
            }
            out.flush();
        }
        for (File file : treeXMLs) {
            try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
            try {
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        try {
            doXMLFileImport(companyXML, out);
        } catch (Exception e) {
            Logger.error(this, "Unable to load " + companyXML.getName() + " : " + e.getMessage(), e);
        }
        try {
            doXMLFileImport(userXML, out);
            try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
        } catch (Exception e) {
            Logger.error(this, "Unable to load " + userXML.getName() + " : " + e.getMessage(), e);
        }
        try {
            doXMLFileImport(roleXML, out);
            try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
        } catch (Exception e) {
            Logger.error(this, "Unable to load " + roleXML.getName() + " : " + e.getMessage(), e);
        }
        try {
            doXMLFileImport(layoutXML, out);
        } catch (Exception e) {
            Logger.error(this, "Unable to load " + layoutXML.getName() + " : " + e.getMessage(), e);
        }
        try {
            doXMLFileImport(groupXML, out);
        } catch (Exception e) {
            Logger.error(this, "Unable to load " + groupXML.getName() + " : " + e.getMessage(), e);
        }
        cleanUpDBFromImport();
        if (hasAssetDir && assetDir != null && assetDir.exists()) copyAssetDir(assetDir);
        out.println("Done Importing");
        deleteTempFiles();
        MaintenanceUtil.flushCache();
        ContentletAPI conAPI = APILocator.getContentletAPI();
        conAPI.reindex();
        MaintenanceUtil.deleteStaticFileStore();
        MaintenanceUtil.deleteMenuCache();
    }

    private void copyAssetDir(File fromAssetDir) {
        File ad;
        if (!UtilMethods.isSet(assetRealPath)) {
            ad = new File(Config.CONTEXT.getRealPath(assetPath));
        } else {
            ad = new File(assetRealPath);
        }
        ad.mkdirs();
        String[] fileNames = fromAssetDir.list();
        for (int i = 0; i < fileNames.length; i++) {
            File f = new File(fromAssetDir.getPath() + File.separator + fileNames[i]);
            if (f.getName().equals(".svn")) {
                continue;
            }
            if (f.isDirectory()) {
                FileUtil.copyDirectory(f.getPath(), ad.getPath() + File.separator + f.getName());
            } else {
                FileUtil.copyFile(f.getPath(), ad.getPath() + File.separator + f.getName());
            }
        }
    }

    /**
	 * Does what it says - deletes all files from the backupTempFilePath
	 * @author Will
	 */
    private void deleteTempFiles() {
        File f = new File(Config.CONTEXT.getRealPath(backupTempFilePath));
        String[] _tempFiles = f.list();
        if (_tempFiles != null) {
            for (int i = 0; i < _tempFiles.length; i++) {
                f = new File(Config.CONTEXT.getRealPath(backupTempFilePath + "/" + _tempFiles[i]));
                f.delete();
            }
        }
    }

    /**
	 * Takes a ZipInputStream and filename and will extract them to the
	 * backupTempFilePath
	 * 
	 * @param zin
	 *            ZipInputStream
	 * @param s
	 *            FileName to be extracted
	 * @throws IOException
	 * @author Will
	 */
    private void unzip(ZipInputStream zin, String s) throws IOException {
        Logger.info(this, "unzipping " + s);
        File f = new File(Config.CONTEXT.getRealPath(backupTempFilePath + File.separator + s));
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        byte[] b = new byte[512];
        int len = 0;
        while ((len = zin.read(b)) != -1) {
            out.write(b, 0, len);
        }
        out.close();
    }

    /**
	 * This is not completed should delete all the dotcms data from an install
	 * @author Will
	 */
    private void deleteDotCMS() {
        try {
            ArrayList<String> _tablesToDelete = new ArrayList<String>();
            Map map;
            map = DotHibernate.getSession().getSessionFactory().getAllClassMetadata();
            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                AbstractEntityPersister cmd = (AbstractEntityPersister) pairs.getValue();
                String tableName = cmd.getTableName();
                if (!tableName.equalsIgnoreCase("inode")) {
                    _tablesToDelete.add(tableName);
                }
            }
            _tablesToDelete.add("inode");
            _tablesToDelete.add("group_");
            _tablesToDelete.add("layout");
            _tablesToDelete.add("user_");
            _tablesToDelete.add("role_");
            _tablesToDelete.add("company");
            _tablesToDelete.add("users_roles");
            _tablesToDelete.add("users_groups");
            _tablesToDelete.add("groups_roles");
            _tablesToDelete.add("counter");
            _tablesToDelete.add("image");
            _tablesToDelete.add("portlet");
            _tablesToDelete.add("portletpreferences");
            DotConnect _dc = null;
            for (String table : _tablesToDelete) {
                Logger.info(this, "About to delete all records from " + table);
                _dc = new DotConnect();
                _dc.setSQL("delete from " + table);
                _dc.getResult();
                Logger.info(this, "Deleted all records from " + table);
            }
        } catch (HibernateException e) {
            Logger.error(this, e.getMessage(), e);
        }
        File ad;
        if (!UtilMethods.isSet(assetRealPath)) {
            ad = new File(Config.CONTEXT.getRealPath(assetPath));
        } else {
            ad = new File(assetRealPath);
        }
        ad.mkdirs();
        String[] fl = ad.list();
        for (String fileName : fl) {
            File f = new File(ad.getPath() + File.separator + fileName);
            if (f.isDirectory()) {
                FileUtil.deltree(f);
            } else {
                f.delete();
            }
        }
    }

    /**
	 * This method takes an xml file and will try to import it via XStream and
	 * Hibernate
	 * 
	 * @param f
	 *            File to be parsed and imported
	 * @param out
	 *            Printwriter to write responses to Reponse Printwriter so this
	 *            method can write to screen.
	 *            
	 * @author Will
	 */
    private void doXMLFileImport(File f, PrintWriter out) throws DotDataException, HibernateException {
        if (f == null) {
            return;
        }
        BufferedInputStream _bin = null;
        Reader charStream = null;
        try {
            XStream _xstream = null;
            String _className = null;
            Class _importClass = null;
            HibernateUtil _dh = null;
            boolean usersRoles = false;
            boolean usersGroups = false;
            boolean groupsRoles = false;
            boolean counter = false;
            boolean image = false;
            boolean portlet = false;
            boolean portletpreferences = false;
            Pattern p = Pattern.compile("_[0-9]{8}");
            Matcher m = p.matcher(f.getName());
            if (m.find()) {
                _className = f.getName().substring(0, f.getName().lastIndexOf("_"));
            } else {
                _className = f.getName().substring(0, f.getName().lastIndexOf("."));
            }
            if (_className.equals("Users_Roles")) {
                usersRoles = true;
            } else if (_className.equals("Users_Groups")) {
                usersGroups = true;
            } else if (_className.equals("Groups_Roles")) {
                groupsRoles = true;
            } else if (_className.equals("Counter")) {
                counter = true;
            } else if (_className.equals("Image")) {
                image = true;
            } else if (_className.equals("Portlet")) {
                portlet = true;
            } else if (_className.equals("Portletpreferences")) {
                portletpreferences = true;
            } else {
                _importClass = Class.forName(_className);
            }
            _xstream = new XStream();
            out.println("Importing:\t" + _className);
            Logger.info(this, "Importing:\t" + _className);
            try {
                charStream = new InputStreamReader(new FileInputStream(f), "UTF-8");
            } catch (UnsupportedEncodingException uet) {
                Logger.error(this, "Reader doesn't not recoginize Encoding type: ", uet);
            }
            List l = new ArrayList();
            try {
                l = (List) _xstream.fromXML(charStream);
            } catch (Exception e) {
                Logger.error(this, "Unable to import " + _className, e);
            }
            out.println("Found :\t" + l.size() + " " + _className + "(s)");
            Logger.info(this, "Found :\t" + l.size() + " " + _className + "(s)");
            if (usersRoles) {
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String, String>) l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into users_roles values (?,?)");
                    dc.addParam(dcResults.get("userid"));
                    dc.addParam(dcResults.get("roleid"));
                    dc.getResults();
                }
            } else if (usersGroups) {
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String, String>) l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into users_groups values (?,?)");
                    dc.addParam(dcResults.get("userid"));
                    dc.addParam(dcResults.get("groupid"));
                    dc.getResults();
                }
            } else if (groupsRoles) {
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String, String>) l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into groups_roles values (?,?)");
                    dc.addParam(dcResults.get("groupid"));
                    dc.addParam(dcResults.get("roleid"));
                    dc.getResults();
                }
            } else if (counter) {
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String, String>) l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into counter values (?,?)");
                    dc.addParam(dcResults.get("name"));
                    dc.addParam(Integer.valueOf(dcResults.get("currentid")));
                    dc.getResults();
                }
            } else if (image) {
                for (int j = 0; j < l.size(); j++) {
                    Image im = (Image) l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into image values (?,?)");
                    if (!UtilMethods.isSet(im.getImageId()) && com.dotmarketing.db.DbConnectionFactory.getDBType().equals(com.dotmarketing.db.DbConnectionFactory.ORACLE)) {
                        continue;
                    }
                    dc.addParam(im.getImageId());
                    dc.addParam(im.getText());
                    dc.getResults();
                }
            } else if (portlet) {
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String, String>) l.get(j);
                    DotConnect dc = new DotConnect();
                    StringBuffer sb = new StringBuffer("insert into portlet values (?,?,?,?,");
                    if (dcResults.get("narrow").equalsIgnoreCase("f") || dcResults.get("narrow").equalsIgnoreCase("false") || dcResults.get("narrow").equalsIgnoreCase("0") || dcResults.get("narrow").equals(DbConnectionFactory.getDBFalse())) sb.append(DbConnectionFactory.getDBFalse() + ",?,"); else sb.append(DbConnectionFactory.getDBTrue() + ",?,");
                    if (dcResults.get("active_").equalsIgnoreCase("f") || dcResults.get("active_").equalsIgnoreCase("false") || dcResults.get("active_").equalsIgnoreCase("0") || dcResults.get("active_").equals(DbConnectionFactory.getDBFalse())) sb.append(DbConnectionFactory.getDBFalse() + ")"); else sb.append(DbConnectionFactory.getDBTrue() + ")");
                    dc.setSQL(sb.toString());
                    dc.addParam(dcResults.get("portletid"));
                    dc.addParam(dcResults.get("groupid"));
                    dc.addParam(dcResults.get("companyid"));
                    dc.addParam(dcResults.get("defaultpreferences"));
                    dc.addParam(dcResults.get("roles"));
                    dc.getResults();
                }
            } else if (portletpreferences) {
                for (int j = 0; j < l.size(); j++) {
                    PortletPreferences portletPreferences = (PortletPreferences) l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into portletpreferences values (?,?,?,?)");
                    dc.addParam(portletPreferences.getPortletId());
                    dc.addParam(portletPreferences.getUserId());
                    dc.addParam(portletPreferences.getLayoutId());
                    dc.addParam(portletPreferences.getPreferences());
                    dc.getResults();
                }
            } else if (_importClass.equals(User.class)) {
                for (int j = 0; j < l.size(); j++) {
                    User u = (User) l.get(j);
                    u.setModified(true);
                    PublicUserFactory.save(u);
                }
            } else if (_importClass.equals(Company.class)) {
                for (int j = 0; j < l.size(); j++) {
                    Company c = (Company) l.get(j);
                    try {
                        c.setModified(true);
                        CompanyManagerUtil.updateCompany(c);
                    } catch (SystemException e) {
                        throw new DotDataException("Unable to load company", e);
                    }
                }
            } else if (_importClass.equals(Role.class)) {
                for (int j = 0; j < l.size(); j++) {
                    Role r = (Role) l.get(j);
                    try {
                        r.setModified(true);
                        RoleManagerUtil.update(r);
                    } catch (SystemException e) {
                        throw new DotDataException("Unable to load role", e);
                    }
                }
            } else if (_importClass.equals(Layout.class)) {
                for (int j = 0; j < l.size(); j++) {
                    Layout lo = (Layout) l.get(j);
                    try {
                        lo.setModified(true);
                        LayoutManagerUtil.updateLayout(lo);
                    } catch (SystemException e) {
                        throw new DotDataException("Unable to load layout", e);
                    }
                }
            } else if (_importClass.equals(Group.class)) {
                for (int j = 0; j < l.size(); j++) {
                    Group g = (Group) l.get(j);
                    try {
                        g.setModified(true);
                        GroupManagerUtil.updateGroup(g);
                    } catch (SystemException e) {
                        throw new DotDataException("Unable to load group", e);
                    }
                }
            } else {
                _dh = new HibernateUtil(_importClass);
                String id = HibernateUtil.getSession().getSessionFactory().getClassMetadata(_importClass).getIdentifierPropertyName();
                HibernateUtil.getSession().close();
                boolean identityOn = false;
                String cName = _className.substring(_className.lastIndexOf(".") + 1);
                String tableName = "";
                if (classesWithIdentity.contains(cName) && dbType.equals(DbConnectionFactory.MSSQL) && !cName.equalsIgnoreCase("inode")) {
                    tableName = tableNames.get(cName);
                    turnIdentityOnMSSQL(tableName);
                    identityOn = true;
                } else if (dbType.equals(DbConnectionFactory.MSSQL)) {
                    DotConnect dc = new DotConnect();
                    dc.executeStatement("set IDENTITY_INSERT inode on;");
                }
                for (int j = 0; j < l.size(); j++) {
                    Object obj = l.get(j);
                    if (l.get(j) instanceof com.dotmarketing.portlets.contentlet.business.Contentlet && dbType.equals(DbConnectionFactory.MSSQL)) {
                        com.dotmarketing.portlets.contentlet.business.Contentlet contentlet = (com.dotmarketing.portlets.contentlet.business.Contentlet) l.get(j);
                        changeDateForSQLServer(contentlet, out);
                    }
                    if (UtilMethods.isSet(id)) {
                        String prop = BeanUtils.getProperty(obj, id);
                        try {
                            Long myId = new Long(Long.parseLong(prop));
                            HibernateUtil.saveWithPrimaryKey(obj, myId);
                        } catch (Exception e) {
                            HibernateUtil.saveWithPrimaryKey(obj, prop);
                        }
                    } else {
                        HibernateUtil.save(obj);
                    }
                    HibernateUtil.getSession().flush();
                    try {
                        Thread.sleep(3);
                    } catch (InterruptedException e) {
                        Logger.error(this, e.getMessage(), e);
                    }
                }
                if (identityOn) {
                    turnIdentityOffMSSQL(tableName);
                } else if (dbType.equals(DbConnectionFactory.MSSQL)) {
                    turnIdentityOffMSSQL("inode");
                }
            }
        } catch (FileNotFoundException e) {
            Logger.error(this, e.getMessage(), e);
        } catch (ClassNotFoundException e1) {
            Logger.error(this, e1.getMessage(), e1);
        } catch (IllegalAccessException e) {
            Logger.error(this, e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Logger.error(this, e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            Logger.error(this, e.getMessage(), e);
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(), e);
        } finally {
            try {
                if (charStream != null) {
                    charStream.close();
                }
            } catch (IOException e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
    }

    /**
	 * Simple FileNameFilter for XML files
	 * 
	 * @author will
	 * 
	 */
    private class XMLFileNameFilter implements FilenameFilter {

        public boolean accept(File f, String s) {
            if (s.toLowerCase().endsWith(".xml")) {
                return true;
            } else {
                return false;
            }
        }
    }

    private void turnIdentityOnMSSQL(String tableName) throws SQLException {
        DotConnect dc = new DotConnect();
        dc.executeStatement("set identity_insert " + tableName + " on");
    }

    private void turnIdentityOffMSSQL(String tableName) throws SQLException {
        DotConnect dc = new DotConnect();
        dc.executeStatement("set identity_insert " + tableName + " off");
    }

    private void cleanUpDBFromImport() {
        String dbType = DbConnectionFactory.getDBType();
        DotConnect dc = new DotConnect();
        if (dbType.equals(DbConnectionFactory.MSSQL)) {
        } else if (dbType.equals(DbConnectionFactory.ORACLE)) {
            for (String clazz : classesWithIdentity) {
                String tableName = tableNames.get(clazz);
                dc.setSQL("drop sequence " + sequences.get(tableName));
                dc.getResults();
                dc.setSQL("select max(" + tableIDColumns.get(tableName) + ") as maxID from " + tableName);
                ArrayList<HashMap<String, String>> results = dc.getResults();
                int max = dc.getResults().size() == 0 ? 0 : Parameter.getInt(dc.getString("maxID"), 1);
                dc.setSQL("CREATE SEQUENCE " + sequences.get(tableName) + " MINVALUE 1 START WITH " + (max + 100) + " INCREMENT BY 1");
                dc.getResults();
            }
        } else if (dbType.equals(DbConnectionFactory.POSTGRESQL)) {
            for (String clazz : classesWithIdentity) {
                String tableName = tableNames.get(clazz);
                dc.setSQL("select max(" + tableIDColumns.get(tableName) + ") as maxID from " + tableName);
                ArrayList<HashMap<String, String>> results = dc.getResults();
                int max = dc.getResults().size() == 0 ? 0 : Parameter.getInt(dc.getString("maxID"), 1);
                dc.setSQL("alter sequence " + sequences.get(tableName) + " restart with " + (max + 1));
                dc.getResults();
            }
        }
    }

    public String getBackupTempFilePath() {
        return backupTempFilePath;
    }

    public void setBackupTempFilePath(String backupTempFilePath) {
        this.backupTempFilePath = backupTempFilePath;
    }

    /**
	 * 
	 * @param zipFile
	 * @return
	 */
    public boolean validateZipFile(File zipFile) {
        String tempdir = Config.CONTEXT.getRealPath(getBackupTempFilePath());
        try {
            deleteTempFiles();
            File ftempDir = new File(tempdir);
            ftempDir.mkdirs();
            File tempZip = new File(tempdir + File.separator + zipFile.getName());
            tempZip.createNewFile();
            FileChannel ic = new FileInputStream(zipFile).getChannel();
            FileChannel oc = new FileOutputStream(tempZip).getChannel();
            for (long i = 0; i <= ic.size(); i++) {
                ic.transferTo(0, 1000000, oc);
                i = i + 999999;
            }
            ic.close();
            oc.close();
            if (zipFile != null && zipFile.getName().toLowerCase().endsWith(".zip")) {
                ZipFile z = new ZipFile(zipFile);
                ZipUtil.extract(z, new File(Config.CONTEXT.getRealPath(backupTempFilePath)));
            }
            return true;
        } catch (Exception e) {
            Logger.error(this, "Error with file", e);
            return false;
        }
    }

    private boolean validateDate(Date date) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(1753, 01, 01);
        boolean validated = true;
        if (date != null && date.before(calendar.getTime())) {
            validated = false;
        }
        return validated;
    }

    private void changeDateForSQLServer(com.dotmarketing.portlets.contentlet.business.Contentlet contentlet, PrintWriter out) {
        if (!validateDate(contentlet.getDate1())) {
            contentlet.setDate1(new Date());
            out.println("Unsupported data in SQL Server, so changed date to current date for contentlet with inode ");
        }
        if (!validateDate(contentlet.getDate2())) {
            contentlet.setDate2(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate3())) {
            contentlet.setDate3(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate4())) {
            contentlet.setDate4(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate5())) {
            contentlet.setDate5(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate6())) {
            contentlet.setDate6(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate7())) {
            contentlet.setDate7(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate8())) {
            contentlet.setDate8(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate9())) {
            contentlet.setDate9(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate10())) {
            contentlet.setDate10(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate11())) {
            contentlet.setDate11(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate12())) {
            contentlet.setDate12(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate13())) {
            contentlet.setDate13(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate14())) {
            contentlet.setDate14(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate15())) {
            contentlet.setDate15(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate16())) {
            contentlet.setDate16(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate17())) {
            contentlet.setDate17(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate18())) {
            contentlet.setDate18(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate19())) {
            contentlet.setDate19(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate20())) {
            contentlet.setDate20(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate21())) {
            contentlet.setDate21(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate22())) {
            contentlet.setDate22(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate23())) {
            contentlet.setDate23(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate24())) {
            contentlet.setDate24(new Date());
            out.println("Date changed to current date");
        }
        if (!validateDate(contentlet.getDate25())) {
            contentlet.setDate25(new Date());
            out.println("Date changed to current date");
        }
    }
}
