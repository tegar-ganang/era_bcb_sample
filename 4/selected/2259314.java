package be.lassi.lanbox;

import be.lassi.context.ShowContext;
import be.lassi.context.ShowContextListener;
import be.lassi.cues.Cue;
import be.lassi.cues.Cues;
import be.lassi.cues.CuesListener;
import be.lassi.cues.DefaultCuesListener;
import be.lassi.cues.LightCueDetail;
import be.lassi.cues.LightCues;
import be.lassi.lanbox.domain.ChannelChange;
import be.lassi.util.Dmx;

/**
 * Listens to channel changes in the current cue and sends 
 * <code>ChannelChange</code>s to the Lanbox engine.
 */
public class CurrentCueChannelChanges {

    private final ShowContext context;

    private final CuesListener lightCuesListener;

    private final ChannelChangeProcessor channelChangeProcessor;

    /**
     * Constructs a new instance.
     * 
     * @param context the show context
     */
    public CurrentCueChannelChanges(final ShowContext context) {
        this(context, context.getLanbox().getEngine());
    }

    /**
     * Constructs a new instance.
     * 
     * @param context the show context
     * @param channelChangeProcessor destination for change change messages
     */
    public CurrentCueChannelChanges(final ShowContext context, final ChannelChangeProcessor channelChangeProcessor) {
        this.context = context;
        this.channelChangeProcessor = channelChangeProcessor;
        lightCuesListener = new LightCuesListener();
        getLightCues().addListener(lightCuesListener);
        context.addShowContextListener(new MyShowContextListener());
    }

    private class MyShowContextListener implements ShowContextListener {

        public void postShowChange() {
            getLightCues().addListener(lightCuesListener);
        }

        public void preShowChange() {
            getLightCues().removeListener(lightCuesListener);
        }
    }

    private class LightCuesListener extends DefaultCuesListener {

        public void channelLevelChanged(final int cueIndex, final int channelIndex) {
            Cues cues = context.getShow().getCues();
            int current = cues.getCurrentIndex();
            if (cueIndex == current) {
                Cue cueDefinition = cues.getCurrentCue();
                if (cueDefinition.isLightCue()) {
                    LightCues lightCues = cues.getLightCues();
                    int lightCueIndex = cueDefinition.getLightCueIndex();
                    LightCueDetail detail = lightCues.getDetail(lightCueIndex);
                    float value = detail.getChannelLevel(channelIndex).getValue();
                    int dmxValue = Dmx.getDmxValue(value);
                    channelChangeProcessor.change(Lanbox.ENGINE_SHEET, new ChannelChange(channelIndex + 1, dmxValue));
                }
            }
        }
    }

    private LightCues getLightCues() {
        return context.getShow().getCues().getLightCues();
    }
}
