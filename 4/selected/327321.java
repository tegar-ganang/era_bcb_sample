package org.apache.commons.vfs.helper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import org.apache.commons.vfs.AllFileSelector;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import uk.ac.dl.escience.vfs.util.VFSUtil;
import uk.ac.dl.escience.vfs.util.MarkerListenerImpl;
import static org.junit.Assert.*;

/**
 *
 * @author David Meredith
 * 
 * Class that contains test routines for VFS FileObjects. Most routines perform 
 * operations relative to a given file object (e.g. creating a subdirectory
 * or file and copying between different file systems). 
 */
public class VfsTestHelper {

    /**
     * Create a new directory with dummyFileDirName with sub files and folders, 
     * and subsequently delete all. Assert that the total number of files 
     * and folders created is same as the total deleted. 
     * 
     * @param relativeToFO create the new directory relative to this file object
     * @param dummyFileDirName create the directory with this name 
     * @param assertContent assert whether the files created have the expected content. 
     * @throws java.lang.Exception
     */
    public void testCreateDirHierarchyAndDeleteAll(FileObject relativeToFO, String dummyFileDirName, boolean assertContent) throws Exception {
        FileObject newFolder = relativeToFO.resolveFile(dummyFileDirName);
        deleteFileOrDirectory(newFolder);
        int numbCreated = this.testCreateDirectoryHeirarchyWithContentFiles(relativeToFO, dummyFileDirName, assertContent);
        FileObject fo3 = relativeToFO.resolveFile(dummyFileDirName);
        if (fo3.exists()) {
            int numbDeleted = fo3.delete(new AllFileSelector());
            System.out.println("numbCreated: " + numbCreated + " numbDeleted: " + numbDeleted);
            assertEquals(numbCreated, numbDeleted);
        } else {
            fail("Could not create dir heirarchy");
        }
    }

    /**
     * Create a folder with the given name dummyFileDirName relative to the 
     * given relativeToFO FileObject. Then rename the folder to the given 
     * dummyFileDirNameRenamed. Assert that the renamed folder has been renamed. 
     * 
     * @param relativeToFO Operations are relative to this FileObject
     * @param dummyFileDirName the name of the directory to be created
     * @param dummyFileDirNameRenamed rename to this name
     * @throws java.lang.Exception
     */
    public void testRenameFolder(FileObject relativeToFO, String dummyFileDirName, String dummyFileDirNameRenamed) throws Exception {
        FileObject newFolder = relativeToFO.resolveFile(dummyFileDirName);
        deleteFileOrDirectory(newFolder);
        newFolder = relativeToFO.resolveFile(dummyFileDirName);
        assertTrue(!newFolder.exists());
        newFolder.createFolder();
        assertTrue(newFolder.exists());
        FileObject movedFolder = relativeToFO.resolveFile(dummyFileDirNameRenamed);
        boolean ok = newFolder.canRenameTo(movedFolder);
        assertTrue(ok);
        newFolder.moveTo(movedFolder);
        assertTrue(movedFolder.exists());
        assertTrue(!newFolder.exists());
        assertTrue(movedFolder.delete());
        System.out.println("New Folder created, renamed and deleted OK");
    }

