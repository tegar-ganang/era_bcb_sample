package com.googlecode.mp4parser;

import com.coremedia.iso.IsoFile;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.builder.FragmentedMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

/**
 *
 */
public class ReadWriteExample {

    public static void main(String[] args) throws IOException {
        Movie video = MovieCreator.build(new FileInputStream("/home/sannies/scm/svn/mp4parser/Solekai022_854_29_640x75_MaxSdSubtitle.uvu").getChannel());
        IsoFile out2 = new DefaultMp4Builder().build(video);
        long starttime2 = System.currentTimeMillis();
        FileChannel fc2 = new RandomAccessFile("output_uvu.mp4", "rw").getChannel();
        fc2.position(0);
        out2.getBox(fc2);
        long size2 = fc2.size();
        fc2.truncate(fc2.position());
        fc2.close();
        System.err.println("Writing " + size2 / 1024 / 1024 + "MB took " + (System.currentTimeMillis() - starttime2));
    }
}
