package com.jpianobar.main;

import com.jPianoBar.PandoraSong;
import com.jPianoBar.PandoraSongDownloader;
import com.jPianoBar.PlaylistDownloader;
import com.jpianobar.main.gui.ImagePanel;
import com.jpianobar.main.gui.JPianoBarMainWindow;
import com.jpianobar.main.gui.SongDetailsPane;
import com.jpianobar.main.player.WavPlayer;
import javazoom.jl.converter.Converter;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;
import net.sourceforge.jaad.mp4.api.Track;
import net.sourceforge.jaad.util.wav.WaveFileWriter;
import sun.audio.AudioPlayer;
import javax.swing.*;
import java.io.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: vincent
 * Date: 7/17/11
 * Time: 1:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class PandoraPlayer {

    public enum PlayerState {

        PLAYING, PAUSED, WAITING_FOR_NEXT_SONG
    }

    private SongDetailsPane trackDetailsField;

    private ImagePanel albumArtPanel;

    private PlayerState playerState = PlayerState.PLAYING;

    private File audioStream;

    private Thread nextSongListener;

    private WavPlayer wavPlayer;

    public PandoraPlayer(ImagePanel albumArtPanel, SongDetailsPane trackDetailsField, JProgressBar musicProgressBar) {
        this.albumArtPanel = albumArtPanel;
        this.trackDetailsField = trackDetailsField;
        wavPlayer = new WavPlayer(musicProgressBar);
        nextSongListener = new Thread(new NextSongListener());
        nextSongListener.start();
    }

    public synchronized void playSong(PandoraSong song) throws Exception {
        JPianoBarMainWindow.setStatus("Request to play song " + song.getSongTitle());
        playerState = PlayerState.PLAYING;
        if (Pandora.songsFromStation.size() < 4) {
            JPianoBarMainWindow.setStatus("Fetching new songs for station...");
            Pandora.songsFromStation.addAll(new PlaylistDownloader().getPlaylistForStation(Pandora.selectedStation, Pandora.pandoraAccount, Pandora.pandoraSettings.getDefaultAudio()));
        }
        if (Pandora.currentlyPlayingSong != song) {
            JPianoBarMainWindow.setStatus("Stopping player...");
            if (wavPlayer.isPaused()) {
                wavPlayer.play();
            }
            while (wavPlayer.isAlive()) {
                wavPlayer.stop();
            }
            JPianoBarMainWindow.setStatus("Updating album image...");
            albumArtPanel.updateImage(song.getArtRadio());
            JPianoBarMainWindow.setStatus("Downloading song...");
            byte[] songBytes = new PandoraSongDownloader().downloadSong(song.getAudioURL());
            File songFile = writeByteArrayToFile(songBytes);
            File encodedSongFile = new File("tempSongFileOut.aac");
            encodedSongFile.delete();
            JPianoBarMainWindow.setStatus("Encoding file...");
            if (Pandora.pandoraSettings.getDefaultAudio().equals(PlaylistDownloader.DEFAULT_AUDIO_FORMAT)) {
                decodeMP4(songFile.getAbsolutePath(), encodedSongFile.getAbsolutePath());
            } else {
                decodeMP3(songFile, encodedSongFile);
            }
            audioStream = encodedSongFile;
            Pandora.currentlyPlayingSong = song;
            Pandora.songsFromStation.remove(song);
            Pandora.tracksTable.updateTableForTracks(Pandora.songsFromStation);
        }
        JPianoBarMainWindow.setStatus("Invoking Player...");
        wavPlayer.play();
        JPianoBarMainWindow.setStatus("Updating track info");
        trackDetailsField.updateForTrack(song);
        JPianoBarMainWindow.setStatus("Playing...");
    }

    public void pauseSong() {
        JPianoBarMainWindow.setStatus("Pausing...");
        playerState = PlayerState.PAUSED;
        wavPlayer.pause();
        JPianoBarMainWindow.setStatus("Paused");
    }

    private File writeByteArrayToFile(byte[] songBytes) {
        try {
            File songFile = new File("tempSongFile.aac");
            songFile.delete();
            FileOutputStream outputStream = new FileOutputStream(songFile);
            outputStream.write(songBytes);
            outputStream.flush();
            outputStream.close();
            return songFile;
        } catch (Exception e) {
            return null;
        }
    }

    private void decodeMP3(File songFile, File encodedSongFile) throws Exception {
        Converter mp3Converter = new Converter();
        mp3Converter.convert(songFile.getAbsolutePath(), encodedSongFile.getAbsolutePath());
    }

    private synchronized void decodeMP4(String in, String out) throws Exception {
        WaveFileWriter wav = null;
        try {
            final MP4Container cont = new MP4Container(new RandomAccessFile(in, "r"));
            final Movie movie = cont.getMovie();
            final List<Track> tracks = movie.getTracks(AudioTrack.AudioCodec.AAC);
            if (tracks.isEmpty()) throw new Exception("movie does not contain any AAC track");
            final AudioTrack track = (AudioTrack) tracks.get(0);
            wav = new WaveFileWriter(new File(out), track.getSampleRate(), track.getChannelCount(), track.getSampleSize());
            final Decoder dec = new Decoder(track.getDecoderSpecificInfo());
            Frame frame;
            final SampleBuffer buf = new SampleBuffer();
            while (track.hasMoreFrames()) {
                frame = track.readNextFrame();
                dec.decodeFrame(frame.getData(), buf);
                wav.write(buf.getData());
            }
        } finally {
            if (wav != null) wav.close();
        }
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    public WavPlayer getWavPlayer() {
        return wavPlayer;
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;
    }
}
