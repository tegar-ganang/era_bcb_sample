package net.emotivecloud.vrmm.dm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.s3.FileSystemStore;
import org.apache.hadoop.fs.s3.S3Credentials;
import org.apache.hadoop.fs.s3.S3FileSystem;
import org.apache.hadoop.fs.s3native.NativeS3FileSystem;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.CanonicalGrantee;
import org.jets3t.service.acl.EmailAddressGrantee;
import org.jets3t.service.acl.GranteeInterface;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import es.bsc.brein.jsdl.DataStaging;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

public class DataManagement {

    private Log log = LogFactory.getLog(DataManagement.class);

    private String scriptsPath = "/home";

    private String localLocation = "/aplic/brein/data";

    private String remoteLocation = "/data";

    private HashMap<String, String> protocols;

    private HashMap<String, String> params;

    public DataManagement() {
        this.protocols = new HashMap<String, String>();
        this.protocols.put("sftp", "org.apache.hadoop.fs.sftp.SFTPFileSystem");
        this.protocols.put("file", "org.apache.hadoop.fs.LocalFileSystem");
        this.params = new HashMap<String, String>();
        URL url = DataManagement.class.getResource("DataManagement.class");
        scriptsPath = url.toString();
        try {
            if (scriptsPath.startsWith("jar:")) {
                scriptsPath = scriptsPath.replaceFirst("jar:", "");
                if (scriptsPath.startsWith("file:")) scriptsPath = scriptsPath.replaceFirst("file:", "");
                String jarPath = scriptsPath.substring(0, scriptsPath.indexOf("!"));
                unJar(jarPath, "bin/stagein.sh");
                scriptsPath = unJar(jarPath, "bin/stageout.sh");
                scriptsPath += "bin";
            } else if (scriptsPath.startsWith("file:")) {
                scriptsPath = scriptsPath.replaceFirst("file:", "");
                scriptsPath = scriptsPath.substring(0, scriptsPath.lastIndexOf("classes/"));
                scriptsPath += "bin";
            }
        } catch (Exception e) {
            log.error("Scripts cannot be extracted");
        }
    }

    public void addProtocol(String protocol, String impl) {
        this.protocols.put(protocol, impl);
    }

    public String getLocalLocation() {
        return localLocation;
    }

    public void setLocalLocation(String localLocation) {
        this.localLocation = localLocation;
    }

    public String getRemoteLocation() {
        return remoteLocation;
    }

    public void setRemoteLocation(String remoteLocation) {
        this.remoteLocation = remoteLocation;
    }

    public void setParameter(String key, String value) {
        this.params.put(key, value);
    }

    /**
	 * Creates an image containing all the files of a given job.
	 * @param jobID Identifier of the job.
	 */
    public void createImg(String jobID, long size) {
        log.info("Stage-in of job with Job ID \"" + jobID + "\"");
        try {
            Process stagein = Runtime.getRuntime().exec("bash " + scriptsPath + "/stagein.sh --path " + localLocation + " --size " + size + " " + jobID);
            stagein.waitFor();
            InputStream in = stagein.getInputStream();
            try {
                String msg = IOUtils.toString(in);
                log.info(msg);
            } finally {
                IOUtils.closeQuietly(in);
            }
            if (stagein.exitValue() != 0) {
                InputStream er = stagein.getErrorStream();
                try {
                    String msg = IOUtils.toString(er);
                    log.error(msg);
                } finally {
                    IOUtils.closeQuietly(er);
                }
            }
        } catch (Exception e) {
            log.error("Performing Stage In");
        }
    }

    /**
	 * Extracts and image containing all the files of a given job and puts it in the repository path.
	 * @param jobID Identifier of the job.
	 */
    public void extractImg(String jobID) {
        log.info("Stage-out of job with Job ID \"" + jobID + "\"");
        try {
            Process stageout = Runtime.getRuntime().exec("bash " + scriptsPath + "/stageout.sh --path " + localLocation + " " + jobID);
            stageout.waitFor();
            InputStream in = stageout.getInputStream();
            try {
                String msg = IOUtils.toString(in);
                log.info(msg);
            } finally {
                IOUtils.closeQuietly(in);
            }
            if (stageout.exitValue() != 0) {
                InputStream er = stageout.getErrorStream();
                try {
                    String msg = IOUtils.toString(er);
                    log.error(msg);
                } finally {
                    IOUtils.closeQuietly(er);
                }
            }
        } catch (Exception e) {
            log.error("Performing Stage-out for Job \"" + jobID + "\"");
        }
    }

