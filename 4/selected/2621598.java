package uk.org.toot.swingui.audioui.meterui;

import uk.org.toot.audio.meter.MeterControls;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.swingui.controlui.*;

public class KMeterPanel extends CompoundControlPanel {

    private MeterControls controls;

    private static MeterPanelFactory factory = new MeterPanelFactory();

    public KMeterPanel(MeterControls mc, int axis) {
        super(mc, axis, null, factory, true, true);
        controls = mc;
        if (controls.getChannelFormat() != ChannelFormat.STEREO) {
            System.out.println("WARNING: MeterPanel only handling first 2 channels");
        }
    }

    public void dispose() {
        removeAll();
    }

    public void setMeterControls(MeterControls mc) {
        controls = mc;
        recreate(controls);
    }
}
