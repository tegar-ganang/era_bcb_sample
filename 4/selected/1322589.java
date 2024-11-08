package org.openi.project;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.filefilter.WildcardFilter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.openi.analysis.Analysis;
import org.openi.analysis.AnalysisGenerator;
import org.openi.analysis.ChangeLog;
import org.openi.analysis.Datasource;
import org.openi.analysis.LogEntry;
import org.openi.application.Application;
import org.openi.feeds.FeedsDatasource;
import org.openi.menu.Menu;
import org.openi.menu.MenuBuilder;
import org.openi.menu.MenuItem;
import org.openi.security.AccessDecisionManager;
import org.openi.security.AccessDecisionManagerImpl;
import org.openi.security.Permission;
import org.openi.security.SecurityDefinition;
import org.openi.stat.r.RFunctionList;
import org.openi.util.Util;
import org.openi.web.controller.admin.ProjectDownloadController;
import org.openi.xml.BeanStorage;
import com.tonbeller.jpivot.olap.model.OlapException;

/**
 * @author plucas
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 */
public class ProjectContext {

    public static final String ALL_USERS = "public";

    private static Logger logger = LogManager.getLogger(ProjectContext.class);

    /**
     * this is the base directory for all projects
     */
    private String baseDirectory;

    private ProjectUser user;

    private Project project;

    private AccessDecisionManager accessDecisionManager = new AccessDecisionManagerImpl(this);

    /**
     * @param project
     * @param baseDirectory
     * @param username
     */
    public ProjectContext(Project project, String baseDirectory, ProjectUser user) {
        this.baseDirectory = baseDirectory;
        this.user = user;
        this.project = project;
    }

    /**
     * @return Returns the baseDirectory.
     */
    public String getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * @param baseDirectory The baseDirectory to set.
     */
    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public String getProjectDirectory() {
        return this.getBaseDirectory() + "/" + this.getProject().getProjectId();
    }

    /**
     * @return Returns the project.
     */
    public Project getProject() {
        return project;
    }

    /**
     * @param project The project to set.
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * @return Returns the ProjectUser.
     */
    public ProjectUser getUser() {
        return user;
    }

    /**
     * @param user The ProjectUser to set.
     */
    public void setUser(ProjectUser user) {
        this.user = user;
    }

    /**
     * returns current user roles
     * @return
     */
    public List getUserRoles() {
        List roles = new ArrayList();
        if (Util.isItemInList(user.getId(), Application.getInstance().getApplicationAdmins())) roles.add("APP_ADMINISTRATOR");
        if (getProject().validateAdmin(user.getId())) roles.add("PROJ_ADMINISTRATOR");
        if (getProject().validateUser(user.getId())) roles.add("PROJ_USER");
        if (getProject().validateReadOnlyUser(user.getId())) roles.add("READ_ONLY");
        Map membermap = this.project.getRoleMembers();
        if (membermap != null) {
            for (Iterator iter = Application.getInstance().getRoleList().iterator(); iter.hasNext(); ) {
                String role = (String) iter.next();
                String members = (String) membermap.get(role);
                if (members != null) {
                    String[] memberarr = members.split(",");
                    for (int i = 0; i < memberarr.length; i++) {
                        if (this.user.getId().equals(memberarr[i])) {
                            roles.add(role);
                            break;
                        }
                    }
                }
            }
        }
        return roles;
    }

    /**
     * returns true if user is member of APP_ADMINISTRATOR
     * @return
     */
    public boolean isAppAdminUser() {
        if (Util.isItemInList(user.getId(), Application.getInstance().getApplicationAdmins())) return true; else return false;
    }

    /**
     * returns true if user is member of PROJ_ADMINISTRATOR
     * @return
     */
    public boolean isProjectAdminUser() {
        if (Util.isItemInList(user.getId(), project.getProjectAdmins())) return true; else return false;
    }

    /**
     * returns datasource object from datasource name
     * @param datasourceName String
     * @return Datasource
     */
    public Object getDatasourceObject(String datasourceName) {
        return project.getDataSourceMap().get(datasourceName);
    }

    /**
     * returns datasource object from datasource name
     * @param datasourceName String
     * @return Datasource
     */
    public Datasource getDatasource(String datasourceName) {
        return (Datasource) project.getDataSourceMap().get(datasourceName);
    }

    /**
     *
     * @param datasourceKey String
     * @param dataSource Datasource
     */
    public void setDatasource(String datasourceKey, Object dataSource) {
        synchronized (this) {
            this.project.getDataSourceMap().put(datasourceKey, dataSource);
        }
    }

    /**
     * returns all available JdbcDatasource
     * @return Map
     */
    public Map getJdbcDatasourceMap() {
        return filterMapByType(project.getDataSourceMap(), JdbcDatasource.class);
    }

    /**
     * returns all available xmla datasource
     * @return Map
     */
    public Map getXmlaDatasourceMap() {
        return filterMapByType(project.getDataSourceMap(), Datasource.class);
    }

