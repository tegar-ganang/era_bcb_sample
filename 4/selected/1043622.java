package com.sitescape.team.ssfs.server.impl;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import javax.activation.FileTypeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.sitescape.team.ic.ICBroker;
import com.sitescape.team.module.admin.AdminModule;
import com.sitescape.team.module.binder.BinderModule;
import com.sitescape.team.module.dashboard.DashboardModule;
import com.sitescape.team.module.definition.DefinitionModule;
import com.sitescape.team.module.file.FileModule;
import com.sitescape.team.module.folder.FolderModule;
import com.sitescape.team.module.ldap.LdapModule;
import com.sitescape.team.module.profile.ProfileModule;
import com.sitescape.team.module.sample.EmployeeModule;
import com.sitescape.team.module.workflow.WorkflowModule;
import com.sitescape.team.module.workspace.WorkspaceModule;
import com.sitescape.team.rss.RssGenerator;
import com.sitescape.team.ssfs.AlreadyExistsException;
import com.sitescape.team.ssfs.CrossContextConstants;
import com.sitescape.team.ssfs.LockException;
import com.sitescape.team.ssfs.NoAccessException;
import com.sitescape.team.ssfs.NoSuchObjectException;
import com.sitescape.team.ssfs.TypeMismatchException;
import com.sitescape.team.ssfs.server.SiteScapeFileSystem;
import com.sitescape.team.util.AllBusinessServicesInjected;

public class SiteScapeFileSystemImpl implements SiteScapeFileSystem, AllBusinessServicesInjected {

    protected final Log logger = LogFactory.getLog(getClass());

    private SiteScapeFileSystemInternal ssfsInt;

    private SiteScapeFileSystemLibrary ssfsLib;

    private EmployeeModule employeeModule;

    private WorkspaceModule workspaceModule;

    private FolderModule folderModule;

    private AdminModule adminModule;

    private ProfileModule profileModule;

    private DefinitionModule definitionModule;

    private WorkflowModule workflowModule;

    private BinderModule binderModule;

    private LdapModule ldapModule;

    private FileModule fileModule;

    private RssGenerator rssGenerator;

    private DashboardModule dashboardModule;

    private ICBroker icBroker;

    public SiteScapeFileSystemImpl() {
        ssfsInt = new SiteScapeFileSystemInternal(this);
        ssfsLib = new SiteScapeFileSystemLibrary(this);
    }

    public RssGenerator getRssGenerator() {
        return rssGenerator;
    }

    public void setRssGenerator(RssGenerator rssGenerator) {
        this.rssGenerator = rssGenerator;
    }

    public void setEmployeeModule(EmployeeModule employeeModule) {
        this.employeeModule = employeeModule;
    }

    public EmployeeModule getEmployeeModule() {
        return employeeModule;
    }

    public void setBinderModule(BinderModule binderModule) {
        this.binderModule = binderModule;
    }

    public BinderModule getBinderModule() {
        return binderModule;
    }

    public void setWorkspaceModule(WorkspaceModule workspaceModule) {
        this.workspaceModule = workspaceModule;
    }

    public WorkspaceModule getWorkspaceModule() {
        return workspaceModule;
    }

    public void setFolderModule(FolderModule folderModule) {
        this.folderModule = folderModule;
    }

    public FolderModule getFolderModule() {
        return folderModule;
    }

    public void setAdminModule(AdminModule adminModule) {
        this.adminModule = adminModule;
    }

    public AdminModule getAdminModule() {
        return adminModule;
    }

    public void setProfileModule(ProfileModule profileModule) {
        this.profileModule = profileModule;
    }

    public ProfileModule getProfileModule() {
        return profileModule;
    }

    public void setDefinitionModule(DefinitionModule definitionModule) {
        this.definitionModule = definitionModule;
    }

    public DefinitionModule getDefinitionModule() {
        return definitionModule;
    }

    public WorkflowModule getWorkflowModule() {
        return workflowModule;
    }

    public void setWorkflowModule(WorkflowModule workflowModule) {
        this.workflowModule = workflowModule;
    }

