package com.melloware.jukes.file.tag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.AbstractTagFrameBody;
import org.jaudiotagger.tag.id3.ID3v11Tag;
import org.jaudiotagger.tag.id3.ID3v24Frame;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.framebody.AbstractID3v2FrameBody;
import org.jaudiotagger.tag.id3.framebody.FrameBodyCOMM;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTALB;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTCON;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTDRC;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTENC;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTIT2;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTLEN;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTPE1;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTPE2;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTPOS;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTRCK;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;
import com.jgoodies.uif.application.Application;
import com.jgoodies.uif.util.ResourceUtils;
import com.melloware.jukes.exception.MusicTagException;
import com.melloware.jukes.gui.view.MainFrame;
import com.melloware.jukes.util.MessageUtil;

/**
 * MusicTag class used for editing MP3 file tags.  Both version ID3v1.1 and
 * IDv2.4 are supported.
 * <p>
 * The JAudioTagger (https://jaudiotagger.dev.java.net/) library is used to read
 * these types of Tags.
 * <p>
 * Copyright (c) 2006
 * Melloware, Inc. <http://www.melloware.com>
 * @author Emil A. Lefkof III <info@melloware.com>
 * @version 4.0
 * AZ (C) 2009,2010
 */
public final class Mp3Tag extends MusicTag {

    private static final Log LOG = LogFactory.getLog(Mp3Tag.class);

    public static final String V2_CODE_ALBUM = "TALB";

    public static final String V2_CODE_TITLE = "TIT2";

    public static final String V2_CODE_ARTIST = "TPE1";

    public static final String V2_CODE_ALBUM_ARTIST = "TPE2";

    public static final String V2_CODE_TXXX = "TXXX";

    public static final String V2_CODE_YEAR = "TDRC";

    public static final String V2_CODE_COMMENT = "COMM";

    public static final String V2_CODE_GENRE = "TCON";

    public static final String V2_CODE_TRACK = "TRCK";

    public static final String V2_CODE_ENCODE = "TENC";

    public static final String V2_CODE_LENGTH = "TLEN";

    private static final String LINE_BREAK = "\n\n";

    private String CODE_ALBUM_ARTIST = "ALBUM ARTIST";

    private String V2_CODE_DISC_NUMBER = "TPOS";

    private MP3File audioFile;

    /**
     * Constructor that takes a file.
     * <p>
     * @param aFile the music file
     * @throws MusicTagException if any error occurs reading file
     */
    public Mp3Tag(File aFile) throws MusicTagException {
        super(aFile);
        try {
            this.audioFile = new MP3File(aFile, MP3File.LOAD_ALL, true);
            initializeTags();
        } catch (IOException ex) {
            LOG.error("IOException " + ex.getMessage(), ex);
            throw new MusicTagException("IOException opening Music File Tag. " + aFile.getAbsolutePath() + LINE_BREAK + ex.getMessage());
        } catch (TagException ex) {
            LOG.error("TagException " + ex.getMessage(), ex);
            throw new MusicTagException("MusicTagException opening Music File Tag. " + aFile.getAbsolutePath() + LINE_BREAK + ex.getMessage());
        } catch (InvalidAudioFrameException ex) {
            LOG.error("InvalidAudioFrameException " + ex.getMessage(), ex);
            throw new MusicTagException("InvalidAudioFrameException opening Music File Tag. " + aFile.getAbsolutePath() + LINE_BREAK + ex.getMessage());
        } catch (RuntimeException ex) {
            LOG.error("RuntimeException " + ex.getMessage(), ex);
            throw new MusicTagException("RuntimeException opening Music File Tag. " + aFile.getAbsolutePath() + LINE_BREAK + ex.getMessage());
        } catch (Exception ex) {
            LOG.error("Exception " + ex.getMessage(), ex);
            throw new MusicTagException("Unexpected exception opening Music File Tag. " + aFile.getAbsolutePath() + LINE_BREAK + ex.getMessage());
        }
    }

