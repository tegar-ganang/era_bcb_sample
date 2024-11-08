package net.sourceforge.x360mediaserve.plugins.jaudiotagger.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.LogManager;
import net.sourceforge.x360mediaserve.api.database.items.media.AudioItem;
import net.sourceforge.x360mediaserve.api.formats.Container;
import net.sourceforge.x360mediaserve.api.formats.Tagger;
import net.sourceforge.x360mediaserve.api.formats.TaggingException;
import net.sourceforge.x360mediaserve.util.database.items.media.AudioItemImp;
import net.sourceforge.x360mediaserve.util.database.items.media.formats.AudioInformationImp;
import net.sourceforge.x360mediaserve.util.database.items.media.resources.AudioResourceImp;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.datatype.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JAudioTagger implements Tagger {

    static Logger logger = LoggerFactory.getLogger(JAudioTagger.class);

    static String[] supportedMimeTypes = { "audio/mpeg", "audio/ogg", "audio/flac", "audio/x-ms-wma" };

    public String getName() {
        return "JAudioTagger";
    }

    @SuppressWarnings("deprecation")
    public JAudioTagger() {
        try {
            LogManager.getLogManager().readConfiguration(new java.io.StringBufferInputStream("org.jaudiotagger.level = OFF"));
        } catch (SecurityException e) {
            logger.error("Errror disabling jaudiologger", e);
        } catch (IOException e) {
            logger.error("Errror disabling jaudiologger", e);
        }
    }

    private String getMimeTypeFromAudioFile(AudioFile file) {
        logger.debug("Getting type for:{} {}", file.getAudioHeader().getFormat(), file.getAudioHeader().getEncodingType());
        if (file.getAudioHeader().getFormat().equals("MPEG-1 Layer 3")) {
            return "audio/mpeg";
        } else if (file.getAudioHeader().getFormat().equals("FLAC 16 bits")) {
            return "audio/flac";
        } else if (file.getAudioHeader().getFormat().equals("Ogg Vorbis v1")) {
            return "audio/ogg";
        }
        return file.getAudioHeader().getFormat();
    }

    public Integer readAudioChannels(AudioFile file) {
        String channelString = file.getAudioHeader().getChannels();
        try {
            Integer result = Integer.parseInt(channelString);
            logger.debug("Got channels:{}", result);
            return result;
        } catch (Exception e) {
        }
        channelString = channelString.toLowerCase();
        if (channelString.contains("stereo")) {
            return 2;
        } else if (channelString.contains("mono")) {
            return 1;
        }
        return null;
    }

    public AudioItem getTag(File file, String mimeType) throws TaggingException {
        logger.debug("Trying to tag:{}", file);
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            AudioHeader header = audioFile.getAudioHeader();
            AudioItemImp audioItem = new AudioItemImp();
            audioItem.setName(tag.getFirstTitle());
            audioItem.setArtistName(tag.getFirstArtist());
            audioItem.setAlbumName(tag.getFirstAlbum());
            audioItem.setGenreName(tag.getFirstGenre());
            try {
                String trackStr = tag.getFirstTrack();
                if (trackStr != null && trackStr.length() > 0) {
                    audioItem.setAlbumOrder(Integer.parseInt(trackStr.split("/", 2)[0]));
                }
            } catch (Exception e) {
                logger.error("Error getting album order for file:{}", file);
            }
            int length = header.getTrackLength();
            audioItem.setDuration((long) length * 1000);
            List<Artwork> artwork = tag.getArtworkList();
            if (artwork.size() > 0) {
                for (Artwork art : artwork) {
                    logger.debug("Art type:" + art.getMimeType());
                    logger.debug(art.getDescription());
                    audioItem.setAlbumArtFormat(art.getMimeType());
                    audioItem.setAlbumArtLocation(file.toURI().toString());
                }
            }
            AudioInformationImp information = new AudioInformationImp();
            information.setBitrate(header.getBitRateAsNumber());
            information.setSampleRate((long) header.getSampleRateAsNumber());
            information.setNumberAudioChannels(readAudioChannels(audioFile));
            AudioResourceImp audioResource = new AudioResourceImp();
            audioResource.setAudioInformation(information);
            audioResource.setMimeType(getMimeTypeFromAudioFile(audioFile));
            audioResource.setSize(file.length());
            audioResource.setDuration(audioItem.getDuration());
            audioResource.setLocation(file.toURI().toString());
            audioItem.setFirstResource(audioResource);
            return audioItem;
        } catch (Exception e) {
            logger.error("Error with jaudiotagger", e);
            logger.error("File {}", file);
            throw new TaggingException("Error with jaudioTagger:", e);
        }
    }

    public boolean supportsContainer(Container container) {
        for (String type : supportedMimeTypes) {
            if (type.equals(container.mimeType)) return true;
        }
        return false;
    }

    public String[] getSupportedMimeTypes() {
        return supportedMimeTypes;
    }

    public byte[] getThumbnailData(File file) {
        logger.debug("Trying to tag:{}", file);
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            AudioHeader header = audioFile.getAudioHeader();
            List<Artwork> artwork = tag.getArtworkList();
            if (artwork.size() > 0) {
                for (Artwork art : artwork) {
                    logger.info("Art type:" + art.getMimeType());
                    logger.info(art.getDescription());
                    return art.getBinaryData();
                }
            }
        } catch (CannotReadException e) {
            logger.error("Errror disabling jaudiologger", e);
        } catch (IOException e) {
            logger.error("Errror disabling jaudiologger", e);
        } catch (TagException e) {
            logger.error("Errror disabling jaudiologger", e);
        } catch (ReadOnlyFileException e) {
            logger.error("Errror disabling jaudiologger", e);
        } catch (InvalidAudioFrameException e) {
            logger.error("Errror disabling jaudiologger", e);
        }
        return null;
    }

    public boolean supportsThumbnail() {
        return true;
    }

    public int getDefaultPriority() {
        return 5;
    }

    public String getId() {
        return "jaudiotagger";
    }
}
