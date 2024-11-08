package edu.psu.its.lionshare.peerserver.wsclient;

import edu.psu.its.lionshare.database.Metadata;
import edu.psu.its.lionshare.database.VirtualDirectory;
import edu.psu.its.lionshare.database.File;
import edu.psu.its.lionshare.database.AccessControl;
import edu.psu.its.lionshare.database.PeerserverHost;
import edu.psu.its.lionshare.security.KeystoreManager;
import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.utils.Options;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.axis.AxisProperties;
import org.apache.axis.components.net.SecureSocketFactory;
import org.apache.axis.attachments.ManagedMemoryDataSource;
import org.apache.axis.encoding.ser.JAFDataHandlerDeserializerFactory;
import org.apache.axis.encoding.ser.JAFDataHandlerSerializerFactory;
import javax.activation.MimetypesFileTypeMap;
import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import java.security.Security;
import java.security.Provider;
import java.io.*;
import java.io.InputStream;
import java.util.Vector;
import java.util.Set;
import java.util.ArrayList;
import java.net.*;
import java.util.*;
import javax.activation.DataSource;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.swing.ProgressMonitorInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * This class handles all of the SOAP message passing that takes place b/w
 * the client and server. It has methods for adding/removing/editing files
 * directories and metadata on a peerserver.
 *
 * @author Lorin Metzger
 *
 */
public class LionShareWSClient {

    private static final Log LOG = LogFactory.getLog(LionShareWSClient.class);

    private static LionShareWSClient instance = null;

    private static final MimetypesFileTypeMap mimes = new MimetypesFileTypeMap();

    static {
        System.setProperty("org.apache.axis.components.net.SecureSocketFactory", "edu.psu.its.lionshare.security.SecureSocketFactoryImpl");
    }

    private LionShareWSClient() {
    }

    public static LionShareWSClient getInstance() {
        if (instance == null) {
            instance = new LionShareWSClient();
        }
        return instance;
    }

    public static synchronized boolean deleteAccessControl(String acid, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/AccessControlWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        QName qn = new QName("urn:AccessControlWS", "AccessControl");
        call.registerTypeMapping(AccessControl.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(AccessControl.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(AccessControl.class, qn));
        Boolean result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("AccessControlWS", "deleteAccessControl"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_STRING, ParameterMode.IN);
            result = (Boolean) call.invoke(new Object[] { acid });
            return result.booleanValue();
        } catch (Exception e) {
            LOG.trace("ERROR adding Metadata ", e);
        }
        return false;
    }

    public static synchronized AccessControl getAccessControl(String acid, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/AccessControlWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        QName qn = new QName("urn:AccessControlWS", "AccessControl");
        call.registerTypeMapping(AccessControl.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(AccessControl.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(AccessControl.class, qn));
        AccessControl result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("AccessControlWS", "getAccessControl"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_STRING, ParameterMode.IN);
            result = (AccessControl) call.invoke(new Object[] { acid });
            return result;
        } catch (Exception e) {
            LOG.trace("ERROR adding Metadata ", e);
        }
        return result;
    }

