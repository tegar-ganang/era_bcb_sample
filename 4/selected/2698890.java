package com.c2b2.ipoint.business;

import com.c2b2.ipoint.model.DocumentRepository;
import com.c2b2.ipoint.model.PersistentModelException;
import com.c2b2.ipoint.model.Property;
import com.c2b2.ipoint.model.User;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class provides a Business Interface on top of the Document Repository
 * persistent representation.
 * <p>
 * $Date: 2007/04/16 09:32:53 $
 * 
 * $Id: DocumentRepositoryServices.java,v 1.4 2007/04/16 09:32:53 steve Exp $<br>
 * 
 * Copyright 2005 C2B2 Consulting Limited. All rights reserved.
 * </p>
 * @author $Author: steve $
 * @version $Revision: 1.4 $
 */
public class DocumentRepositoryServices {

    private DocumentRepository myRepository;

    private File myRootFile;

    public DocumentRepositoryServices(DocumentRepository repo) {
        myRepository = repo;
        myRootFile = new File(repo.getDirectoryPath());
    }

    public void setRepository(DocumentRepository repository) {
        this.myRepository = repository;
    }

    public DocumentRepository getRepository() {
        return myRepository;
    }

    public void setRootFile(File rootFile) {
        this.myRootFile = rootFile;
    }

    public File getRootFile() {
        return myRootFile;
    }

    /**
   * Deletes a set of files from the repository
   * @param files A String[] of the paths of the files to delete
   */
    public void delete(String files[]) {
        for (String filePath : files) {
            delete(filePath);
        }
    }

    /**
   * Deletes a file in the repository
   * @param relativePath The relative path to the file in the repository
   */
    public void delete(String relativePath) {
        File file = convertToFile(relativePath);
        if (file.exists()) {
            if (file.isDirectory()) {
                this.deleteDir(file);
            } else {
                file.delete();
            }
            File descriptionFile = new File(file.getAbsolutePath() + ".description");
            if (descriptionFile.exists()) {
                descriptionFile.delete();
            }
        }
    }

    /**
   * Returns the description of the File at the specified path
   * @param filePath The path of the file
   * @return The description of the file or a blank String if no description is
   * available
   */
    public String getDescription(String filePath) {
        return getDescription(convertToFile(filePath));
    }

    /**
   * Returns the description of the File
   * @param file The file
   * @return The description of the file or a blank String if no description is
   * available
   */
    public String getDescription(File file) {
        String result = "";
        if (file != null) {
            try {
                String canonicalPath = file.getCanonicalPath();
                String descriptionPath = canonicalPath + ".description";
                File descriptionFile = new File(descriptionPath);
                if (descriptionFile.exists() && descriptionFile.canRead() && descriptionFile.isFile()) {
                    BufferedReader reader = new BufferedReader(new FileReader(descriptionFile));
                    result = reader.readLine();
                    reader.close();
                }
            } catch (Exception e) {
                result = null;
            }
        }
        return result;
    }

    /**
   * Converts a String to a File object
   * @param relativePath The path relative to the root of the repository of the File
   * @return
   */
    public File convertToFile(String relativePath) {
        String pathName = myRootFile.getAbsolutePath() + relativePath;
        File result = new File(pathName);
        try {
            if (isParent(result)) {
                result = myRootFile;
            }
        } catch (IOException e) {
            result = myRootFile;
        }
        return result;
    }

    /**
   * Gets the repository relative path to the file
   * @param file
   * @return The repository relative path or null if the file is not in this repository
   */
    public String getRelativePath(File file) {
        String result = null;
        String fileNamePath = file.getAbsolutePath();
        String rootPath = myRootFile.getAbsolutePath();
        if (fileNamePath.startsWith(rootPath)) {
            result = fileNamePath.substring(rootPath.length());
            if (!result.startsWith(File.separator)) {
                result = File.separator + result;
            }
        }
        result = result.replace('\\', '/');
        return result;
    }

    /**
   * Creates a new Folder in the Repository with the specified name
   * @param description The description for the new folder
   * @param folderName The name of the new folder
   * @param relativePath The path in the repository to create the folder
   * @return The new Folder
   * @throws IOException
   */
    public File createFolder(String description, String folderName, String relativePath) throws IOException {
        File result = null;
        File directory = convertToFile(relativePath);
        File newFile = new File(directory + File.separator + folderName);
        newFile.mkdir();
        writeDescriptionFile(description, newFile);
        result = newFile;
        return result;
    }

    /**
   * Creates a new file at the root of the repository
   * @param description The description of the new file
   * @param data The dataFile to copy into the repository
   * @param relativePath The relative Path in the Repository to create the file
   * @param deleteOnExists If true and a file already exists with that name it is deleted
   * @return The newly created File or null if the File could not be created
   * @throws IOException
   */
    public File createFile(String description, File data, String relativePath, boolean deleteOnExists) throws IOException {
        File result = null;
        File directory = convertToFile(relativePath);
        File newFile = new File(directory + File.separator + data.getName());
        if (newFile.exists() && deleteOnExists) {
            newFile.delete();
        } else if (newFile.exists()) {
            newFile.renameTo(new File(newFile.getAbsolutePath() + ".old"));
        }
        data.renameTo(newFile);
        result = newFile;
        writeDescriptionFile(description, newFile);
        result = newFile;
        return result;
    }