    /**
     * Create a file with the given name dummyFileDirName relative to the 
     * given relativeToFO FileObject. Then rename the file to the given 
     * dummyFileDirNameRenamed. Assert that the renamed file has been renamed.
     * 
     * @param relativeToFO operations are relative to this FileObject
     * @param dummyFileDirName create a file with this name 
     * @param dummyFileDirNameRenamed rename the file with this name 
     * @param assertContent assert that the content of the file is created
     * created correctly. 
     * @throws java.lang.Exception
     */
    public void testRenameFile(FileObject relativeToFO, String dummyFileDirName, String dummyFileDirNameRenamed, boolean assertContent) throws Exception {
        FileObject fileNew = relativeToFO.resolveFile(dummyFileDirName);
        System.out.println("testRenameFile = deleteFileOrDirectory");
        deleteFileOrDirectory(fileNew);
        String content = "Here is some sample content for the file.  Blah Blah Blah.";
        fileNew = relativeToFO.resolveFile(dummyFileDirName);
        if (!fileNew.exists()) {
            fileNew = this.createFileWithDummyContent(relativeToFO, dummyFileDirName, content);
            System.out.println("Created new file in testRenameFile: " + fileNew.getName());
        } else {
            fail("" + dummyFileDirName + " already exits");
        }
        assertTrue(fileNew.exists());
        if (assertContent) {
            assertSameContent(content, fileNew);
        }
        FileObject fileMove = relativeToFO.resolveFile(dummyFileDirNameRenamed);
        boolean ok = fileNew.canRenameTo(fileMove);
        assertTrue(ok);
        System.out.println("moving file: " + fileMove.getName());
        fileNew.moveTo(fileMove);
        assertTrue(fileMove.exists());
        assertTrue(!fileNew.exists());
        assertTrue(fileMove.delete());
        System.out.println("New File created, renamed and deleted OK");
    }

    /**
     * Create a new file and folder relative to the given relativeToFO 
     * FileObject and delete again. 
     * 
     * @param fsManager
     * @param relativeToFO perform operations relative to this FileObject
     * @param dummyFileDirName create new file and folder with this name
     * @throws org.apache.commons.vfs.FileSystemException
     */
    public void testCreateAndDeleteNewFileAndFolder(FileSystemManager fsManager, FileObject relativeToFO, String dummyFileDirName) throws FileSystemException {
        FileObject fo3 = fsManager.resolveFile(relativeToFO, dummyFileDirName);
        deleteFileOrDirectory(fo3);
        fo3 = fsManager.resolveFile(relativeToFO, dummyFileDirName);
        if (!fo3.exists()) {
            System.out.println("Creating File: " + dummyFileDirName);
            fo3.createFile();
            assertTrue(fo3.exists());
            assertTrue(fo3.getType() == FileType.FILE);
            System.out.println("File exists and is of type FILE, now deleting");
            fo3.delete();
            assertFalse(fo3.exists());
            System.out.println("File now deleted");
        } else {
            fail("File/folder already exists");
        }
        fo3 = fsManager.resolveFile(relativeToFO, dummyFileDirName);
        if (!fo3.exists()) {
            System.out.println("Creating Dir: " + dummyFileDirName);
            fo3.createFolder();
            assertTrue(fo3.exists());
            assertTrue(fo3.getType() == FileType.FOLDER);
            System.out.println("File exists and is of type FOLDER, now deleting");
            fo3.delete();
            assertFalse(fo3.exists());
            System.out.println("Folder now deleted");
        } else {
            fail("File/folder already exists");
        }
    }

    public void doExistsTest(FileObject fo) throws FileSystemException {
        assertTrue(fo.exists());
        System.out.println("\tClient FileObject Type: " + fo.getType().getName());
    }

    /**
     * Get the children of the given FileObject and print the info of the 
     * fileNew object's children (e.g. fileNew, folder, length, name). 
     * 
     * @param fo The FileObject to list 
     * @throws org.apache.commons.vfs.FileSystemException
     */
    public void doListTest(FileObject fo) throws FileSystemException {
        System.out.println("doListTest");
        System.out.println("fo: " + fo.getURL());
        assertTrue(fo.exists());
        assertEquals(FileType.FOLDER, fo.getType());
        System.out.println("\tClient FileObject Type: " + fo.getType().getName());
        System.out.println("\tClient getChildren(): ");
        FileObject[] children = fo.getChildren();
        System.out.println("" + children.length);
        System.out.println("");
        for (int i = 0; i < children.length; i++) {
            FileObject f = children[i];
            System.out.println("\tClient getType()");
            String type = f.getType().getName();
            long modtime = f.getContent().getLastModifiedTime();
            Date d = new Date(modtime);
            System.out.println("" + f.getName().getBaseName() + " " + type + " " + d);
            if (f.getType() == FileType.FILE) {
                System.out.println("FILE");
                String[] atts = f.getContent().getAttributeNames();
                System.out.println("" + atts.length);
                for (String s : atts) {
                    System.out.println("" + s);
                }
                System.out.println("r: " + f.isReadable());
                System.out.println("w: " + f.isWriteable());
                System.out.println("size: " + f.getContent().getSize());
            } else if (f.getType() == FileType.FOLDER) {
                System.out.println("FOLDER");
                System.out.println("r: " + f.isReadable());
                System.out.println("w: " + f.isWriteable());
            }
            System.out.println("");
        }
    }