    public static synchronized boolean updateAccessControl(AccessControl ac, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/AccessControlWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        QName qn = new QName("urn:AccessControlWS", "AccessControl");
        call.registerTypeMapping(AccessControl.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(AccessControl.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(AccessControl.class, qn));
        Boolean result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("AccessControlWS", "updateAccessControl"));
            call.addParameter("arg1", qn, ParameterMode.IN);
            result = (Boolean) call.invoke(new Object[] { ac });
            return result.booleanValue();
        } catch (Exception e) {
            LOG.trace("ERROR adding Metadata ", e);
        }
        return false;
    }

    public static synchronized long uploadAccessControl(AccessControl ac, String file_id, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/AccessControlWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        QName qn = new QName("urn:AccessControlWS", "AccessControl");
        call.registerTypeMapping(AccessControl.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(AccessControl.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(AccessControl.class, qn));
        Long result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("AccessControlWS", "addAccessControl"));
            call.addParameter("arg1", qn, ParameterMode.IN);
            call.addParameter("arg2", org.apache.axis.encoding.XMLType.XSD_STRING, ParameterMode.IN);
            result = (Long) call.invoke(new Object[] { ac, file_id });
            return result.longValue();
        } catch (Exception e) {
            LOG.trace("ERROR adding Metadata ", e);
        }
        return 0l;
    }

    public static synchronized int uploadMetadata(Long fid, Metadata metadata, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/MetadataWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        QName qn = new QName("urn:MetadataWS", "Metadata");
        call.registerTypeMapping(Metadata.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(Metadata.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(Metadata.class, qn));
        Integer result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("MetadataWS", "addMetadata"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_LONG, ParameterMode.IN);
            call.addParameter("arg2", qn, ParameterMode.IN);
            result = (Integer) call.invoke(new Object[] { fid, metadata });
            return result.intValue();
        } catch (Exception e) {
            LOG.trace("ERROR adding Metadata ", e);
        }
        return WSConstants.METADATA_ADD_FAILED;
    }

    public static synchronized long deleteMetadata(long mid, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/MetadataWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        Long result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("MetadataWS", "removeMetadataById"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_LONG, ParameterMode.IN);
            result = (Long) call.invoke(new Object[] { mid });
            return result.longValue();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        return WSConstants.METADATA_DELETE_FAILED;
    }

    public static synchronized Metadata[] downloadMetadata(long fid, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/MetadataWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        QName qn = new QName("urn:MetadataWS", "Metadata");
        call.registerTypeMapping(Metadata.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(Metadata.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(Metadata.class, qn));
        Metadata[] result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("MetadataWS", "getMetadataByFid"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_LONG, ParameterMode.IN);
            result = (Metadata[]) call.invoke(new Object[] { fid });
            return result;
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        return null;
    }

    public static synchronized VirtualDirectory[] getAllVirtualDirectories(PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/VirtualDirectoryWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        QName qn = new QName("urn:VirtualDirectoryWS", "VirtualDirectory");
        call.registerTypeMapping(VirtualDirectory.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(VirtualDirectory.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(VirtualDirectory.class, qn));
        VirtualDirectory[] result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("VirtualDirectoryWS", "getAll"));
            result = (VirtualDirectory[]) call.invoke((Object[]) null);
            if (result != null) {
                for (int i = 0; i < result.length; i++) {
                    result[i].setPeerserverID(host.getId().longValue());
                }
            }
            return result;
        } catch (IncompatibleClassChangeError ice) {
            LOG.trace(" ", ice);
        } catch (Exception e) {
            LOG.trace("Unable to retrieve virtual directories, most likely cause " + "is User was NOT AUTHNICATED", e);
        }
        LOG.debug("Returning null here....   ");
        return null;
    }

    public static synchronized VirtualDirectory addVirtualDirectory(Long pvdid, VirtualDirectory vd, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/VirtualDirectoryWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        QName qn = new QName("urn:VirtualDirectoryWS", "VirtualDirectory");
        call.registerTypeMapping(VirtualDirectory.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(VirtualDirectory.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(VirtualDirectory.class, qn));
        VirtualDirectory result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("VirtualDirectoryWS", "addNew"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_LONG, ParameterMode.IN);
            call.addParameter("arg2", qn, ParameterMode.IN);
            result = (VirtualDirectory) call.invoke(new Object[] { pvdid, vd });
            return result;
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        return null;
    }

    public static synchronized int deleteVirtualDirectory(Long vdid, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/VirtualDirectoryWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        Integer result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("VirtualDirectoryWS", "delete"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_LONG, ParameterMode.IN);
            result = (Integer) call.invoke(new Object[] { (vdid) });
            return result.intValue();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        return WSConstants.DELETE_VIRTUAL_FAILED;
    }

    public static synchronized long[] getStorageSpaceAvailable(PeerserverHost hst) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + hst.getIp() + ":" + hst.getPort() + "/services/VirtualDirectoryWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        long[] result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("VirtualDirectoryWS", "getAvailableSpace"));
            result = (long[]) call.invoke(new Object[] {});
            return result;
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        return null;
    }

    public static synchronized boolean fileHasChanged(String urn, Long fid, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/FileWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        Boolean result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("FileWS", "hasChanged"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_STRING, ParameterMode.IN);
            call.addParameter("arg2", org.apache.axis.encoding.XMLType.XSD_LONG, ParameterMode.IN);
            result = (Boolean) call.invoke(new Object[] { urn, fid });
            return result.booleanValue();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        return false;
    }

    public static synchronized File[] getFiles(long vdid, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/FileWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        QName qn = new QName("urn:FileWS", "File");
        call.registerTypeMapping(File.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(File.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(File.class, qn));
        File[] result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("FileWS", "getAll"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_LONG, ParameterMode.IN);
            result = (File[]) call.invoke(new Object[] { new Long(vdid) });
            return result;
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        return null;
    }

    public static long getFile(long fid, PeerserverHost host, FileOutputStream fos) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/FileWS");
            call = (Call) service.createCall();
            QName qnameAttachment = new QName("urn:FileWS", "DataHandler");
            call.registerTypeMapping(DataHandler.class, qnameAttachment, JAFDataHandlerSerializerFactory.class, JAFDataHandlerDeserializerFactory.class);
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        DataHandler result;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("FileWS", "getFile"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_LONG, ParameterMode.IN);
            result = (DataHandler) call.invoke(new Object[] { new Long(fid) });
            if (result != null) {
                InputStream in_stream = result.getInputStream();
                byte[] bytes = new byte[4096];
                int read = in_stream.read(bytes);
                while (read != -1) {
                    fos.write(bytes, 0, read);
                    read = in_stream.read(bytes);
                }
                fos.flush();
                fos.close();
                in_stream.close();
            }
            return 0l;
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        return 0l;
    }

    public static File updateFile(long vdid, File file, PeerserverHost host, ProgressDataSource ads) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/FileWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        QName qn = new QName("urn:FileWS", "File");
        QName qnstatus = new QName("urn:FileWS", "Status");
        call.registerTypeMapping(File.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(File.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(File.class, qn));
        call.registerTypeMapping(FileStatus.class, qnstatus, new org.apache.axis.encoding.ser.BeanSerializerFactory(FileStatus.class, qnstatus), new org.apache.axis.encoding.ser.BeanDeserializerFactory(FileStatus.class, qnstatus));
        FileStatus result = null;
        try {
            String type = "";
            synchronized (mimes) {
                type = mimes.getContentType(file.getFilename());
            }
            DataSource ds = ads;
            DataHandler dh = new DataHandler(ds);
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("FileWS", "updateFile"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_LONG, ParameterMode.IN);
            call.addParameter("arg2", qn, ParameterMode.IN);
            call.setProperty(Call.ATTACHMENT_ENCAPSULATION_FORMAT, Call.ATTACHMENT_ENCAPSULATION_FORMAT_DIME);
            call.addAttachmentPart(new AttachmentPart(dh));
            result = (FileStatus) call.invoke(new Object[] { new Long(vdid), file });
            ads.setState(result.convertStatus());
            return result.getFile();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        return null;
    }

    public static File addFile(long vdid, File file, PeerserverHost host, ProgressDataSource ads) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/FileWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        QName qn = new QName("urn:FileWS", "File");
        QName qnstatus = new QName("urn:FileWS", "Status");
        call.registerTypeMapping(File.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(File.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(File.class, qn));
        call.registerTypeMapping(FileStatus.class, qnstatus, new org.apache.axis.encoding.ser.BeanSerializerFactory(FileStatus.class, qnstatus), new org.apache.axis.encoding.ser.BeanDeserializerFactory(FileStatus.class, qnstatus));
        FileStatus result = null;
        try {
            String type = "";
            synchronized (mimes) {
                type = mimes.getContentType(file.getFilename());
            }
            DataSource ds = ads;
            DataHandler dh = new DataHandler(ds);
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("FileWS", "addFile"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_LONG, ParameterMode.IN);
            call.addParameter("arg2", qn, ParameterMode.IN);
            call.setProperty(Call.ATTACHMENT_ENCAPSULATION_FORMAT, Call.ATTACHMENT_ENCAPSULATION_FORMAT_DIME);
            call.addAttachmentPart(new AttachmentPart(dh));
            result = (FileStatus) call.invoke(new Object[] { new Long(vdid), file });
            ads.setState(result.convertStatus());
            return result.getFile();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        return null;
    }

    public static synchronized int deleteFile(long fid, PeerserverHost host) {
        URL url = null;
        Service service = new Service();
        Call call = null;
        try {
            url = new URL("https://" + host.getIp() + ":" + host.getPort() + "/services/FileWS");
            call = (Call) service.createCall();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        Integer result = null;
        try {
            call.setTargetEndpointAddress(url);
            call.setOperationName(new QName("FileWS", "deleteFile"));
            call.addParameter("arg1", org.apache.axis.encoding.XMLType.XSD_LONG, ParameterMode.IN);
            result = (Integer) call.invoke(new Object[] { new Long(fid) });
            return result.intValue();
        } catch (Exception e) {
            LOG.trace(" ", e);
        }
        return WSConstants.DELETE_FILE_FAILED;
    }
}
