package com.googlecode.mp4parser.android;

import android.app.Activity;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Mp4ParserPerformance extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        TextView tv = (TextView) findViewById(R.id.text);
        String text = "";
        File sdCard = Environment.getExternalStorageDirectory();
        try {
            long a = System.currentTimeMillis();
            tv.append("1");
            Movie movie = new MovieCreator().build(new RandomAccessFile(new File(sdCard, "suckerpunch-distantplanet_h1080p.mov").getAbsolutePath(), "r").getChannel());
            tv.append("2");
            Log.v("PERF", "Parsing took " + (System.currentTimeMillis() - a));
            text += "Parsing took " + Long.toString(System.currentTimeMillis() - a) + "\n";
            List<Track> tracks = movie.getTracks();
            movie.setTracks(new LinkedList<Track>());
            double startTime = 35.000;
            double endTime = 145.000;
            boolean timeCorrected = false;
            for (Track track : tracks) {
                if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                    if (timeCorrected) {
                        throw new RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.");
                    }
                    startTime = correctTimeToNextSyncSample(track, startTime);
                    endTime = correctTimeToNextSyncSample(track, endTime);
                    timeCorrected = true;
                }
            }
            for (Track track : tracks) {
                long currentSample = 0;
                double currentTime = 0;
                long startSample = -1;
                long endSample = -1;
                for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
                    TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
                    for (int j = 0; j < entry.getCount(); j++) {
                        if (currentTime <= startTime) {
                            startSample = currentSample;
                        }
                        if (currentTime <= endTime) {
                            endSample = currentSample;
                        } else {
                            break;
                        }
                        currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
                        currentSample++;
                    }
                }
                movie.addTrack(new CroppedTrack(track, startSample, endSample));
            }
            a = System.currentTimeMillis();
            tv.append("3");
            IsoFile mp4 = new DefaultMp4Builder().build(movie);
            Log.v("PERF", "Building took " + (System.currentTimeMillis() - a));
            text += "Building took " + (System.currentTimeMillis() - a) + "\n";
            tv.append("4");
            FileOutputStream fos = new FileOutputStream(new File(sdCard, String.format("output-%f-%f.mp4", startTime, endTime)));
            FileChannel outFC = fos.getChannel();
            a = System.currentTimeMillis();
            mp4.getBox(outFC);
            long fileSize = outFC.size();
            fos.close();
            outFC.close();
            tv.append("5");
            long systemEndTime = System.currentTimeMillis();
            Log.v("PERF", "Writing took " + (systemEndTime - a));
            text += "Writing took " + (systemEndTime - a) + "\n";
            text += "Writing speed " + (fileSize / (systemEndTime - a) / 1000) + " MB/s\n";
            tv.setText(text);
            Debug.stopMethodTracing();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static double correctTimeToNextSyncSample(Track track, double cutHere) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
            for (int j = 0; j < entry.getCount(); j++) {
                if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                    timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
                }
                currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
                currentSample++;
            }
        }
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                return timeOfSyncSample;
            }
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }
}
