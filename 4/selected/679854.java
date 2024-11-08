package de.herberlin.pss.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.List;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.Tag;
import de.herberlin.pss.AppException;
import de.herberlin.pss.ServiceLocator;
import de.herberlin.pss.model.Artist;
import de.herberlin.pss.model.Song;
import de.herberlin.pss.ui.Frame;
import de.herberlin.pss.ui.SongTableModel;

/**
 * Import file into PSS
 * 
 * @author Hans Joachim Herbertz
 * @created 30.08.2008
 */
public class ImportAction extends AbstractProgressDialogAction {

    private File source = null;

    private List<Object> sourceList = null;

    private File destination = null;

    public ImportAction(String fileName) {
        try {
            source = getSourceFile(fileName);
        } catch (Exception e) {
            logger.error(e, e);
            Frame.showMessage(e.getLocalizedMessage(), e);
        }
    }

    public ImportAction(List<Object> fileList) {
        this.sourceList = fileList;
    }

    public ImportAction(File file) {
        this.source = file;
    }

    protected void doPerform() throws Exception {
        if (source != null) {
            doImport(source);
        } else if (sourceList != null) {
            for (Object n : sourceList) {
                try {
                    if (n instanceof File) {
                        doImport((File) n);
                    } else {
                        doImport(getSourceFile(n + ""));
                    }
                } catch (Exception e) {
                    logger.error(e, e);
                    Frame.showMessage(e.getLocalizedMessage(), e);
                    rollback();
                }
            }
        }
        SongTableModel.getInstance().updateContent();
    }

    private void doImport(File file) {
        destination = null;
        AudioFile audioFile = null;
        try {
            audioFile = readMetadata(file);
        } catch (Exception e) {
            logger.error(e, e);
            throw new AppException(ServiceLocator.getText("Error.file.type") + file.getName(), e);
        }
        try {
            if (!copyFile(file)) {
                return;
            }
        } catch (Exception e) {
            logger.error(e, e);
            throw new AppException(ServiceLocator.getText("Error.copy.file") + file, e);
        }
        Tag tag = audioFile.getTag();
        if (tag == null) {
            throw new AppException("Tag must not be null");
        }
        AudioHeader audioHeader = audioFile.getAudioHeader();
        if (audioHeader == null) {
            throw new AppException("AudioHeader must not be null!");
        }
        Song song = new Song();
        song.set(Song.Key.cSongTitle, fixDataEncoding(tag.getFirstTitle()));
        song.set(Song.Key.cFileName, file.getName());
        song.set(Song.Key.iAlbumId, DefaultDataAction.ALBUM_ID);
        song.set(Song.Key.iDirId, DefaultDataAction.DIR_ID);
        song.set(Song.Key.iTrackNr, 0);
        song.set(Song.Key.iTrackLength, audioHeader.getTrackLength());
        song.set(Song.Key.iNrPlayed, null);
        song.set(Song.Key.iFileSize, file.length());
        song.set(Song.Key.iGenreId, DefaultDataAction.GENRE_ID);
        song.set(Song.Key.iBitRate, (int) audioHeader.getBitRateAsNumber());
        song.set(Song.Key.iMediaType, 1);
        Artist artist = new Artist();
        artist.set(Artist.Key.cArtistName, fixDataEncoding(tag.getFirstArtist()));
        song.setArtist(artist);
        ServiceLocator.getDatabaseManager().storeSong(song);
        SongTableModel.getInstance().updateContent();
    }

    /**
	 * Fix a bug in the AudioIo; sometimes the last char of a tag entry is -1
	 * 
	 * @param source
	 * @return
	 */
    private String fixDataEncoding(String source) {
        String result = null;
        if (source == null) {
            result = null;
        } else if (Character.getNumericValue(source.charAt(source.length() - 1)) == -1) {
            result = source.substring(0, source.length() - 1);
        } else {
            result = source;
        }
        return result;
    }

    private File getSourceFile(String name) throws Exception {
        if (name.startsWith("file://")) {
            name = name.substring("file://".length());
        }
        File file = new File("" + URLDecoder.decode(name, "utf-8").trim() + "");
        logger.debug("file=" + file);
        if (!file.exists()) {
            String msg = "File: " + file + " does not exist.";
            logger.error(msg);
            throw new AppException(msg);
        }
        if (!file.canRead()) {
            String msg = "File: " + file + " cant read.";
            logger.error(msg);
            throw new AppException(msg);
        }
        return file;
    }

    private AudioFile readMetadata(File file) throws Exception {
        AudioFile audioFile = AudioFileIO.read(file);
        logger.debug("AudioFile created for: " + file);
        if (audioFile == null) {
            throw new AppException("Could not create AudioFile");
        }
        return audioFile;
    }

    private boolean copyFile(File file) throws Exception {
        destination = new File(ServiceLocator.getSqliteDir(), file.getName());
        logger.debug("Writing to: " + destination);
        if (destination.exists()) {
            Frame.showMessage(ServiceLocator.getText("Error.file.exists") + file.getName(), null);
            logger.debug("File already exists: " + file);
            return false;
        }
        destination.createNewFile();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(file);
            out = new FileOutputStream(destination);
            int read = 0;
            byte[] buffer = new byte[2048];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
        return true;
    }

    protected void doRollback() {
        if (destination != null) destination.delete();
    }
}