    /**
	 * Downloads a set of files from a remote location.
	 * @param jobId Identifier of the job whose own the files.
	 * @param files The list of files that will be downloaded.
	 * @return Where it is downloaded.
	 */
    public String downloadFiles(String jobId, List<DataStaging> files) {
        for (DataStaging file : files) downloadFile(jobId, file);
        return localLocation + "/" + jobId;
    }

    /**
     * Download a files from a remote location.
     * @param jobId Identifier of the job whose own the files.
     * @param files The file that will be downloaded.
     * @return Where it is downloaded.
     */
    public String downloadFile(String jobId, DataStaging file) {
        try {
            URI fileURI = file.getSourceURI();
            if (fileURI == null) {
                fileURI = file.getTargetURI();
            }
            if (fileURI.getScheme() == null) {
                log.info("File " + fileURI.toString() + " has no scheme specified. Using local (file://) scheme by default.");
                if (fileURI.toString().startsWith("/")) fileURI = new URI("file://" + fileURI.toString());
            }
            Path src = new Path(fileURI.toString());
            Path dst = null;
            if (jobId != null) {
                dst = new Path(localLocation + "/" + jobId);
            } else {
                dst = new Path(localLocation);
            }
            if (fileURI.getScheme().equals("http")) {
                log.info(src + " -> " + dst);
                downloadHTTPFile(src.toString(), dst.toString());
            } else {
                if (fileURI.getScheme().equals("s3")) {
                    fileURI = URI.create(fileURI.toString().replaceFirst("s3", "s3n"));
                }
                Configuration conf = new Configuration();
                for (String protocol : this.protocols.keySet()) {
                    conf.set("fs." + protocol + ".impl", this.protocols.get(protocol));
                }
                if (file.getIssuer() != null) {
                    conf.set("fs.bss.issuer", file.getIssuer());
                }
                if (file.getIdentifier() != null) {
                    conf.set("fs.bss.identifier", file.getIdentifier());
                }
                for (String k : params.keySet()) {
                    conf.set(k, params.get(k));
                }
                FileSystem fs = FileSystem.get(fileURI, conf);
                log.info(src + " -> " + dst);
                fs.copyToLocalFile(src, dst);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jobId == null) {
            return localLocation;
        } else {
            return localLocation + "/" + jobId;
        }
    }

    /**
         * Downloads a file from a HTTP location to the local hard disk. Uses the
         * same file name as in the remote location.
         * @param srcFile The URL of the file to be downloaded.
         * @param dstPath The path where the file is going to be stored (excluding the file name).
         */
    private void downloadHTTPFile(String srcFile, String dstPath) {
        URL u;
        InputStream is = null;
        DataInputStream dis;
        String filename = srcFile.substring(srcFile.lastIndexOf("/") + 1, srcFile.length());
        try {
            u = new URL(srcFile);
            is = u.openStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            File f = new File(dstPath);
            f.mkdirs();
            f = new File(dstPath + "/" + filename);
            FileOutputStream fos = new FileOutputStream(f);
            byte[] tmpbuffer = new byte[1024];
            int readbytes = bis.read(tmpbuffer);
            while (readbytes > 0) {
                fos.write(tmpbuffer, 0, readbytes);
                readbytes = bis.read(tmpbuffer);
            }
            fos.close();
        } catch (MalformedURLException mue) {
            System.out.println("Ouch - a MalformedURLException happened.");
            mue.printStackTrace();
            System.exit(1);
        } catch (IOException ioe) {
            System.out.println("Oops- an IOException happened.");
            ioe.printStackTrace();
            System.exit(1);
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
                log.error("DataManagement.java: Error closing Input Stream.");
            }
        }
    }

    public String uploadFiles(String jobId, List<DataStaging> files) {
        for (DataStaging file : files) uploadFile(jobId, file);
        return "fileId";
    }

