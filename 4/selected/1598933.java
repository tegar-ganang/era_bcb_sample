package org.gstreamer.lowlevel;

import org.gstreamer.interfaces.Mixer;
import org.gstreamer.interfaces.MixerTrack;
import org.gstreamer.lowlevel.GlibAPI.GList;
import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface GstMixerAPI extends Library {

    GstMixerAPI GSTMIXER_API = GstNative.load("gstinterfaces", GstMixerAPI.class);

    GType gst_mixer_get_type();

    GType gst_mixer_track_get_type();

    GList gst_mixer_list_tracks(Mixer mixer);

    void gst_mixer_set_volume(Mixer mixer, MixerTrack track, int[] volumes);

    void gst_mixer_get_volume(Mixer mixer, MixerTrack track, int[] volumes);

    void gst_mixer_set_mute(Mixer mixer, MixerTrack track, boolean mute);

    void gst_mixer_set_record(Mixer mixer, MixerTrack track, boolean record);

    void gst_mixer_mute_toggled(Mixer mixer, MixerTrack track, boolean mute);

    void gst_mixer_record_toggled(Mixer mixer, MixerTrack track, boolean record);

    void gst_mixer_volume_changed(Mixer mixer, MixerTrack track, int[] volumes);

    void gst_mixer_mixer_changed(Mixer mixer);

    int gst_mixer_get_mixer_flags(Mixer mixer);

    public static final class MixerTrackStruct extends com.sun.jna.Structure {

        public volatile GObjectAPI.GObjectStruct parent;

        public volatile String label;

        public volatile int flags;

        public volatile int num_channels;

        public volatile int min_volume;

        public volatile int max_volume;

        public int getChannelCount() {
            return (Integer) readField("num_channels");
        }

        public int getMinimumVolume() {
            return (Integer) readField("min_volume");
        }

        public int getMaximumVolume() {
            return (Integer) readField("max_volume");
        }

        public int getFlags() {
            return (Integer) readField("flags");
        }

        public void read() {
        }

        public void write() {
        }

        public MixerTrackStruct(Pointer ptr) {
            useMemory(ptr);
        }
    }
}
