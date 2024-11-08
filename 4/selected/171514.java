package org.photovault.imginfo;

import java.io.*;
import junit.framework.*;
import java.util.*;
import java.sql.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import org.photovault.common.PhotovaultException;
import org.photovault.dbhelper.ImageDb;
import org.photovault.common.PhotovaultSettings;
import org.photovault.common.JUnitOJBManager;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.test.PhotovaultTestCase;

public class Test_PhotoInfo extends PhotovaultTestCase {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Test_PhotoInfo.class.getName());

    String testImgDir = "testfiles";

    String nonExistingDir = "/tmp/_dirThatDoNotExist";

    /**
   * Default constructor to set up OJB environment
   */
    public Test_PhotoInfo() {
        super();
    }

    /**
       Tears down the testing environment
    */
    public void tearDown() {
    }

    File testRefImageDir = new File("tests/images/photovault/imginfo");

    /**
       Test case that verifies that an existing photo infor record 
       can be loaded successfully
    */
    public void testRetrievalSuccess() {
        int photoId = 1;
        try {
            PhotoInfo photo = PhotoInfo.retrievePhotoInfo(photoId);
            assertTrue(photo != null);
        } catch (PhotoNotFoundException e) {
            fail("Photo " + photoId + " not found");
        }
    }

    /**
       Test case that verifies that an existing photo info record 
       can be loaded successfully
    */
    public void testRetrievalNotFound() {
        int photoId = -1;
        try {
            PhotoInfo photo = PhotoInfo.retrievePhotoInfo(photoId);
        } catch (PhotoNotFoundException e) {
            return;
        }
        fail("Image " + photoId + " should not exist.");
    }

    /** 
	Test updating object to DB
    */
    public void testUpdate() {
        int photoId = 1;
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.retrievePhotoInfo(photoId);
            assertTrue(photo != null);
        } catch (PhotoNotFoundException e) {
            fail("Photo " + photoId + " not found");
        }
        String shootingPlace = photo.getShootingPlace();
        String newShootingPlace = "Testipaikka";
        photo.setShootingPlace(newShootingPlace);
        try {
            photo = PhotoInfo.retrievePhotoInfo(photoId);
            assertTrue(photo != null);
        } catch (PhotoNotFoundException e) {
            fail("Photo " + photoId + " not found after updating");
        }
        assertEquals(newShootingPlace, photo.getShootingPlace());
        photo.setShootingPlace(shootingPlace);
    }

    /** 
	Test updating object to DB when shooting date has not been specified
    */
    public void testNullShootDateUpdate() {
        int photoId = 1;
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.retrievePhotoInfo(photoId);
            assertTrue(photo != null);
        } catch (PhotoNotFoundException e) {
            fail("Photo " + photoId + " not found");
        }
        java.util.Date origTime = photo.getShootTime();
        photo.setShootTime(null);
        try {
            photo = PhotoInfo.retrievePhotoInfo(photoId);
            assertNull("Shooting time was supposedly set to null", photo.getShootTime());
        } catch (PhotoNotFoundException e) {
            fail("Photo " + photoId + " not found after updating");
        }
        photo.setShootTime(origTime);
    }

    /**
       Test normal creation of a persistent PhotoInfo object
    */
    public void testPhotoCreation() {
        PhotoInfo photo = PhotoInfo.create();
        assertNotNull(photo);
        photo.setPhotographer("TESTIKUVAAJA");
        photo.setShootingPlace("TESTPLACE");
        photo.setShootTime(new java.util.Date());
        photo.setFStop(5.6);
        photo.setShutterSpeed(0.04);
        photo.setFocalLength(50);
        photo.setCamera("Canon FTb");
        photo.setFilm("Tri-X");
        photo.setFilmSpeed(400);
        photo.setLens("Canon FD 50mm/F1.4");
        photo.setDescription("This is a long test description that tries to verify that the description mechanism really works");
        try {
            PhotoInfo photo2 = PhotoInfo.retrievePhotoInfo(photo.getUid());
            assertEquals(photo.getPhotographer(), photo2.getPhotographer());
            assertEquals(photo.getShootingPlace(), photo2.getShootingPlace());
            assertEquals(photo.getDescription(), photo2.getDescription());
            assertEquals(photo.getCamera(), photo2.getCamera());
            assertEquals(photo.getLens(), photo2.getLens());
            assertEquals(photo.getFilm(), photo2.getFilm());
            assertTrue(photo.getShutterSpeed() == photo2.getShutterSpeed());
            assertTrue(photo.getFilmSpeed() == photo2.getFilmSpeed());
            assertTrue(photo.getFocalLength() == photo2.getFocalLength());
            assertTrue(photo.getFStop() == photo2.getFStop());
            assertTrue(photo.getUid() == photo2.getUid());
        } catch (PhotoNotFoundException e) {
            fail("inserted photo not found");
        }
        photo.delete();
    }

    public void testPhotoDeletion() {
        PhotoInfo photo = PhotoInfo.create();
        assertNotNull(photo);
        Connection conn = ImageDb.getConnection();
        String sql = "SELECT * FROM photos WHERE photo_id = " + photo.getUid();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            if (!rs.next()) {
                fail("Matching DB record not found");
            }
        } catch (SQLException e) {
            fail("DB error:; " + e.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e) {
                }
            }
        }
        photo.delete();
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                fail("Found matching DB record after delete");
            }
        } catch (SQLException e) {
            fail("DB error:; " + e.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public void testInstanceAddition() {
        File testFile = new File(testImgDir, "test1.jpg");
        File instanceFile = VolumeBase.getDefaultVolume().getFilingFname(testFile);
        try {
            FileUtils.copyFile(testFile, instanceFile);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        PhotoInfo photo = PhotoInfo.create();
        assertNotNull(photo);
        int numInstances = photo.getNumInstances();
        photo.addInstance(VolumeBase.getDefaultVolume(), instanceFile, ImageInstance.INSTANCE_TYPE_ORIGINAL);
        assertEquals(numInstances + 1, photo.getNumInstances());
        Vector instances = photo.getInstances();
        assertEquals(instances.size(), numInstances + 1);
        File testFile2 = new File(testImgDir, "test2.jpg");
        File instanceFile2 = VolumeBase.getDefaultVolume().getFilingFname(testFile2);
        try {
            FileUtils.copyFile(testFile2, instanceFile2);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        numInstances = photo.getNumInstances();
        ImageInstance inst = ImageInstance.create(VolumeBase.getDefaultVolume(), instanceFile2);
        photo.addInstance(inst);
        assertEquals(numInstances + 1, photo.getNumInstances());
        instances = photo.getInstances();
        assertEquals(instances.size(), numInstances + 1);
        boolean found1 = false;
        boolean found2 = false;
        for (int i = 0; i < photo.getNumInstances(); i++) {
            ImageInstance ifile = photo.getInstance(i);
            if (ifile.getImageFile().equals(instanceFile)) {
                found1 = true;
            }
            if (ifile.getImageFile().equals(instanceFile2)) {
                found2 = true;
            }
        }
        assertTrue("Image instance 1 not found", found1);
        assertTrue("Image instance 2 not found", found2);
        photo.delete();
    }

    public void testCreationFromImage() {
        String fname = "test1.jpg";
        File f = new File(testImgDir, fname);
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.addToDB(f);
        } catch (PhotoNotFoundException e) {
            fail("Could not find photo: " + e.getMessage());
        }
        assertNotNull(photo);
        assertTrue(photo.getNumInstances() > 0);
        photo.delete();
    }

    /**
       Test that an exception is generated when trying to add
       nonexisting file to DB
    */
    public void testfailedCreation() {
        String fname = "test1.jpg";
        File f = new File(nonExistingDir, fname);
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.addToDB(f);
            fail("Image file should have been nonexistent");
        } catch (PhotoNotFoundException e) {
        }
    }

    /**
       Test that creating a new thumbnail using createThumbnail works
     */
    public void testThumbnailCreate() {
        String fname = "test1.jpg";
        File f = new File(testImgDir, fname);
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.addToDB(f);
        } catch (PhotoNotFoundException e) {
            fail("Could not find photo: " + e.getMessage());
        }
        assertNotNull(photo);
        int instanceCount = photo.getNumInstances();
        photo.createThumbnail();
        assertEquals("InstanceNum should be 1 greater after adding thumbnail", instanceCount + 1, photo.getNumInstances());
        boolean foundThumbnail = false;
        ImageInstance thumbnail = null;
        for (int n = 0; n < instanceCount + 1; n++) {
            ImageInstance instance = photo.getInstance(n);
            if (instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_THUMBNAIL) {
                foundThumbnail = true;
                thumbnail = instance;
                break;
            }
        }
        assertTrue("Could not find the created thumbnail", foundThumbnail);
        assertEquals("Thumbnail width should be 100", 100, thumbnail.getWidth());
        File thumbnailFile = thumbnail.getImageFile();
        assertTrue("Image file does not exist", thumbnailFile.exists());
        Thumbnail thumb = photo.getThumbnail();
        assertNotNull(thumb);
        assertFalse("Thumbnail exists, should not return default thumbnail", thumb == Thumbnail.getDefaultThumbnail());
        assertEquals("Thumbnail exists, getThumbnail should not create a new instance", instanceCount + 1, photo.getNumInstances());
        PhotoInfo photo2 = null;
        try {
            photo2 = PhotoInfo.retrievePhotoInfo(photo.getUid());
        } catch (PhotoNotFoundException e) {
            fail("Photo not storein into DB");
        }
        foundThumbnail = false;
        ImageInstance thumbnail2 = null;
        for (int n = 0; n < instanceCount + 1; n++) {
            ImageInstance instance = photo2.getInstance(n);
            if (instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_THUMBNAIL) {
                foundThumbnail = true;
                thumbnail2 = instance;
                break;
            }
        }
        assertTrue("Could not find the created thumbnail", foundThumbnail);
        assertEquals("Thumbnail width should be 100", 100, thumbnail2.getWidth());
        assertTrue("Thumbnail filename not saved correctly", thumbnailFile.equals(thumbnail2.getImageFile()));
        photo.delete();
        assertFalse("Image file does exist after delete", thumbnailFile.exists());
    }

    /**
       Tests thumbnail creation when there are no photo instances.
    */
    public void testThumbnailCreateNoInstances() throws Exception {
        PhotoInfo photo = PhotoInfo.create();
        try {
            photo.createThumbnail();
            assertEquals("Should not create a thumbnail instance when there are no original", 0, photo.getNumInstances());
        } catch (Exception e) {
            throw e;
        } finally {
            photo.delete();
        }
    }

    /**
       Tests thumbnail creation when the database is corrupted & files
       that photo instances refer to do not exist.
    */
    public void testThumbnailCreateCorruptInstances() throws Exception {
        String fname = "test1.jpg";
        File f = new File(testImgDir, fname);
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.addToDB(f);
        } catch (PhotoNotFoundException e) {
            fail("Could not find photo: " + e.getMessage());
        }
        int numInstances = photo.getNumInstances();
        for (int n = 0; n < numInstances; n++) {
            ImageInstance instance = photo.getInstance(n);
            File instFile = instance.getImageFile();
            instFile.delete();
        }
        photo.createThumbnail();
        try {
            Thumbnail thumb = photo.getThumbnail();
            assertNotNull(thumb);
            assertTrue("Database is corrupt, should return error thumbnail", thumb == Thumbnail.getErrorThumbnail());
            assertEquals("Database is corrupt, getThumbnail should not create a new instance", numInstances, photo.getNumInstances());
        } finally {
            photo.delete();
        }
    }

    /**
       Test that creating a new thumbnail using getThumbnail works
     */
    public void testGetThumbnail() {
        String fname = "test1.jpg";
        File f = new File(testImgDir, fname);
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.addToDB(f);
        } catch (PhotoNotFoundException e) {
            fail("Could not find photo: " + e.getMessage());
        }
        assertNotNull(photo);
        int instanceCount = photo.getNumInstances();
        Thumbnail thumb = photo.getThumbnail();
        assertNotNull(thumb);
        assertFalse("Thumbnail exists, should not return default thumbnail", thumb == Thumbnail.getDefaultThumbnail());
        assertEquals("Thumbnail exists, getThumbnail should not create a new instance", instanceCount + 1, photo.getNumInstances());
        assertEquals("InstanceNum should be 1 greater after adding thumbnail", instanceCount + 1, photo.getNumInstances());
        boolean foundThumbnail = false;
        ImageInstance thumbnail = null;
        for (int n = 0; n < instanceCount + 1; n++) {
            ImageInstance instance = photo.getInstance(n);
            if (instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_THUMBNAIL) {
                foundThumbnail = true;
                thumbnail = instance;
                break;
            }
        }
        assertTrue("Could not find the created thumbnail", foundThumbnail);
        assertEquals("Thumbnail width should be 100", 100, thumbnail.getWidth());
        File thumbnailFile = thumbnail.getImageFile();
        assertTrue("Image file does not exist", thumbnailFile.exists());
        photo.delete();
        assertFalse("Image file does exist after delete", thumbnailFile.exists());
    }

    /**
       Test getThumbnail in situation where there is no image instances for the PhotoInfo
    */
    public void testThumbWithNoInstances() {
        log.setLevel(org.apache.log4j.Level.DEBUG);
        org.apache.log4j.Logger photoLog = org.apache.log4j.Logger.getLogger(PhotoInfo.class.getName());
        photoLog.setLevel(org.apache.log4j.Level.DEBUG);
        PhotoInfo photo = PhotoInfo.create();
        Thumbnail thumb = photo.getThumbnail();
        assertTrue("getThumbnail should return error thumbnail", thumb == Thumbnail.getErrorThumbnail());
        assertEquals("No new instances should have been created", 0, photo.getNumInstances());
        File testFile = new File(testImgDir, "test1.jpg");
        if (!testFile.exists()) {
            fail("could not find test file " + testFile);
        }
        File instanceFile = VolumeBase.getDefaultVolume().getFilingFname(testFile);
        try {
            FileUtils.copyFile(testFile, instanceFile);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        photo.addInstance(VolumeBase.getDefaultVolume(), instanceFile, ImageInstance.INSTANCE_TYPE_ORIGINAL);
        Thumbnail thumb2 = photo.getThumbnail();
        log.setLevel(org.apache.log4j.Level.WARN);
        photoLog.setLevel(org.apache.log4j.Level.WARN);
        assertFalse("After instance addition, getThumbnail should not return default thumbnail", thumb == thumb2);
        assertEquals("There should be 2 instances: original & thumbnail", 2, photo.getNumInstances());
        photo.delete();
    }

    /**
       Test that thumbnail is rotated if prefRotation is nonzero
    */
    public void testThumbnailRotation() {
        String fname = "test1.jpg";
        File f = new File(testImgDir, fname);
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.addToDB(f);
        } catch (PhotoNotFoundException e) {
            fail("Could not find photo: " + e.getMessage());
        }
        photo.setPrefRotation(-45);
        Thumbnail thumb = photo.getThumbnail();
        File testFile = new File(testRefImageDir, "thumbnailRotation1.png");
        assertTrue("Thumbnail with 45 deg rotation does not match", org.photovault.test.ImgTestUtils.compareImgToFile(thumb.getImage(), testFile));
        photo.setPrefRotation(-90);
        thumb = photo.getThumbnail();
        testFile = new File(testRefImageDir, "thumbnailRotation2.png");
        assertTrue("Thumbnail with 90 deg rotation does not match", org.photovault.test.ImgTestUtils.compareImgToFile(thumb.getImage(), testFile));
        photo.delete();
    }

    /**
       PhotoInfoListener used for test cases
    */
    class TestListener implements PhotoInfoChangeListener {

        public boolean isNotified = false;

        public void photoInfoChanged(PhotoInfoChangeEvent e) {
            isNotified = true;
        }
    }

    /**
       Tests that the listener is working correctly
    */
    public void testListener() {
        PhotoInfo photo = PhotoInfo.create();
        TestListener l1 = new TestListener();
        TestListener l2 = new TestListener();
        photo.addChangeListener(l1);
        photo.addChangeListener(l2);
        photo.setPhotographer("TEST");
        assertTrue("l1 was not notified", l1.isNotified);
        assertTrue("l2 was not notified", l2.isNotified);
        photo.removeChangeListener(l2);
        l1.isNotified = false;
        l2.isNotified = false;
        photo.setPhotographer("TEST2");
        assertTrue("l1 was not notified", l1.isNotified);
        assertFalse("l2 was not supposed to be notified", l2.isNotified);
        l1.isNotified = false;
        photo.setShootingPlace("TEST");
        assertTrue("no notification when changing shootingPlace", l1.isNotified);
        l1.isNotified = false;
        photo.setFStop(12);
        assertTrue("no notification when changing f-stop", l1.isNotified);
        l1.isNotified = false;
        photo.setFocalLength(10);
        assertTrue("no notification when changing focalLength", l1.isNotified);
        l1.isNotified = false;
        photo.setShootTime(new java.util.Date());
        assertTrue("no notification when changing shooting time", l1.isNotified);
        l1.isNotified = false;
        photo.setShutterSpeed(1.0);
        assertTrue("no notification when changing shutter speed", l1.isNotified);
        l1.isNotified = false;
        photo.setCamera("Leica");
        assertTrue("no notification when changing camera", l1.isNotified);
        l1.isNotified = false;
        photo.setLens("TESTLENS");
        assertTrue("no notification when changing lens", l1.isNotified);
        l1.isNotified = false;
        photo.setFilm("Pan-X");
        assertTrue("no notification when changing film", l1.isNotified);
        l1.isNotified = false;
        photo.setFilmSpeed(160);
        assertTrue("no notification when changing film speed", l1.isNotified);
        l1.isNotified = false;
        photo.setPrefRotation(107);
        assertTrue("no notification when changing preferred rotation", l1.isNotified);
        l1.isNotified = false;
        photo.setDescription("Test with lots of characters");
        assertTrue("no notification when changing description", l1.isNotified);
        photo.delete();
    }

    /**
       Test normal case of exporting image from database
    */
    public void testExport() {
        String fname = "test1.jpg";
        File f = new File(testImgDir, fname);
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.addToDB(f);
        } catch (PhotoNotFoundException e) {
            fail("Could not find photo: " + e.getMessage());
        }
        photo.setPrefRotation(-90);
        File exportFile = new File("/tmp/exportedImage.png");
        try {
            photo.exportPhoto(exportFile, 400, 400);
        } catch (PhotovaultException e) {
            fail(e.getMessage());
        }
        BufferedImage exportedImage = null;
        try {
            exportedImage = ImageIO.read(exportFile);
        } catch (IOException e) {
            fail("Could not read the exported image " + exportFile);
        }
        File exportRef = new File(testRefImageDir, "exportedImage.png");
        assertTrue("Exported image " + exportFile + " does not match reference " + exportRef, org.photovault.test.ImgTestUtils.compareImgToFile(exportedImage, exportRef));
        photo.delete();
    }

    /**
     
     */
    public void testOriginalHash() {
        String fname = "test1.jpg";
        File f = new File(testImgDir, fname);
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.addToDB(f);
        } catch (PhotoNotFoundException e) {
            fail("Could not find photo: " + e.getMessage());
        }
        byte hash[] = photo.getOrigInstanceHash();
        byte instanceHash[] = null;
        assertNotNull("No hash for original photo", hash);
        while (photo.getNumInstances() > 0) {
            ImageInstance i = photo.getInstance(0);
            photo.removeInstance(0);
            if (i.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL) {
                instanceHash = i.getHash();
            }
            i.delete();
        }
        assertTrue("PhotoInfo & origInstance hashes differ", Arrays.equals(hash, instanceHash));
        byte hash2[] = photo.getOrigInstanceHash();
        assertTrue("Hash after deleting instances is changed", Arrays.equals(hash, hash2));
        photo.delete();
    }

    /**
      Test that it is possible to find a PhotoInfo based on original's hash code
     */
    public void testRetrievalByHash() {
        String fname = "test1.jpg";
        File f = new File(testImgDir, fname);
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.addToDB(f);
        } catch (PhotoNotFoundException e) {
            fail("Could not find photo: " + e.getMessage());
        }
        byte[] hash = photo.getOrigInstanceHash();
        PhotoInfo[] photos = PhotoInfo.retrieveByOrigHash(hash);
        assertNotNull("No Photos with matching hash found!!", photos);
        boolean found = false;
        for (int n = 0; n < photos.length; n++) {
            if (photos[n] == photo) {
                found = true;
            }
        }
        assertTrue("Photo not found by original hash", found);
    }

    public void testRawSettings() {
        PhotoInfo p = PhotoInfo.create();
        double chanMul[] = { 1., .7, .5, .7 };
        double daylightMul[] = { .3, .5, .7 };
        RawConversionSettings rs = RawConversionSettings.create(chanMul, daylightMul, 16000, 0, -.5, 0., RawConversionSettings.WB_MANUAL, false);
        p.setRawSettings(rs);
        RawConversionSettings rs2 = p.getRawSettings();
        assertTrue(rs.equals(rs2));
        assertEquals(16000, rs2.getWhite());
    }

    public void testRetrievalByHashNoPhoto() {
        byte[] hash = new byte[16];
        for (int n = 0; n < 16; n++) {
            hash[n] = 0;
        }
        PhotoInfo[] photos = PhotoInfo.retrieveByOrigHash(hash);
        assertNull("retrieveByOrigHash should result null", photos);
    }

    public static void main(String[] args) {
        log.setLevel(org.apache.log4j.Level.DEBUG);
        org.apache.log4j.Logger photoLog = org.apache.log4j.Logger.getLogger(PhotoInfo.class.getName());
        photoLog.setLevel(org.apache.log4j.Level.DEBUG);
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(Test_PhotoInfo.class);
    }
}