    /**
     * Assert that adding a new file to a foler causes auto-refresh. 
     *  
     * @param relativeToFOSrc
     * @param dummyFileDirName
     * @throws java.lang.Exception
     */
    public void testNewFileAddedToFolderAfterRefresh2(FileObject relativeToFOSrc, String dummyFileDirName) throws Exception {
        FileObject newFile = null;
        try {
            FileObject[] kidspre = relativeToFOSrc.getChildren();
            int sizepre = kidspre.length;
            System.out.println("sizepre: " + sizepre);
            newFile = relativeToFOSrc.resolveFile(dummyFileDirName);
            System.out.println("Creating new file");
            newFile.createFile();
            System.out.println("Getting a list of children");
            kidspre = relativeToFOSrc.getChildren();
            int sizepost = kidspre.length;
            System.out.println("sizepost: " + sizepost);
            assertTrue(sizepre + 1 == sizepost);
        } finally {
            newFile.delete();
        }
    }

    /**
     * Assert that adding a new file to a foler causes auto-refresh. 
     *  
     * @param relativeToFOSrc
     * @param dummyFileDirName
     * @throws java.lang.Exception
     */
    public void testNewFileAddedToFolderAfterRefresh(FileObject relativeToFOSrc, String dummyFileDirName) throws Exception {
        FileObject newFolder = relativeToFOSrc.resolveFile(dummyFileDirName);
        assertTrue(!newFolder.exists());
        newFolder.createFolder();
        assertTrue(newFolder.exists());
        FileObject[] children = newFolder.getChildren();
        System.out.println("children in new empty folder : " + children.length);
        assertEquals(children.length, 0);
        FileObject newFile = newFolder.resolveFile(dummyFileDirName);
        newFile.createFile();
        children = newFolder.getChildren();
        System.out.println("children in folder after redo getChildren : " + children.length);
        assertEquals(children.length, 1);
        this.doListTest(newFolder);
        int numbDeletedAtDest = newFolder.delete(new AllFileSelector());
        assertEquals(numbDeletedAtDest, 2);
    }

    /**
     * Create a file relative to 
     * the given relativeToFOSrc FileObject. Copy the file 
     * into a newly created empty directory 
     * 
     * @param relativeToFOSrc source FileObject
     * @param relativeToFODest destination FileObject
     * @param dummyFileDirName name of folder that gets created and copied
     * @param assertContent
     * @param doThirdPartyTransferForTwoGridFtpFileObjects
     * @throws java.lang.Exception
     */
    public void testCopyFileIntoFolder(FileObject relativeToFOSrc, FileObject relativeToFODest, String dummyFileDirName, boolean assertContent, boolean doThirdPartyTransferForTwoGridFtpFileObjects) throws Exception {
        this.failIfDummyFileObjectExists(relativeToFOSrc, dummyFileDirName);
        this.failIfDummyFileObjectExists(relativeToFODest, dummyFileDirName);
        String content = "this is a dummy test file, you can delete me";
        System.out.println("Copy from relativeToFOSrc -to-> relativeToFODest");
        FileObject dummyDataFileSrc = this.createFileWithDummyContent(relativeToFOSrc, dummyFileDirName, content);
        assertEquals(FileType.FILE, dummyDataFileSrc.getType());
        if (assertContent) {
            this.assertSameContent(content, dummyDataFileSrc);
        }
        FileObject newFolder = relativeToFODest.resolveFile(dummyFileDirName);
        assertTrue(!newFolder.exists());
        newFolder.createFolder();
        assertTrue(newFolder.exists());
        VFSUtil.copy(dummyDataFileSrc, newFolder, new MarkerListenerImpl(), doThirdPartyTransferForTwoGridFtpFileObjects);
        FileObject dummyDataFileCopy = newFolder.resolveFile(dummyFileDirName);
        assertEquals(FileType.FILE, dummyDataFileCopy.getType());
        if (assertContent) {
            this.assertSameContent(content, dummyDataFileCopy);
        }
        assertTrue(dummyDataFileSrc.delete());
        int numbDeletedAtDest = newFolder.delete(new AllFileSelector());
        assertEquals(numbDeletedAtDest, 2);
    }