    public File unzipFile(String description, File zipFile, String relativePath, boolean deleteOnExists) throws IOException {
        if ((zipFile.getName().indexOf(".zip") == -1) && zipFile.getName().indexOf(".ZIP") == -1) {
            return this.createFile(description, zipFile, relativePath, deleteOnExists);
        }
        File result = null;
        File topDirectory = convertToFile(relativePath);
        String newDirName = zipFile.getName().substring(0, zipFile.getName().indexOf(".zip"));
        File oldDirectory = new File(topDirectory + File.separator + newDirName);
        if (oldDirectory.exists() && deleteOnExists) {
            deleteDir(oldDirectory);
        } else if (oldDirectory.exists()) {
            oldDirectory.renameTo(new File(oldDirectory.getAbsolutePath() + ".old"));
        }
        File newDirectory = new File(topDirectory + File.separator + newDirName);
        newDirectory.mkdir();
        ZipFile zip = new ZipFile(zipFile);
        Enumeration entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
                File dir = new File(newDirectory, entry.getName());
                dir.mkdir();
            } else {
                File outputFile = new File(newDirectory, entry.getName());
                this.copyInputStream(zip.getInputStream(entry), new FileOutputStream(outputFile));
            }
        }
        zip.close();
        writeDescriptionFile(description, newDirectory);
        zipFile.delete();
        return newDirectory;
    }

    /**
   * Creates a new file at the root of the repository
   * @param description The description of the new file
   * @param data The dataFile to copy into the repository
   * @param deleteOnExists If true and a file already exists with that name it is deleted
   * @return The newly created File or null if the File could not be created
   * @throws IOException
   */
    public File createFile(String description, File data, boolean deleteOnExists) throws IOException {
        return createFile(description, data, "", deleteOnExists);
    }

    /**
   * Creates a new DocumentRepositoryServices object and the underlying DocumentRepository
   * persistent object
   * @param abstractName The name of the directory to create. This will be created in the
   * Portal Media Repository directory.
   * @param owner The owner of the directory
   * @return The new services object
   * @throws PersistentModelException
   */
    public static DocumentRepositoryServices createNew(String abstractName, User owner) throws PersistentModelException {
        String portalproperty = Property.getPropertyValue("MediaRepository");
        File newFile = new File(portalproperty + File.separator + abstractName);
        newFile.mkdirs();
        DocumentRepository newRepo = DocumentRepository.create(newFile, owner);
        return new DocumentRepositoryServices(newRepo);
    }

    public static DocumentRepositoryServices createNew(File directory, User owner) throws PersistentModelException {
        DocumentRepository newRepo = DocumentRepository.create(directory, owner);
        return new DocumentRepositoryServices(newRepo);
    }

    /**
   * Moves a file in the repository
   * @param relativePath The path of the file or directory to move
   * @param pathToMoveTo The new relative path in the repository
   */
    public void moveFile(String relativePath, String pathToMoveTo) {
        File oldFile = convertToFile(relativePath);
        File newFile = convertToFile(pathToMoveTo);
        if (newFile.isDirectory() && oldFile.isFile()) {
            newFile = new File(newFile.getAbsolutePath() + File.separator + oldFile.getName());
        }
        if (oldFile.isFile()) {
            File oldFileDescription = new File(oldFile.getAbsolutePath() + ".description");
            File newFileDescription = new File(newFile.getAbsolutePath() + ".description");
            oldFileDescription.renameTo(newFileDescription);
        }
        oldFile.renameTo(newFile);
    }

    /**
   * Renames a file in the repository
   * @param relativePath The relative path of the file to rename
   * @param newName The new name for the file
   */
    public File renameFile(String relativePath, String newName) {
        File file = convertToFile(relativePath);
        File renamedFile = null;
        if (file.exists()) {
            String newFileName = file.getParentFile().getAbsolutePath() + File.separator + newName;
            renamedFile = new File(newFileName);
            file.renameTo(renamedFile);
            String newDescription = newFileName + ".description";
            String oldDescription = file.getAbsolutePath() + ".description";
            File oldDesc = new File(oldDescription);
            File newDesc = new File(newDescription);
            oldDesc.renameTo(newDesc);
        }
        return renamedFile;
    }

    /**
   * Resets the description of the file
   * @param relativePath The path of the file in the repository
   * @param description The new description
   * @throws IOException if the description can not be written
   */
    public void setDescription(String relativePath, String description) throws IOException {
        File file = convertToFile(relativePath);
        writeDescriptionFile(description, file);
    }

    private boolean isParent(File file) throws IOException {
        boolean result = false;
        if (myRootFile.getCanonicalPath().startsWith(file.getCanonicalPath())) {
            result = true;
        }
        return result;
    }

    private void writeDescriptionFile(String description, File file) throws IOException {
        String descriptionFileName = file.getCanonicalPath() + ".description";
        File descriptionFile = new File(descriptionFileName);
        if (!descriptionFile.exists()) {
            descriptionFile.createNewFile();
        }
        FileWriter fw = new FileWriter(descriptionFile);
        fw.write(description);
        fw.flush();
        fw.close();
    }

    private void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    private void deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                deleteDir(new File(dir, children[i]));
            }
        }
        dir.delete();
    }
}
