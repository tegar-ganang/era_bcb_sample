package net.sourceforge.texture.boundary;

import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import net.sourceforge.texture.audio.AudioAnalyzerResults;

public class WaveformGraphsContainer extends JPanel {

    private static final long serialVersionUID = 8157382099365328109L;

    private final WaveformGraphsFrame waveformGraphsFrame;

    private final WaveformLevelsContainer waveformLevelsContainer;

    private final WaveformCanvasesContainer waveformCanvasesContainer;

    public WaveformGraphsContainer(WaveformGraphsFrame waveformGraphsFrame) {
        this.waveformGraphsFrame = waveformGraphsFrame;
        this.waveformLevelsContainer = new WaveformLevelsContainer(this);
        this.waveformCanvasesContainer = new WaveformCanvasesContainer(this);
        this.setOpaque(false);
        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        this.add(this.waveformLevelsContainer);
        this.add(this.waveformCanvasesContainer);
    }

    public void prepare(AudioAnalyzerResults audioAnalyzerResults) throws IOException {
        this.waveformLevelsContainer.prepare(audioAnalyzerResults.getFormat().getChannels(), audioAnalyzerResults.getFormat().getEncoding().toString());
        this.waveformCanvasesContainer.prepare(audioAnalyzerResults);
    }

    public void clear() {
        this.waveformLevelsContainer.clear();
        this.waveformCanvasesContainer.clear();
    }

    public void setBinaryScaleFactor(int binaryScaleFactor) {
        this.waveformCanvasesContainer.setBinaryScaleFactor(binaryScaleFactor);
    }

    public void refreshPosition(int position) {
        this.waveformCanvasesContainer.refreshPosition(position);
    }

    public void notifyWaveformCanvasesContainerPrepared(int widthMinimumBinaryScaleFactor) {
        this.waveformGraphsFrame.notifyWaveformGraphsContainerPrepared();
    }

    public void notifyIOException(IOException e) {
        this.waveformGraphsFrame.notifyIOException(e);
    }
}