    /**
     * Gets the artist.
     * <p>
     * @return Returns the artist.
     */
    public String getArtist() {
        CODE_ALBUM_ARTIST = settings.getAlbumArtistTag();
        if (StringUtils.isBlank(this.artist)) {
            String v1 = null;
            String v2 = null;
            String d1 = null;
            if (audioFile.hasID3v1Tag()) {
                v1 = StringUtils.defaultIfEmpty(audioFile.getID3v1Tag().getFirst(FieldKey.ARTIST), NO_TAG);
            }
            final AbstractID3v2Frame frame2 = loadFrame(V2_CODE_TXXX);
            if (frame2 != null) {
                final AbstractID3v2FrameBody frameBody = ((FrameBodyTXXX) frame2.getBody());
                d1 = ((FrameBodyTXXX) frameBody).getDescription().toUpperCase().trim();
                if (d1.equals(CODE_ALBUM_ARTIST)) {
                    v2 = ((FrameBodyTXXX) frameBody).getText().trim();
                }
            } else {
                final AbstractID3v2Frame frame1 = loadFrame(V2_CODE_ALBUM_ARTIST);
                if (frame1 != null) {
                    final AbstractID3v2FrameBody frameBody = ((FrameBodyTPE2) frame1.getBody());
                    v2 = ((FrameBodyTPE2) frameBody).getText().trim();
                }
            }
            if (v2 == null) {
                final AbstractID3v2Frame frame = loadFrame(V2_CODE_ARTIST);
                if (frame != null) {
                    final AbstractID3v2FrameBody frameBody = ((FrameBodyTPE1) frame.getBody());
                    v2 = ((FrameBodyTPE1) frameBody).getText();
                }
            }
            this.artist = StringUtils.defaultIfEmpty(v2, v1).trim();
        }
        return this.artist;
    }

    /**
     * Gets the maximum bitrate for this file.
     * <p>
     * @return Returns the maximum bitrate for this file
     */
    public Long getBitRate() {
        if (this.bitRate == null) {
            final Integer bitrate = (header == null) ? Integer.valueOf(1000) : ((Integer) header.get("mp3.bitrate.nominal.bps"));
            this.bitRate = Long.valueOf((bitrate.longValue() / 1000));
        }
        return this.bitRate;
    }

    /**
     * Gets the comment.
     * <p>
     * @return Returns the comment.
     */
    public String getComment() {
        if (StringUtils.isBlank(this.comment)) {
            String v1 = null;
            String v2 = null;
            if (audioFile.hasID3v1Tag()) {
                v1 = StringUtils.defaultIfEmpty(audioFile.getID3v1Tag().getFirst(FieldKey.COMMENT), " ");
            }
            final AbstractID3v2Frame frame = loadFrame(V2_CODE_COMMENT);
            if (frame != null) {
                final AbstractID3v2FrameBody frameBody = ((FrameBodyCOMM) frame.getBody());
                v2 = ((FrameBodyCOMM) frameBody).getText();
            }
            this.comment = StringUtils.defaultIfEmpty(v2, v1);
        }
        return this.comment;
    }

    public String getCopyrighted() {
        return Boolean.toString(audioFile.getMP3AudioHeader().isCopyrighted());
    }

    /**
     * Gets the disc.
     * <p>
     * @return Returns the disc.
     */
    public String getDisc() {
        if (StringUtils.isBlank(this.disc)) {
            String v1 = null;
            String v2 = null;
            String v3 = null;
            if (audioFile.hasID3v1Tag()) {
                v1 = StringUtils.defaultIfEmpty(audioFile.getID3v1Tag().getFirst(FieldKey.ALBUM), NO_TAG);
            }
            if (settings.isUseCDNumber()) {
                final AbstractID3v2Frame frame2 = loadFrame(V2_CODE_DISC_NUMBER);
                if (frame2 != null) {
                    final AbstractID3v2FrameBody frameBody = ((FrameBodyTPOS) frame2.getBody());
                    v3 = ((FrameBodyTPOS) frameBody).getText().trim();
                }
            }
            final AbstractID3v2Frame frame = loadFrame(V2_CODE_ALBUM);
            if (frame != null) {
                final AbstractID3v2FrameBody frameBody = ((FrameBodyTALB) frame.getBody());
                v2 = ((FrameBodyTALB) frameBody).getText();
                v2 = v2.trim();
                if ((v3 != null) & (v3.length() != 0)) {
                    v3 = " - CD " + v3;
                    v2 = v2.concat(v3);
                }
            }
            this.disc = StringUtils.defaultIfEmpty(v2, v1).trim();
        }
        return this.disc;
    }

