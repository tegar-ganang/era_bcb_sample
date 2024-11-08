package com.googlecode.legendtv.test.drivers.core;

import java.io.File;
import com.googlecode.legendtv.intf.vlc.Audio;
import com.googlecode.legendtv.intf.vlc.Input;
import com.googlecode.legendtv.intf.vlc.Playlist;
import com.googlecode.legendtv.intf.vlc.VLCInstance;
import com.googlecode.legendtv.intf.vlc.Audio.AudioChannel;

public class VLCTest {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) printUsage(); else runTest(args[0]);
    }

    private static void printUsage() {
        System.out.println("Usage: java org.legendtv.test.drivers.core.VLCTest <file to play>");
    }

    private static void testPlaylist(VLCInstance instance, String filename) throws Exception {
        File file = new File(filename);
        Playlist plst = instance.getPlaylist();
        System.out.println("File: " + file.getAbsolutePath() + " (" + file.getName() + ")");
        System.out.println("Attempting to add item to playlist...");
        plst.add("file://" + file.getAbsolutePath(), file.getName());
        plst.play(-1, null);
        while (!plst.isPlaying()) {
            Thread.sleep(100);
        }
    }

    private static void testAudio(VLCInstance instance) throws Exception {
        Audio audio = instance.getAudio();
        AudioChannel currentChannel;
        System.out.print("Muting... ");
        audio.setMuted(true);
        Thread.sleep(3000);
        System.out.println("done.");
        System.out.println();
        System.out.println("Unmuting... ");
        audio.setMuted(false);
        Thread.sleep(3000);
        System.out.println("done.");
        System.out.println();
        System.out.println("Volume is: " + audio.getVolume());
        System.out.print("Setting volume to 150... ");
        audio.setVolume(150);
        System.out.println("done.");
        System.out.println();
        currentChannel = audio.getChannel();
        System.out.println("== AUDIO INFO ==");
        System.out.println("Audio track number: " + audio.getTrack());
        System.out.println("Audio channel info: " + currentChannel);
        System.out.println();
        System.out.print("Setting left channel... ");
        audio.setChannel(AudioChannel.LEFT);
        Thread.sleep(3000);
        System.out.println("done.");
        System.out.println();
        System.out.print("Setting right channel... ");
        audio.setChannel(AudioChannel.RIGHT);
        Thread.sleep(3000);
        System.out.println("done.");
        System.out.println();
        System.out.print("Reverting to original channel... ");
        audio.setChannel(currentChannel);
        Thread.sleep(3000);
        System.out.println("done.");
        System.out.println();
    }

    private static void testInput(VLCInstance instance) throws Exception {
        Input input = instance.getInput();
        Playlist plst = instance.getPlaylist();
        System.out.println("INPUT INFORMATION");
        System.out.println("-----------------");
        System.out.println("Total length   (ms) :\t" + input.getLength());
        System.out.println("Input time     (ms) :\t" + input.getTime());
        while (plst.isPlaying()) {
            System.out.print("Input position [0-1]:\t" + input.getPosition() + "\r");
            Thread.sleep(100);
        }
        System.out.println();
    }

    private static void runTest(String filename) throws Exception {
        VLCInstance instance;
        System.out.println("== Starting VLCTest ==");
        System.out.print("Loading VLC... ");
        instance = new VLCInstance();
        System.out.println("done.");
        System.out.print("Testing playlist functionality... ");
        testPlaylist(instance, filename);
        System.out.println("done.");
        System.out.print("Testing audio functionality... ");
        testAudio(instance);
        System.out.println("done.");
        System.out.print("Testing input functionality... ");
        testInput(instance);
        System.out.println("done.");
    }
}