    /**
     * Create a file relative to relativeToFOSrc FileObject. 
     * Assert it was created ok. 
     * Create an empty directory relative to relativeToFOSrc. 
     * Assert that the new folder contains 0 children. 
     * Copy the file into the empty directory. 
     * Assert that the the directory contains 1 child. 
     * Delete file and directory. 
     * 
     * @param relativeToFOSrc source FileObject
     * @param relativeToFODest destination FileObject
     * @param dummyFileDirName name of folder that gets created and copied
     * @param assertContent
     * @param doThirdPartyTransferForTwoGridFtpFileObjects
     * @throws java.lang.Exception
     */
    public void testCopyFileIntoFolderAssertExpectedChildren(FileObject relativeToFOSrc, FileObject relativeToFODest, String dummyFileDirName, boolean assertContent, boolean doThirdPartyTransferForTwoGridFtpFileObjects) throws Exception {
        this.failIfDummyFileObjectExists(relativeToFOSrc, dummyFileDirName);
        this.failIfDummyFileObjectExists(relativeToFODest, dummyFileDirName);
        String content = "this is a dummy test file, you can delete me";
        System.out.println("Copy from relativeToFOSrc -to-> relativeToFODest");
        FileObject dummyDataFileSrc = this.createFileWithDummyContent(relativeToFOSrc, dummyFileDirName, content);
        assertEquals(FileType.FILE, dummyDataFileSrc.getType());
        if (assertContent) {
            this.assertSameContent(content, dummyDataFileSrc);
        }
        FileObject newFolder = relativeToFODest.resolveFile(dummyFileDirName);
        assertTrue(!newFolder.exists());
        newFolder.createFolder();
        assertTrue(newFolder.exists());
        try {
            FileObject[] children = newFolder.getChildren();
            assertTrue(children.length == 0);
            VFSUtil.copy(dummyDataFileSrc, newFolder, new MarkerListenerImpl(), doThirdPartyTransferForTwoGridFtpFileObjects);
            children = newFolder.getChildren();
            assertTrue(children.length == 1);
            FileObject copied = children[0];
            assertEquals(FileType.FILE, copied.getType());
            if (assertContent) {
                this.assertSameContent(content, copied);
            }
        } finally {
            assertTrue(dummyDataFileSrc.delete());
            int numbDeletedAtDest = newFolder.delete(new AllFileSelector());
            assertEquals(numbDeletedAtDest, 2);
        }
    }

    public void testCopyFileIntoExistingFolderAssertExpectedChildren(FileObject relativeToFOSrc, FileObject relativeToFODest, String dummyFileDirName, boolean assertContent, boolean doThirdPartyTransferForTwoGridFtpFileObjects) throws Exception {
        this.failIfDummyFileObjectExists(relativeToFOSrc, dummyFileDirName);
        this.failIfDummyFileObjectExists(relativeToFODest, dummyFileDirName);
        System.out.println("testCopyFileIntoExistingFolderAssertExpectedChildren");
        assertTrue(relativeToFODest.exists());
        assertTrue(relativeToFODest.getType() == FileType.FOLDER);
        assertTrue(relativeToFODest.isWriteable());
        FileObject dummyDataFileSrc = null;
        FileObject copied = null;
        try {
            String content = "this is a dummy test file, you can delete me";
            dummyDataFileSrc = this.createFileWithDummyContent(relativeToFOSrc, dummyFileDirName, content);
            assertEquals(FileType.FILE, dummyDataFileSrc.getType());
            if (assertContent) {
                this.assertSameContent(content, dummyDataFileSrc);
            }
            FileObject[] children = relativeToFODest.getChildren();
            int preChildrenLength = children.length;
            VFSUtil.copy(dummyDataFileSrc, relativeToFODest, new MarkerListenerImpl(), doThirdPartyTransferForTwoGridFtpFileObjects);
            children = relativeToFODest.getChildren();
            int newChildrenLength = children.length;
            assertTrue((preChildrenLength + 1) == newChildrenLength);
            System.out.println("preChildrenLength: " + preChildrenLength + " newChildrenLength: " + newChildrenLength);
            copied = relativeToFODest.resolveFile(dummyFileDirName);
            assertEquals(FileType.FILE, copied.getType());
            if (assertContent) {
                this.assertSameContent(content, copied);
            }
        } finally {
            boolean srcDeleted = false;
            try {
                srcDeleted = dummyDataFileSrc.delete();
            } catch (Exception ex) {
            }
            boolean copyDeleted = false;
            try {
                copyDeleted = copied.delete();
            } catch (Exception ex) {
            }
            assertTrue(srcDeleted);
            assertTrue(copyDeleted);
        }
    }