    public String getEmphasis() {
        return audioFile.getMP3AudioHeader().getEmphasis();
    }

    /**
     * Gets the encodedBy.
     * <p>
     * @return Returns the encodedBy.
     */
    public String getEncodedBy() {
        if (StringUtils.isBlank(this.encodedBy)) {
            final AbstractID3v2Frame frame = loadFrame(V2_CODE_ENCODE);
            if (frame != null) {
                final AbstractID3v2FrameBody frameBody = ((FrameBodyTENC) frame.getBody());
                this.encodedBy = ((FrameBodyTENC) frameBody).getText().trim();
            }
        }
        return this.encodedBy;
    }

    public String getFrequency() {
        return audioFile.getMP3AudioHeader().getSampleRate();
    }

    /**
     * Gets the genre.
     * <p>
     * @return Returns the genre.
     */
    public String getGenre() {
        if (StringUtils.isBlank(this.genre)) {
            String v1 = null;
            String v2 = null;
            if (audioFile.hasID3v1Tag()) {
                v1 = audioFile.getID3v1Tag().getFirst(FieldKey.GENRE);
                if (v1.trim().length() == 0) {
                    v1 = NO_TAG;
                }
            }
            final AbstractID3v2Frame frame = loadFrame(V2_CODE_GENRE);
            if (frame != null) {
                final AbstractID3v2FrameBody frameBody = ((FrameBodyTCON) frame.getBody());
                v2 = StringUtils.defaultIfEmpty(((FrameBodyTCON) frameBody).getText(), NO_TAG);
            }
            this.genre = StringUtils.defaultIfEmpty(v2, v1);
            if (this.genre.startsWith("(")) {
                final String genreString = this.genre.toString();
                if (genreString.indexOf(")") != -1) {
                    try {
                        int i = Integer.parseInt(genreString.substring(1, genreString.indexOf(")")));
                        this.genre = getStandardGenreType(i);
                    } catch (NumberFormatException nfe) {
                        LOG.error("NumberFormatException: " + nfe.getMessage());
                        final MainFrame mainFrame = (MainFrame) Application.getDefaultParentFrame();
                        MessageUtil.showError(mainFrame, "NumberFormatException: " + nfe.getMessage());
                    }
                }
            }
            final String s = Character.toString((char) 00);
            final String tempString = this.genre;
            if (tempString.endsWith(s)) {
                this.genre = tempString.substring(0, tempString.length() - 1);
            }
        }
        return this.genre;
    }

    public Map getHeader() {
        return header;
    }

    public String getLayer() {
        return audioFile.getMP3AudioHeader().getMpegLayer();
    }

    public String getMode() {
        return audioFile.getMP3AudioHeader().getChannels();
    }

    /**
     * Gets the title.
     * <p>
     * @return Returns the title.
     */
    public String getTitle() {
        if (StringUtils.isNotBlank(this.title)) {
            return this.title;
        }
        String v1 = null;
        String v2 = null;
        if (audioFile.hasID3v1Tag()) {
            v1 = StringUtils.defaultIfEmpty(audioFile.getID3v1Tag().getFirstTitle(), NO_TAG);
            if ((v1.length() == 30) || (v1.equals(NO_TAG)) || (v1.startsWith("Track"))) {
                v1 = extractTitleFromFilename();
                LOG.debug("Filename extracted");
            }
        }
        final AbstractID3v2Frame frame = loadFrame(V2_CODE_TITLE);
        if (frame != null) {
            final AbstractID3v2FrameBody frameBody = ((FrameBodyTIT2) frame.getBody());
            v2 = ((FrameBodyTIT2) frameBody).getText();
        }
        this.title = StringUtils.defaultIfEmpty(v2, v1).trim();
        return this.title;
    }

