package com.dotmarketing.portlets.files.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.db.DotHibernate;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.HostFactory;
import com.dotmarketing.factories.IdentifierFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.files.factories.FileFactory;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.files.struts.FileForm;
import com.dotmarketing.portlets.folders.factories.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.ParamUtil;
import com.liferay.util.servlet.SessionMessages;
import com.liferay.util.servlet.UploadPortletRequest;

/**
 * @author Maria
 */
public class UploadMultipleFilesAction extends DotPortletAction {

    private PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    /**
	 * @param permissionAPI the permissionAPI to set
	 */
    public void setPermissionAPI(PermissionAPI permissionAPIRef) {
        permissionAPI = permissionAPIRef;
    }

    public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {
        String cmd = req.getParameter(Constants.CMD);
        String referer = req.getParameter("referer");
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
        if ((referer != null) && (referer.length() != 0)) {
            referer = URLDecoder.decode(referer, "UTF-8");
        }
        Logger.debug(this, "UploadMultipleFilesAction cmd=" + cmd);
        DotHibernate.startTransaction();
        User user = _getUser(req);
        try {
            Logger.debug(this, "Calling Edit Method");
            _editWebAsset(req, res, config, form, user);
        } catch (Exception e) {
        }
        if ((cmd != null) && cmd.equals(Constants.ADD)) {
            try {
                Logger.debug(this, "Calling Save Method");
                String subcmd = req.getParameter("subcmd");
                _saveWebAsset(req, res, config, form, user, subcmd);
                _sendToReferral(req, res, referer);
            } catch (ActionException ae) {
                _handleException(ae, req);
                if (ae.getMessage().equals("message.file_asset.error.filename.exists")) {
                    _sendToReferral(req, res, referer);
                } else if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
                    SessionMessages.add(httpReq, "error", "message.insufficient.permissions.to.save");
                    _sendToReferral(req, res, referer);
                }
            }
        }
        Logger.debug(this, "Unspecified Action");
        DotHibernate.commitTransaction();
        setForward(req, "portlet.ext.files.upload_multiple");
    }

    public void _editWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user) throws Exception {
        Folder parentFolder = (Folder) com.dotmarketing.factories.InodeFactory.getInode(req.getParameter("parent"), Folder.class);
        FileForm cf = (FileForm) form;
        cf.setSelectedparent(parentFolder.getName());
        cf.setParent(parentFolder.getInode());
        cf.setSelectedparentPath(parentFolder.getPath());
        req.setAttribute("PARENT_FOLDER", parentFolder);
    }

    public void _saveWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String subcmd) throws WebAssetException, Exception {
        long maxsize = 50;
        long maxwidth = 3000;
        long maxheight = 3000;
        long minheight = 10;
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
        try {
            UploadPortletRequest uploadReq = PortalUtil.getUploadPortletRequest(req);
            String parent = ParamUtil.getString(req, "parent");
            int countFiles = ParamUtil.getInteger(req, "countFiles");
            int fileCounter = 0;
            Folder folder = (Folder) InodeFactory.getInode(parent, Folder.class);
            _checkUserPermissions(folder, user, PERMISSION_WRITE);
            String userId = user.getUserId();
            String customMessage = "Some file does not match the filters specified by the folder: ";
            boolean filterError = false;
            for (int k = 0; k < countFiles; k++) {
                File file = new File();
                String title = ParamUtil.getString(req, "title" + k);
                String friendlyName = ParamUtil.getString(req, "friendlyName" + k);
                Date publishDate = new Date();
                String fileName = ParamUtil.getString(req, "fileName" + k);
                fileName = checkMACFileName(fileName);
                if (!FolderFactory.matchFilter(folder, fileName)) {
                    customMessage += fileName + ", ";
                    filterError = true;
                    continue;
                }
                if (fileName.length() > 0) {
                    String mimeType = FileFactory.getMimeType(fileName);
                    String URI = folder.getPath() + fileName;
                    String suffix = UtilMethods.getFileExtension(fileName);
                    file.setTitle(title);
                    file.setFileName(fileName);
                    file.setFriendlyName(friendlyName);
                    file.setPublishDate(publishDate);
                    file.setModUser(userId);
                    InodeFactory.saveInode(file);
                    String filePath = FileFactory.getRealAssetsRootPath();
                    new java.io.File(filePath).mkdir();
                    java.io.File uploadedFile = uploadReq.getFile("uploadedFile" + k);
                    Logger.debug(this, "bytes" + uploadedFile.length());
                    file.setSize((int) uploadedFile.length() - 2);
                    file.setMimeType(mimeType);
                    Host host = HostFactory.getCurrentHost(httpReq);
                    Identifier ident = IdentifierFactory.getIdentifierByURI(URI, host);
                    String message = "";
                    if ((FileFactory.existsFileName(folder, fileName))) {
                        InodeFactory.deleteInode(file);
                        message = "The uploaded file " + fileName + " already exists in this folder";
                        SessionMessages.add(req, "custommessage", message);
                    } else {
                        String fileInodePath = String.valueOf(file.getInode());
                        if (fileInodePath.length() == 1) {
                            fileInodePath = fileInodePath + "0";
                        }
                        fileInodePath = fileInodePath.substring(0, 1) + java.io.File.separator + fileInodePath.substring(1, 2);
                        new java.io.File(filePath + java.io.File.separator + fileInodePath.substring(0, 1)).mkdir();
                        new java.io.File(filePath + java.io.File.separator + fileInodePath).mkdir();
                        java.io.File f = new java.io.File(filePath + java.io.File.separator + fileInodePath + java.io.File.separator + file.getInode() + "." + suffix);
                        java.io.FileOutputStream fout = new java.io.FileOutputStream(f);
                        FileChannel outputChannel = fout.getChannel();
                        FileChannel inputChannel = new java.io.FileInputStream(uploadedFile).getChannel();
                        outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                        outputChannel.force(false);
                        outputChannel.close();
                        inputChannel.close();
                        Logger.debug(this, "SaveFileAction New File in =" + filePath + java.io.File.separator + fileInodePath + java.io.File.separator + file.getInode() + "." + suffix);
                        if (suffix.equals("jpg") || suffix.equals("gif")) {
                            com.dotmarketing.util.Thumbnail.resizeImage(filePath + java.io.File.separator + fileInodePath + java.io.File.separator, String.valueOf(file.getInode()), suffix);
                            int height = javax.imageio.ImageIO.read(f).getHeight();
                            file.setHeight(height);
                            Logger.debug(this, "File height=" + height);
                            int width = javax.imageio.ImageIO.read(f).getWidth();
                            file.setWidth(width);
                            Logger.debug(this, "File width=" + width);
                            long size = (f.length() / 1024);
                            WebAssetFactory.createAsset(file, userId, folder);
                        } else {
                            WebAssetFactory.createAsset(file, userId, folder);
                        }
                        WorkingCache.addToWorkingAssetToCache(file);
                        _setFilePermissions(folder, file, user);
                        fileCounter += 1;
                        if ((subcmd != null) && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH)) {
                            try {
                                PublishFactory.publishAsset(file, httpReq);
                                if (fileCounter > 1) {
                                    SessionMessages.add(req, "message", "message.file_asset.save");
                                } else {
                                    SessionMessages.add(req, "message", "message.fileupload.save");
                                }
                            } catch (WebAssetException wax) {
                                Logger.error(this, wax.getMessage(), wax);
                                SessionMessages.add(req, "error", "message.webasset.published.failed");
                            }
                        }
                    }
                }
            }
            if (filterError) {
                customMessage = customMessage.substring(0, customMessage.lastIndexOf(","));
                SessionMessages.add(req, "custommessage", customMessage);
            }
        } catch (IOException e) {
            Logger.error(this, "Exception saving file: " + e.getMessage());
            throw new ActionException(e.getMessage());
        }
    }

    private void _setFilePermissions(Folder folder, File file, User user) throws DotDataException {
        Identifier id = IdentifierFactory.getIdentifierByInode(file);
        if (id.getInode() == 0) return;
        List perms = permissionAPI.getPermissions(folder);
        Iterator it = perms.iterator();
        while (it.hasNext()) {
            Permission folderPerm = (Permission) it.next();
            Permission permission = new Permission(id.getInode(), folderPerm.getRoleId(), folderPerm.getPermission());
            permissionAPI.save(permission);
        }
    }

    private String checkMACFileName(String fileName) {
        if (UtilMethods.isSet(fileName)) {
            if (fileName.contains("/")) fileName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length());
            if (fileName.contains("\\")) fileName = fileName.substring(fileName.lastIndexOf("\\") + 1, fileName.length());
            fileName = fileName.replaceAll("'", "");
        }
        return fileName;
    }
}
