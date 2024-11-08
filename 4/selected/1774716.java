package edu.psu.its.lionshare.peerserver.ws;

import org.hibernate.Session;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.axis.attachments.Attachments;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import edu.psu.its.lionshare.database.File;
import edu.psu.its.lionshare.database.FileSelect;
import edu.psu.its.lionshare.database.ActualFile;
import edu.psu.its.lionshare.database.ActualFileSelect;
import edu.psu.its.lionshare.database.UrnHistory;
import edu.psu.its.lionshare.database.UrnHistorySelect;
import edu.psu.its.lionshare.database.Metadata;
import edu.psu.its.lionshare.database.MetadataSelect;
import edu.psu.its.lionshare.database.VirtualDirectory;
import edu.psu.its.lionshare.database.VirtualDirectorySelect;
import edu.psu.its.lionshare.database.UserData;
import edu.psu.its.lionshare.database.UserDataSelect;
import edu.psu.its.lionshare.database.AccessControl;
import edu.psu.its.lionshare.database.AccessControlSelect;
import edu.psu.its.lionshare.peerserver.wsclient.Status;
import edu.psu.its.lionshare.peerserver.wsclient.FileStatus;
import edu.psu.its.lionshare.peerserver.RouterService;
import edu.psu.its.lionshare.peerserver.security.UserAuthentication;
import edu.psu.its.lionshare.peerserver.wsclient.WSConstants;
import edu.psu.its.lionshare.peerserver.settings.PeerserverProperties;
import edu.psu.its.lionshare.peerserver.util.StreamDataSource;
import edu.psu.its.lionshare.util.KeyGenerator;
import com.limegroup.gnutella.URN;
import edu.psu.its.lionshare.peerserver.util.UserQuotaHelper;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.activation.DataHandler;
import java.util.List;
import java.util.Iterator;
import java.util.ListIterator;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

public class FileWS {

    private static final Logger logger = Logger.getLogger(FileWS.class);

    public java.io.File getFileStorageDirectory(String principal) {
        java.io.File base = new java.io.File(PeerserverProperties.getInstance().getFileStorageDir());
        if (principal != null && principal.length() > 1) {
            for (int i = 0; i < 2; i++) {
                base = new java.io.File(base, principal.substring(i, i + 1));
            }
            int idx = principal.indexOf("@");
            if (idx != -1) {
                base = new java.io.File(base, principal.substring(0, idx));
            } else {
                base = new java.io.File(base, principal);
            }
        }
        return base;
    }