    /**
     * Gets the track.
     * <p>
     * @return Returns the track.
     */
    public String getTrack() {
        if (StringUtils.isBlank(this.track)) {
            String v1 = null;
            String v2 = null;
            if ((audioFile.hasID3v1Tag()) && (audioFile.getID3v1Tag() instanceof ID3v11Tag)) {
                v1 = ((ID3v11Tag) audioFile.getID3v1Tag()).getFirstTrack();
                v1 = StringUtils.defaultIfEmpty(v1, "X");
            }
            final AbstractID3v2Frame frame = loadFrame(V2_CODE_TRACK);
            if (frame != null) {
                final AbstractID3v2FrameBody frameBody = ((FrameBodyTRCK) frame.getBody());
                v2 = ((FrameBodyTRCK) frameBody).getText();
                v2 = StringUtils.defaultIfEmpty(v2, "X");
            }
            int track1 = 0;
            int track2 = 0;
            if (StringUtils.isNumeric(v1)) {
                track1 = Integer.parseInt(v1);
            }
            if (StringUtils.isNumeric(v2)) {
                track2 = Integer.valueOf(v2).intValue();
            }
            final int bestMatch = Math.max(track1, track2);
            this.track = StringUtils.leftPad(String.valueOf(bestMatch), 2, "0").trim();
        }
        return this.track;
    }

    /**
     * Gets the track length in seconds.
     * <p>
     * @return Returns the track length in seconds.
     */
    public long getTrackLength() {
        if (this.trackLength > 1) {
            return this.trackLength;
        }
        final Long duration = (header == null) ? Long.valueOf(100000) : ((Long) header.get("duration"));
        this.trackLength = ((duration.longValue() / 1000) / 1000);
        return this.trackLength;
    }

    public String getVersion() {
        return audioFile.getMP3AudioHeader().getMpegVersion();
    }

    /**
     * Gets the year.
     * <p>
     * @return Returns the year.
     */
    public String getYear() {
        if (StringUtils.isBlank(this.year)) {
            String v1 = null;
            String v2 = null;
            if (audioFile.hasID3v1Tag()) {
                v1 = audioFile.getID3v1Tag().getFirstYear();
                v1 = StringUtils.defaultIfEmpty(v1, CURRENT_YEAR);
            }
            final AbstractID3v2Frame frame = loadFrame(V2_CODE_YEAR);
            if (frame != null) {
                final AbstractID3v2FrameBody frameBody = ((FrameBodyTDRC) frame.getBody());
                v2 = ((FrameBodyTDRC) frameBody).getText();
            }
            this.year = StringUtils.defaultIfEmpty(v2, v1).trim();
        }
        return this.year;
    }

    /**
     * Sets the artist.
     * <p>
     * @param aArtist The artist to set.
     */
    public void setArtist(final String aArtist) {
        this.artist = StringUtils.defaultIfEmpty(aArtist, NO_TAG).trim();
        if (audioFile.hasID3v1Tag()) {
            audioFile.getID3v1Tag().setArtist(this.artist);
        }
        final AbstractID3v2Frame frame = loadFrame(V2_CODE_ARTIST);
        if (frame != null) {
            final AbstractID3v2FrameBody frameBody = ((FrameBodyTPE1) frame.getBody());
            ((FrameBodyTPE1) frameBody).setText(this.artist);
        }
    }

    /**
     * Sets the comment.
     * <p>
     * @param aComment The comment to set.
     */
    public void setComment(final String aComment) {
        this.comment = StringUtils.defaultIfEmpty(aComment, "").trim();
        if (audioFile.hasID3v1Tag()) {
            audioFile.getID3v1Tag().setComment(this.comment);
        }
        final AbstractID3v2Frame frame = loadFrame(V2_CODE_COMMENT);
        if (frame != null) {
            final AbstractID3v2FrameBody frameBody = ((FrameBodyCOMM) frame.getBody());
            ((FrameBodyCOMM) frameBody).setText(this.comment);
        }
    }

    /**
     * Sets the disc.
     * <p>
     * @param aDisc The disc to set.
     */
    public void setDisc(final String aDisc) {
        this.disc = StringUtils.defaultIfEmpty(aDisc, NO_TAG).trim();
        if (audioFile.hasID3v1Tag()) {
            audioFile.getID3v1Tag().setAlbum(this.disc);
        }
        final AbstractID3v2Frame frame = loadFrame(V2_CODE_ALBUM);
        if (frame != null) {
            final AbstractID3v2FrameBody frameBody = ((FrameBodyTALB) frame.getBody());
            ((FrameBodyTALB) frameBody).setText(this.disc);
        }
    }