    /**
	 * Upload a files to a remote location.
	 * @param jobId Identifier of the job whose own the file.
	 * @param files The file that will be downloaded.
	 * @return Where it is downloaded.
	 */
    public String uploadFile(String jobId, DataStaging file) {
        try {
            URI fileURI = file.getTargetURI();
            if (fileURI == null) fileURI = file.getSourceURI();
            if (fileURI.getScheme().equals("s3")) fileURI = URI.create(fileURI.toString().replaceFirst("s3", "s3n"));
            Configuration conf = new Configuration();
            for (String protocol : this.protocols.keySet()) conf.set("fs." + protocol + ".impl", this.protocols.get(protocol));
            if (file.getIssuer() != null) conf.set("fs.bss.issuer", file.getIssuer());
            if (file.getIdentifier() != null) conf.set("fs.bss.identifier", file.getIdentifier());
            for (String k : params.keySet()) conf.set(k, params.get(k));
            FileSystem fs = FileSystem.get(fileURI, conf);
            Path src = new Path(localLocation + "/" + jobId + "/" + file.getFileName());
            Path dst = new Path(fileURI.toString());
            log.info(src + " -> " + dst);
            fs.copyFromLocalFile(src, dst);
            this.setReadable(dst.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "fileId";
    }

    public boolean addOwner(String userId, String fileURI) throws URISyntaxException {
        boolean ret = false;
        URI uri = new URI(fileURI);
        if (uri.getScheme().startsWith("s3")) {
            System.out.println("Changing S3...");
            try {
                String awsAccessKey = params.get("fs.s3n.awsAccessKeyId");
                String awsSecretKey = params.get("fs.s3n.awsSecretAccessKey");
                String bucket = uri.getHost();
                String object = uri.getPath();
                while (object.startsWith("/")) {
                    object = object.replaceFirst("/", "");
                }
                GranteeInterface user = new CanonicalGrantee(userId);
                if (userId.contains("@")) {
                    user = new EmailAddressGrantee(userId);
                }
                AWSCredentials awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey);
                S3Service s3Service = new RestS3Service(awsCredentials);
                AccessControlList acl = s3Service.getObjectAcl(bucket, object);
                acl.grantPermission(user, Permission.PERMISSION_FULL_CONTROL);
                s3Service.putObjectAcl(bucket, object, acl);
                ret = true;
            } catch (Exception e) {
                log.error("Error adding new owner: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return ret;
    }

    public boolean setReadable(String fileURI) throws URISyntaxException {
        boolean ret = false;
        URI uri = new URI(fileURI);
        if (uri.getScheme().startsWith("s3")) {
            try {
                String awsAccessKey = params.get("fs.s3n.awsAccessKeyId");
                String awsSecretKey = params.get("fs.s3n.awsSecretAccessKey");
                String bucket = uri.getHost();
                String object = uri.getPath();
                while (object.startsWith("/")) {
                    object = object.replaceFirst("/", "");
                }
                AWSCredentials awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey);
                S3Service s3Service = new RestS3Service(awsCredentials);
                AccessControlList objAcl = s3Service.getObjectAcl(bucket, object);
                objAcl.grantPermission(GroupGrantee.ALL_USERS, Permission.PERMISSION_READ);
                s3Service.putObjectAcl(bucket, object, objAcl);
                ret = true;
            } catch (Exception e) {
                log.error("Error adding new owner: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
	 * Extract a given entry from its JAR file.
	 * @param jarPath
	 * @param jarEntry
	 */
    private String unJar(String jarPath, String jarEntry) {
        String path;
        if (jarPath.lastIndexOf("lib/") >= 0) path = jarPath.substring(0, jarPath.lastIndexOf("lib/")); else path = jarPath.substring(0, jarPath.lastIndexOf("/"));
        String relPath = jarEntry.substring(0, jarEntry.lastIndexOf("/"));
        try {
            new File(path + "/" + relPath).mkdirs();
            JarFile jar = new JarFile(jarPath);
            ZipEntry ze = jar.getEntry(jarEntry);
            InputStream finput = jar.getInputStream(ze);
            File bin = new File(path + "/" + jarEntry);
            FileOutputStream foutput = new FileOutputStream(bin);
            byte[] mBuffer = new byte[1024];
            int n;
            while ((n = finput.read(mBuffer)) > 0) foutput.write(mBuffer, 0, n);
            foutput.close();
            finput.close();
        } catch (Exception e) {
            log.error("Error unjaring " + jarEntry + " from " + jarEntry);
        }
        return path;
    }
}
