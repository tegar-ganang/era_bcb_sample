public class Cursor extends Thread {

    private int channelLengthInSamples, position;

    private WaveformPanelContainer container;

    private boolean paused, stopped;

    private Plei plei;

    public Cursor(WaveformPanelContainer container, Plei plei) {
        this.container = container;
        this.plei = plei;
        position = 0;
        channelLengthInSamples = plei.getChannelLength();
        paused = true;
        stopped = false;
    }

    public void run() {
        while (!stopped) {
            if (!paused) {
                container.update((int) Math.round(container.getWidth() * (position / (double) channelLengthInSamples)));
                position = plei.getFramePos();
            }
        }
    }

    public void pause() {
        paused = true;
    }

    public void play() {
        paused = false;
    }

    public void kill() {
        stopped = true;
        container.update(0);
    }
}
