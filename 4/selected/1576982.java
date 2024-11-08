package net.sourceforge.parser.mp4.test;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import javax.swing.tree.TreeNode;
import net.sourceforge.parser.mp4.mp4parser;
import net.sourceforge.parser.mp4.box.AudioSampleEntry;
import net.sourceforge.parser.mp4.box.Box;
import net.sourceforge.parser.mp4.box.ChunkOffsetBox;
import net.sourceforge.parser.mp4.box.HandlerBox;
import net.sourceforge.parser.mp4.box.MediaHeaderBox;
import net.sourceforge.parser.mp4.box.SampleDescriptionBox;
import net.sourceforge.parser.mp4.box.SampleEntry;
import net.sourceforge.parser.mp4.box.SampleSizeBox;
import net.sourceforge.parser.mp4.box.SampleToChunkBox;
import net.sourceforge.parser.mp4.box.SampleToChunkBox.SampleToChunkTableEntry;
import net.sourceforge.parser.mp4.box.TrackBox;
import net.sourceforge.parser.mp4.box.TrackHeaderBox;
import net.sourceforge.parser.mp4.box.VisualSampleEntry;

/**
 * @author aza_sf@yahoo.com
 *
 * @version $Revision: 30 $
 */
public class Mp4ParserWrapper {

    private mp4parser mp4parser;

    private Box box;

    public Mp4ParserWrapper(File file) throws Exception {
        mp4parser = new mp4parser();
        box = (Box) mp4parser.parse(file);
    }

    public mp4parser getParser() {
        return mp4parser;
    }