    /**
     * Create a new file that is relative to relativeToFOSrc and copy it to the 
     * destination relative to relativeToFODest. Choose to assert whether 
     * copied content is identical to the original content. Delete both files 
     * after creation. 
     * 
     * @param relativeToFODest
     * @param relativeToFOSrc
     * @param dummyFileDirName
     * @param assertContent
     * @param doThirdPartyTransferForTwoGridFtpFileObjects
     * @throws java.lang.Exception 
     */
    public void testCopyFileToFile(FileObject relativeToFOSrc, FileObject relativeToFODest, String dummyFileDirName, boolean assertContent, boolean doThirdPartyTransferForTwoGridFtpFileObjects) throws Exception {
        this.failIfDummyFileObjectExists(relativeToFOSrc, dummyFileDirName);
        this.failIfDummyFileObjectExists(relativeToFODest, dummyFileDirName);
        FileObject dummyDataFileSrc = null;
        FileObject dummyDataFileCopy = null;
        try {
            String content = "this is a dummy test file, you can delete me";
            dummyDataFileSrc = this.createFileWithDummyContent(relativeToFOSrc, dummyFileDirName, content);
            assertEquals(FileType.FILE, dummyDataFileSrc.getType());
            if (assertContent) {
                this.assertSameContent(content, dummyDataFileSrc);
            }
            dummyDataFileCopy = relativeToFODest.resolveFile(dummyFileDirName);
            dummyDataFileCopy.createFile();
            VFSUtil.copy(dummyDataFileSrc, dummyDataFileCopy, new MarkerListenerImpl(), doThirdPartyTransferForTwoGridFtpFileObjects);
            assertEquals(FileType.FILE, dummyDataFileCopy.getType());
            if (assertContent) {
                this.assertSameContent(content, dummyDataFileCopy);
            }
        } finally {
            boolean srcDeleted = false;
            try {
                srcDeleted = dummyDataFileSrc.delete();
            } catch (Exception ex) {
            }
            boolean copyDeleted = false;
            try {
                copyDeleted = dummyDataFileCopy.delete();
            } catch (Exception ex) {
            }
            assertTrue(srcDeleted);
            assertTrue(copyDeleted);
        }
    }