    public void setLdapModule(LdapModule ldapModule) {
        this.ldapModule = ldapModule;
    }

    public LdapModule getLdapModule() {
        return ldapModule;
    }

    public void setFileModule(FileModule fileModule) {
        this.fileModule = fileModule;
    }

    public FileModule getFileModule() {
        return fileModule;
    }

    public void setDashboardModule(DashboardModule dashboardModule) {
        this.dashboardModule = dashboardModule;
    }

    public DashboardModule getDashboardModule() {
        return dashboardModule;
    }

    public void setMimeTypes(FileTypeMap mimeTypes) {
        ssfsInt.setMimeTypes(mimeTypes);
        ssfsLib.setMimeTypes(mimeTypes);
    }

    public void createResource(Map uri) throws NoAccessException, AlreadyExistsException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.createResource(uri); else ssfsLib.createResource(uri);
    }

    public void setResource(Map uri, InputStream content) throws NoAccessException, NoSuchObjectException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.setResource(uri, content); else ssfsLib.setResource(uri, content);
    }

    public void createAndSetResource(Map uri, InputStream content) throws NoAccessException, AlreadyExistsException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.createAndSetResource(uri, content); else ssfsLib.createAndSetResource(uri, content);
    }

    public void createDirectory(Map uri) throws NoAccessException, AlreadyExistsException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.createDirectory(uri); else ssfsLib.createDirectory(uri);
    }

    public InputStream getResource(Map uri) throws NoAccessException, NoSuchObjectException, TypeMismatchException {
        if (isInternal(uri)) return ssfsInt.getResource(uri); else return ssfsLib.getResource(uri);
    }

    public void removeObject(Map uri) throws NoAccessException, NoSuchObjectException {
        if (isInternal(uri)) ssfsInt.removeObject(uri); else ssfsLib.removeObject(uri);
    }

    public String[] getChildrenNames(Map uri) throws NoAccessException, NoSuchObjectException {
        if (isInternal(uri)) return ssfsInt.getChildrenNames(uri); else return ssfsLib.getChildrenNames(uri);
    }

    public Map getProperties(Map uri) throws NoAccessException, NoSuchObjectException {
        if (isInternal(uri)) return ssfsInt.getProperties(uri); else return ssfsLib.getProperties(uri);
    }

    public void lockResource(Map uri, String lockId, String lockSubject, Date lockExpirationDate, String lockOwnerInfo) throws NoAccessException, NoSuchObjectException, LockException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.lockResource(uri, lockId, lockSubject, lockExpirationDate, lockOwnerInfo); else ssfsLib.lockResource(uri, lockId, lockSubject, lockExpirationDate, lockOwnerInfo);
    }

    public void unlockResource(Map uri, String lockId) throws NoAccessException, NoSuchObjectException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.unlockResource(uri, lockId); else ssfsLib.unlockResource(uri, lockId);
    }

    public void copyObject(Map sourceUri, Map targetUri, boolean overwrite, boolean recursive) throws NoAccessException, NoSuchObjectException, AlreadyExistsException, TypeMismatchException {
        if (isInternal(sourceUri)) ssfsInt.copyObject(sourceUri, targetUri, overwrite, recursive); else ssfsLib.copyObject(sourceUri, targetUri, overwrite, recursive);
    }

    public void moveObject(Map sourceUri, Map targetUri, boolean overwrite) throws NoAccessException, NoSuchObjectException, AlreadyExistsException, TypeMismatchException {
        if (isInternal(sourceUri)) ssfsInt.moveObject(sourceUri, targetUri, overwrite); else ssfsLib.moveObject(sourceUri, targetUri, overwrite);
    }

    private boolean isInternal(Map uri) {
        if (((String) uri.get(CrossContextConstants.URI_TYPE)).equals(CrossContextConstants.URI_TYPE_INTERNAL)) return true; else return false;
    }

    public ICBroker getIcBroker() {
        return icBroker;
    }

    public void setIcBroker(ICBroker icBroker) {
        this.icBroker = icBroker;
    }
}
