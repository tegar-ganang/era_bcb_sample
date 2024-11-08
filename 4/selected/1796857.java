package org.ialbum.core.util.tests;

import java.io.File;
import java.nio.channels.*;
import java.util.logging.*;
import java.awt.Image;
import org.ialbum.core.mod.Album;
import org.ialbum.core.mod.AlbumImage;
import org.ialbum.core.util.AlbumManager;
import org.ialbum.ui.swing.tests.TestImageViewer;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class AlbumManagerTest {

    private static Logger logger = Logger.getLogger(AlbumManagerTest.class.getName());

    public static void main(String args[]) throws Exception {
        setupLogging();
        try {
            test001a(args);
        } catch (Exception e) {
            System.out.println("e" + e);
            e.printStackTrace();
        }
    }

    public static void setupLogging() throws Exception {
        Logger gl = Logger.getLogger("com.pm");
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        gl.addHandler(consoleHandler);
        gl.setLevel(Level.FINEST);
    }

    public static void copyFile(File in, File out) throws Exception {
        FileChannel sourceChannel = new FileInputStream(in).getChannel();
        FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }

    public static String getAlbumDirectoryPath() throws Exception {
        String directoryPath = "C:\\pro\\dev\\apps\\album\\sb\\albumTest";
        return directoryPath;
    }

    public static void setupAlbum() throws Exception {
        File albumXmlFileTemplate = new File(getAlbumDirectoryPath() + File.separator + "album-info.xml.template");
        File albumXmlFile = new File(getAlbumDirectoryPath() + File.separator + "album-info.xml");
        logger.fine("copy " + albumXmlFileTemplate.getName() + " to " + albumXmlFile.getName());
        copyFile(albumXmlFileTemplate, albumXmlFile);
    }

    public static void test003(String args[]) throws Exception {
        setupAlbum();
        File albumDirectoryPath = new File(getAlbumDirectoryPath());
        Album album = AlbumManager.loadAlbum(albumDirectoryPath);
        AlbumManager albumManager = AlbumManager.getInstance(album);
        albumManager.saveAlbum();
    }

    public static void test002(String args[]) throws Exception {
        File albumDirectoryPath = new File(getAlbumDirectoryPath());
        Album album = AlbumManager.loadAlbum(albumDirectoryPath);
    }

    public static void test001a(String args[]) throws Exception {
        Album album = AlbumFactory.createTestAlbumFromMemory();
        AlbumManager albumManager = AlbumManager.getInstance(album);
        albumManager.saveAlbum();
    }

    public static void test001(String args[]) throws Exception {
        Album album = AlbumFactory.createTestAlbumFromMemory();
        AlbumManager albumManager = AlbumManager.getInstance(album);
        AlbumImage albumImage = (AlbumImage) album.getImageList().get(0);
        Image image = albumManager.getImage(albumImage);
        System.out.println("image = " + image);
        TestImageViewer dialog = new TestImageViewer(new java.awt.Frame(), true);
        dialog.setSize(600, 350);
        dialog.getImageViewer().setImage(image);
        dialog.show();
    }
}