    /**
     * Sets the Encoded By.
     * <p>
     * @param aEncodedBy The encoded by to set.
     */
    public void setEncodedBy(final String aEncodedBy) {
        this.encodedBy = aEncodedBy;
        final AbstractID3v2Frame frame = loadFrame(V2_CODE_ENCODE);
        if (frame != null) {
            final AbstractID3v2FrameBody frameBody = ((FrameBodyTENC) frame.getBody());
            ((FrameBodyTENC) frameBody).setText(encodedBy);
        }
    }

    /**
     * Sets the genre.
     * <p>
     * @param aGenre The genre to set.
     */
    public void setGenre(final String aGenre) {
        this.genre = StringUtils.defaultIfEmpty(aGenre, NO_TAG).trim();
        if (audioFile.hasID3v1Tag()) {
            audioFile.getID3v1Tag().setGenre(this.genre);
        }
        final AbstractID3v2Frame frame = loadFrame(V2_CODE_GENRE);
        if (frame != null) {
            final AbstractID3v2FrameBody frameBody = ((FrameBodyTCON) frame.getBody());
            ((FrameBodyTCON) frameBody).setText(this.genre);
        }
    }

    /**
     * Sets the title.
     * <p>
     * @param aTitle The title to set.
     */
    public void setTitle(final String aTitle) {
        this.title = StringUtils.defaultIfEmpty(aTitle, NO_TAG).trim();
        if (audioFile.hasID3v1Tag()) {
            audioFile.getID3v1Tag().setTitle(this.title);
        }
        final AbstractID3v2Frame frame = loadFrame(V2_CODE_TITLE);
        if (frame != null) {
            final AbstractID3v2FrameBody frameBody = ((FrameBodyTIT2) frame.getBody());
            ((FrameBodyTIT2) frameBody).setText(this.title);
        }
    }

    /**
     * Sets the track.
     * <p>
     * @param aTrack The track to set.
     */
    public void setTrack(final String aTrack) {
        setTrack(aTrack, 2);
    }

    /**
     * Sets the track.
     * <p>
     * @param aTrack The track to set.
     * @param aPadding the number of 0's to pad this track with
     */
    public void setTrack(final String aTrack, final int aPadding) {
        final String current = StringUtils.defaultIfEmpty(aTrack, "0").trim();
        this.track = StringUtils.leftPad(current, aPadding, "0").trim();
        if ((audioFile.hasID3v1Tag()) && (audioFile.getID3v1Tag() instanceof ID3v11Tag)) {
            ((ID3v11Tag) audioFile.getID3v1Tag()).setTrack(this.track);
        }
        final AbstractID3v2Frame frame = loadFrame(V2_CODE_TRACK);
        if (frame != null) {
            final AbstractID3v2FrameBody frameBody = ((FrameBodyTRCK) frame.getBody());
            ((FrameBodyTRCK) frameBody).setText(this.track);
        }
    }

    /**
     * Sets the trackLength.
     * <p>
     * @param aTrackLength The trackLength to set.
     */
    public void setTrackLength(final long aTrackLength) {
        this.trackLength = aTrackLength;
        if (audioFile.hasID3v2Tag()) {
            final AbstractID3v2Frame frame = loadFrame(V2_CODE_LENGTH);
            if (frame != null) {
                final AbstractID3v2FrameBody frameBody = ((FrameBodyTLEN) frame.getBody());
                ((FrameBodyTLEN) frameBody).setText(String.valueOf(this.trackLength * 1000));
            }
        }
    }

    /**
     * Sets the year.
     * <p>
     * @param aYear The year to set.
     */
    public void setYear(final String aYear) {
        this.year = StringUtils.defaultIfEmpty(aYear, CURRENT_YEAR).trim();
        if (audioFile.hasID3v1Tag()) {
            audioFile.getID3v1Tag().setYear(this.year);
        }
        final AbstractID3v2Frame frame = loadFrame(V2_CODE_YEAR);
        if (frame != null) {
            final AbstractID3v2FrameBody frameBody = ((FrameBodyTDRC) frame.getBody());
            ((FrameBodyTDRC) frameBody).setText(this.year);
        }
    }

    /**
     * Is this file a variable bit rate.
     * <p>
     * @return true if variable false if constant bit rate
     */
    public boolean isVBR() {
        return ((header == null) ? Boolean.FALSE : ((Boolean) header.get("mp3.vbr"))).booleanValue();
    }