    /**
     * returns  jdbc connection properties of specified analysis. returns default one if datasource is not 
     * specified for analysis 
     * @return Properties
     */
    public Properties getJdbcDataSourceProperties(Analysis analysis) {
        JdbcDatasource jdbcSource = getJdbcDatasource(analysis.getDrillthroughDatasource());
        if (jdbcSource == null) {
            throw new NullPointerException("There is no configuration for jdbc datasource in project");
        }
        Properties properties = new Properties();
        properties.put("driver", jdbcSource.getDriverClassName());
        properties.put("url", jdbcSource.getUrl());
        properties.put("user", jdbcSource.getUsername());
        properties.put("password", jdbcSource.getPassword());
        return properties;
    }

    /**
     * returns  jdbc connection properties. returns default one if datasourcename is  not 
     * specified explicity
     * @return Properties
     */
    public Properties getJdbcDataSourceProperties(String dataSourceName) {
        JdbcDatasource jdbcSource = getJdbcDatasource(dataSourceName);
        if (jdbcSource == null) {
            throw new NullPointerException("There is no configuration for jdbc datasource in project");
        }
        Properties properties = new Properties();
        properties.put("driver", jdbcSource.getDriverClassName());
        properties.put("url", jdbcSource.getUrl());
        properties.put("user", jdbcSource.getUsername());
        properties.put("password", jdbcSource.getPassword());
        return properties;
    }

    /**
     * returns jdbc datasource. Returns default one, if dataSourceName is  not specified
     * @param analysis Analysis
     * @return JdbcDatasource
     */
    public JdbcDatasource getJdbcDatasource(String dataSourceName) {
        if (dataSourceName != null && !"".equals(dataSourceName)) {
            Object datasource = getDatasourceObject(dataSourceName);
            if (datasource instanceof JdbcDatasource) return (JdbcDatasource) datasource; else throw new ClassCastException("Not a valid jdbc datasource name");
        } else {
            Map dsMap = getJdbcDatasourceMap();
            if (dsMap != null && dsMap.size() > 0) return (JdbcDatasource) dsMap.values().iterator().next();
        }
        return null;
    }

    /**
     * returns FeedsDatasource object
     * @param datasourceName
     * @return
     */
    public FeedsDatasource getFeedsDatasource(String datasourceName) {
        Object datasource = getDatasourceObject(datasourceName);
        if (datasource != null) {
            if (datasource instanceof FeedsDatasource) return (FeedsDatasource) datasource; else throw new IllegalArgumentException("Invalid datasource specified");
        }
        return null;
    }

    /**
     * returns FeedsDatasource map
     * @param datasourceName
     * @return
     */
    public Map getFeedsDatasourceMap() {
        return filterMapByType(project.getDataSourceMap(), FeedsDatasource.class);
    }

