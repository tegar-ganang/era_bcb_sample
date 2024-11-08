package com.magnatune.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import junit.framework.Assert;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;
import com.evolution.player.common.ext.core.DB;
import com.evolution.player.core.media.LocalMediaInfo;
import com.evolution.player.core.media.MediaInfo;
import com.magnatune.internal.core.MagnatuneCore;
import com.magnatune.internal.core.db.MagnatuneAlbumInfo;
import com.magnatune.internal.core.db.MagnatuneArtistInfo;
import com.magnatune.internal.core.db.MagnatuneDB;
import com.magnatune.internal.core.db.MagnatuneDBLoader;
import com.magnatune.internal.core.db.MagnatuneDBParser;
import com.magnatune.internal.core.db.MagnatuneSongInfo;
import com.magnatune.internal.core.db.MagnatuneDB.FileProvider;

/**
 * @since 0.7
 */
public class DBTests {

    private static int COUNTER = 0;

    private IProject fProject;

    private File fDownloadFolder;

    ;

    @Before
    public void setup() throws Exception {
        fProject = ResourcesPlugin.getWorkspace().getRoot().getProject("DownloadTest" + (COUNTER++));
        fProject.create(new NullProgressMonitor());
        fProject.open(new NullProgressMonitor());
        fDownloadFolder = new File(fProject.getLocation().toFile(), "downloads");
        fDownloadFolder.mkdir();
    }

    @Test
    public void download() throws Exception {
        MagnatuneDBLoader dbLoader = new MagnatuneDBLoader();
        DB db = dbLoader.load(new NullProgressMonitor());
        assertDB((MagnatuneDB) db);
    }

    @Test
    public void parseIssue114() throws Exception {
        File xml = TestPlugin.getDefault().getFileInPlugin(new Path("resources/db/song_info_114.xml"));
        DB db = new MagnatuneDBParser().parse(new FileInputStream(xml));
        assertDB((MagnatuneDB) db);
    }

    @Test
    public void defaultTest() throws Exception {
        MagnatuneDB db = MagnatuneCore.getMagnatuneDBAccessor().getUnsynchronized();
        assertDB(db);
    }

    @Test
    public void defaultYoungest() throws Exception {
        FileProvider oldProvider = MagnatuneDB.fgFileProvider;
        final File file1 = new File("./magnatuneDB-1.db");
        final File file2 = new File("./magnatuneDB-2.db");
        try {
            copy(TestPlugin.getDefault().getFileInPlugin(new Path("resources/db/magnatuneDB-1.db")), file1);
            copy(TestPlugin.getDefault().getFileInPlugin(new Path("resources/db/magnatuneDB-2.db")), file2);
            MagnatuneDB.fgFileProvider = new FileProvider() {

                @Override
                public File[] getFiles() {
                    return new File[] { file1, file2 };
                }
            };
            MagnatuneDB db = MagnatuneCore.getMagnatuneDBAccessor().getUnsynchronized();
            Assert.assertNull(db.getArtist("Joram - 1"));
            Assert.assertNotNull(db.getArtist("Joram - 2"));
            Assert.assertTrue(!file1.exists());
            Assert.assertTrue(file2.exists());
        } finally {
            MagnatuneDB.fgFileProvider = oldProvider;
            MagnatuneDB.fgDefault = null;
            file1.delete();
            file2.delete();
        }
    }

    private static void assertDB(MagnatuneDB db) throws CoreException {
        LocalMediaInfo media = MediaInfo.create(TestPlugin.getDefault().getFileInPlugin(new Path("resources/utils/test.mp3")));
        MagnatuneArtistInfo artist = db.getArtist(media);
        Assert.assertNotNull(artist);
        Assert.assertEquals("Daniel Berkman", artist.getArtistName());
        Assert.assertEquals(media.getArtistName(), artist.getArtistName());
        MagnatuneAlbumInfo album = artist.getAlbum(media);
        Assert.assertNotNull(album);
        Assert.assertEquals("Calabashmoon", album.getAlbumName());
        Assert.assertEquals(media.getAlbumName(), album.getAlbumName());
        MagnatuneSongInfo song = album.getSong(media);
        Assert.assertNotNull(song);
        Assert.assertEquals("folkways", song.getTrackName());
        Assert.assertEquals(media.getSongName(), song.getTrackName());
        Assert.assertEquals("http://he3.magnatune.com/all/01-folkways-Daniel%20Berkman.mp3", song.getDownloadUrl());
        MagnatuneArtistInfo adam = db.getArtist("Adam Fielding");
        Assert.assertNotNull(adam);
        MagnatuneAlbumInfo distance = adam.getAlbum("Distant Activity");
        Assert.assertNotNull(distance);
        Assert.assertEquals("http://he3.magnatune.com/music/Adam%20Fielding/Distant%20Activity/cover_200.jpg", distance.getCoverUrl());
        MagnatuneSongInfo nowSong = distance.getSong("Don't Look Now");
        Assert.assertNotNull(nowSong);
        String downloadURL = nowSong.getDownloadUrl().replaceAll("%27", "'");
        Assert.assertEquals("http://he3.magnatune.com/all/04-Don't%20Look%20Now-Adam%20Fielding.mp3", downloadURL);
    }

    private static void copy(File source, File target) throws IOException {
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(source);
            to = new FileOutputStream(target);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
            }
        }
    }
}
