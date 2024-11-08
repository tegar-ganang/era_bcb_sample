package com.googlecode.mp4parser;

import com.coremedia.iso.IsoFile;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sannies
 * Date: 3/7/12
 * Time: 12:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class DavidAppend {

    public static void main(String[] args) throws IOException {
        List<Movie> movies = new LinkedList<Movie>();
        movies.add(MovieCreator.build(Channels.newChannel(AppendExample.class.getResourceAsStream("/davidappend/v1.mp4"))));
        movies.add(MovieCreator.build(Channels.newChannel(AppendExample.class.getResourceAsStream("/davidappend/v2.mp4"))));
        movies.add(MovieCreator.build(Channels.newChannel(AppendExample.class.getResourceAsStream("/davidappend/v2.mp4"))));
        List<Track> videoTracks = new LinkedList<Track>();
        List<Track> audioTracks = new LinkedList<Track>();
        for (Movie m : movies) {
            for (Track track : m.getTracks()) {
                if (track.getHandler().equals("vide")) {
                    videoTracks.add(track);
                }
                if (track.getHandler().equals("soun")) {
                    audioTracks.add(track);
                }
            }
        }
        Movie concatMovie = new Movie();
        concatMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        concatMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        IsoFile out2 = new DefaultMp4Builder().build(concatMovie);
        {
            FileChannel fc = new RandomAccessFile(String.format("output.mp4"), "rw").getChannel();
            fc.position(0);
            out2.getBox(fc);
            fc.close();
        }
    }
}
