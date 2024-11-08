package com.tomczarniecki.s3.tests;

import com.tomczarniecki.s3.ProgressListener;
import com.tomczarniecki.s3.S3Bucket;
import com.tomczarniecki.s3.S3Object;
import com.tomczarniecki.s3.Service;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalService implements Service {

    private final File root;

    public LocalService() {
        root = new File(SystemUtils.USER_DIR, "build");
        root.mkdirs();
    }

    public List<S3Bucket> listAllMyBuckets() {
        List<S3Bucket> buckets = new ArrayList<S3Bucket>();
        FileFilter filter = DirectoryFileFilter.INSTANCE;
        for (File dir : root.listFiles(filter)) {
            buckets.add(new S3Bucket(dir.getName()));
        }
        return buckets;
    }

    public boolean bucketExists(String bucketName) {
        return bucketFile(bucketName).isDirectory();
    }

    public void createBucket(String bucketName) {
        bucketFile(bucketName).mkdirs();
    }

    public void deleteBucket(String bucketName) {
        File file = bucketFile(bucketName);
        Validate.isTrue(file.delete(), "Cannot delete ", file);
    }

    public List<S3Object> listObjectsInBucket(String bucketName) {
        List<S3Object> objects = new ArrayList<S3Object>();
        FileFilter filter = FileFileFilter.FILE;
        for (File file : bucketFile(bucketName).listFiles(filter)) {
            objects.add(new S3Object(file.getName(), file.length(), new LocalDateTime(file.lastModified())));
        }
        return objects;
    }

    public boolean objectExists(String bucketName, String objectKey) {
        return objectFile(bucketName, objectKey).isFile();
    }

    public void createObject(String bucketName, String objectKey, File source, ProgressListener listener) {
        try {
            FileUtils.copyFile(source, objectFile(bucketName, objectKey));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPublicUrl(String bucketName, String objectKey, DateTime expires) {
        LocalDateTime local = new LocalDateTime(expires);
        return objectFile(bucketName, objectKey).toURI() + "?expires=" + local.toString("yyyy-MM-dd-HH-mm-ss");
    }

    public void downloadObject(String bucketName, String objectKey, File target, ProgressListener listener) {
        try {
            FileUtils.copyFile(objectFile(bucketName, objectKey), target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteObject(String bucketName, String objectKey) {
        File file = objectFile(bucketName, objectKey);
        Validate.isTrue(file.delete(), "Cannot delete ", file);
    }

    private File bucketFile(String bucketName) {
        return new File(root, bucketName);
    }

    private File objectFile(String bucketName, String objectKey) {
        return new File(bucketFile(bucketName), objectKey);
    }
}
