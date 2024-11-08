package uk.org.toot.audio.tool;

import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.audio.core.SimpleAudioProcess;

/**
 * @author st
 */
public class FormatProcess extends SimpleAudioProcess {

    private FormatControls controls;

    private ChannelFormat format;

    public FormatProcess(FormatControls controls) {
        this.controls = controls;
    }

    public int processAudio(AudioBuffer buffer) {
        ChannelFormat fmt = buffer.getChannelFormat();
        if (format != fmt) {
            format = fmt;
            controls.setFormat(format);
        }
        return AUDIO_OK;
    }
}
