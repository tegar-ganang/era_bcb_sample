package net.mp3spider.search;

import java.security.NoSuchAlgorithmException;
import java.util.Vector;
import net.mp3spider.utils.Utilidades;

/**
 *
 * @author Esteban Fuentealba
 */
public class ResultSearch extends Mp3SpiderResult {

    private Class idServer;

    private String filename;

    private String artist;

    private String album;

    private String year;

    private int trackNumber;

    private String genre;

    private String title;

    private String comment;

    private String duration;

    private String bitrate;

    private String channels;

    private String audioSample;

    private String urlDownload;

    public ResultSearch() {
    }

    public ResultSearch(String filename, String artist, String album, String year, int trackNumber, String genre, String title, String comment, String duration, String bitrate, String channels, String audioSample) {
        this.filename = filename;
        this.artist = artist;
        this.album = album;
        this.year = year;
        this.trackNumber = trackNumber;
        this.genre = genre;
        this.title = title;
        this.comment = comment;
        this.duration = duration;
        this.bitrate = bitrate;
        this.channels = channels;
        this.audioSample = audioSample;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
    }

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public String getAudioSample() {
        return audioSample;
    }

    public void setAudioSample(String audioSample) {
        this.audioSample = audioSample;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUrlDownload() {
        return urlDownload;
    }

    public void setUrlDownload(String urlDownload) {
        this.urlDownload = urlDownload;
    }

    public Vector<Object> getRow() {
        Vector<Object> vec = new Vector<Object>();
        vec.add(this.getArtist());
        vec.add(this.getTitle());
        vec.add(this);
        return vec;
    }

    @Override
    public String toString() {
        try {
            return Utilidades.MD5(this.getTrackID());
        } catch (NoSuchAlgorithmException ex) {
        }
        return null;
    }

    public Class getIdServer() {
        return idServer;
    }

    public void setIdServer(Class idServer) {
        this.idServer = idServer;
    }
}