    /**
   *
   * The id returned from this method should be used to later upload the
   * contents of the file to the peerserver, so it can be linked to its file
   * record.
   *
   */
    public FileStatus addFile(long vdid, File file) {
        Session session = null;
        Session session_2 = null;
        Long afid = null;
        MessageContext msgc = MessageContext.getCurrentContext();
        UserData ud = UserAuthentication.authenticateUser(msgc);
        VirtualDirectory vd = null;
        try {
            logger.info("User: " + ud.getPrincipal() + " is uploading file " + file.getFilename() + " " + file.getUrn());
            vd = VirtualDirectorySelect.getByIDAndUID(new Long(vdid), ud.getId());
            file.setVdid(vdid);
            file.setUserProfile(ud.getId().longValue());
            if (vd == null) {
                logger.info("Invalid Directory id " + vdid + " file will NOT be added");
                return new FileStatus(file, Status.ADD_FILE_FAILED_INVALID_VDID);
            }
            List listVirDirIDs = VirtualDirectorySelect.getAllByUserId(ud.getId().longValue());
            long nTotalVirDirsSize = UserQuotaHelper.calculateUserVirDirsSize(listVirDirIDs);
            if (nTotalVirDirsSize + file.getFilesize() > PeerserverProperties.getInstance().getUserQuotaValue() * 1000000) {
                logger.info("User " + ud.getPrincipal() + " has reached their file storage quota of " + PeerserverProperties.getInstance().getUserQuotaValue() * 1000000 + "MB");
                return new FileStatus(file, Status.ADD_FILE_FAILED_USER_QUOTA_EXCEEDED);
            }
            List vds = VirtualDirectorySelect.getAllByUserId(ud.getId().longValue());
            for (ListIterator vds_iter = vds.listIterator(); vds_iter.hasNext(); ) {
                VirtualDirectory virdir = (VirtualDirectory) vds_iter.next();
                File exists = FileSelect.getFileByUrnAndVdid(file.getUrn(), virdir.getId().longValue());
                if (exists != null) {
                    logger.info("User is attempting to add a file, that is already on the peerserver");
                    exists.setUrn(null);
                    return new FileStatus(exists, Status.ADD_FILE_FAILED_FILE_ALREADY_EXISTS);
                }
            }
            Message msg = msgc.getRequestMessage();
            Attachments attach = msg.getAttachmentsImpl();
            if (attach == null) {
                logger.info("Add File Failed, no file data attached to message");
                return new FileStatus(file, Status.ADD_FILE_FAILED_NO_ATTACHEMENT);
            }
            Iterator iter = attach.getAttachments().iterator();
            if (iter.hasNext()) {
                AttachmentPart part = (AttachmentPart) iter.next();
                DataHandler dh = part.getDataHandler();
                String username = ud.getPrincipal();
                username = username.trim();
                java.io.File storage_dir = getFileStorageDirectory(username);
                storage_dir.mkdirs();
                String filename = file.getFilename();
                int index = 1;
                while (new java.io.File(storage_dir, filename).exists()) {
                    int idx = file.getFilename().lastIndexOf(".");
                    if (idx != -1) {
                        String one = file.getFilename().substring(0, idx);
                        String two = file.getFilename().substring(idx + 1, file.getFilename().length());
                        filename = one + "-" + index + "." + two;
                    } else {
                        filename = file.getFilename() + "_" + index;
                    }
                }
                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(new java.io.File(storage_dir, filename));
                } catch (Exception exp) {
                    logger.info("Error saving file at path " + storage_dir);
                    fos = new FileOutputStream(new java.io.File(filename));
                }
                byte[] bytes = new byte[4096];
                InputStream in_stream = dh.getInputStream();
                int read = in_stream.read(bytes);
                while (read != -1) {
                    fos.write(bytes, 0, read);
                    read = in_stream.read(bytes);
                }
                fos.flush();
                fos.close();
                in_stream.close();
                URN urn = null;
                if (new java.io.File(storage_dir, filename).exists()) {
                    urn = URN.createSHA1Urn(new java.io.File(storage_dir, filename));
                } else if (new java.io.File(filename).exists()) {
                    urn = URN.createSHA1Urn(new java.io.File(filename));
                }
                if (urn != null) {
                    ActualFile af = new ActualFile();
                    if (new java.io.File(storage_dir, filename).exists()) {
                        af.setLocation(new java.io.File(storage_dir, filename).getAbsolutePath());
                    } else if (new java.io.File(filename).exists()) {
                        af.setLocation(new java.io.File(filename).getAbsolutePath());
                    }
                    session = ActualFileSelect.getSession();
                    ActualFileSelect.insert(af, session);
                    ActualFileSelect.closeSession(session);
                    afid = af.getId();
                    logger.info("File uploaded file was save at " + af.getLocation());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ActualFileSelect.closeSession(session);
        }
        session = null;
        try {
            if (afid == null) {
                logger.info("Add file failed, could not insert record into database");
                return new FileStatus(file, Status.ADD_FILE_FAILED_DATABASE_ERROR);
            }
            session = FileSelect.getSession();
            file.setActualfile(afid);
            if (!vd.isPublic_folder()) {
                file.setLocation(new String("http://" + RouterService.getInstance().getIPAddress() + ":" + RouterService.getInstance().getPort() + "/uri-res/N2R?" + file.getUrn() + "&userid=" + vd.getUserID()));
            }
            FileSelect.insert(file, session);
            FileSelect.closeSession(session);
            if (vd.isPublic_folder()) {
                session = FileSelect.getSession();
                file = FileSelect.getById(file.getId().longValue());
                file.setLocation("http://" + RouterService.getInstance().getIPAddress() + ":" + "8080" + "/servlet/PublicFolderBrowser.jsp?userid=" + vd.getUserID() + "&fileid=" + file.getId().toString());
                FileSelect.update(file, session);
                FileSelect.closeSession(session);
            }
        } catch (Exception e) {
            FileSelect.closeSession(session);
            e.printStackTrace();
        }
        RouterService.getInstance().getFileManager().addFileIfShared(file);
        return new FileStatus(file, Status.ADD_FILE_SUCCESSFUL);
    }

    public FileStatus updateFile(long vdid, File file) {
        Session session = null;
        Session session_2 = null;
        Long afid = null;
        MessageContext msgc = MessageContext.getCurrentContext();
        UserData ud = UserAuthentication.authenticateUser(msgc);
        VirtualDirectory vd = null;
        try {
            vd = VirtualDirectorySelect.getByIDAndUID(new Long(vdid), ud.getId());
            file.setVdid(vdid);
            file.setUserProfile(ud.getId().longValue());
            logger.info("User " + ud.getId() + " is updating file " + file.getFilename());
            if (vd == null) {
                logger.info("Could no update file, invalid directory id " + vdid);
                return new FileStatus(file, Status.ADD_FILE_FAILED_INVALID_VDID);
            }
            List listVirDirIDs = VirtualDirectorySelect.getAllByUserId(ud.getId().longValue());
            long nTotalVirDirsSize = UserQuotaHelper.calculateUserVirDirsSize(listVirDirIDs);
            if (nTotalVirDirsSize + file.getFilesize() > PeerserverProperties.getInstance().getUserQuotaValue() * 1000000) {
                logger.info("Adding this file would go over your alloted " + "total file size quota.");
                return new FileStatus(file, Status.ADD_FILE_FAILED_USER_QUOTA_EXCEEDED);
            }
            File exists = FileSelect.getFileByUrn(file.getUrn());
            if (exists != null) {
                logger.info("User is attempting to update to a file that already exists on peerserver");
                exists.setUrn(null);
                return new FileStatus(exists, Status.ADD_FILE_FAILED_FILE_ALREADY_EXISTS);
            }
            Message msg = msgc.getRequestMessage();
            Attachments attach = msg.getAttachmentsImpl();
            if (attach == null) {
                logger.info("No file data was attache to file update message");
                return new FileStatus(file, Status.ADD_FILE_FAILED_NO_ATTACHEMENT);
            }
            Iterator iter = attach.getAttachments().iterator();
            if (iter.hasNext()) {
                AttachmentPart part = (AttachmentPart) iter.next();
                DataHandler dh = part.getDataHandler();
                String username = ud.getPrincipal();
                username = username.trim();
                java.io.File storage_dir = getFileStorageDirectory(username);
                storage_dir.mkdirs();
                String filename = file.getFilename();
                int index = 1;
                while (new java.io.File(storage_dir, filename).exists()) {
                    int idx = file.getFilename().lastIndexOf(".");
                    if (idx != -1) {
                        String one = file.getFilename().substring(0, idx);
                        String two = file.getFilename().substring(idx + 1, file.getFilename().length());
                        filename = one + "-" + index + "." + two;
                    } else {
                        filename = file.getFilename() + "_" + index;
                    }
                }
                FileOutputStream fos = new FileOutputStream(new java.io.File(storage_dir, filename));
                byte[] bytes = new byte[4096];
                InputStream in_stream = dh.getInputStream();
                int read = in_stream.read(bytes);
                while (read != -1) {
                    fos.write(bytes, 0, read);
                    read = in_stream.read(bytes);
                }
                fos.flush();
                fos.close();
                in_stream.close();
                URN urn = URN.createSHA1Urn(new java.io.File(storage_dir, filename));
                ActualFile af = new ActualFile();
                af.setLocation(new java.io.File(storage_dir, filename).getAbsolutePath());
                session = ActualFileSelect.getSession();
                ActualFileSelect.insert(af, session);
                afid = af.getId();
                ActualFileSelect.closeSession(session);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ActualFileSelect.closeSession(session);
        }
        try {
            if (afid == null) {
                return new FileStatus(file, Status.ADD_FILE_FAILED_DATABASE_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (file.getActualfile() != null) {
            ActualFile acfile = ActualFileSelect.getByID(file.getActualfile().longValue());
            try {
                Session af_session = ActualFileSelect.getSession();
                ActualFileSelect.delete(acfile, af_session);
                ActualFileSelect.closeSession(af_session);
            } catch (Exception e2) {
                logger.error("", e2);
            }
        }
        try {
            Session file_session = FileSelect.getSession();
            file.setActualfile(afid);
            FileSelect.update(file, file_session);
            FileSelect.closeSession(file_session);
        } catch (Exception e3) {
            logger.error("", e3);
        }
        return new FileStatus(file, Status.ADD_FILE_SUCCESSFUL);
    }

    public int deleteFile(long fid) {
        Session session = null;
        MessageContext msgc = MessageContext.getCurrentContext();
        UserData ud = UserAuthentication.authenticateUser(msgc);
        try {
            logger.info("User " + ud.getPrincipal() + " is deleting file with id " + fid);
            session = FileSelect.getSession();
            File file = FileSelect.getById(fid);
            VirtualDirectory vd = VirtualDirectorySelect.getByIDAndUID(new Long(file.getVdid()), ud.getId());
            if (vd == null) {
                logger.info("File delete file, invalid directory id " + file.getVdid());
                return WSConstants.DELETE_FILE_FAILED;
            }
            if (file != null) {
                FileSelect.delete(file, session);
                FileSelect.closeSession(session);
            } else {
                return WSConstants.FILE_NOT_FOUND;
            }
            RouterService.getInstance().getFileManager().removeFileIfShared(file);
            if (file.getMetaId() != 0) {
                session = MetadataSelect.getSession();
                Metadata meta = MetadataSelect.getMetaByID(file.getMetaId());
                if (meta != null) {
                    MetadataSelect.delete(meta, session);
                }
                MetadataSelect.closeSession(session);
            }
            if (file.getXacmlId() != 0) {
                session = AccessControlSelect.getSession();
                AccessControl ac = AccessControlSelect.getACByID(file.getXacmlId());
                if (ac != null) {
                    AccessControlSelect.delete(ac, session);
                }
                AccessControlSelect.closeSession(session);
            }
            return WSConstants.DELETE_FILE_SUCCESSFUL;
        } catch (Exception e) {
            MetadataSelect.closeSession(session);
        }
        return WSConstants.DELETE_FILE_FAILED;
    }

    public File[] getAll(long vdid) {
        try {
            List lst = FileSelect.getFilesByVDID(vdid);
            if (lst != null) {
                File[] files = new File[lst.size()];
                for (int i = 0; i < lst.size(); i++) {
                    files[i] = (File) lst.get(i);
                }
                return files;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean hasChanged(String urn, long id) {
        Session session = null;
        try {
            session = UrnHistorySelect.getSession();
            UrnHistory hurn = UrnHistorySelect.getByOldUrnId(urn, id);
            UrnHistorySelect.closeSession(session);
            if (hurn == null) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public DataHandler getFile(long fid) {
        MessageContext msgc = MessageContext.getCurrentContext();
        UserData ud = UserAuthentication.authenticateUser(msgc);
        if (ud != null) {
            File dbfile = FileSelect.getById(fid);
            ActualFile afile = null;
            if (dbfile != null) {
                if (dbfile.getActualfile() != null) {
                    afile = ActualFileSelect.getByID(dbfile.getActualfile().longValue());
                }
            }
            if (afile == null) {
                logger.info("YOU DIDn'T UPDATE YOUR FILE OBJECT ID WAS: " + dbfile.getActualfile());
            }
            if (afile != null && dbfile != null) {
                try {
                    InputStream stream = new FileInputStream(afile.getLocation());
                    StreamDataSource source = new StreamDataSource(stream, dbfile.getFilename(), dbfile.getFilesize());
                    DataHandler dh = new DataHandler(source);
                    return dh;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
