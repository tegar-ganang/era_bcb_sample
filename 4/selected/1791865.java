package com.strongauth.cloud.adapter;

import com.strongauth.cloud.valueobject.CloudVO;
import com.strongauth.skcews.common.CommonWS;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.Constants;

/**
  * This class serves as an adapter to the Eucalyptus Walrus storage
  * service, and handles all communication between the CloudProcessor 
  * and the Walrus service.  Each instance of this class is associated 
  * with a Walrus account identified by the access key and the secret 
  * key supplied in the constructor.
  */
public class EucalyptusAdapter implements CloudAdapter {

    private S3Service cloudClient = null;

    /**
     * Constructs an S3Service Object using the cloud credentials supplied.
     * @param cloudName The logical Name of the cloud account
     * @param cloudCredential The access key for this cloud account.
     * @param cloudAuthenticator The secret access key for this cloud account.
     */
    public EucalyptusAdapter(String cloudName, String cloudCredential, String cloudAuthenticator) throws Exception {
        Jets3tProperties jets3tprops = getEucalyptusProperties(cloudName);
        AWSCredentials awsCredentials = new AWSCredentials(cloudCredential, cloudAuthenticator);
        cloudClient = new RestS3Service(awsCredentials, "Walrus", null, jets3tprops);
        if (cloudClient == null) throw new Exception("S3 Service for Walrus is not initialized properly");
    }

    /**
     * Returns a list of the names of all buckets in the specific cloud instance represented by this cloud adapter.
     * @return List A list of the names of all buckets in the cloud instance represented by this cloud adapter.
     */
    @Override
    public List getAllBucketNames() throws Exception {
        List<String> bucketNames = new ArrayList();
        S3Bucket[] buckets = null;
        buckets = cloudClient.listAllBuckets();
        for (S3Bucket bucket : buckets) {
            bucketNames.add(bucket.getName());
        }
        return bucketNames;
    }

    /**
     * Returns a list of CloudVO objects. Each CloudVO object is a representation of a file in
     * the specified bucket in the cloud instance represented by this cloud adapter.
     * @return List A list of CloudVO objects.
     * @param containerName The name of the bucket whose files are to be listed.
     */
    @Override
    public List getAllCloudFiles(String containerName) throws Exception {
        List<CloudVO> cloudFiles = new ArrayList();
        CloudVO cloudFile = null;
        S3Object[] objects = cloudClient.listObjects(containerName);
        for (int o = 0; o < objects.length; o++) {
            cloudFile = new CloudVO();
            cloudFile.setName(objects[o].getKey());
            cloudFile.setSize(Long.toString(objects[o].getContentLength()));
            cloudFile.setLastModifiedDate(objects[o].getLastModifiedDate().toString());
            cloudFiles.add(cloudFile);
        }
        return cloudFiles;
    }

