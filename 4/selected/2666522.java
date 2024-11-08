package dk.i2m.converge.plugins.actions.transcode;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.MediaToolAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.*;
import com.xuggle.xuggler.*;
import java.awt.image.BufferedImage;

/**
 * Transcodes a video from one format to another.
 *
 * @author Allan Lykke Christensen
 */
public class TranscodeVideo {

    private final String input;

    private final String output;

    private int width;

    private int height;

    private IMediaWriter writer;

    public TranscodeVideo(String input, String output) {
        this.input = input;
        this.output = output;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void encode() {
        IMediaReader mediaReader = ToolFactory.makeReader(input);
        mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
        mediaReader.addListener(new Transcoder());
        writer = ToolFactory.makeWriter(output, mediaReader);
        mediaReader.addListener(writer);
        try {
            while (mediaReader.readPacket() == null) ;
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    class Transcoder extends MediaToolAdapter {

        private IVideoResampler videoResampler = null;

        private IAudioResampler audioResampler = null;

        @Override
        public void onVideoPicture(IVideoPictureEvent event) {
            IVideoPicture pic = event.getPicture();
            if (videoResampler == null) {
                videoResampler = IVideoResampler.make(getWidth(), getHeight(), pic.getPixelType(), pic.getWidth(), pic.getHeight(), pic.getPixelType());
            }
            IVideoPicture out = IVideoPicture.make(pic.getPixelType(), getWidth(), getHeight());
            videoResampler.resample(out, pic);
            IVideoPictureEvent asc = new VideoPictureEvent(event.getSource(), out, event.getStreamIndex());
            super.onVideoPicture(asc);
            out.delete();
        }

        @Override
        public void onAudioSamples(IAudioSamplesEvent event) {
            IAudioSamples samples = event.getAudioSamples();
            if (audioResampler == null) {
                audioResampler = IAudioResampler.make(2, samples.getChannels(), 44100, samples.getSampleRate());
            }
            if (event.getAudioSamples().getNumSamples() > 0) {
                IAudioSamples out = IAudioSamples.make(samples.getNumSamples(), samples.getChannels());
                audioResampler.resample(out, samples, samples.getNumSamples());
                AudioSamplesEvent asc = new AudioSamplesEvent(event.getSource(), out, event.getStreamIndex());
                super.onAudioSamples(asc);
                out.delete();
            }
        }
    }
}
