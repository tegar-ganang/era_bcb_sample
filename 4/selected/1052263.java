package com.dotmarketing.webdav;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * @author jasontesser
 *
 */
public class TempFolderResourceImpl implements FolderResource, LockableResource {

    private DotWebdavHelper dotDavHelper;

    private File folder;

    private String path;

    private boolean isAutoPub = false;

    private User user;

    public TempFolderResourceImpl(String path, File folder, boolean isAutoPub) {
        dotDavHelper = new DotWebdavHelper();
        this.isAutoPub = isAutoPub;
        this.path = path;
        this.folder = folder;
    }

    public CollectionResource createCollection(String newName) {
        dotDavHelper.createTempFolder(folder.getPath() + File.separator + newName);
        File f = new File(folder.getPath() + File.separator + newName);
        TempFolderResourceImpl tr = new TempFolderResourceImpl(folder.getPath() + File.separator + newName, f, isAutoPub);
        return tr;
    }

    public Resource child(String childName) {
        List<? extends Resource> children = getChildren();
        for (Resource resource : children) {
            if (resource instanceof TempFolderResourceImpl) {
                String name = ((TempFolderResourceImpl) resource).getFolder().getName();
                if (name.equalsIgnoreCase(childName)) {
                    return resource;
                }
            } else {
                String name = ((TempFileResourceImpl) resource).getFile().getName();
                if (name.equalsIgnoreCase(childName)) {
                    return resource;
                }
            }
        }
        return null;
    }

    public List<? extends Resource> getChildren() {
        File[] children = folder.listFiles();
        List<Resource> result = new ArrayList<Resource>();
        for (File file : children) {
            if (file.isDirectory()) {
                TempFolderResourceImpl tr = new TempFolderResourceImpl(file.getPath(), file, isAutoPub);
                result.add(tr);
            } else {
                TempFileResourceImpl tr = new TempFileResourceImpl(file, file.getPath(), isAutoPub);
                result.add(tr);
            }
        }
        return result;
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
        if (auth == null) return false; else {
            return true;
        }
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
        return new Date(folder.lastModified());
    }

    public String getRealm() {
        return null;
    }

    public String getUniqueId() {
        return folder.hashCode() + "";
    }

    public Resource createNew(String newName, InputStream in, Long length, String contentType) throws IOException {
        File f = new File(folder.getPath() + File.separator + newName);
        if (!f.exists()) {
            String p = f.getPath().substring(0, f.getPath().lastIndexOf(File.separator));
            File fe = new File(p);
            fe.mkdirs();
            f.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(f);
        byte[] buf = new byte[256];
        int read = -1;
        while ((read = in.read()) != -1) {
            fos.write(read);
        }
        TempFileResourceImpl tr = new TempFileResourceImpl(f, f.getPath(), isAutoPub);
        return tr;
    }

    public void copyTo(CollectionResource collRes, String name) {
        if (collRes instanceof TempFolderResourceImpl) {
            TempFolderResourceImpl tr = (TempFolderResourceImpl) collRes;
            try {
                String p = tr.getFolder().getPath();
                File dest = new File(tr.getFolder().getPath() + File.separator + name);
                FileUtil.copyDirectory(folder, dest);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
                return;
            }
        } else if (collRes instanceof FolderResourceImpl) {
            FolderResourceImpl fr = (FolderResourceImpl) collRes;
            String p = fr.getPath();
            if (!p.endsWith("/")) p = p + "/";
            try {
                dotDavHelper.copyTempDirToStorage(folder, p + name, user, isAutoPub);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
    }

    public void delete() {
        folder.delete();
    }

    public Long getMaxAgeSeconds() {
        return new Long(60);
    }

    public void sendContent(OutputStream arg0, Range arg1, Map<String, String> arg2) throws IOException {
        return;
    }

    public void moveTo(CollectionResource collRes, String name) {
        if (collRes instanceof TempFolderResourceImpl) {
            TempFolderResourceImpl tr = (TempFolderResourceImpl) collRes;
            try {
                File dest = new File(tr.getFolder().getPath() + File.separator + name);
                FileUtil.copyDirectory(folder, dest);
                FileUtil.deltree(folder, true);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
                return;
            }
        } else if (collRes instanceof FolderResourceImpl) {
            FolderResourceImpl fr = (FolderResourceImpl) collRes;
            String p = fr.getPath();
            if (!p.endsWith("/")) p = p + "/";
            try {
                dotDavHelper.copyTempDirToStorage(folder, p + name, user, isAutoPub);
                FileUtil.deltree(folder, true);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
        } else if (collRes instanceof HostResourceImpl) {
            HostResourceImpl hr = (HostResourceImpl) collRes;
            try {
                dotDavHelper.copyTempDirToStorage(folder, "/" + hr.getHost().getHostname() + "/" + name, user, isAutoPub);
                FileUtil.deltree(folder, true);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
    }

    public Date getCreateDate() {
        Date dt = new Date(folder.lastModified());
        return dt;
    }

    public String getName() {
        return folder.getName();
    }

    public int compareTo(Object o) {
        return 0;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public File getFolder() {
        return folder;
    }

    public void setFolder(File folder) {
        this.folder = folder;
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
