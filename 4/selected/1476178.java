package org.dcm4chex.archive.hsm.module.cloud;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.dcm4chex.archive.common.FileStatus;
import org.dcm4chex.archive.hsm.module.AbstractHSMModule;
import org.dcm4chex.archive.hsm.module.HSMException;
import org.dcm4chex.archive.util.FileUtils;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class CloudHSMModule extends AbstractHSMModule {

    private static final int KB = 1024;

    private static final int MB = KB * KB;

    private File outgoingDir;

    private File absOutgoingDir;

    private File incomingDir;

    private File absIncomingDir;

    private String bucketName;

    private String accessKey;

    private String secretKey;

    private AmazonS3 s3;

    public final String getOutgoingDir() {
        return outgoingDir.getPath();
    }

    public final void setOutgoingDir(String dir) {
        this.outgoingDir = new File(dir);
        this.absOutgoingDir = FileUtils.resolve(this.outgoingDir);
    }

    public final String getIncomingDir() {
        return incomingDir.getPath();
    }

    public final void setIncomingDir(String dir) {
        this.incomingDir = new File(dir);
        this.absIncomingDir = FileUtils.resolve(this.incomingDir);
    }

    @Override
    public void failedHSMFile(File file, String fsID, String filePath) throws HSMException {
        log.debug("failedHSMFile called with file=" + file + ", fsID=" + fsID + ", filePath=" + filePath);
        log.warn("failedHSMFile is not yet implemented for S3 integration");
    }

    @Override
    public File fetchHSMFile(String fsID, String filePath) throws HSMException {
        log.debug("fetchHSMFile called with fsID=" + fsID + ", filePath=" + filePath);
        if (absIncomingDir.mkdirs()) {
            log.info("M-WRITE " + absIncomingDir);
        }
        File tarFile;
        try {
            tarFile = File.createTempFile("hsm_", ".tar", absIncomingDir);
        } catch (IOException x) {
            throw new HSMException("Failed to create temp file in " + absIncomingDir, x);
        }
        log.info("Fetching " + filePath + " from cloud storage");
        FileOutputStream fos = null;
        try {
            if (s3 == null) createClient();
            S3Object object = s3.getObject(new GetObjectRequest(bucketName, filePath));
            fos = new FileOutputStream(tarFile);
            IOUtils.copy(object.getObjectContent(), fos);
        } catch (AmazonClientException ace) {
            s3 = null;
            throw new HSMException("Could not list objects for: " + filePath, ace);
        } catch (Exception x) {
            throw new HSMException("Failed to retrieve " + filePath, x);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    log.error("Couldn't close output stream for: " + tarFile);
                }
            }
        }
        return tarFile;
    }

    @Override
    public void fetchHSMFileFinished(String fsID, String filePath, File file) throws HSMException {
        log.debug("fetchHSMFileFinished called with fsID=" + fsID + ", filePath=" + filePath + ", file=" + file);
        log.info("M-DELETE " + file);
        file.delete();
    }

    @Override
    public File prepareHSMFile(String fsID, String filePath) throws HSMException {
        log.debug("prepareHSMFile called with fsID=" + fsID + ", filePath=" + filePath);
        return new File(absOutgoingDir, new File(filePath).getName());
    }

    @Override
    public Integer queryStatus(String fsID, String filePath, String userInfo) throws HSMException {
        log.debug("queryStatus called with fsID=" + fsID + ", filePath=" + filePath + ", userInfo=" + userInfo);
        try {
            if (s3 == null) createClient();
            ObjectListing objectListing = s3.listObjects(bucketName, filePath);
            if (objectListing != null) {
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    log.debug("Listed " + objectSummary.getKey() + "  " + "(size = " + objectSummary.getSize() + ")");
                    if (filePath.equals(objectSummary.getKey())) {
                        return FileStatus.ARCHIVED;
                    }
                }
            }
        } catch (AmazonClientException ace) {
            s3 = null;
            throw new HSMException("Could not list objects for: " + filePath, ace);
        } catch (Exception e) {
            throw new HSMException("Could not list objects for: " + filePath, e);
        }
        return FileStatus.DEFAULT;
    }

    @Override
    public String storeHSMFile(File file, String fsID, String filePath) throws HSMException {
        log.debug("storeHSMFile called with file=" + file + ", fsID=" + fsID + ", filePath=" + filePath);
        String awsFileKey = filePath;
        log.info("Uploading " + file + " to S3 with key of " + awsFileKey);
        try {
            if (s3 == null) createClient();
            s3.putObject(new PutObjectRequest(bucketName, awsFileKey, file));
        } catch (AmazonClientException ace) {
            s3 = null;
            throw new HSMException("Could not store: " + filePath, ace);
        } catch (Exception e) {
            throw new HSMException("Could not store: " + filePath, e);
        }
        return filePath;
    }

    private synchronized void createClient() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        s3 = new AmazonS3Client(credentials);
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    private void prompt(String sentOrFetched, int numberOfFilesSent, float totalSizeSent, float seconds) {
        StringBuilder sb = new StringBuilder();
        sb.append(sentOrFetched).append(" ").append(numberOfFilesSent).append(" objects (=");
        promptBytes(sb, totalSizeSent).append(") in ").append(seconds).append("s (=");
        promptBytes(sb, totalSizeSent / seconds).append("/s)");
    }

    private StringBuilder promptBytes(StringBuilder sb, float totalSizeSent) {
        if (totalSizeSent > MB) {
            sb.append(totalSizeSent / MB).append("MB");
        } else {
            sb.append(totalSizeSent / KB).append("KB");
        }
        return sb;
    }
}