    /**
     * Removes tags from the audio file.
     * <p>
     * @throws MusicTagException if any error occurs removing the tag
     */
    public void removeTags() throws MusicTagException {
        try {
            if (audioFile != null) {
                if (audioFile.hasID3v1Tag()) {
                    audioFile.setID3v1Tag(null);
                }
                if (audioFile.hasID3v2Tag()) {
                    audioFile.setID3v2Tag(null);
                }
                audioFile.save();
                initializeTags();
            }
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
            throw new MusicTagException("IOException removing Music File Tag.", ex);
        } catch (TagException ex) {
            LOG.error(ex.getMessage(), ex);
            throw new MusicTagException("TagException removing Music File Tag.", ex);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            throw new MusicTagException("Exception removing Music File Tag.", ex);
        }
    }

    /**
     * Renames this Music file based on a format from prefs.  The format is in
     * aFormat and can have values %n for track number, %t for title,
     * %a for artist, and %d for disc.    Replaces any invalid characters
     * (\\, /, :, , *, ?, ", <, >, or |) with underscores _ to prevent any
     * errors on file systems.
     *
     * Examples:
     * %n -%t = 01 - Track.mp3
     * %a - %d - %n - %t = Artist - Album - 01 - Track.mp3
     * <p>
     * @param aFormat the string format like %n -%t to rename 01 - Track.mp3
     * @return true if renamed, false if failure
     */
    public boolean renameFile(final String aFormat) {
        boolean result = false;
        try {
            final String newFileName = createFilenameFromFormat(aFormat);
            final File newFile = new File(newFileName);
            audioFile = null;
            result = this.file.renameTo(newFile);
            if (result) {
                this.file = newFile;
                audioFile = new MP3File(newFile);
                initializeTags();
            }
        } catch (IOException ex) {
            final MainFrame mainFrame = (MainFrame) Application.getDefaultParentFrame();
            final String errorMessage = ResourceUtils.getString("messages.ErrorRenamingFile");
            MessageUtil.showError(mainFrame, errorMessage);
            LOG.error(errorMessage, ex);
        } catch (TagException ex) {
            final MainFrame mainFrame = (MainFrame) Application.getDefaultParentFrame();
            MessageUtil.showError(mainFrame, "TagException");
            LOG.error("TagException", ex);
        } catch (ReadOnlyFileException ex) {
            final MainFrame mainFrame = (MainFrame) Application.getDefaultParentFrame();
            MessageUtil.showError(mainFrame, "ReadOnlyFileException");
            LOG.error("ReadOnlyFileException", ex);
        } catch (InvalidAudioFrameException ex) {
            final MainFrame mainFrame = (MainFrame) Application.getDefaultParentFrame();
            MessageUtil.showError(mainFrame, "InvalidAudioFrameException");
            LOG.error("InvalidAudioFrameException", ex);
        }
        return result;
    }

    /**
     * Saves the tag back to the file.
     * <p>
     * @throws MusicTagException if any error occurs saving the file
     */
    public void save() throws MusicTagException {
        try {
            if (audioFile != null) {
                this.synchronize();
                audioFile.save();
            }
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
            throw new MusicTagException("IOException saving Music File Tag " + this.file.getName(), ex);
        } catch (TagException ex) {
            LOG.error(ex.getMessage(), ex);
            throw new MusicTagException("MusicTagException saving Music File Tag " + this.file.getName(), ex);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            throw new MusicTagException("Error saving Music File Tag " + this.file.getName(), ex);
        }
    }