    /**
     * Return map containing objects of specified  map filtered by specified type
     * @param map
     * @param type
     * @return
     */
    private Map filterMapByType(Map map, Class type) {
        Map filteredMap = new HashMap();
        Iterator iter = map.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Object obj = map.get(key);
            if ((obj != null) && type.isInstance(obj)) {
                filteredMap.put(key, obj);
            }
        }
        return filteredMap;
    }

    /**
     * To remove datasource from project
     * @param datasourceName String
     * @return boolean
     */
    public void deleteDatasource(String datasourceName) {
        try {
            this.project.getDataSourceMap().remove(datasourceName);
            this.saveProject();
        } catch (Exception e) {
        }
    }

    /**
     * return JdbcDatasource object of specified template-i.e .jrxml file
     * @param template String
     * @return JdbcDatasource
     */
    public JdbcDatasource getJasperDatasource(String template) {
        if (project.getJdbcMap() == null) {
            return null;
        }
        Iterator iter = project.getJdbcMap().keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (key.equalsIgnoreCase(template)) {
                String datasource = (String) project.getJdbcMap().get(key);
                if ((datasource != null) && !datasource.equals("")) {
                    Object obj = getDatasourceObject(datasource);
                    if ((obj != null) && obj instanceof JdbcDatasource) {
                        return (JdbcDatasource) obj;
                    }
                }
            }
        }
        return null;
    }

    public Menu buildMenu(boolean applyFilterPattern) throws IOException {
        List modules = getProjectModules(false, true);
        List tmp = new LinkedList(modules);
        tmp.add("/" + user.getId().toLowerCase());
        MenuBuilder builder = new MenuBuilder();
        Menu menu = null;
        if (applyFilterPattern) menu = builder.build(this.getProjectDirectory(), tmp, getMenuFilter(), this); else menu = builder.build(this.getProjectDirectory(), tmp, this);
        if (isAppAdminUser()) {
            menu.getChildNodes().add(project.getProjectMenu(user.getLocale()));
        } else {
            menu.getChildNodes().add(getFilteredProjectMenuList(getFilteredProjectMenuList(project.getProjectMenu(user.getLocale()))));
        }
        return menu;
    }

    public String toPrivateMenuUri(String publicMenu) {
        if (publicMenu.startsWith("managefiles.htm")) return "manageprivatefiles.htm";
        if (publicMenu.startsWith("uploadfile.htm")) return "uploadprivatefile.htm";
        if (publicMenu.startsWith("saveanalysis.htm")) return "saveprivateanalysis.htm";
        if (publicMenu.startsWith("deleteanalysis.htm")) return "deleteprivateanalysis.htm";
        return null;
    }

    /**
     * returns menu list containg allowed menu items for current user
     * @param menunodes
     * @return
     */
    private List getFilteredProjectMenuList(List menunodes) {
        List roles = getUserRoles();
        List securityDefs = this.getSecurityDefinitionList();
        List filteredlist = new ArrayList();
        for (Iterator iter = menunodes.iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            if (item instanceof MenuItem) {
                MenuItem menuitem = (MenuItem) item;
                if (checkPermission("/" + menuitem.getUrl())) {
                    filteredlist.add(menuitem.clone());
                } else {
                    String privateUri = toPrivateMenuUri(menuitem.getUrl());
                    if (privateUri != null) {
                        menuitem.setUrl(privateUri);
                        filteredlist.add(menuitem.clone());
                    }
                }
            } else {
                Menu menu = (Menu) item;
                filteredlist.add(getFilteredMenu(menu, roles, securityDefs));
            }
        }
        return filteredlist;
    }

    /**
     * returns cloned instance of specified menu containg childs allowed for the current user
     * @param menu
     * @param roles
     * @param securityDefs     
     * @return
     */
    private Menu getFilteredMenu(Menu menu, List roles, List securityDefs) {
        if (menu == null) return null;
        Menu newMenu = new Menu(menu);
        if (menu.getChildNodes() == null || menu.getChildNodes().size() == 0) return newMenu;
        List childNodes = new ArrayList();
        for (Iterator iter = menu.getChildNodes().iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            if (item instanceof MenuItem) {
                MenuItem menuitem = (MenuItem) item;
                if (this.checkPermission("/" + menuitem.getUrl())) {
                    childNodes.add(menuitem.clone());
                } else {
                    String privateUri = toPrivateMenuUri(menuitem.getUrl());
                    if (privateUri != null) {
                        menuitem.setUrl(privateUri);
                        childNodes.add(menuitem.clone());
                    }
                }
            } else {
                Menu submenu = getFilteredMenu((Menu) item, roles, securityDefs);
                if (submenu != null && submenu.getChildNodes() != null && submenu.getChildNodes().size() != 0) childNodes.add(submenu);
            }
        }
        newMenu.setChildNodes(childNodes);
        return newMenu;
    }

    /**
     * returns file filter for building menu from filesystem
     * @return
     */
    private FileFilter getMenuFilter() {
        List filters = Application.getInstance().getWildCardFilters();
        if (filters != null && filters.size() > 0) {
            final FileFilter filter = new WildcardFilter(filters);
            return new FileFilter() {

                public boolean accept(File pathname) {
                    return !filter.accept(pathname);
                }
            };
        }
        return null;
    }

    /**
     * Determines appropriate base project directory, and which directories to include in menu
     * delegates all menu building to the MenuBuilder
     *
     * @return menu appropriate for the given project context
     *  (project context implies user context)
     *
     * @throws IOException for a bad project directory
     * @deprecated 
     */
    public Menu buildMenu() throws IOException {
        return buildMenu(false);
    }

    /**
     * Method which creates admin menu.
     * Refactored to create admin submenu based on project.xml configuration.
     *
     * @return Menu
     * @deprecated no longer used
     */
    private Menu createAdminMenu() {
        logger.debug("creating admin menu");
        ResourceBundle res = ResourceBundle.getBundle("org.openi.labels", user.getLocale());
        Menu adminMenu = new Menu(res.getString("java_ProjectContext.adminmenu.Administration"));
        Map config = this.getProject().getMenuConfig(user.getLocale());
        if (Util.isItemInList(this.getUser().getId(), Application.getInstance().getApplicationAdmins())) {
            this.addItemList(adminMenu, (List) config.get("appAdminList"));
            this.addItemList(adminMenu, (List) config.get("projectAdminList"));
            this.addItemList(adminMenu, (List) config.get("projectUserList"));
        }
        if (Util.isItemInList(this.getUser().getId(), this.getProject().getProjectAdmins())) {
            this.addItemList(adminMenu, (List) config.get("projectAdminList"));
            this.addItemList(adminMenu, (List) config.get("projectUserList"));
        }
        if (Util.isItemInList(this.getUser().getId(), this.getProject().getProjectUsers())) {
            this.addItemList(adminMenu, (List) config.get("projectUserList"));
        }
        return adminMenu;
    }

    /**
     * 
     * @param menu
     * @param list 
     * @deprecated no longer used
     */
    private void addItemList(Menu menu, List list) {
        Iterator items = list.iterator();
        while (items.hasNext()) {
            MenuItem item = (MenuItem) items.next();
            menu.addMenuItem(item);
        }
    }

    public void savePublic(String relativeUrl, Analysis analysis, String comment) throws IOException {
        logger.debug("trying to save public analysis for: " + this.getProject().getProjectId());
        String filename = ALL_USERS + "/" + relativeUrl;
        save(filename, analysis, comment);
    }

    public void savePersonal(String relativeUrl, Analysis analysis, String comment) throws IOException {
        logger.debug("trying to save personal analysis for user: " + this.getUser().getId().toLowerCase());
        String relativeFilename = this.getUser().getId().toLowerCase() + "/" + relativeUrl;
        save(relativeFilename, analysis, comment);
    }

    public void save(String relativeFilename, Analysis analysis, String comment) throws IOException {
        String filename = this.getProjectDirectory() + "/" + relativeFilename;
        logger.info("saving: " + filename);
        BeanStorage storage = new BeanStorage();
        storage.saveBeanToFile(filename, analysis);
        ChangeLog changelog = this.getChangeLog();
        changelog.addLogEntry(new LogEntry(relativeFilename, new Date(), this.getUser().getId(), comment));
        storage.saveBeanToFile(this.getChangeLogFilename(), changelog);
    }

    /**
     * @param relativeUrl
     */
    public void deleteAnalysis(String relativeUrl) {
        String filename = this.getProjectDirectory() + "/" + relativeUrl;
        logger.debug("trying to delete analysis using filename: " + filename);
        BeanStorage storage = new BeanStorage();
        storage.deleteFile(filename);
    }

    public Analysis restoreAnalysis(String analysisConfigName) throws IOException {
        String filename = this.getProjectDirectory() + "/" + analysisConfigName;
        logger.debug("trying to restore analysis using filename: " + filename);
        BeanStorage storage = new BeanStorage();
        Analysis analysis = (Analysis) storage.restoreBeanFromFile(filename, new Analysis());
        analysis.setAnalysisTitle(MenuBuilder.constructDisplayName(new File(filename), this));
        return analysis;
    }

    /**
     * Save project
     * @throws IOException
     */
    public void saveProject() {
        String filename = this.getProjectDirectory() + "/project.xml";
        logger.info("saving: " + filename);
        BeanStorage storage = new BeanStorage();
        try {
            storage.saveBeanToFile(filename, this.project);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    /**
     * for getChangeLog - need to trap IOException, this is non critical
     *
     * @return
     */
    public ChangeLog getChangeLog() {
        ChangeLog changelog = null;
        String filename = this.getChangeLogFilename();
        logger.debug("trying to restore changelog using filename: " + filename);
        BeanStorage storage = new BeanStorage();
        try {
            changelog = (ChangeLog) storage.restoreBeanFromFile(filename);
        } catch (IOException e) {
            logger.info(e);
            changelog = new ChangeLog();
        }
        return changelog;
    }

    private String getChangeLogFilename() {
        return this.getProjectDirectory() + "/changelog.xml";
    }

    /**
     * Method to check user's permission
     *
     * @param permission Permission
     * @return boolean
     * @deprecated use {@link ProjectContext#checkPermission(String)}
     */
    public boolean hasPermission(Permission permission) {
        boolean granted = false;
        if (Util.isItemInList(user.getId(), Application.getInstance().getApplicationAdmins())) {
            if (Util.isItemInList(permission.toString(), Application.getInstance().getApplicationAdminPermissions())) {
                granted = true;
            }
        } else if (Util.isItemInList(user.getId(), project.getProjectAdmins())) {
            if (Util.isItemInList(permission.toString(), Application.getInstance().getProjectAdminPermissions())) {
                granted = true;
            }
        } else if (Util.isItemInList(user.getId(), project.getProjectUsers())) {
            if (Util.isItemInList(permission.toString(), Application.getInstance().getProjectUserPermissions())) {
                granted = true;
            }
        } else if (Util.isItemInList(user.getId(), project.getProjectReadOnlyUsers())) {
            if (permission == Permission.READ_ONLY) {
                granted = true;
            } else if (Util.isItemInList(permission.toString(), Application.getInstance().getProjectUserPermissions())) {
                granted = true;
            }
        }
        return granted;
    }

    /**
     * Creates a directory below this project context's projectDirectory. Will not attempt
     * to create a directory that already exists.
     * @param relativeDirectoryName
     */
    public synchronized void createDirectory(String relativeDirectoryName) {
        File newDir = new File(this.getProjectDirectory() + "/" + relativeDirectoryName);
        if (!newDir.exists()) {
            try {
                logger.info("creating new subdirectory: " + newDir.getCanonicalPath());
            } catch (IOException e) {
                logger.error(e);
            }
            boolean result = newDir.mkdir();
            logger.debug("directory create result: " + result);
        }
    }

    /**
     * return list of accessible modules(folders) for the project user
     * @return List module list
     */
    public List getProjectModules(boolean includePrivate, boolean includePublic) throws IOException {
        List modules = new LinkedList();
        Iterator iterator = this.project.getModules().iterator();
        while (iterator.hasNext()) {
            Module module = (Module) iterator.next();
            String folderName = module.getFolderName().replace('\\', '/');
            boolean isPrivate = isPathBeneathUserDir(folderName);
            if (!includePublic && !isPrivate) {
                continue;
            }
            if (isModuleAllowed(module)) {
                modules.add(module.getFolderName());
            }
        }
        if (includePrivate) {
            modules.add(this.user.getId());
        }
        return modules;
    }

    /**
     * test whether path is allowed or not based on folder security
     * @param path String
     * @return boolean
     */
    public boolean isPathAllowed(String path) {
        path = extractModulePath(path);
        Iterator iterator = project.getModules().iterator();
        while (iterator.hasNext()) {
            Module module = (Module) iterator.next();
            if (path.startsWith(module.getFolderName())) {
                if (!isModuleAllowed(module)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * test whether path is allowed or not based on folder security and user's permission.
     * returns true if it is allowed
     * @param path String
     * @param permission Permission
     * @return boolean
     * @deprecated no longer used
     */
    public boolean isPathAllowed(String path, Permission permission) {
        if (!isPathAllowed(path)) {
            return false;
        }
        path = extractModulePath(path);
        if (path.toLowerCase().startsWith("public")) {
            if (permission.equals(Permission.DELETE_PUBLIC) || permission.equals(Permission.SAVE_PUBLIC)) {
                if (!hasPermission(permission)) {
                    return false;
                }
            }
        }
        if (path.startsWith(getUser().getId())) {
            if (permission.equals(Permission.DELETE_PRIVATE) || permission.equals(Permission.SAVE_PRIVATE)) {
                if (!hasPermission(permission)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * extracts path from base directory
     * @param path String
     * @return String
     */
    public String extractModulePath(String path) {
        if (path.toLowerCase().startsWith(getProjectDirectory().toLowerCase())) {
            path = path.substring(getProjectDirectory().length());
        }
        return path;
    }

    private boolean isModuleAllowed(Module module) {
        String users = module.getAllowedUsers();
        if ((users == null) || users.trim().equals("")) {
            return true;
        }
        if (module != null) {
            String[] allowedUsers;
            allowedUsers = users.trim().split(",");
            for (int i = 0; i < allowedUsers.length; i++) {
                String user = allowedUsers[i].trim();
                if ((user != null) && !user.equals("") && user.equals(this.user.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * creates specified folder  to the base path . base should be either absolute or
     * relative to the project dir
     * @param base String
     * @param folderName String
     * @return boolean
     */
    public boolean addSubFolder(String base, String folderName) throws Exception {
        String path = constructPathWithProjectDir(base);
        File file = new File(path);
        if (!isPathBeneathProjectDir(path)) {
            throw new SecurityException("Folder creation is not allowed outside project directory. Base path is:" + file.getCanonicalPath());
        }
        if (!file.exists()) {
            throw new java.io.FileNotFoundException("Specified path '" + path + "'  does not exists");
        }
        if (!file.isDirectory()) {
            throw new java.io.IOException("Specified path'" + path + "' is not a directory");
        }
        String newfilepath = file.getPath() + "/" + folderName;
        newfilepath = new File(newfilepath).getCanonicalPath();
        File newfile = new File(newfilepath);
        if (newfile.exists()) {
            throw new java.io.IOException("Specified path '" + newfilepath + "' already exists");
        }
        return newfile.mkdir();
    }

    /**
     * tests whether the path starts with project dir or not
     * @param path String
     * @return boolean
     */
    public boolean isPathBeneathProjectDir(String path) throws Exception {
        File file = new File(path);
        String fullpath = file.getCanonicalPath();
        File prjdir = new File(getProjectDirectory());
        return fullpath.startsWith(prjdir.getCanonicalPath());
    }

    /**
     * renames existing file or dir. path should either be absolute or relative to
     * the project dir
     * @param existing String
     * @param newname String
     * @return boolean
     * @throws Exception
     */
    public boolean renameFileFolder(String existing, String newname) throws Exception {
        String path = constructPathWithProjectDir(existing);
        File file = new File(path);
        if (!isPathBeneathProjectDir(path)) {
            throw new SecurityException("Rename is not allowed outside project directory");
        }
        if (!file.exists()) {
            throw new java.io.FileNotFoundException("Specified path '" + path + "'  does not exists");
        }
        String newpath = file.getParentFile().getPath() + "/" + newname;
        File newfile = new File(newpath);
        if (newfile.exists()) {
            throw new java.io.IOException("Specified path '" + newpath + "' already exists");
        }
        return file.renameTo(newfile);
    }

    /**
     *
     * @param path String
     * @return boolean
     * @throws Exception
     */
    public boolean removeFileFolder(String pathname) throws Exception {
        String path = constructPathWithProjectDir(pathname);
        File file = new File(path);
        if (!file.exists()) {
            throw new java.io.FileNotFoundException("Specified path '" + path + "'  does not exists");
        }
        if (!isPathBeneathProjectDir(path)) {
            throw new SecurityException("Delete is not allowed outside project directory");
        }
        return deleteDir(file);
    }

    private String constructPathWithProjectDir(String path) {
        String actualpath = "";
        if (path.startsWith("/") || path.startsWith("\\")) {
            actualpath = getProjectDirectory() + path;
        } else {
            actualpath = path;
        }
        return actualpath;
    }

    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
     * construct absoulute path with project dir
     * @param path String
     * @return String
     */
    public String resolvePathWithProjectDir(String path) {
        if (!(path.startsWith("\\") || path.startsWith("/"))) {
            path = "/" + path;
        }
        return getProjectDirectory() + path;
    }

    /**
     *
     * @return List
     */
    public List getProjectSubDirs() {
        return DirectoryLister.buildProjectRootSubDirList(getProjectDirectory());
    }

    /**
     * returns all files available under project dir filtered with extension
     * @param extension String
     * @return List
     */
    public List getFilesByExtension(String extension) throws IOException {
        return DirectoryLister.buildFileListByExtension(getProjectDirectory(), getProjectModules(true, true), extension);
    }

    /**
     * Zips the contents of project directory, for this project context.
     *
     * @param zos zos zip output stream, caller permitted to wrap various stream
     * 			in the zip os (file, servlet, etc)
     * @throws IOException
     */
    public void zipProjectContents(ZipOutputStream zos) throws IOException {
        logger.debug("zipping project directory: " + this.getProjectDirectory());
        if (this.checkPermission(ProjectDownloadController.SERVLET_PATH)) {
            zipDir(this.getBaseDirectory(), this.getProjectDirectory(), zos);
        } else {
            throw new SecurityException("user does not have this permission");
        }
    }

    /**
     *
     * @param baseDir absolute file path to base directory, used to trim zip entry name
     * 			so that it does not contain the full path names on the server
     * @param dir2zip absolute file path to the project directory
     * @param zos zip output stream, caller permitted to wrap various stream
     * 			in the zip os (file, servlet, etc)
     * @throws IOException
     */
    private void zipDir(String baseDir, String dir2zip, ZipOutputStream zos) throws IOException {
        File zipDir = new File(dir2zip);
        String[] dirList = zipDir.list();
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                String filePath = f.getPath();
                zipDir(baseDir, filePath, zos);
            } else {
                FileInputStream fis = new FileInputStream(f);
                String basePath = new File(baseDir).getCanonicalPath();
                String entryName = f.getCanonicalPath();
                if (entryName.length() > basePath.length()) entryName = entryName.substring(basePath.length() + 1);
                ZipEntry anEntry = new ZipEntry(entryName);
                zos.putNextEntry(anEntry);
                byte[] readBuffer = new byte[512];
                int bytesIn = 0;
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        }
    }

    /**
     * save text content to the specified file
     * @param file String relative path of the file
     * @param content String text content
     * @throws Exception
     */
    public void saveTextFileContent(String file, String content) throws Exception {
        logger.info("saving text content to file :" + file);
        File fileToSave = new File(resolvePathWithProjectDir(file));
        if (!fileToSave.exists()) throw new java.io.FileNotFoundException("Specified path '" + file + "'  does not exists");
        if (!fileToSave.isFile()) throw new java.io.FileNotFoundException("Specified path '" + file + "'  is not a file");
        if (!isPathBeneathProjectDir(fileToSave.getCanonicalPath())) {
            throw new SecurityException("save is not allowed outside project directory");
        }
        if (!isPathAllowed(file)) {
            throw new SecurityException("user does not have this permission");
        }
        FileOutputStream out = new FileOutputStream(fileToSave);
        out.write(content.getBytes());
        out.close();
    }

    /**
     * returns RFunctionList bean restored from rfunction.xml.
     * returns new instance of RFunctionList if rfunction.xml file not found
     * @return RFunctionList
     * @throws Exception
     */
    public RFunctionList getRFunctionList() throws Exception {
        String xmlFile = this.getProjectDirectory() + "/" + "rfunction.xml";
        RFunctionList rFunctionList;
        if (!new File(xmlFile).exists()) {
            logger.error("R Function file 'rfunction.xml' in project dir is not defined");
            rFunctionList = new RFunctionList();
        } else {
            BeanStorage bean = new BeanStorage();
            rFunctionList = (RFunctionList) bean.restoreBeanFromFile(xmlFile);
        }
        return rFunctionList;
    }

    /**
     * serialize RFunctionList object into rfunction.xml file
     * @param object RFunctionList
     * @throws Exception
     */
    public void saveRFunctionListBean(RFunctionList object) throws Exception {
        BeanStorage bean = new BeanStorage();
        String xmlFile = this.getProjectDirectory() + "/" + "rfunction.xml";
        bean.saveBeanToFile(xmlFile, object);
    }

    /**
     * @param uri
     * @param catalog
     * @param cube
     * @param relativeDir
     */
    public void autogenerate(String datasourceName, String cube, String relativeDir, List measures) throws OlapException, IOException {
        this.createDirectory(relativeDir);
        AnalysisGenerator gen = new AnalysisGenerator();
        Datasource ds = this.getDatasource(datasourceName);
        Map mdx = gen.generateMdxForDimensions(ds, cube, measures);
        Iterator keys = mdx.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Analysis analysis = new Analysis();
            analysis.setAnalysisTitle(key);
            analysis.setChartType(1);
            analysis.setMdxQuery((String) mdx.get(key));
            analysis.setDescription("automatically generated on: " + new java.util.Date());
            analysis.setDataSourceName(datasourceName);
            String filename = relativeDir + "/" + key + ".analysis";
            savePublic(filename.replaceAll(" ", "_"), analysis, null);
        }
    }

    /**
     * 
     * @return a non-null list of menu names, under "Auto Generated"
     */
    public List getAutoGeneratedMenuNames() {
        List displayNames = new LinkedList();
        try {
            Menu subMenu = this.buildMenu().getSubMenu("Auto Generated");
            if (subMenu != null) {
                displayNames = subMenu.getMenuDisplayNames();
            }
        } catch (IOException e) {
            logger.error(e);
        }
        return displayNames;
    }

    /**
     * returns resource string if id for currect user locale
     * from the standard resource bundle
     * @param resourceId
     * @return
     */
    public String getResourceString(String resourceId) {
        return getResourceString(resourceId, "org.openi.labels");
    }

    /**
     * returns resource string if id for currect user locale
     * from the customizable resource bundle
     * @param resourceId
     * @return
     */
    public String getCustomResourceString(String resourceId) {
        String result = resourceId;
        if (project.isCustomText()) result = getResourceString(resourceId, "usr." + this.project.getProjectId());
        return result;
    }

    private String getResourceString(String resourceId, String bundleName) {
        String result = "";
        try {
            ResourceBundle bund = ResourceBundle.getBundle(bundleName, getUser().getLocale());
            result = bund.getString(resourceId);
        } catch (Exception e) {
            result = resourceId;
        }
        return result;
    }

    /**
     * Checks whether the the resource is allowed for current user or not
     * @param resource resource  path. e.g 'deleteanalysis.htm'
     * @return true if allowed
     */
    public boolean checkPermission(String resource) {
        return checkPermission(resource, null);
    }

    /**
     * Checks whether the the resource is allowed for current user or not
     * @param resource resource  path. e.g 'deleteanalysis.htm'
     * @param params Request Parameter
     * @return true if allowed
     */
    public boolean checkPermission(String resource, Map params) {
        boolean granted = false;
        if (this.isAppAdminUser()) granted = true; else granted = this.accessDecisionManager.decide(resource, params, this.getSecurityDefinitionList(), this.getUserRoles());
        return granted;
    }

    /**
     * returns SecurityDefinition entries defined in Application. For backward 
     * compatibility, it returns SecurityDefinition, created from old permission list
     * @return 
     */
    public List getSecurityDefinitionList() {
        List secdefs = Application.getInstance().getSecurityConfig();
        if (secdefs == null) {
            secdefs = buildSecurityDefsFromOldPermission();
            Application.getInstance().setSecurityConfig(secdefs);
        }
        return secdefs;
    }

    private List buildSecurityDefsFromOldPermission() {
        List secdefs = new ArrayList();
        String prjadmins = Application.getInstance().getProjectAdminPermissions();
        SecurityDefinition def = new SecurityDefinition();
        Map defsMapByPath = new LinkedHashMap();
        def.setResourcePath("/editapplication.htm*");
        def.setAllowedRoles("APP_ADMINISTRATOR");
        secdefs.add(def);
        defsMapByPath.put(def.getResourcePath(), def);
        if (prjadmins != null && !prjadmins.equals("")) {
            String permsns[] = prjadmins.split(",");
            def = new SecurityDefinition();
            def.setResourcePath("/editproject.htm*");
            def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR");
            defsMapByPath.put(def.getResourcePath(), def);
            secdefs.add(def);
            for (Iterator iter = permissionToResource(permsns).iterator(); iter.hasNext(); ) {
                String path = (String) iter.next();
                if (defsMapByPath.containsKey(path)) continue;
                def = new SecurityDefinition();
                def.setResourcePath(path);
                def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR");
                secdefs.add(def);
                defsMapByPath.put(def.getResourcePath(), def);
            }
        }
        String prjusers = Application.getInstance().getProjectUserPermissions();
        if (prjusers != null && !prjusers.equals("")) {
            String permsns[] = prjusers.split(",");
            for (Iterator iter = permissionToResource(permsns).iterator(); iter.hasNext(); ) {
                String path = (String) iter.next();
                if (defsMapByPath.containsKey(path)) {
                    def = (SecurityDefinition) defsMapByPath.get(path);
                } else {
                    def = new SecurityDefinition();
                    secdefs.add(def);
                }
                def.setResourcePath(path);
                def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR,PROJ_USER");
            }
        }
        SecurityDefinition alldef = new SecurityDefinition();
        addSecDefForOtherResources(secdefs);
        alldef.setResourcePath("/**");
        alldef.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR,PROJ_USER,READ_ONLY");
        secdefs.add(alldef);
        return secdefs;
    }

    private void addSecDefForOtherResources(List secDefs) {
        SecurityDefinition def;
        def = new SecurityDefinition();
        def.setResourcePath("/menuconfig.htm*");
        def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR");
        secDefs.add(def);
        def = new SecurityDefinition();
        def.setResourcePath("/project_download.zip*");
        def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR");
        secDefs.add(def);
        def = new SecurityDefinition();
        def.setResourcePath("/managefeeds.htm*");
        def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR,PROJ_USER");
        secDefs.add(def);
        def = new SecurityDefinition();
        def.setResourcePath("/dashboard.htm*");
        def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR,PROJ_USER");
        secDefs.add(def);
        def = new SecurityDefinition();
        def.setResourcePath("/manageoverview.htm*");
        def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR");
        secDefs.add(def);
        def = new SecurityDefinition();
        def.setResourcePath("/liveeda.htm*");
        def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR");
        secDefs.add(def);
        def = new SecurityDefinition();
        def.setResourcePath("/projectupload.htm*");
        def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR");
        secDefs.add(def);
        def = new SecurityDefinition();
        def.setResourcePath("/segment.htm*");
        def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR,PROJ_USER");
        secDefs.add(def);
        def = new SecurityDefinition();
        def.setResourcePath("/segment_include.htm*");
        def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR,PROJ_USER");
        secDefs.add(def);
        def = new SecurityDefinition();
        def.setResourcePath("/managerfunction.htm*");
        def.setAllowedRoles("APP_ADMINISTRATOR,PROJ_ADMINISTRATOR");
        secDefs.add(def);
    }

    private List permissionToResource(String[] permissions) {
        List resourceList = new ArrayList();
        for (int i = 0; i < permissions.length; i++) {
            String resource = getResourceBelongsToPermission(permissions[i]);
            if (resource != null && !resourceList.contains(resource)) resourceList.add(resource);
        }
        return resourceList;
    }

    private String getResourceBelongsToPermission(String permissionName) {
        String resourcename = null;
        Permission permission = Permission.getPermissionByName(permissionName);
        if (permission != null) {
            if (permission == Permission.CONFIGURE_DATASOURCE) resourcename = "/datasource.htm*"; else if (permission == Permission.CREATE_NEW) resourcename = "/newanalysis.htm*"; else if (permission == Permission.UPLOAD_FILE) resourcename = "/uploadfile.htm*"; else if (permission == Permission.MANAGE_FILES) resourcename = "/managefiles.htm*"; else if (permission == Permission.AUTOGENERATE) resourcename = "/autogenerate.htm*"; else if (permission == Permission.SAVE_PRIVATE || permission == Permission.SAVE_PRIVATE) resourcename = "/saveanalysis.htm*"; else if (permission == Permission.DELETE_PRIVATE || permission == Permission.DELETE_PUBLIC) resourcename = "/deleteanalysis.htm*";
        }
        return resourcename;
    }

    /**
	 * checks whether the specified path belongs for private contents or not
	 * @param path
	 * @return
	 * @throws Exception
	 */
    public boolean isPathBeneathUserDir(String path) throws IOException {
        path = this.resolvePathWithProjectDir(path);
        path = new File(path).getCanonicalPath();
        String userPath = this.getProjectDirectory() + "/" + this.getUser().getId();
        userPath = new File(userPath).getCanonicalPath();
        if (!path.startsWith(userPath)) {
            return false;
        } else {
            return true;
        }
    }

    /**
	 * returns map of security definition with value as a map of role selection 
	 * @return Map of SecurirityDefinition
	 */
    public Map getSecurityDefinitionSelectionMap() {
        Map securityMap = new LinkedHashMap();
        List availableRoles = Application.getInstance().getAllRoles();
        for (Iterator iter = getSecurityDefinitionList().iterator(); iter.hasNext(); ) {
            SecurityDefinition def = (SecurityDefinition) iter.next();
            String allowedRoles = def.getAllowedRoles();
            if (allowedRoles == null) allowedRoles = "";
            Map roleMap = new LinkedHashMap();
            for (Iterator roleiter = availableRoles.iterator(); roleiter.hasNext(); ) {
                String role = (String) roleiter.next();
                if (allowedRoles.indexOf(role) != -1 || "APP_ADMINISTRATOR".equals(role)) {
                    roleMap.put(role, Boolean.TRUE);
                } else {
                    roleMap.put(role, Boolean.FALSE);
                }
            }
            securityMap.put(def, roleMap);
        }
        return securityMap;
    }

    /**
	 * checks whether specified path belongs to project modules 
	 * @param path
	 * @return true if yes, otherwise false
	 */
    public boolean isPathBeneathPublicDir(String path) throws Exception {
        boolean exists = false;
        String fullPath = new File(path).getCanonicalPath();
        for (Iterator iter = this.getProject().getModules().iterator(); iter.hasNext(); ) {
            String modulePath = resolvePathWithProjectDir((String) iter.next());
            if (fullPath.startsWith(modulePath)) {
                exists = true;
                break;
            }
        }
        return exists;
    }
}
