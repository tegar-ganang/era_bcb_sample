package uk.org.toot.synth.synths.multi;

import uk.org.toot.control.CompoundControl;
import uk.org.toot.synth.ChannelledSynthControls;
import uk.org.toot.synth.SynthChannelControls;
import static uk.org.toot.synth.id.TootSynthControlsId.MULTI_SYNTH_ID;

public class MultiSynthControls extends ChannelledSynthControls {

    public static final int ID = MULTI_SYNTH_ID;

    public static final String NAME = "MultiSynth";

    public MultiSynthControls() {
        super(ID, NAME);
    }

    public void setChannelControls(int chan, SynthChannelControls c) {
        CompoundControl old = getChannelControls(chan);
        if (old != null) {
            remove(old);
            old.close();
        }
        if (c != null) {
            String name = c.getName();
            if (find(name) != null) {
                disambiguate(c);
                c.setAnnotation(name);
            }
        }
        super.setChannelControls(chan, c);
        setChanged();
        notifyObservers(chan);
    }

    public boolean isPluginParent() {
        return true;
    }
}