    /**
     * Create a directory heirarchy relative to relativeToFOSrc containing sub 
     * files and folders with name given by dummyFileDirName. Recursivley copy the new
     * directory heirarchy to the destination relative to 'relativeToFODest'. 
     * Delete the newly created directories at both source and target.  
     * 
     * @param relativeToFOSrc Create a directory heirarchy relative to this FileObject
     * @param relativeToFODest Copy relative to this FileObject
     * @param dummyFileDirName Create directory heirarchy with this name
     * @param assertContent
     * @param doThirdPartyTransferForTwoGridFtpFileObjects 
     * @throws java.lang.Exception
     */
    public void testCopyRecursiveDir(FileObject relativeToFOSrc, FileObject relativeToFODest, String dummyFileDirName, boolean assertContent, boolean doThirdPartyTransferForTwoGridFtpFileObjects) throws Exception {
        this.failIfDummyFileObjectExists(relativeToFOSrc, dummyFileDirName);
        this.failIfDummyFileObjectExists(relativeToFODest, dummyFileDirName);
        System.out.println("testCopyRecursiveDir() (from relativeToFOSrc -to-> relativeToFODest");
        FileObject dummyDataDirSrc = relativeToFOSrc.resolveFile(dummyFileDirName);
        int numbCreated = this.testCreateDirectoryHeirarchyWithContentFiles(relativeToFOSrc, dummyFileDirName, assertContent);
        assertEquals(FileType.FOLDER, dummyDataDirSrc.getType());
        FileObject dummyDataDirCopy = relativeToFODest.resolveFile(dummyFileDirName);
        System.out.println("creating destination directory");
        dummyDataDirCopy.createFolder();
        assertEquals(FileType.FOLDER, dummyDataDirCopy.getType());
        System.out.println("Yes, you've just created a folder!");
        System.out.println("About to copy..");
        VFSUtil.copy(dummyDataDirSrc, dummyDataDirCopy, new MarkerListenerImpl(), doThirdPartyTransferForTwoGridFtpFileObjects);
        int numbDeletedSrc = dummyDataDirSrc.delete(new AllFileSelector());
        int numbDeletedCopy = dummyDataDirCopy.delete(new AllFileSelector());
        System.out.println("numbCreated: " + numbCreated);
        System.out.println("numbDeleted on Src: " + numbDeletedSrc + "  numbDeleted on Copy: " + numbDeletedCopy);
        FileObject[] parentsChildren = dummyDataDirCopy.getParent().getChildren();
        for (int i = 0; i < parentsChildren.length; i++) {
            System.out.println("Children: " + parentsChildren[i].getURL() + " type: " + parentsChildren[i].getType());
        }
        System.out.println("parentsChildren: " + parentsChildren.length);
        assertEquals(numbDeletedSrc, numbDeletedCopy);
        assertEquals(numbDeletedSrc, numbCreated);
    }

    private void failIfDummyFileObjectExists(FileObject relativeToFOSrc, String dummyFileDirName) throws Exception {
        FileObject testFoExist = relativeToFOSrc.resolveFile(dummyFileDirName);
        if (testFoExist.exists()) {
            fail("Dummy data FileObject already exists: [" + dummyFileDirName + "]");
        }
    }

    /**
     * If FileObject fo exists and is either a file or a dir, 
     * then delete it (recursivley deletes dirs). 
     * 
     * @param fo the file or dir to delete. 
     * @throws org.apache.commons.vfs.FileSystemException
     */
    private void deleteFileOrDirectory(FileObject fo) throws FileSystemException {
        if (fo.exists()) {
            if (fo.getType() == FileType.FILE) {
                System.out.println("Deleting existing file: " + fo.getName());
                fo.delete();
            } else if (fo.getType() == FileType.FOLDER) {
                System.out.println("Deleting existing directory: " + fo.getName());
                fo.delete(new AllFileSelector());
            }
            assertFalse(fo.exists());
        }
    }

    /**
     * Create a file that is relative to 'relativeToFO' with the given content.
     *  
     * @param relativeToFO - create new file relative to this FileObject
     * @param fileName - the name of the new file to create
     * @param content - the conent of the file
     * @return the newly created FileObject
     * @throws java.lang.Exception
     */
    private FileObject createFileWithDummyContent(FileObject relativeToFO, String fileName, String content) throws Exception {
        FileObject fileNew = null;
        OutputStream os = null;
        try {
            fileNew = relativeToFO.resolveFile(fileName);
            if (!fileNew.exists()) {
                fileNew.createFile();
                byte[] bytes = content.getBytes("utf-8");
                os = fileNew.getContent().getOutputStream();
                os.write(bytes);
            }
        } finally {
            if (os != null) {
                os.close();
            }
            fileNew.getContent().close();
        }
        return fileNew;
    }

