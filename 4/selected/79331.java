package com.ctrcv.framework.persistence;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

public class S3FilePersistence extends FilePersistence {

    AWSCredentials awsCredentials = null;

    String bucketName = null;

    S3Bucket bucketObj = null;

    S3Service s3Service = null;

    S3FilePersistence(String bucket, String awsKey, String awsSecretKey) throws PersistenceException {
        awsCredentials = new AWSCredentials(awsKey, awsSecretKey);
        bucketName = bucket;
        initS3();
    }

    private void initS3() throws PersistenceException {
        try {
            s3Service = new RestS3Service(awsCredentials);
            bucketObj = s3Service.createBucket(bucketName);
        } catch (S3ServiceException e) {
            throw new PersistenceException("fail to connect s3", e);
        }
    }

    @Override
    public String[] list(String path) throws PersistenceException {
        try {
            S3Object[] files = s3Service.listObjects(bucketObj, fmtPath(path), null);
            List<String> subs = new ArrayList<String>();
            if (files != null) {
                for (S3Object s : files) {
                    subs.add(s.getKey());
                }
            }
            return subs.toArray(new String[subs.size()]);
        } catch (S3ServiceException e) {
            throw new PersistenceException("fail to list for path - " + path, e);
        }
    }

    @Override
    public byte[] read(String path) throws PersistenceException {
        path = fmtPath(path);
        try {
            S3Object fileObj = s3Service.getObject(bucketObj, path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(fileObj.getDataInputStream(), out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new PersistenceException("fail to read s3 file - " + path, e);
        }
    }

    @Override
    public void write(String path, InputStream is) throws PersistenceException {
        path = fmtPath(path);
        S3Object fileObj = new S3Object(path);
        try {
            fileObj.setDataInputStream(is);
            s3Service.putObject(bucketObj, fileObj);
        } catch (S3ServiceException e) {
            throw new PersistenceException("failt to write the data into + " + path, e);
        }
    }

    private String fmtPath(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

    @Override
    public void delete(String path) throws PersistenceException {
        try {
            s3Service.deleteObject(bucketObj, path);
        } catch (S3ServiceException e) {
            throw new PersistenceException(e);
        }
    }
}
