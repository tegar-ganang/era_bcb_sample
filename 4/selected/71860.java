package jokeboxjunior.core.processing.audio.entagged;

import cb_commonobjects.logging.GlobalLog;
import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileIO;
import entagged.audioformats.exceptions.CannotReadException;
import java.io.File;
import java.util.Map;
import jokeboxjunior.core.data.analyse.audio.AudioFileAnalyser.AudioFileAttribs;
import jokeboxjunior.core.processing.audio.AbstractAudioAnalysisProcessor;

/**
 *
 * @author B1
 */
public class EntaggedAudioformatsAnalysisProcessor extends AbstractAudioAnalysisProcessor {

    @Override
    public boolean analyseFile(String thisFile) {
        boolean myReturnVal = false;
        try {
            AudioFile myAudioFile = AudioFileIO.read(new File(thisFile));
            this.put(AudioFileAttribs.AUDIO_BITRATE, myAudioFile.getBitrate());
            this.put(AudioFileAttribs.AUDIO_LENGTH_MS, new Integer(Math.round(myAudioFile.getPreciseLength() * 1000)));
            this.put(AudioFileAttribs.AUDIO_SAMPLING_RATE, myAudioFile.getSamplingRate());
            this.put(AudioFileAttribs.AUDIO_CHANNELS, myAudioFile.getChannelNumber());
            this.put(AudioFileAttribs.AUDIO_IS_VBR, myAudioFile.isVbr());
            myReturnVal = true;
        } catch (CannotReadException ex) {
            GlobalLog.logError(ex);
        }
        return myReturnVal;
    }

    @Override
    public boolean postAnalyseFile(Map<AudioFileAttribs, Object> thisReadAttribs) {
        return true;
    }

    public EntaggedAudioformatsAnalysisProcessorConfig getNewConfigObj() {
        return new EntaggedAudioformatsAnalysisProcessorConfig();
    }
}