    public void dispose() {
        try {
            mp4parser.close();
            box = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<TreeNode> getTracks() {
        ArrayList<TreeNode> tracks = new ArrayList<TreeNode>();
        TreeNode moov = findChildByName("moov", box);
        for (int i = 0; i < moov.getChildCount(); i++) {
            TreeNode child = moov.getChildAt(i);
            if ("trak".equals(child.toString())) tracks.add(child);
        }
        return tracks;
    }

    public TreeNode getVideoTrack() {
        ArrayList<TreeNode> tracks = getTracks();
        Iterator<TreeNode> it = tracks.iterator();
        while (it.hasNext()) {
            TreeNode trak = (TreeNode) it.next();
            TreeNode mdia = findChildByName("mdia", trak);
            HandlerBox hdlr = (HandlerBox) findChildByName("hdlr", mdia);
            if (hdlr != null && "vide".equals(hdlr.handler_type)) return trak;
        }
        return null;
    }

    private TreeNode getSoundTrack() {
        ArrayList<TreeNode> tracks = getTracks();
        Iterator<TreeNode> it = tracks.iterator();
        while (it.hasNext()) {
            TreeNode trak = (TreeNode) it.next();
            TreeNode mdia = findChildByName("mdia", trak);
            HandlerBox hdlr = (HandlerBox) findChildByName("hdlr", mdia);
            if (hdlr != null && "soun".equals(hdlr.handler_type)) return trak;
        }
        return null;
    }

    public SampleToChunkTableEntry[] getSampleToChunkTable(TrackBox trak) {
        SampleToChunkBox stsc = (SampleToChunkBox) getTrackTable(trak, "stsc");
        return stsc.table;
    }

    public IntBuffer getChunkOffsetTable(TrackBox trak) {
        ChunkOffsetBox stco = (ChunkOffsetBox) getTrackTable(trak, "stco");
        return stco.chunk_offset;
    }

    public IntBuffer getSampleSizeTable(TrackBox trak) {
        SampleSizeBox stsz = (SampleSizeBox) getTrackTable(trak, "stsz");
        return stsz.table;
    }

    public TreeNode getTrackTable(TreeNode trak, String tableName) {
        TreeNode mdia = findChildByName("mdia", trak);
        if (mdia != null) {
            TreeNode minf = findChildByName("minf", mdia);
            if (minf != null) {
                TreeNode stbl = findChildByName("stbl", minf);
                if (stbl != null) return findChildByName(tableName, stbl);
            }
        }
        return null;
    }

    private String getHandlerType(TreeNode trak) {
        TreeNode mdia = findChildByName("mdia", trak);
        HandlerBox hdlr = (HandlerBox) findChildByName("hdlr", mdia);
        return hdlr.handler_type;
    }

    public int getTrackType(TreeNode trak) {
        String handler_type = getHandlerType(trak);
        if ("vide".equals(handler_type)) {
            return 0;
        } else if ("soun".equals(handler_type)) {
            return 1;
        } else if ("hint".equals(handler_type)) {
            return 2;
        } else if ("meta".equals(handler_type)) {
            return 3;
        }
        return -1;
    }

    public SampleEntry getVisualSampleEntry(int index) {
        TreeNode trak = getVideoTrack();
        SampleDescriptionBox stsd = (SampleDescriptionBox) getTrackTable(trak, "stsd");
        return stsd.sampleEntry[index];
    }

    public SampleEntry getAudioSampleEntry(int index) {
        TreeNode trak = getSoundTrack();
        SampleDescriptionBox stsd = (SampleDescriptionBox) getTrackTable(trak, "stsd");
        return stsd.sampleEntry[index];
    }

    public double getVideoResolutionWidth() {
        VisualSampleEntry vse = (VisualSampleEntry) getVisualSampleEntry(0);
        return vse.width;
    }

    public double getVideoResolutionHeight() {
        VisualSampleEntry vse = (VisualSampleEntry) getVisualSampleEntry(0);
        return vse.height;
    }

    public int getTrackDurationSeconds(TreeNode trak) {
        return (int) getTrackDuration(trak) / getTrackTimescale(trak);
    }

    public int getVideoTrackDuration() {
        return getTrackDurationSeconds(getVideoTrack());
    }

    public int getVideoTrackTimescale() {
        TreeNode mdia = findChildByName("mdia", getVideoTrack());
        MediaHeaderBox mdhd = (MediaHeaderBox) findChildByName("mdhd", mdia);
        return mdhd.timescale;
    }

    private long getTrackDuration(TreeNode trak) {
        TreeNode mdia = findChildByName("mdia", trak);
        MediaHeaderBox mdhd = (MediaHeaderBox) findChildByName("mdhd", mdia);
        return mdhd.duration;
    }

    private int getTrackTimescale(TreeNode trak) {
        TreeNode mdia = findChildByName("mdia", trak);
        MediaHeaderBox mdhd = (MediaHeaderBox) findChildByName("mdhd", mdia);
        return mdhd.timescale;
    }

    public double getTrackResolutionWidth(TreeNode trak) {
        TrackHeaderBox tkhd = (TrackHeaderBox) findChildByName("tkhd", trak);
        return tkhd.width;
    }

    public double getTrackResolutionHeight(TreeNode trak) {
        TrackHeaderBox tkhd = (TrackHeaderBox) findChildByName("tkhd", trak);
        return tkhd.height;
    }

    public int getChannelCount(TreeNode trak) {
        AudioSampleEntry ase = (AudioSampleEntry) getSampleEntry(trak, 0);
        return ase.channelcount;
    }

    public double getAudioSamplerate() {
        AudioSampleEntry ase = (AudioSampleEntry) getSampleEntry(getSoundTrack(), 0);
        return ase.samplerate;
    }

    public double getVideoSamplerate() {
        return 0;
    }

    public SampleEntry getSampleEntry(TreeNode trak, int index) {
        SampleDescriptionBox stsd = (SampleDescriptionBox) getTrackTable(trak, "stsd");
        return stsd.sampleEntry[index];
    }

    public String getVideoCodec() {
        return getSampleEntry(getVideoTrack(), 0).toString();
    }

    public String getAudioCodec() {
        return getSampleEntry(getSoundTrack(), 0).toString();
    }

    private TreeNode findChildByName(String name, TreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            TreeNode child = node.getChildAt(i);
            if (name.equals(child.toString())) return child;
        }
        return null;
    }
}