    /**
     * Create a directory heirarchy that contains both sub folders and files 
     * which have content and files with no-content. The content of the files 
     * is asserted to ensure that the files do have the expected conent. 
     * Method fails with assertion error if 'dummyFileDirName' already exists 
     * relative to relativeToFO. 
     * 
     * @param relativeToFO - create new dummyFileDirName relative to this 
     * FileObject
     * @param dummyFileDirName the name of a directory that can get safely 
     * created and deleted on the target host. If dummyFileDirName already 
     * exists, then method fails with assertion error. 
     * @param assertContent assert that the files that get created by this 
     * method with their dummy conent have identical size and bytes to the 
     * dummy conent used. 
     * @return Integer representing the total number of files + folders created
     * @throws java.lang.Exception
     */
    private Integer testCreateDirectoryHeirarchyWithContentFiles(FileObject relativeToFO, String dummyFileDirName, boolean assertContent) throws Exception {
        String content;
        FileObject newFileCreation;
        FileObject fo3 = relativeToFO.resolveFile(dummyFileDirName);
        if (fo3.exists()) {
            fo3.delete(new AllFileSelector());
        }
        if (!fo3.exists()) {
            fo3.createFolder();
            System.out.println("created folder: " + fo3.getURL());
            content = "a text can delete me";
            newFileCreation = this.createFileWithDummyContent(fo3, "a_.txt", content);
            if (assertContent) {
                this.assertSameContent(content, newFileCreation);
            }
            content = "b text can delete me";
            newFileCreation = this.createFileWithDummyContent(fo3, "b_.txt", content);
            if (assertContent) {
                this.assertSameContent(content, newFileCreation);
            }
            content = "c text can delete me";
            newFileCreation = this.createFileWithDummyContent(fo3, "c_.txt", content);
            if (assertContent) {
                this.assertSameContent(content, newFileCreation);
            }
            fo3.resolveFile("aNoContent.txt").createFile();
            fo3.resolveFile("bNoContent.txt").createFile();
            fo3.resolveFile("a").createFolder();
            fo3.resolveFile("b").createFolder();
            FileObject aDir = fo3.resolveFile("a");
            FileObject bDir = fo3.resolveFile("b");
            content = "d text can delete me";
            newFileCreation = this.createFileWithDummyContent(aDir, "d_.txt", content);
            if (assertContent) {
                this.assertSameContent(content, newFileCreation);
            }
            content = "e text can delete me";
            newFileCreation = this.createFileWithDummyContent(bDir, "e_.txt", content);
            if (assertContent) {
                this.assertSameContent(content, newFileCreation);
            }
        } else if (fo3.exists()) {
            fail("Dummy data directory already exists");
        }
        if (!fo3.exists()) {
            fail("did not create dir heirarchy");
        }
        return 10;
    }

    /**
     * Asserts that the content of a fileNew is the same as expected. Checks the
     * length reported by getSize() is correct, then reads the content as
     * a byte stream and compares the result with the expected content.
     * Assumes files are encoded using UTF-8.
     * 
     * @param expected The expected content of the file
     * @param file Should have the content same as expected 
     * @throws java.lang.Exception
     */
    private void assertSameContent(String expected, FileObject file) throws Exception {
        assertTrue(file.exists());
        assertSame(FileType.FILE, file.getType());
        final byte[] expectedBin = expected.getBytes("utf-8");
        final FileContent content = file.getContent();
        System.out.println("file: " + file.getURL() + " type: " + file.getType());
        System.out.println("content: " + content + " map: " + content.getAttributes());
        Map map = content.getAttributes();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) (it.next());
        }
        assertEquals("same content length", expectedBin.length, content.getSize());
        final InputStream instr = content.getInputStream();
        final ByteArrayOutputStream outstr;
        try {
            outstr = new ByteArrayOutputStream(expectedBin.length);
            final byte[] buffer = new byte[256];
            int nread = 0;
            while (nread >= 0) {
                outstr.write(buffer, 0, nread);
                nread = instr.read(buffer);
            }
        } finally {
            instr.close();
        }
        assertTrue("same binary content", Arrays.equals(expectedBin, outstr.toByteArray()));
    }
}