    /**
     * Initialize the V1 and V2 tags for this audio file.
     */
    private void initializeTags() {
        if (audioFile.hasID3v1Tag()) {
            if (audioFile.getID3v1Tag().getMajorVersion() == 0) {
                LOG.debug("Updating to v11 tag from old V10 tag.");
                final ID3v11Tag newTagVersion = new ID3v11Tag(audioFile.getID3v1Tag());
                audioFile.setID3v1Tag((ID3v11Tag) newTagVersion);
            }
        } else {
            LOG.debug("Create v11 tag.");
            audioFile.setID3v1Tag(new ID3v11Tag());
        }
        if (audioFile.hasID3v2Tag()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("V2 Version = " + audioFile.getID3v2Tag().getMajorVersion());
            }
            final AbstractID3v2Tag newTagVersion = audioFile.getID3v2TagAsv24();
            audioFile.setID3v2TagOnly(newTagVersion);
        }
        this.getDisc();
        this.getArtist();
        this.getComment();
        this.getGenre();
        this.getTitle();
        this.getTrack();
        this.getYear();
        this.getTrackLength();
        this.getEncodedBy();
        LOG.debug("Create v24 tag.");
        audioFile.setID3v2Tag(new ID3v24Tag());
        audioFile.setID3v2TagOnly(audioFile.getID3v2TagAsv24());
    }

    /**
    * Gets the V2 tag frame requested by the aFrameCode value.
    * <p>
    * @param aFrameCode the code such as TALB, COMM, TIT2
    * @return the AbstractID3v2Frame or null if not found
    */
    private AbstractID3v2Frame loadFrame(final String aFrameCode) {
        AbstractID3v2Frame frame = null;
        AbstractTagFrameBody frameBody = null;
        if (StringUtils.isBlank(aFrameCode)) {
            throw new IllegalArgumentException("aFrameCode must have a value.");
        }
        if (audioFile.hasID3v2Tag()) {
            final Object frames = audioFile.getID3v2Tag().getFrame(aFrameCode);
            if (frames instanceof AbstractID3v2Frame) {
                frame = (AbstractID3v2Frame) frames;
            } else if (frames instanceof ArrayList) {
                final ArrayList frameList = (ArrayList) frames;
                frame = (AbstractID3v2Frame) frameList.get(0);
            } else {
                frame = null;
            }
        } else {
            return null;
        }
        if (frame == null) {
            if (V2_CODE_ALBUM.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_ALBUM);
                frameBody = frame.getBody();
                final FrameBodyTALB body = (FrameBodyTALB) frameBody;
                body.setText("");
            } else if (V2_CODE_ARTIST.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_ARTIST);
                frameBody = frame.getBody();
                final FrameBodyTPE1 body = (FrameBodyTPE1) frameBody;
                body.setText("");
            } else if (V2_CODE_ALBUM_ARTIST.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_ALBUM_ARTIST);
                frameBody = frame.getBody();
                final FrameBodyTPE2 body = (FrameBodyTPE2) frameBody;
                body.setText("");
            } else if (V2_CODE_TXXX.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_TXXX);
                frameBody = frame.getBody();
                final FrameBodyTXXX body = (FrameBodyTXXX) frameBody;
                body.setText("");
            } else if (V2_CODE_DISC_NUMBER.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_DISC_NUMBER);
                frameBody = frame.getBody();
                final FrameBodyTPOS body = (FrameBodyTPOS) frameBody;
                body.setText("");
            } else if (V2_CODE_TITLE.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_TITLE);
                frameBody = frame.getBody();
                final FrameBodyTIT2 body = (FrameBodyTIT2) frameBody;
                body.setText("");
            } else if (V2_CODE_YEAR.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_YEAR);
                frameBody = frame.getBody();
                final FrameBodyTDRC body = (FrameBodyTDRC) frameBody;
                body.setText("");
            } else if (V2_CODE_COMMENT.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_COMMENT);
                frameBody = frame.getBody();
                final FrameBodyCOMM body = (FrameBodyCOMM) frameBody;
                body.setText("");
            } else if (V2_CODE_GENRE.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_GENRE);
                frameBody = frame.getBody();
                final FrameBodyTCON body = (FrameBodyTCON) frameBody;
                body.setText("");
            } else if (V2_CODE_TRACK.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_TRACK);
                frameBody = frame.getBody();
                final FrameBodyTRCK body = (FrameBodyTRCK) frameBody;
                body.setText("");
            } else if (V2_CODE_ENCODE.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_ENCODE);
                frameBody = frame.getBody();
                final FrameBodyTENC body = (FrameBodyTENC) frameBody;
                body.setText("");
            } else if (V2_CODE_LENGTH.equals(aFrameCode)) {
                frame = new ID3v24Frame(V2_CODE_LENGTH);
                frameBody = frame.getBody();
                final FrameBodyTLEN body = (FrameBodyTLEN) frameBody;
                body.setText("1");
            } else {
                throw new IllegalArgumentException(aFrameCode + " is not a valid Frame Type.");
            }
            if (frame != null) {
                audioFile.getID3v2Tag().setFrame(frame);
            }
        }
        return frame;
    }
}
