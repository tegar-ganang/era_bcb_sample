package unibg.overencrypt.domain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import unibg.overencrypt.server.ResourcesManager;
import unibg.overencrypt.server.ServerConfiguration;
import unibg.overencrypt.server.managers.RequestManager;
import unibg.overencrypt.utility.TreePaths;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.CopyableResource;
import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockingCollectionResource;
import com.bradmcevoy.http.MakeCollectionableResource;
import com.bradmcevoy.http.MoveableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.PutableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 *
 * @author Flavio Giovarruscio & Riccardo Tribbia
 * @version 1.0
 */
public class OverEncryptedFriendsFolder extends OverEncryptedFriendsResource implements MakeCollectionableResource, PutableResource, CopyableResource, DeletableResource, MoveableResource, PropFindableResource, LockingCollectionResource, GetableResource {

    private static final Logger LOGGER = Logger.getLogger(OverEncryptedFriendsFolder.class);

    private Integer ownerID;

    private Integer loggedUserID;

    private String folderPath;

    public OverEncryptedFriendsFolder(OverEncryptResourceFactory factory, String folderPath, int ownerID, int loggedUserID) {
        super(factory, true);
        this.ownerID = ownerID;
        this.loggedUserID = loggedUserID;
        this.folderPath = folderPath;
        String realPath = ServerConfiguration.getWebDAVrootPath() + "/";
        if (folderPath.equals(Integer.toString(ownerID))) {
            realPath += ownerID;
        } else {
            if (folderPath.startsWith("/")) realPath += folderPath.substring(1); else realPath += folderPath;
        }
        LOGGER.debug("Real shared folder path: " + realPath);
        this.realFile = new File(realPath);
    }

    @Override
    public Resource child(String name) {
        File file = new File(realFile, name);
        if (file.isDirectory()) {
            ResourcesManager resMan = (ResourcesManager) factory;
            HashMap<Integer, ArrayList<String>> permissions = resMan.getUserPermissions(loggedUserID);
            ArrayList<String> pathsAllowed = permissions.get(ownerID);
            for (Iterator<String> iterator = pathsAllowed.iterator(); iterator.hasNext(); ) {
                String path = (String) iterator.next();
                if (path.equals(file.getAbsolutePath().replace(ServerConfiguration.getWebDAVrootPath(), ""))) {
                    return new OverEncryptedFriendsFolder(factory, path, ownerID, loggedUserID);
                }
            }
            return null;
        } else {
            return new OverEncryptedFriendsFile(factory, folderPath + "/" + name, true);
        }
    }

    @Override
    public List<? extends Resource> getChildren() {
        ArrayList<Resource> list = new ArrayList<Resource>();
        ResourcesManager resMan = (ResourcesManager) factory;
        HashMap<Integer, ArrayList<String>> permissions = resMan.getUserPermissions(loggedUserID);
        ArrayList<String> pathsAllowed = permissions.get(ownerID);
        File[] files = realFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (!file.isDirectory()) {
                list.add(new OverEncryptedFriendsFile(factory, folderPath + "/" + file.getName(), false));
            }
        }
        TreePaths tree = new TreePaths("/" + ownerID);
        tree.addArrayOfPaths(pathsAllowed);
        ArrayList<String> pathViewable = tree.getDirectSubfolders(folderPath);
        for (Iterator<String> iterator = pathViewable.iterator(); iterator.hasNext(); ) {
            String path = iterator.next();
            list.add(new OverEncryptedFriendsFolder(factory, path, ownerID, loggedUserID));
        }
        return list;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public String getContentType(String arg0) {
        return "text/html";
    }

    @Override
    public Long getMaxAgeSeconds(Auth arg0) {
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException {
        String subpath = realFile.getCanonicalPath().substring(factory.getRoot().getCanonicalPath().length()).replace('\\', '/');
        String uri = "/" + factory.getContextPath() + subpath;
        XmlWriter w = new XmlWriter(out);
        w.open("html");
        w.open("body");
        w.begin("h1").open().writeText(this.getName()).close();
        w.open("table");
        for (Resource r : getChildren()) {
            w.open("tr");
            w.open("td");
            w.begin("a").writeAtt("href", uri + "/" + r.getName()).open().writeText(r.getName()).close();
            w.close("td");
            w.begin("td").open().writeText(r.getModifiedDate() + "").close();
            w.close("tr");
        }
        w.close("table");
        w.close("body");
        w.close("html");
        w.flush();
    }

    @Override
    public LockToken createAndLock(String arg0, LockTimeout arg1, LockInfo arg2) throws NotAuthorizedException {
        return null;
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
    }

    @Override
    public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException {
        File dest = new File(this.realFile, name);
        if (allowedClient) {
            if (".request".equals(name) || ".tokens".equals(name)) {
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(dest);
                    IOUtils.copy(in, out);
                } finally {
                    IOUtils.closeQuietly(out);
                }
                if (".request".equals(name)) {
                    File request = new File(realFile.getAbsolutePath() + "/" + name);
                    RequestManager.manageRequest(request, null, true);
                    return new OverEncryptedFriendsFile(factory, folderPath + "/.response", allowedClient);
                }
                return new OverEncryptedFriendsFile(factory, folderPath + "/" + name, allowedClient);
            } else {
                return null;
            }
        } else {
            LOGGER.error("User isn't owner of this folder");
            return null;
        }
    }

    @Override
    public CollectionResource createCollection(String arg0) throws NotAuthorizedException, ConflictException {
        return null;
    }
}
