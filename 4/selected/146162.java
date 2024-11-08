package com.avatal.content.scorm.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.struts.upload.FormFile;
import org.xml.sax.InputSource;
import com.avatal.ConfigurableConstants;
import com.avatal.business.exception.ScormImportException;
import com.avatal.content.vo.course.scorm.Manifest;

/**
 * @author c. ferdinand
 * @date 20.05.2003
 *
 */
public class CourseImport {

    static final String path = ConfigurableConstants.SCORM_COURSE_UNZIP_FOLDER;

    private String title;

    private FormFile file;

    private java.io.File destFile;

    private Manifest manifestVO;

    Integer courseId = com.avatal.business.util.EjbUtil.getNextId(com.avatal.DatabaseTableConstants.SCORM_COURSE_TABLE);

    public void importCourse(String title, FormFile file) throws ScormImportException {
        this.title = title;
        this.file = file;
        upload();
        extract();
        java.io.File zipUpload = new File(path + title + ".zip");
        String courseDirectory = ConfigurableConstants.SCORM_COURSE_UPLOAD_FOLDER + courseId + "/";
        System.out.println("++++++ Course Directory:" + courseDirectory);
        ManifestImport manifestImport = new ManifestImport(courseDirectory);
        manifestVO = manifestImport.getManifest();
    }

    public Manifest getManifest() {
        return this.manifestVO;
    }

    private boolean upload() {
        String contentType = file.getContentType();
        boolean writeFile = true;
        String size = (file.getFileSize() + " bytes");
        String data = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream stream = file.getInputStream();
            OutputStream bos = new FileOutputStream(path + title + ".zip");
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = stream.read(buffer, 0, 8192)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            bos.close();
            System.out.println("The file has been written to \"" + ConfigurableConstants.SCORM_COURSE_UPLOAD_FOLDER + "\"");
            stream.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
        file.destroy();
        return true;
    }

    private boolean extract() {
        try {
            String uploadDir = path + title;
            java.io.File theUploadDir = new java.io.File(uploadDir);
            if (!theUploadDir.isDirectory()) {
                theUploadDir.mkdirs();
            }
            String courseTitle = title;
            String zipFile = uploadDir + ".zip";
            String controlType = "choice";
            ZipFile archive = new ZipFile(zipFile);
            byte[] buffer = new byte[16384];
            for (Enumeration e = archive.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (!entry.isDirectory()) {
                    String filename = entry.getName();
                    filename = filename.replace('/', java.io.File.separatorChar);
                    filename = path + "CourseImports/" + courseId + "/" + filename;
                    destFile = new java.io.File(filename);
                    String parent = destFile.getParent();
                    if (parent != null) {
                        java.io.File parentFile = new java.io.File(parent);
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                    }
                    InputStream in = archive.getInputStream(entry);
                    OutputStream outStream = new FileOutputStream(filename);
                    int count;
                    while ((count = in.read(buffer)) != -1) outStream.write(buffer, 0, count);
                    in.close();
                    outStream.close();
                }
            }
            System.out.println("Try to get files \n vv");
            boolean wasdeleted = false;
            java.io.File uploadFiles[] = theUploadDir.listFiles();
            for (int i = 0; i < uploadFiles.length; i++) {
                System.out.println("Try to get files \n" + uploadFiles[i].getName());
                uploadFiles[i].delete();
            }
            System.out.println("Try to delete dir");
            System.out.println(theUploadDir.delete());
            return true;
        } catch (Exception e) {
            System.out.println("Exception caught in CourseImport");
            e.printStackTrace();
            return false;
        } finally {
        }
    }

    public Integer getCourseId() {
        return courseId;
    }

    private static InputSource setUpInputSource(String fileName) {
        InputSource is = new InputSource();
        is = setupFileSource(fileName);
        return is;
    }

    private static InputSource setupFileSource(String filename) {
        try {
            java.io.File xmlFile = new java.io.File(filename);
            if (xmlFile.isFile()) {
                FileReader fr = new FileReader(xmlFile);
                InputSource is = new InputSource(fr);
                return is;
            } else {
            }
        } catch (NullPointerException npe) {
            System.out.println("Null pointer exception in CourseImport.setupFileSource" + npe);
        } catch (SecurityException se) {
            System.out.println("Security Exception" + se);
        } catch (FileNotFoundException fnfe) {
            System.out.println("File Not Found Exception" + fnfe);
        }
        return new InputSource();
    }
}
