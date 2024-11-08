package com.dotmarketing.webdav;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.HostFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 *
 */
public class FolderResourceImpl implements FolderResource, LockableResource {

    private DotWebdavHelper dotDavHelper;

    private Folder folder;

    private String path;

    private User user;

    private boolean isAutoPub = false;

    private PermissionAPI perAPI;

    public FolderResourceImpl(Folder folder, String path) {
        this.perAPI = APILocator.getPermissionAPI();
        this.dotDavHelper = new DotWebdavHelper();
        this.isAutoPub = dotDavHelper.isAutoPub(path);
        this.path = path;
        this.folder = folder;
    }

    public CollectionResource createCollection(String newName) {
        if (dotDavHelper.isTempResource(newName)) {
            Host host = HostFactory.getHost(folder.getHostInode());
            dotDavHelper.createTempFolder(File.separator + host.getHostname() + folder.getPath() + File.separator + newName);
            File f = new File(File.separator + host.getHostname() + folder.getPath());
            TempFolderResourceImpl tr = new TempFolderResourceImpl(f.getPath(), f, isAutoPub);
            return tr;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        try {
            Folder f = dotDavHelper.createFolder(path + newName, user);
            FolderResourceImpl fr = new FolderResourceImpl(f, path + newName + "/");
            return fr;
        } catch (IOException e) {
            Logger.error(this, e.getMessage(), e);
            return null;
        }
    }

    public Resource child(String childName) {
        List<Resource> children = dotDavHelper.getChildrenOfFolder(folder, isAutoPub);
        for (Resource resource : children) {
            if (resource instanceof FolderResourceImpl) {
                String name = ((FolderResourceImpl) resource).getFolder().getName();
                if (name.equalsIgnoreCase(childName)) {
                    return resource;
                }
            } else {
                String name = ((FileResourceImpl) resource).getFile().getFileName();
                if (name.equalsIgnoreCase(childName)) {
                    return resource;
                }
            }
        }
        return null;
    }

    public List<? extends Resource> getChildren() {
        List<Resource> children = dotDavHelper.getChildrenOfFolder(folder, isAutoPub);
        return children;
    }

    public Object authenticate(String username, String password) {
        try {
            this.user = dotDavHelper.authorizePrincipal(username, password);
            return user;
        } catch (DotSecurityException e) {
            Logger.error(this, e.getMessage(), e);
            return null;
        }
    }

    public boolean authorise(Request req, Method method, Auth auth) {
        if (auth == null) return false; else if (method.isWrite && isAutoPub) {
            return perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_PUBLISH, user, false);
        } else if (method.isWrite && !isAutoPub) {
            return perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT, user, false);
        } else if (!method.isWrite) {
            return perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, false);
        }
        return false;
    }

    public String checkRedirect(Request req) {
        return null;
    }

    public Long getContentLength() {
        return (long) 0;
    }

    public String getContentType(String arg0) {
        return null;
    }

    public Date getModifiedDate() {
        return folder.getiDate();
    }

    public String getRealm() {
        return null;
    }

    public String getUniqueId() {
        return new Long(folder.getInode()).toString();
    }

    public Resource createNew(String newName, InputStream in, Long length, String contentType) throws IOException {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        if (!dotDavHelper.isTempResource(newName)) {
            dotDavHelper.createResource(path + newName, isAutoPub, user);
            dotDavHelper.setResourceContent(path + newName, in, contentType, null, java.util.Calendar.getInstance().getTime());
            com.dotmarketing.portlets.files.model.File f = dotDavHelper.loadFile(path + newName);
            FileResourceImpl fr = new FileResourceImpl(f, f.getFileName());
            return fr;
        }
        String p = folder.getPath();
        if (!p.endsWith("/")) {
            p = p + "/";
        }
        Host host = HostFactory.getHost(folder.getHostInode());
        File f = dotDavHelper.createTempFile("/" + host.getHostname() + p + newName);
        FileOutputStream fos = new FileOutputStream(f);
        byte[] buf = new byte[256];
        int read = -1;
        while ((read = in.read()) != -1) {
            fos.write(read);
        }
        TempFileResourceImpl tr = new TempFileResourceImpl(f, path + newName, isAutoPub);
        return tr;
    }

    public void copyTo(CollectionResource collRes, String name) {
        if (collRes instanceof TempFolderResourceImpl) {
            TempFolderResourceImpl tr = (TempFolderResourceImpl) collRes;
            try {
                dotDavHelper.copyFolderToTemp(folder, tr.getFolder(), name, isAutoPub);
            } catch (IOException e) {
                Logger.error(this, e.getMessage(), e);
                return;
            }
        } else if (collRes instanceof FolderResourceImpl) {
            FolderResourceImpl fr = (FolderResourceImpl) collRes;
            try {
                String p = fr.getPath();
                if (!p.endsWith("/")) p = p + "/";
                dotDavHelper.copyFolder(this.getPath(), p + name, user, isAutoPub);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
        } else if (collRes instanceof HostResourceImpl) {
            HostResourceImpl hr = (HostResourceImpl) collRes;
            String p = this.getPath();
            if (!p.endsWith("/")) p = p + "/";
            try {
                dotDavHelper.copyFolder(p, "/" + hr.getName() + "/" + name, user, isAutoPub);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
    }

    public void delete() {
        try {
            dotDavHelper.removeObject(path, user);
        } catch (IOException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    public Long getMaxAgeSeconds() {
        return new Long(0);
    }

    public void sendContent(OutputStream arg0, Range arg1, Map<String, String> arg2) throws IOException {
        return;
    }

    public void moveTo(CollectionResource collRes, String name) {
        if (collRes instanceof TempFolderResourceImpl) {
            Logger.debug(this, "Webdav clients wants to move a file from dotcms to a tempory storage but we don't allow this in fear that the tranaction may break and delete a file from dotcms");
            TempFolderResourceImpl tr = (TempFolderResourceImpl) collRes;
            try {
                dotDavHelper.copyFolderToTemp(folder, tr.getFolder(), name, isAutoPub);
            } catch (IOException e) {
                Logger.error(this, e.getMessage(), e);
                return;
            }
        } else if (collRes instanceof FolderResourceImpl) {
            FolderResourceImpl fr = (FolderResourceImpl) collRes;
            if (dotDavHelper.isTempResource(name)) {
                Host host = HostFactory.getHost(fr.getFolder().getHostInode());
                dotDavHelper.createTempFolder(File.separator + host.getHostname() + fr.getFolder().getPath() + name);
                return;
            }
            try {
                String p = fr.getPath();
                if (!p.endsWith("/")) p = p + "/";
                dotDavHelper.move(this.getPath(), p + name, user, isAutoPub);
            } catch (IOException e) {
                Logger.error(this, e.getMessage(), e);
            }
        } else if (collRes instanceof HostResourceImpl) {
            HostResourceImpl hr = (HostResourceImpl) collRes;
            if (dotDavHelper.isTempResource(name)) {
                Host host = hr.getHost();
                dotDavHelper.createTempFolder(File.separator + host.getHostname());
                return;
            }
            try {
                String p = this.getPath();
                if (!p.endsWith("/")) p = p + "/";
                dotDavHelper.move(p, "/" + hr.getName() + "/" + name, user, isAutoPub);
            } catch (IOException e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
    }

    public Date getCreateDate() {
        return folder.getiDate();
    }

    public String getName() {
        return folder.getName();
    }

    public int compareTo(Object o) {
        return 0;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LockToken lock(LockTimeout timeout, LockInfo lockInfo) {
        return dotDavHelper.lock(lockInfo, user, getUniqueId());
    }

    public LockToken refreshLock(String token) {
        return dotDavHelper.refreshLock(token);
    }

    public void unlock(String tokenId) {
        dotDavHelper.unlock(tokenId);
    }
}