    /**
     * Downloads a file from a cloud bucket in the cloud instance represented by this cloud adapter
     * to a destination directory.
     * @return boolean true if the download is a success and false otherwise.
     * @param downloadDir The full path of the target directory where the file needs to be downloaded.
     * @param fileName The name of the file (in this cloud instance) which is to be downloaded.
     * @param sourceContainer The name of the bucket (in this cloud instance) which contains the file to be downloaded
     */
    @Override
    public boolean downloadFile(String downloadDir, String fileName, String sourceContainer) throws Exception {
        boolean success = false;
        S3Object object = cloudClient.getObject(sourceContainer, fileName);
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = object.getDataInputStream();
            int data;
            fos = new FileOutputStream(new File(downloadDir + CommonWS.fs + fileName));
            while ((data = is.read()) != -1) fos.write(data);
            fos.close();
            success = true;
        } finally {
            if (is != null) is.close();
            if (fos != null) fos.close();
        }
        return success;
    }

    /**
     * Uploads a file from a directory to a cloud bucket in the cloud instance represented by this cloud adapter.
     * @return boolean true if the upload is a success and false otherwise.
     * @param targetContainer The name of an existing bucket (to which you have Permission.Write permission) to which the file needs to be uploaded.
     * @param fileName The key under which to store the specified file.
     * @param file The file containing the data to be uploaded.
     */
    @Override
    public boolean uploadFile(String targetContainer, String fileName, File file) throws Exception {
        boolean success = false;
        if (cloudClient != null) {
            S3Object fileObj = new S3Object(file);
            fileObj.setKey(fileName);
            cloudClient.putObject(targetContainer, fileObj);
            success = true;
        }
        return success;
    }

    /**
     * Provides the client for accessing the Eucalyptus Walrus.
     * @return S3Service The client for accessing the Eucalyptus Walrus.
     */
    @Override
    public S3Service getCloudClient() {
        return cloudClient;
    }

    /**
     * Copies a file from one bucket to another within the same instance of the cloud represented by this cloud adapter.
     * @return boolean - true if the file copy is a success and false otherwise.
     * @param srcBucket - The name of the bucket containing the source object to copy.
     * @param srcFileName - The key in the source bucket under which the source object is stored.
     * @param destBucket - The name of the bucket in which the new object will be created.
     * @param destFileName - The key under which the source object will be copied to in the destination bucket.
     */
    @Override
    public boolean copyObject(String srcBucket, String srcFileName, String destBucket, String destFileName) throws Exception {
        boolean status = false;
        InputStream is = getObjectAsStream(srcBucket, srcFileName);
        putObject(destBucket, destFileName, is);
        status = true;
        return status;
    }

    /**
     * Provides the input stream containing the specified object's data. The object is the
     * file by the specified name in the specified bucket of the cloud instance represented by
     * this adapter.
     * @return InputStream - the input stream containing the specified object's data.
     * @param bucketName - The name of the bucket containing the object for which an InputStream has to be obtained.
     * @param fileName - The name of the file to which an InputStream has to be obtained.
     */
    @Override
    public InputStream getObjectAsStream(String bucketName, String fileName) throws Exception {
        S3Object srcObj = cloudClient.getObject(bucketName, fileName);
        InputStream is = srcObj.getDataInputStream();
        return is;
    }

    /**
     * Not implemented yet. This method however is to upload a stream of data to the specified bucket and key.
     * @param bucketName - The name of an existing bucket to which the new object will be uploaded.
     * @param fileName - The key under which to store the new object.
     * @param inputStream - The stream of data to upload to the cloud.     *
     */
    @Override
    public void putObject(String bucketName, String fileName, InputStream inputStream) throws Exception {
        S3Object object = new S3Object();
        object.setDataInputStream(inputStream);
        object.setKey(fileName);
        cloudClient.putObject(bucketName, object);
    }

    /**
     * Constructs a Jets3tProperties object that encapsulates all jets3t related properties for this Walrus account.
     * @param instanceName The logical name of the walrus account
     * @return Jets3tProperties Jets3tProperties object that encapsulates all jets3t related properties
     * required to construct the EucalyptusAdapter.
     */
    private static Jets3tProperties getEucalyptusProperties(String instanceName) throws Exception {
        Jets3tProperties jets3tprops = null;
        String propLocationKey = "skcews.euca." + instanceName + ".jets3tprop.location";
        String propLocation = CommonWS.getConfigurationProperty(propLocationKey);
        File f = new File(propLocation);
        FileInputStream fis = new FileInputStream(f);
        jets3tprops = Jets3tProperties.getInstance(fis, Constants.JETS3T_PROPERTIES_FILENAME);
        fis.close();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Enumeration<Object> enm = jets3tprops.getProperties().keys();
        List<String> keys = new ArrayList();
        while (enm.hasMoreElements()) {
            keys.add((String) enm.nextElement());
        }
        Collections.sort((List<String>) keys);
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            baos.write(("\n\t" + key + ": " + jets3tprops.getStringProperty(key, "")).getBytes());
        }
        baos.close();
        return jets3tprops;
    }
}
