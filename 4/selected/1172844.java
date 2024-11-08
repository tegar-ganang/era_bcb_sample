package net.sourceforge.pyrus.mcollection;

import java.io.File;
import net.sourceforge.pyrus.mcollection.api.Album;
import net.sourceforge.pyrus.mcollection.api.Author;
import net.sourceforge.pyrus.mcollection.api.Song;

public class SongImpl implements Song {

    private int id;

    private Album album;

    private Author author;

    private File file;

    private String title;

    private int track;

    private long duration;

    private int channels;

    private int bitrate;

    private boolean bitrateVariable;

    private long size;

    private long lastModified;

    public SongImpl(int id) {
        this.id = id;
    }

    public SongImpl(int id, Album album, Author author, File file, String title, int track, long duration, int channels, int bitrate, boolean bitrateVariable, long size, long lastModified) {
        this.id = id;
        this.album = album;
        this.author = author;
        this.file = file;
        this.title = title;
        this.track = track;
        this.duration = duration;
        this.channels = channels;
        this.bitrate = bitrate;
        this.bitrateVariable = bitrateVariable;
        this.size = size;
        this.lastModified = lastModified;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTrack(int track) {
        this.track = track;
    }

    public void setBitrateVariable(boolean bitrateVariable) {
        this.bitrateVariable = bitrateVariable;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Album getAlbum() {
        return album;
    }

    public Author getAuthor() {
        return author;
    }

    public int getBitrate() {
        return bitrate;
    }

    public int getChannels() {
        return channels;
    }

    public long getDuration() {
        return duration;
    }

    public File getFile() {
        return file;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getSize() {
        return size;
    }

    public String getTitle() {
        return title;
    }

    public int getTrack() {
        return track;
    }

    public boolean isBitrateVariable() {
        return bitrateVariable;
    }

    public String getFormattedDuration() {
        StringBuilder sb = new StringBuilder();
        long s = getDuration() / 1000;
        long m = s / 60;
        s -= m * 60;
        sb.append(m);
        sb.append(":");
        if (s < 10) {
            sb.append("0");
        }
        sb.append(s);
        return sb.toString();
    }
}
